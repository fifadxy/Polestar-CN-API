package io.github.polestarcn.companion;
import android.content.Context;
import android.os.Bundle;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
/** Explicit, bounded root operations for this device's CarLink package only. No global policy changes. */
final class OppoCarLinkRoot {
  static File report(Context c){return new File(c.getFilesDir(),"oppo-carlink-root-contract.txt");}
  static String quote(String s){if(s==null||s.indexOf('\0')>=0||s.indexOf('\n')>=0||s.indexOf('\r')>=0)throw new IllegalArgumentException("Unsafe argument");return "'"+s.replace("'","'\\''")+"'";}
  static String launchCommand(Bundle route){String pkg=route.getString("bindActivityPackage",""),name=route.getString("bindActivityName","");
    if(!OppoOfficialCarLink.PACKAGE.equals(pkg)||!name.matches("[A-Za-z0-9_.$]{1,240}"))throw new IllegalArgumentException("Unverified CarLink component");
    return "am start -W --user current -a android.intent.action.VIEW -d "+quote(OppoOfficialCarLink.BIND_URI.toString())+" -n "+quote(pkg+"/"+name);
  }
  static String open(Context c)throws Exception {Bundle route=OppoOfficialCarLink.status(c);if(!route.getBoolean("bindRouteAvailable"))return "未发现官方绑定Activity，未尝试root启动";return "Root入口测试：\n"+run(launchCommand(route))+"\n启动成功只表示页面已打开；车辆资料、SDK鉴权和云端车型配置仍需独立验证。";}
  static String run(String command)throws Exception{
    Process p=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    Thread drain=new Thread(()->{try(InputStream in=p.getInputStream()){byte[] block=new byte[1024];int n;while((n=in.read(block))!=-1){int left=4096-bytes.size();if(left>0)bytes.write(block,0,Math.min(left,n));}}catch(IOException ignored){}},"carlink-root-output");drain.start();
    boolean done=p.waitFor(15,TimeUnit.SECONDS);if(!done)p.destroyForcibly();drain.join(1000);
    if(!done)throw new IOException("Root operation timed out");if(p.exitValue()!=0)throw new IOException("Root exit="+p.exitValue());return bytes.toString("UTF-8");
  }
  static String capture(Context c){File temporary=new File(c.getCacheDir(),"carlink-contract-source.apk");StringBuilder text=new StringBuilder("CarLink root static contract v1\nNo user database, tokens, VIN, method bodies or live logs.\n");
    try {
      android.content.pm.ApplicationInfo app=c.getPackageManager().getApplicationInfo(OppoOfficialCarLink.PACKAGE,0);
      String source=app.sourceDir;
      if(source==null||!source.endsWith(".apk")||!(source.startsWith("/data/app/")||source.startsWith("/system/")||source.startsWith("/product/")||source.startsWith("/system_ext/")||source.startsWith("/vendor/")||source.startsWith("/my_")))throw new IOException("Unrecognized installed APK path");
      // Copy only the installed code APK into app-private cache, never /data/data or another user.
      try(FileOutputStream owned=new FileOutputStream(temporary)){} // App-created destination retains the correct SELinux label.
      int uid=android.os.Process.myUid();run("cp -- "+quote(source)+" "+quote(temporary.getAbsolutePath())+" && chown "+uid+":"+uid+" "+quote(temporary.getAbsolutePath())+" && chmod 600 "+quote(temporary.getAbsolutePath()));
      if(temporary.length()>100_000_000L)throw new IOException("APK size limit");
      android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(OppoOfficialCarLink.PACKAGE,0);
      text.append("apkScope=base-only; splitCount=").append(app.splitSourceDirs==null?0:app.splitSourceDirs.length).append("\n").append("hostVersion=").append(info.versionName).append("\n").append(OppoOfficialCarLink.describe(c)).append('\n');
      text.append(CarLinkDexContract.scan(temporary));
    }catch(Exception denied){text.append("captureError=").append(denied.getClass().getSimpleName()).append("; root may be denied or host not visible.\n");}
    finally{temporary.delete();}
    String result=text.substring(0,Math.min(text.length(),180_000));
    try{Files.write(report(c).toPath(),result.getBytes(StandardCharsets.UTF_8));}catch(IOException failed){return "静态契约保存失败："+failed.getClass().getSimpleName();}
    return "CarLink定向采集完成（"+result.getBytes(StandardCharsets.UTF_8).length+"字节）；临时APK已删除。\n"+(result.contains("captureError=")?"部分采集失败，原因已写入资料包。":"仅导出类名/方法签名，不包含完整APK。") ;
  }
}
