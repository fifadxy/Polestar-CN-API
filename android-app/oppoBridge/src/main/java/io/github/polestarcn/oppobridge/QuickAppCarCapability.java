package io.github.polestarcn.oppobridge;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Static exact-build capability detection for dormant MyDevices 17.4.15 CAR QuickApp routes. */
final class QuickAppCarCapability {
  static final String HOST_VERSION="17.4.15";
  static final String RPK_ASSET="assets/com.heytap.mydevices.quickapp.kdRpk.rpk";
  static final String INDEX_ENTRY="cards/SingleDeviceCard4x2/index.js";
  static final int INDEX_SIZE=1_405_170;
  static final int MAX_INDEX_SIZE=4*1024*1024;
  static final String INDEX_SHA256="69bdfacad2cd7ad7a65da678b9435eaa54fa7011debb7381164e1d699cfb86e2";
  static final String PATCH_ANCHOR="'sportData', 'healthData'];";
  private static final String PATCH_REPLACEMENT="'sportData', 'healthData',\n"
      + "// PsAssistant exact-gated CAR controls\n"
      + "'chesuo', 'kongtiao', 'shanguang', 'mingdi'];";

  static final class Result {
    final String hostVersion;
    final boolean rpkPresent;
    final boolean carRegistry;
    final boolean smartHomeDefaultCar;
    final boolean eventSummaryCar;
    final boolean exactPatchSupported;
    final String indexSha256;
    final String reason;

    Result(String hostVersion,boolean rpkPresent,boolean carRegistry,
        boolean smartHomeDefaultCar,boolean eventSummaryCar,boolean exactPatchSupported,
        String indexSha256,String reason) {
      this.hostVersion=hostVersion;
      this.rpkPresent=rpkPresent;
      this.carRegistry=carRegistry;
      this.smartHomeDefaultCar=smartHomeDefaultCar;
      this.eventSummaryCar=eventSummaryCar;
      this.exactPatchSupported=exactPatchSupported;
      this.indexSha256=indexSha256;
      this.reason=reason;
    }

    Result(String hostVersion,boolean rpkPresent,boolean carRegistry,
        boolean smartHomeDefaultCar,boolean eventSummaryCar,String reason) {
      this(hostVersion,rpkPresent,carRegistry,smartHomeDefaultCar,eventSummaryCar,false,"",reason);
    }

    boolean routeSafe() {
      // Newer hosts may expose the CAR path natively. Unknown builds are accepted only when the
      // inspected RPK itself proves that all four CAR event IDs are present. Memory patching stays
      // restricted to the legacy exact SHA below.
      return rpkPresent && carRegistry && (eventSummaryCar || exactPatchSupported);
    }

    String mode() {
      if(!routeSafe())return "fallback-native-home";
      return eventSummaryCar ? "event-summary-oem-shortcuts"
          : exactPatchSupported ? "exact-rpk-memory-patch" : "fallback-native-home";
    }
  }

  private static volatile Result cached;
  private QuickAppCarCapability() {}

  static Result detect(Context context) {
    Result current=cached;
    if(current!=null)return current;
    synchronized(QuickAppCarCapability.class) {
      if(cached!=null)return cached;
      cached=scan(context);
      return cached;
    }
  }

  static void clearForReload() { cached=null; }

  private static Result scan(Context context) {
    String version="";
    try {
      version=context.getPackageManager().getPackageInfo("com.heytap.mydevices",0).versionName;
      ApplicationInfo info=context.getPackageManager().getApplicationInfo("com.heytap.mydevices",0);
      try(ZipFile apk=new ZipFile(info.sourceDir)) {
        ZipEntry rpk=apk.getEntry(RPK_ASSET);
        if(rpk==null)return new Result(version,false,false,false,false,false,"","rpk-missing");
        byte[] index=null;String indexEntry="";
        try(ZipInputStream nested=new ZipInputStream(apk.getInputStream(rpk))) {
          ZipEntry entry;
          while((entry=nested.getNextEntry())!=null) {
            String name=entry.getName();
            if(!entry.isDirectory()&&(INDEX_ENTRY.equals(name)||name.endsWith("/SingleDeviceCard4x2/index.js"))) {
              indexEntry=name;index=readBounded(nested,MAX_INDEX_SIZE+1);break;
            }
          }
        }
        if(index==null)return new Result(version,true,false,false,false,false,"","4x2-index-missing");
        if(index.length>MAX_INDEX_SIZE)return new Result(version,true,false,false,false,false,"","4x2-index-too-large");
        String text=new String(index,StandardCharsets.UTF_8);
        boolean registry=hasAny(text,"carEventIconName","kongtiao","chesuo","shanguang","mingdi");
        boolean smart=hasCarDefaults(text),event=hasCarEvents(text);
        String sha=sha256(index);
        boolean exact=HOST_VERSION.equals(version)&&INDEX_ENTRY.equals(indexEntry)
            &&INDEX_SIZE==index.length&&INDEX_SHA256.equals(sha)
            &&occurrences(text,PATCH_ANCHOR)==1&&!event;
        String reason="entry="+indexEntry+" indexSize="+index.length+" sha="+sha+" registry="+registry
            +" smartHomeDefault="+smart+" eventSummary="+event+" exactPatch="+exact;
        return new Result(version,true,registry,smart,event,exact,sha,reason);
      }
    } catch(Throwable failure) {
      return new Result(version,false,false,false,false,false,"",
          "scan-"+failure.getClass().getSimpleName());
    }
  }

  static byte[] patchExact(byte[] source) {
    if(source==null||source.length!=INDEX_SIZE||!INDEX_SHA256.equals(sha256(source)))return null;
    String text=new String(source,StandardCharsets.UTF_8);
    String patched=bindCarClick(patchVerifiedText(text));
    if(patched==null)return null;
    return patched.getBytes(StandardCharsets.UTF_8);
  }

  /** A future host can be patched only if every CAR JS anchor is unique and the result validates.
   *  A loose "contains CAR" match is explicitly insufficient. */
  static byte[] patchFeature(byte[] source){
    if(source==null||source.length<200||source.length>MAX_INDEX_SIZE)return null;
    String text=new String(source,StandardCharsets.UTF_8);
    if(!java.util.Arrays.equals(source,text.getBytes(StandardCharsets.UTF_8)))return null;
    if(occurrences(text,PATCH_ANCHOR)!=1
        ||occurrences(text,"carEventIconName: _config.carEventIconName,")!=1
        ||occurrences(text,"clickEvent(this.deviceInfo.id, item.id, Number(!item.state));")!=1
        ||hasCarEvents(text)
        ||text.contains("var clickEvent = __webpack_require__(\"./src/cards/public/api.js\").clickEvent;"))return null;
    String patched=bindCarClick(patchVerifiedText(text));
    if(patched==null||!hasCarEvents(patched)||patched.length()-text.length()>1024)return null;
    return patched.getBytes(StandardCharsets.UTF_8);
  }

  static String patchVerifiedText(String text) {
    if(occurrences(text,PATCH_ANCHOR)!=1||hasCarEvents(text))return null;
    String patched=text.replace(PATCH_ANCHOR,PATCH_REPLACEMENT);
    return hasCarEvents(patched)?patched:null;
  }

  // The verified 17.4.15 CAR module calls a free clickEvent without importing public/api.js.
  // Resolve it inside that one module; do not rewrite other devices' controls.
  static String bindCarClick(String text) {
    if(text==null)return null;
    String marker="carEventIconName: _config.carEventIconName,";
    if(occurrences(text,marker)!=1)return null;
    int car=text.indexOf(marker),start=text.lastIndexOf("var _default = exports.default = {",car);
    if(start<0)return null;
    int call=text.indexOf("clickEvent(this.deviceInfo.id, item.id, Number(!item.state));",car);
    int next=text.indexOf("/***/ }),",car);
    if(call<0||next<0||call>next)return null;
    String binding="var clickEvent = __webpack_require__(\"./src/cards/public/api.js\").clickEvent;\n";
    return text.substring(0,start)+binding+text.substring(start);
  }

  static int occurrences(String text,String needle) {
    if(text==null||needle==null||needle.isEmpty())return 0;
    int count=0,at=0;
    while((at=text.indexOf(needle,at))>=0){count++;at+=needle.length();}
    return count;
  }

  static String sha256(byte[] bytes) {
    try {
      byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder out=new StringBuilder(64);
      for(byte value:digest)out.append(String.format(Locale.US,"%02x",value&255));
      return out.toString();
    } catch(Exception impossible) {throw new IllegalStateException(impossible);}
  }

  static boolean hasCarDefaults(String text) {
    String object=initializer(text,"smartHomeDefault",'{','}');
    java.util.regex.Matcher key=java.util.regex.Pattern.compile("(?:\\{|,)\\s*(?:CAR|[\"']CAR[\"'])\\s*:").matcher(object);
    if(!key.find())return false;
    return allCarIds(initializer("const controls = "+object.substring(key.end()),"controls",'[',']'));
  }
  static boolean hasCarEvents(String text) {
    return allCarIds(initializer(text,"eventSummary",'[',']'));
  }
  private static boolean allCarIds(String text) {
    for(String id:new String[]{"chesuo","kongtiao","mingdi","shanguang"})
      if(!java.util.regex.Pattern.compile("[\"']"+id+"[\"']").matcher(text).find())return false;
    return true;
  }
  // Skip strings and comments while balancing; imports/export declarations alone are not initializers.
  static String initializer(String text,String name,char open,char close) {
    if(text==null)return "";
    java.util.regex.Matcher match=java.util.regex.Pattern.compile("\\bconst\\s+"+name+"\\s*=[^;\\n]*?"+java.util.regex.Pattern.quote(String.valueOf(open))).matcher(text);
    if(!match.find())return "";
    int start=match.end()-1,depth=0;char quote=0;boolean line=false,block=false,escape=false;
    StringBuilder out=new StringBuilder();
    for(int i=start;i<text.length();i++) {
      char c=text.charAt(i),next=i+1<text.length()?text.charAt(i+1):0;
      if(line){if(c=='\n')line=false;continue;}
      if(block){if(c=='*'&&next=='/'){block=false;i++;}continue;}
      if(quote!=0){out.append(c);if(escape)escape=false;else if(c=='\\')escape=true;else if(c==quote)quote=0;continue;}
      if(c=='/'&&next=='/'){line=true;i++;continue;}
      if(c=='/'&&next=='*'){block=true;i++;continue;}
      if(c=='\"'||c=='\''){quote=c;out.append(c);continue;}
      out.append(c);if(c==open)depth++;else if(c==close&&--depth==0)return out.toString();
    }
    return "";
  }

  private static boolean hasAny(String text,String... values) {
    if(text==null)return false;
    for(String value:values)if(text.contains(value))return true;
    return false;
  }

  private static byte[] readBounded(InputStream in,int max) throws Exception {
    ByteArrayOutputStream out=new ByteArrayOutputStream(Math.min(max,64*1024));
    byte[] block=new byte[8192];
    int total=0,read;
    while((read=in.read(block))!=-1&&total<max) {
      int count=Math.min(read,max-total);
      if(count>0)out.write(block,0,count);
      total+=count;
    }
    return out.toByteArray();
  }
}
