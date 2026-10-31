package io.github.polestarcn.companion;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.*;

/** Explicit local export of public OEM code and allowlisted environment facts only. */
final class OppoHostApkExport {
  interface Progress { void update(String message); }
  static final String[] APK_PACKAGES={"com.coloros.assistantscreen","com.heytap.mydevices"};
  private OppoHostApkExport() {}
  static final class Result {
    final int apkFiles; final boolean complete;
    Result(int apkFiles,boolean complete){this.apkFiles=apkFiles;this.complete=complete;}
  }
  static Result write(Context context,OutputStream output,Progress progress) throws Exception {
    int copied=0;boolean complete=true;
    JSONObject report=new JSONObject();report.put("schemaVersion",2);
    report.put("generatedAt",System.currentTimeMillis());
    report.put("environment",environment(context));
    report.put("module",module(context));
    JSONArray packages=new JSONArray();report.put("packages",packages);
    try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(output))) {
      zip.setLevel(0); // APKs are compressed already; avoid recompression work.
      for(String pkg:new String[]{APK_PACKAGES[0],APK_PACKAGES[1],"com.android.launcher","com.oplus.pantanal.ums","com.nearme.instant.platform"}) {
        boolean includeApk=pkg.equals(APK_PACKAGES[0])||pkg.equals(APK_PACKAGES[1]);
        progress.update("收集 "+pkg+(includeApk?" 安装文件":" 版本与组件信息"));
        JSONObject item=new JSONObject().put("packageName",pkg).put("apkRequested",includeApk);
        packages.put(item);
        PackageInfo info;
        try {info=context.getPackageManager().getPackageInfo(pkg,PackageManager.GET_ACTIVITIES|PackageManager.GET_SERVICES|
            PackageManager.GET_RECEIVERS|PackageManager.GET_PROVIDERS|PackageManager.GET_PERMISSIONS|
            (Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES));}
        catch(PackageManager.NameNotFoundException|SecurityException missing) {
          item.put("status","not-installed-or-not-visible");if(includeApk)complete=false;continue;
        }
        item.put("versionName",info.versionName).put("versionCode",Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode);
        item.put("components",components(info));
        item.put("requestedPermissions",info.requestedPermissions==null?new JSONArray():new JSONArray(java.util.Arrays.asList(info.requestedPermissions)));
        try {
          Signature[] certs=Build.VERSION.SDK_INT>=28&&info.signingInfo!=null?info.signingInfo.getApkContentsSigners():info.signatures;
          JSONArray hashes=new JSONArray();if(certs!=null)for(Signature cert:certs)hashes.put(hex(MessageDigest.getInstance("SHA-256").digest(cert.toByteArray())));
          item.put("signerSha256",hashes);
        } catch(RuntimeException unavailable) {item.put("signerStatus","unavailable");}
        item.put("status","metadata-only");
        if(!includeApk)continue;
        ApplicationInfo app=info.applicationInfo;
        JSONArray files=new JSONArray();item.put("files",files);
        boolean packageComplete=true;
        if(app==null||app.sourceDir==null) {item.put("status","apk-source-unavailable");complete=false;continue;}
        JSONObject base=copy(zip,app.sourceDir,pkg+"/base.apk");files.put(base);
        if(base.getBoolean("copied"))copied++;else packageComplete=false;
        if(app.splitSourceDirs!=null)for(int i=0;i<app.splitSourceDirs.length;i++) {
          JSONObject split=copy(zip,app.splitSourceDirs[i],pkg+"/split-"+i+".apk");files.put(split);
          if(split.getBoolean("copied"))copied++;else packageComplete=false;
        }
        item.put("status",packageComplete?"apk-complete":"apk-incomplete");complete&=packageComplete;
      }
      report.put("requiredApksComplete",complete).put("copiedApkFiles",copied);
      entry(zip,"report.json",report.toString(2));
      entry(zip,"README.txt","PsAssistant OPPO 负一屏适配资料包\n\n"+
          "包含：负一屏与设备空间 base/split APK、公开组件声明、权限名、签名摘要、系统构建信息、模块加载历史。\n"+
          "不包含：账号、Token、VIN、车辆状态或缓存、操作日志、系统日志、设备序列号、Android ID、应用私有目录。\n"+
          "先检查 report.json 的 requiredApksComplete 和每个文件 copied 字段。缺失不代表系统未安装，也可能是系统限制读取。\n"+
          "模块历史记录不证明当前仍启用或车辆控制成功。分享文件由用户选择接收应用。\n");
    }
    return new Result(copied,complete);
  }
  private static JSONObject environment(Context c) throws JSONException {
    DisplayMetrics m=c.getResources().getDisplayMetrics();
    return new JSONObject().put("appVersion",BuildConfig.VERSION_NAME).put("appVersionCode",BuildConfig.VERSION_CODE)
        .put("manufacturer",Build.MANUFACTURER).put("model",Build.MODEL).put("device",Build.DEVICE)
        .put("sdk",Build.VERSION.SDK_INT).put("android",Build.VERSION.RELEASE).put("securityPatch",Build.VERSION.SECURITY_PATCH)
        .put("buildDisplay",Build.DISPLAY).put("buildFingerprint",Build.FINGERPRINT)
        .put("abis",new JSONArray(java.util.Arrays.asList(Build.SUPPORTED_ABIS)))
        .put("widthPixels",m.widthPixels).put("heightPixels",m.heightPixels).put("densityDpi",m.densityDpi)
        .put("fontScale",c.getResources().getConfiguration().fontScale).put("uiMode",c.getResources().getConfiguration().uiMode);
  }
  private static JSONObject module(Context c) throws JSONException {
    JSONObject out=new JSONObject().put("integrationEnabled",OppoDeviceIntegration.enabled(c)).put("historicalOnly",true);
    Bundle state=OppoHostDiagnostics.status(c);
    for(String prefix:new String[]{"main_","cards_"}) {
      out.put(prefix+"seenAt",state.getLong(prefix+"seenAt")).put(prefix+"ready",state.getBoolean(prefix+"ready"));
      for(String key:new String[]{"phase","hostVersion","moduleVersion"})out.put(prefix+key,state.getString(prefix+key,""));
    }
    return out;
  }
  private static JSONArray components(PackageInfo info) throws JSONException {
    JSONArray out=new JSONArray();
    for(ComponentInfo[] group:new ComponentInfo[][]{info.activities,info.receivers,info.services,info.providers}) {
      if(group==null)continue;
      for(ComponentInfo component:group) {
        JSONObject item=new JSONObject().put("name",component.name).put("process",component.processName)
            .put("enabled",component.enabled).put("exported",component.exported);
        if(component instanceof ProviderInfo) {
          ProviderInfo p=(ProviderInfo)component;item.put("kind","provider").put("authority",p.authority)
              .put("readPermission",p.readPermission).put("writePermission",p.writePermission);
        } else if(component instanceof ServiceInfo) item.put("kind","service").put("permission",((ServiceInfo)component).permission);
        else item.put("kind",group==info.receivers?"receiver":"activity").put("permission",((ActivityInfo)component).permission);
        out.put(item);
      }
    }
    return out;
  }
  private static JSONObject copy(ZipOutputStream zip,String source,String name) throws Exception {
    JSONObject info=new JSONObject().put("entry",name).put("copied",false);
    InputStream input;
    try{input=new BufferedInputStream(new FileInputStream(source));}
    catch(IOException|SecurityException denied){return info.put("error",denied.getClass().getSimpleName());}
    MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;boolean readable=true;
    try(InputStream in=input) {
      zip.putNextEntry(new ZipEntry(name));byte[] bytes=new byte[32768];
      while(true) {
        int count;
        try{count=in.read(bytes);}catch(IOException failed){readable=false;info.put("error","source-read-failed");break;}
        if(count<0)break;
        zip.write(bytes,0,count);digest.update(bytes,0,count);size+=count;
      }
      zip.closeEntry();
    }
    info.put("bytes",size).put("copied",readable);
    if(readable)info.put("sha256",hex(digest.digest()));
    return info;
  }
  private static void entry(ZipOutputStream zip,String name,String value) throws IOException {
    zip.putNextEntry(new ZipEntry(name));zip.write(value.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
  }
  private static String hex(byte[] bytes) {
    StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
  }
}
