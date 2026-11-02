package io.github.polestarcn.oppobridge;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Static exact-build capability detection for dormant MyDevices 17.4.15 CAR QuickApp routes. */
final class QuickAppCarCapability {
  static final class Result {
    final String hostVersion;
    final boolean rpkPresent;
    final boolean carRegistry;
    final boolean smartHomeDefaultCar;
    final boolean eventSummaryCar;
    final String reason;

    Result(String hostVersion,boolean rpkPresent,boolean carRegistry,
        boolean smartHomeDefaultCar,boolean eventSummaryCar,String reason) {
      this.hostVersion=hostVersion;
      this.rpkPresent=rpkPresent;
      this.carRegistry=carRegistry;
      this.smartHomeDefaultCar=smartHomeDefaultCar;
      this.eventSummaryCar=eventSummaryCar;
      this.reason=reason;
    }

    boolean routeSafe() {
      return "17.4.15".equals(hostVersion) && rpkPresent && carRegistry
          && (smartHomeDefaultCar || eventSummaryCar);
    }

    String mode() {
      if(!routeSafe())return "fallback-native-home";
      return eventSummaryCar ? "event-summary-oem-shortcuts" : "smart-home-default";
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
      if(!"17.4.15".equals(version))
        return new Result(version,false,false,false,false,"host-version-mismatch");
      ApplicationInfo info=context.getPackageManager().getApplicationInfo("com.heytap.mydevices",0);
      try(ZipFile apk=new ZipFile(info.sourceDir)) {
        ZipEntry rpk=apk.getEntry("assets/com.heytap.mydevices.quickapp.kdRpk.rpk");
        if(rpk==null)return new Result(version,false,false,false,false,"rpk-missing");
        boolean registry=false,smart=false,event=false;
        try(ZipInputStream nested=new ZipInputStream(apk.getInputStream(rpk))) {
          ZipEntry entry;
          int scanned=0;
          while((entry=nested.getNextEntry())!=null&&scanned<240) {
            if(entry.isDirectory())continue;
            String name=entry.getName()==null?"":entry.getName();
            String lower=name.toLowerCase(Locale.ROOT);
            if(!(lower.endsWith(".js")||lower.endsWith(".json")||lower.endsWith(".ux")))continue;
            byte[] data=readBounded(nested,900_000);
            String text=new String(data,java.nio.charset.StandardCharsets.UTF_8);
            scanned++;
            if(hasAny(text,"carEventIconName","kongtiao","chesuo","shanguang","mingdi"))
              registry=true;
            if(hasCarDefaults(text))
              smart=true;
            if(hasCarEvents(text))
              event=true;
            if(registry&&smart&&event)break;
          }
        }
        String reason=(registry?"registry ":"")
            +(smart?"smartHomeDefault ":"")
            +(event?"eventSummary ":"");
        if(reason.isEmpty())reason="car-route-not-confirmed";
        return new Result(version,true,registry,smart,event,reason.trim());
      }
    } catch(Throwable failure) {
      return new Result(version,false,false,false,false,
          "scan-"+failure.getClass().getSimpleName());
    }
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
