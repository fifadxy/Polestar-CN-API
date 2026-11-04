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
  static String panelCommand(Bundle route){return launchCommand(route)+" --ez psassistant.local_panel true";}
  static String openPanel(Context c)throws Exception {
    Bundle route=OppoOfficialCarLink.status(c);
    if(!route.getBoolean("bindRouteAvailable",false))return "未发现 CarLink 入口";
    String output=run(panelCommand(route));
    return "Root 启动结果：\n"+output+"\n请返回本页核对 CarLink 宿主面板时间；启动结果不等于 IOT 权限已授予。";
  }
  static String open(Context c)throws Exception {Bundle route=OppoOfficialCarLink.status(c);if(!route.getBoolean("bindRouteAvailable"))return "未发现官方绑定Activity，未尝试root启动";return "Root入口测试：\n"+run(launchCommand(route))+"\n启动成功只表示页面已打开；车辆资料、SDK鉴权和云端车型配置仍需独立验证。";}
  static String run(String command)throws Exception{
    Process p=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    Thread drain=new Thread(()->{try(InputStream in=p.getInputStream()){byte[] block=new byte[1024];int n;while((n=in.read(block))!=-1){int left=4096-bytes.size();if(left>0)bytes.write(block,0,Math.min(left,n));}}catch(IOException ignored){}},"carlink-root-output");drain.start();
    boolean done=p.waitFor(15,TimeUnit.SECONDS);if(!done)p.destroyForcibly();drain.join(1000);
    if(!done)throw new IOException("Root operation timed out");if(p.exitValue()!=0)throw new IOException("Root exit="+p.exitValue());return bytes.toString("UTF-8");
  }
  static String capture(Context c){File temporary=new File(c.getCacheDir(),"carlink-contract-source.apk");StringBuilder text=new StringBuilder("CarLink root static contract v2\nNo user database, tokens, VIN, method bodies or live logs.\n");
    try {
      android.content.pm.ApplicationInfo app=c.getPackageManager().getApplicationInfo(OppoOfficialCarLink.PACKAGE,0);
      android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(OppoOfficialCarLink.PACKAGE,0);
      java.util.ArrayList<String> sources=new java.util.ArrayList<>();sources.add(app.sourceDir);
      if(app.splitSourceDirs!=null)for(String split:app.splitSourceDirs)if(sources.size()<17)sources.add(split);
      text.append("apkScope=base+splits; splitCount=").append(Math.max(0,sources.size()-1))
          .append("\nhostVersion=").append(info.versionName).append("\n").append(OppoOfficialCarLink.describe(c)).append('\n');
      int uid=android.os.Process.myUid(),scanned=0;
      for(String source:sources) {
        if(!installedCodePath(source)) {text.append("apk[").append(scanned).append("] skipped=unrecognized-path\n");continue;}
        temporary.delete();
        // Copy one installed code APK at a time into app-private cache. No user database is read.
        try(FileOutputStream owned=new FileOutputStream(temporary)){}
        run("cp -- "+quote(source)+" "+quote(temporary.getAbsolutePath())+" && chown "+uid+":"+uid+" "+quote(temporary.getAbsolutePath())+" && chmod 600 "+quote(temporary.getAbsolutePath()));
        if(temporary.length()<=0||temporary.length()>100_000_000L)throw new IOException("APK size limit");
        text.append("\n-- codeApk[").append(scanned).append("] ")
            .append(source.equals(app.sourceDir)?"base":new File(source).getName().replaceAll("[^A-Za-z0-9._-]","_"))
            .append(" bytes=").append(temporary.length()).append(" --\n");
        text.append(CarLinkDexContract.scan(temporary));scanned++;
        temporary.delete();
        if(text.length()>170_000){text.append("\nscanTruncated=true\n");break;}
      }
      text.append("codeApksScanned=").append(scanned).append('\n');
    }catch(Exception denied){text.append("captureError=").append(denied.getClass().getSimpleName()).append("; root may be denied or host not visible.\n");}
    finally{temporary.delete();}
    String result=text.substring(0,Math.min(text.length(),180_000));
    try{Files.write(report(c).toPath(),result.getBytes(StandardCharsets.UTF_8));}catch(IOException failed){return "静态契约保存失败："+failed.getClass().getSimpleName();}
    return "CarLink定向采集完成（"+result.getBytes(StandardCharsets.UTF_8).length+"字节）；临时APK已删除。\n"+(result.contains("captureError=")?"部分采集失败，原因已写入资料包。":"仅导出类名/方法签名，不包含完整APK。") ;
  }
  static boolean installedCodePath(String source){return source!=null&&source.endsWith(".apk")
      &&(source.startsWith("/data/app/")||source.startsWith("/system/")||source.startsWith("/product/")
      ||source.startsWith("/system_ext/")||source.startsWith("/vendor/")||source.startsWith("/my_"));}
}
