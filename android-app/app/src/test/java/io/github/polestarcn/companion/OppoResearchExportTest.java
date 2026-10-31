package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class OppoResearchExportTest {
  private final Context context=RuntimeEnvironment.getApplication();
  private PackageInfo install(String name,boolean readable) throws Exception {
    PackageInfo info=new PackageInfo();info.packageName=name;info.versionName="17.test";info.versionCode=17;
    info.applicationInfo=new ApplicationInfo();info.applicationInfo.packageName=name;
    File file=new File(context.getCacheDir(),name+".apk");
    if(readable)try(FileOutputStream out=new FileOutputStream(file)){out.write(new byte[]{1,2,3,4,5});}
    else file.delete();
    info.applicationInfo.sourceDir=file.getAbsolutePath();
    shadowOf(context.getPackageManager()).installPackage(info);return info;
  }
  private Map<String,byte[]> entries(byte[] bytes) throws Exception {
    Map<String,byte[]> entries=new HashMap<>();
    try(ZipInputStream input=new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry entry;while((entry=input.getNextEntry())!=null) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int count;
        while((count=input.read(buffer))!=-1)out.write(buffer,0,count);
        entries.put(entry.getName(),out.toByteArray());
      }
    }
    return entries;
  }
  @Test public void archivePreservesApkBytesAndReportsMissingHostWithoutAborting() throws Exception {
    install("com.coloros.assistantscreen",false);install("com.heytap.mydevices",true);
    ByteArrayOutputStream output=new ByteArrayOutputStream();
    OppoHostApkExport.Result result=OppoHostApkExport.write(context,output,message->{});
    Map<String,byte[]> files=entries(output.toByteArray());
    assertFalse(result.complete);assertEquals(1,result.apkFiles);
    assertArrayEquals(new byte[]{1,2,3,4,5},files.get("com.heytap.mydevices/base.apk"));
    assertFalse(files.containsKey("com.coloros.assistantscreen/base.apk"));
    JSONObject report=new JSONObject(new String(files.get("report.json"),"UTF-8"));
    assertFalse(report.getBoolean("requiredApksComplete"));
    JSONArray hosts=report.getJSONArray("packages");
    assertEquals("apk-incomplete",hosts.getJSONObject(0).getString("status"));
    assertEquals("apk-complete",hosts.getJSONObject(1).getString("status"));
    assertEquals(64,hosts.getJSONObject(1).getJSONArray("files").getJSONObject(0).getString("sha256").length());
  }
  @Test public void completeArchiveIncludesSplitsAndExcludesPrivatePreferences() throws Exception {
    PackageInfo one=install("com.coloros.assistantscreen",true);install("com.heytap.mydevices",true);
    File split=new File(context.getCacheDir(),"split.apk");
    try(FileOutputStream output=new FileOutputStream(split)){output.write(99);}
    one.applicationInfo.splitSourceDirs=new String[]{split.getAbsolutePath()};
    shadowOf(context.getPackageManager()).installPackage(one);
    context.getSharedPreferences("account",0).edit().putString("token","DO_NOT_EXPORT_PRIVATE_TOKEN").commit();
    ByteArrayOutputStream output=new ByteArrayOutputStream();
    OppoHostApkExport.Result result=OppoHostApkExport.write(context,output,message->{});
    assertTrue(result.complete);assertEquals(3,result.apkFiles);
    Map<String,byte[]> files=entries(output.toByteArray());
    assertArrayEquals(new byte[]{99},files.get("com.coloros.assistantscreen/split-0.apk"));
    for(byte[] data:files.values())assertFalse(new String(data,"UTF-8").contains("DO_NOT_EXPORT_PRIVATE_TOKEN"));
  }
  @Test public void failedDestinationDoesNotReturnSuccessfulReport() throws Exception {
    try {
      OppoHostApkExport.write(context,new OutputStream(){@Override public void write(int value)throws IOException{throw new IOException("full");}},message->{});
      fail("Destination failure must propagate");
    } catch(IOException expected) { }
  }
  @Test public void sharingGrantsReadOnlyAccessToOneContentUri() {
    Uri uri=Uri.parse("content://io.github.polestarcn.companion.oppoexports/oppo_research/research.zip");
    Intent intent=OppoWidgetSetupActivity.shareIntent(uri);
    assertEquals(Intent.ACTION_SEND,intent.getAction());assertEquals("application/zip",intent.getType());
    assertEquals(uri,intent.getParcelableExtra(Intent.EXTRA_STREAM));
    assertEquals(uri,intent.getClipData().getItemAt(0).getUri());
    assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
    assertEquals(0,intent.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  }
  @Test public void fileProviderCannotShareOutsideDedicatedExportDirectory() throws Exception {
    File outside=new File(context.getFilesDir(),"private-session.txt");
    try {
      androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".oppoexports",outside);
      fail("Private app data must be outside the export root");
    } catch(IllegalArgumentException expected) { }
    File inside=new File(context.getCacheDir(),"oppo-research/research.zip");
    Uri uri=androidx.core.content.FileProvider.getUriForFile(context,context.getPackageName()+".oppoexports",inside);
    assertEquals("content",uri.getScheme());
  }
  @Test public void lightCaptureSkipsLargeDexAndApksAndIncludesBoundedManifest() throws Exception {
    PackageInfo info=install("com.coloros.assistantscreen",true);install("com.heytap.mydevices",false);
    try(ZipOutputStream apk=new ZipOutputStream(new FileOutputStream(info.applicationInfo.sourceDir))) {
      apk.putNextEntry(new ZipEntry("AndroidManifest.xml"));apk.write(new byte[]{8,9});apk.closeEntry();
      apk.putNextEntry(new ZipEntry("classes.dex"));apk.write(new byte[2_100_000]);apk.closeEntry();
    }
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    OppoLightResearchExport.write(context,out,message->{});
    assertTrue(out.size()<2_000_000);
    Map<String,byte[]> files=entries(out.toByteArray());
    assertArrayEquals(new byte[]{8,9},files.get("com.coloros.assistantscreen/AndroidManifest.xml"));
    for(String name:files.keySet()){assertFalse(name.endsWith(".apk"));assertFalse(name.endsWith(".dex"));}
    JSONObject report=new JSONObject(new String(files.get("report.json"),"UTF-8"));
    assertEquals("light",report.getString("mode"));assertFalse(report.getBoolean("fullApksIncluded"));
    assertTrue(report.has("watchData"));assertEquals(2_000_000,report.getInt("archiveLimitBytes"));
  }
  @Test public void oversizedStaticAssetIsSkippedAndRecorded() throws Exception {
    PackageInfo info=install("com.coloros.assistantscreen",true);
    try(ZipOutputStream apk=new ZipOutputStream(new FileOutputStream(info.applicationInfo.sourceDir))) {
      apk.putNextEntry(new ZipEntry("AndroidManifest.xml"));apk.write(new byte[131073]);apk.closeEntry();
    }
    ByteArrayOutputStream out=new ByteArrayOutputStream();OppoLightResearchExport.write(context,out,message->{});
    Map<String,byte[]> files=entries(out.toByteArray());assertFalse(files.containsKey("com.coloros.assistantscreen/AndroidManifest.xml"));
    assertTrue(new String(files.get("report.json"),"UTF-8").contains("size-limit"));
  }
  @Test public void vehicleRoutesPrecedeGenericCardComponents() {
    assertEquals(0,OppoLightResearchExport.componentPriority("com.oplus.assistantscreen.card.car.SearchCarProvider"));
    assertEquals(0,OppoLightResearchExport.componentPriority("com.oplus.mydevices.remotecards.provider.DeviceLiteCardProvider"));
    assertEquals(1,OppoLightResearchExport.componentPriority("com.oplus.assistantscreen.card.weather.WeatherProvider"));
    assertEquals(2,OppoLightResearchExport.componentPriority("com.example.SettingsActivity"));
  }
  @Test public void compressedOutputHasAnEnforcedUploadLimit() throws Exception {
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();OppoLightResearchExport.CappedOutput out=new OppoLightResearchExport.CappedOutput(bytes);
    out.write(new byte[OppoLightResearchExport.MAX_BYTES]);
    try{out.write(1);fail("Must reject byte above budget");}catch(IOException expected){}
    assertEquals(OppoLightResearchExport.MAX_BYTES,bytes.size());
  }
}
