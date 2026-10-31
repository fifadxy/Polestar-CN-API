package io.github.polestarcn.companion;

import android.content.Context;
import android.content.pm.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Small first-pass capture: metadata and bounded public static assets, never full APKs or DEX. */
final class OppoLightResearchExport {
  static final int MAX_BYTES=2_000_000;
  private OppoLightResearchExport() {}
  static final class CappedOutput extends FilterOutputStream {
    private int count;
    CappedOutput(OutputStream output){super(output);}
    private void reserve(int length)throws IOException {
      if(length>MAX_BYTES-count)throw new IOException("Light export size limit");count+=length;
    }
    @Override public void write(int value)throws IOException{reserve(1);out.write(value);}
    @Override public void write(byte[] b,int off,int len)throws IOException{reserve(len);out.write(b,off,len);}
  }
  static OppoHostApkExport.Result write(Context context,OutputStream output,OppoHostApkExport.Progress progress)throws Exception {
    JSONObject report=new JSONObject().put("schemaVersion",3).put("mode","light")
        .put("archiveLimitBytes",MAX_BYTES).put("fullApksIncluded",false)
        .put("generatedAt",System.currentTimeMillis()).put("environment",OppoHostApkExport.environment(context))
        .put("module",OppoHostApkExport.module(context))
        .put("watchData","Phone capture cannot read watch firmware or prove endurance-mode support");
    JSONArray packages=new JSONArray(),assets=new JSONArray();report.put("packages",packages).put("staticAssets",assets);
    boolean criticalComplete=true;
    try(ZipOutputStream zip=new ZipOutputStream(new CappedOutput(output))) {
      zip.setLevel(6);
      for(String pkg:new String[]{"com.coloros.assistantscreen","com.heytap.mydevices","com.heytap.health",
          "com.heytap.speechassist","com.oplus.ocar","com.android.launcher","com.oplus.pantanal.ums","com.nearme.instant.platform"}) {
        progress.update("轻量收集 "+pkg);
        JSONObject item=new JSONObject().put("packageName",pkg);packages.put(item);
        PackageInfo info;
        try {info=context.getPackageManager().getPackageInfo(pkg,PackageManager.GET_ACTIVITIES|PackageManager.GET_SERVICES|
            PackageManager.GET_RECEIVERS|PackageManager.GET_PROVIDERS|PackageManager.GET_PERMISSIONS);}
        catch(PackageManager.NameNotFoundException|SecurityException missing){
          item.put("status","not-installed-or-not-visible");
          if(pkg.equals("com.coloros.assistantscreen")||pkg.equals("com.heytap.mydevices"))criticalComplete=false;
          continue;
        }
        item.put("status","metadata-only").put("versionName",info.versionName)
            .put("versionCode",android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode);
        JSONArray all=OppoHostApkExport.components(info),selected=new JSONArray();
        for(int pass=0;pass<2;pass++)for(int i=0;i<all.length()&&selected.length()<128;i++) {
          JSONObject c=all.getJSONObject(i);String name=c.optString("name","").toLowerCase(Locale.ROOT);
          boolean relevant=name.matches(".*(vehicle|carcontrol|mydevices|card|agent|bridge|wear|watch|remote|speech|bluetooth).*");
          if(relevant==(pass==0))selected.put(c);
        }
        item.put("componentCount",all.length()).put("components",selected).put("componentsTruncated",selected.length()<all.length());
        JSONArray permissions=new JSONArray();if(info.requestedPermissions!=null)
          for(int i=0;i<Math.min(128,info.requestedPermissions.length);i++)permissions.put(info.requestedPermissions[i]);
        item.put("requestedPermissions",permissions);
        ApplicationInfo app=info.applicationInfo;
        if(app!=null&&app.sourceDir!=null) {
          item.put("baseApkBytes",new File(app.sourceDir).length()).put("splitCount",app.splitSourceDirs==null?0:app.splitSourceDirs.length);
          if(pkg.equals("com.coloros.assistantscreen")||pkg.equals("com.heytap.mydevices"))
            staticAssets(zip,app.sourceDir,pkg,assets);
        }
      }
      report.put("criticalMetadataAvailable",criticalComplete);
      byte[] json=report.toString(2).getBytes(StandardCharsets.UTF_8);
      if(json.length>1_000_000) {
        for(int i=0;i<packages.length();i++)packages.getJSONObject(i).remove("components");
        report.put("componentDetailsOmittedForSize",true);json=report.toString(2).getBytes(StandardCharsets.UTF_8);
      }
      if(json.length>1_000_000)throw new IOException("Metadata size limit");
      put(zip,"report.json",json);
      put(zip,"README.txt",("轻量适配资料包，最大 2 MB。\\n不包含完整 APK、DEX 方法体、账号、Token、VIN、车辆状态、蓝牙地址或系统日志。\\n"+
          "包含版本、公开组件信息、少量静态 Manifest/设备配置。缺失和截断会标明；这不是完整反编译资料。\\n"+
          "无法从手机读取手表 RTOS 固件；手表需另行运行只读 ADB 采集脚本。\\n").replace("\\n","\n").getBytes(StandardCharsets.UTF_8));
    }
    return new OppoHostApkExport.Result(0,criticalComplete);
  }
  private static void staticAssets(ZipOutputStream output,String source,String pkg,JSONArray report)throws Exception {
    ZipFile apk;
    try {apk=new ZipFile(source);}catch(IOException|SecurityException failure){report.put(new JSONObject().put("package",pkg).put("status","source-unreadable"));return;}
    try(ZipFile input=apk) {
      for(String name:new String[]{"AndroidManifest.xml","assets/support/device_app_config.json"}) {
        JSONObject info=new JSONObject().put("package",pkg).put("sourceEntry",name);report.put(info);
        ZipEntry entry=input.getEntry(name);int limit=name.equals("AndroidManifest.xml")?131072:65536;
        if(entry==null){info.put("status","missing");continue;}
        if(entry.getSize()<0||entry.getSize()>limit){info.put("status","size-limit");continue;}
        byte[] bytes;
        try(InputStream stream=input.getInputStream(entry)) {
          ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] block=new byte[8192];int count;
          while((count=stream.read(block))!=-1){if(buffer.size()+count>limit)throw new IOException("Asset limit");buffer.write(block,0,count);}
          bytes=buffer.toByteArray();
        }catch(IOException|SecurityException failure){info.put("status","read-failed");continue;}
        String target=pkg+"/"+name;put(output,target,bytes);
        StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(Locale.ROOT,"%02x",b&255));
        info.put("status","copied").put("entry",target).put("bytes",bytes.length).put("sha256",hash.toString());
      }
    }
  }
  private static void put(ZipOutputStream output,String name,byte[] bytes)throws IOException {
    output.putNextEntry(new ZipEntry(name));output.write(bytes);output.closeEntry();
  }
}
