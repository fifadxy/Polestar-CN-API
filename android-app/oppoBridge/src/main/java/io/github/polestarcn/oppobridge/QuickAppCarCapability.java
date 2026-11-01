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
            if(windowHas(text,"smartHomeDefault","CAR","kongtiao","chesuo","shanguang","mingdi"))
              smart=true;
            if(windowHas(text,"eventSummary","CAR","kongtiao","chesuo","shanguang","mingdi"))
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

  private static boolean windowHas(String text,String anchor,String... tokens) {
    if(text==null||text.isEmpty())return false;
    int from=0;
    while(true) {
      int at=text.indexOf(anchor,from);
      if(at<0)return false;
      int start=Math.max(0,at-1800);
      int end=Math.min(text.length(),at+12000);
      String window=text.substring(start,end);
      boolean hasCar=window.contains("CAR")||window.contains("Car");
      boolean hasId=false;
      for(String token:tokens)
        if(!"CAR".equals(token)&&window.contains(token)) {hasId=true;break;}
      if(hasCar&&hasId)return true;
      from=at+anchor.length();
    }
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
