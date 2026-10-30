package io.github.polestarcn.companion;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Exports only installed OEM executable files. Never opens app-private data directories. */
final class OppoHostApkExport {
  private OppoHostApkExport() {}
  static void write(Context context,Uri destination) throws Exception {
    OutputStream output=context.getContentResolver().openOutputStream(destination,"wt");
    if(output==null)throw new IOException("No output stream");
    try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(output))) {
      zip.setLevel(0); // APK payloads are already compressed; avoid recompression CPU work.
      StringBuilder manifest=new StringBuilder("OEM executable files only; no app data or account/session information.\n");
      for(String pkg:new String[]{"com.coloros.assistantscreen","com.heytap.mydevices"}) {
        PackageInfo info=context.getPackageManager().getPackageInfo(pkg,0);
        ApplicationInfo app=info.applicationInfo;
        if(app==null||app.sourceDir==null)throw new IOException("APK unavailable");
        manifest.append(pkg).append(" version=").append(info.versionName).append('\n');
        copy(zip,app.sourceDir,pkg+"/base.apk",manifest);
        if(app.splitSourceDirs!=null)for(int i=0;i<app.splitSourceDirs.length;i++)
          copy(zip,app.splitSourceDirs[i],pkg+"/split-"+i+".apk",manifest);
      }
      zip.putNextEntry(new ZipEntry("versions-and-sha256.txt"));
      zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }
  }
  private static void copy(ZipOutputStream zip,String source,String name,StringBuilder manifest) throws Exception {
    MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;
    zip.putNextEntry(new ZipEntry(name));
    try(InputStream input=new BufferedInputStream(new FileInputStream(source))) {
      byte[] bytes=new byte[32768];int count;
      while((count=input.read(bytes))!=-1){zip.write(bytes,0,count);digest.update(bytes,0,count);size+=count;}
    }
    zip.closeEntry();StringBuilder hex=new StringBuilder();
    for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
    manifest.append(name).append(" bytes=").append(size).append(" sha256=").append(hex).append('\n');
  }
}
