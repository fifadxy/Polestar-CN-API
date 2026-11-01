package io.github.polestarcn.oppobridge;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Builds a bounded, credential-free research bundle for exact ColorOS host analysis. */
final class OppoResearchExport {
  private static final long MAX_OUTPUT_BYTES = 9_000_000L;
  private static final long MAX_RAW_BYTES = 4_500_000L;
  private static final int MAX_TEXT_ENTRY = 700_000;
  private static final String[] PACKAGES = {
      "io.github.polestarcn.companion",
      "com.heytap.mydevices",
      "com.coloros.assistantscreen",
      "com.oplus.pantanal.ums",
      "com.nearme.instant.platform",
      "com.heytap.speechassist",
      "com.oplus.ocar",
      "com.coloros.sceneservice",
      "com.oplus.sceneservice"
  };

  private OppoResearchExport() {}

  static File create(Context context) throws Exception {
    File dir = new File(context.getCacheDir(), "oppo-research");
    if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create export directory");
    File[] old = dir.listFiles();
    if (old != null) for (File file : old) if (file.getName().endsWith(".zip")) file.delete();

    String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    File out = new File(dir, "PsAssistant-OPPO-light-" + stamp + ".zip");
    long[] rawBudget = {0L};

    try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
      putText(zip, rawBudget, "README.txt",
          "PsAssistant OPPO lightweight research bundle\n"
          + "Credential-free by design: no phone/password/token/VIN/raw account payload.\n"
          + "Target: MyDevices/AssistantScreen/Pantanal/SceneService integration.\n");
      putText(zip, rawBudget, "system.txt", systemSummary(context));
      putText(zip, rawBudget, "bridge-status.txt", bridgeSummary(context));
      putText(zip, rawBudget, "packages.txt", packageSummary(context));
      extractMyDevicesAssets(context, zip, rawBudget);
      extractAssistantScreenAssets(context, zip, rawBudget);
      putText(zip, rawBudget, "scene-provider.txt", sceneProviderSummary(context));
      String hookLog = rootHookLog();
      if (!hookLog.isEmpty()) putText(zip, rawBudget, "root-hook-log.txt", hookLog);
    }
    if (out.length() > MAX_OUTPUT_BYTES) {
      out.delete();
      throw new IOException("Export exceeded 9 MB safety limit");
    }
    return out;
  }

  private static String systemSummary(Context context) {
    StringBuilder out = new StringBuilder();
    out.append("generatedAt=").append(new Date()).append('\n');
    out.append("manufacturer=").append(Build.MANUFACTURER).append('\n');
    out.append("brand=").append(Build.BRAND).append('\n');
    out.append("model=").append(Build.MODEL).append('\n');
    out.append("device=").append(Build.DEVICE).append('\n');
    out.append("android=").append(Build.VERSION.RELEASE).append('\n');
    out.append("sdk=").append(Build.VERSION.SDK_INT).append('\n');
    out.append("fingerprint=").append(Build.FINGERPRINT).append('\n');
    out.append("bridgePackage=").append(context.getPackageName()).append('\n');
    return out.toString();
  }

  private static String bridgeSummary(Context context) {
    StringBuilder out = new StringBuilder();
    for (String method : new String[]{"capabilities","snapshot","host-status","quickapp-runtime-status","assistant-host-status","shelf-status","account-bind-status","discovery-baseline-diff"}) {
      out.append("[").append(method).append("]\n");
      try { appendBundle(out, BridgeClient.call(context, method, null)); }
      catch (Throwable failure) { out.append("error=").append(failure.getClass().getSimpleName()).append('\n'); }
      out.append('\n');
    }
    return out.toString();
  }

  private static void appendBundle(StringBuilder out, Bundle bundle) {
    if (bundle == null) { out.append("null\n"); return; }
    TreeSet<String> keys = new TreeSet<>(bundle.keySet());
    for (String key : keys) {
      String lower = key.toLowerCase(Locale.ROOT);
      if (lower.contains("token") || lower.contains("password") || lower.contains("vin")
          || lower.contains("accountid") || lower.contains("url")) continue;
      Object value = bundle.get(key);
      if (value instanceof String[] && ((String[]) value).length < 64)
        out.append(key).append('=').append(Arrays.toString((String[]) value)).append('\n');
      else if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean)
        out.append(key).append('=').append(String.valueOf(value)).append('\n');
    }
  }

  @SuppressWarnings("deprecation")
  private static String packageSummary(Context context) {
    StringBuilder out = new StringBuilder();
    PackageManager pm = context.getPackageManager();
    for (String pkg : PACKAGES) {
      out.append("[").append(pkg).append("]\n");
      try {
        PackageInfo info = pm.getPackageInfo(pkg,
            PackageManager.GET_PROVIDERS | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        out.append("versionName=").append(info.versionName).append('\n');
        if (Build.VERSION.SDK_INT >= 28) out.append("versionCode=").append(info.getLongVersionCode()).append('\n');
        ApplicationInfo app = info.applicationInfo;
        if (app != null) {
          File apk = new File(app.sourceDir);
          out.append("apkBytes=").append(apk.length()).append('\n');
          out.append("apkSha256=").append(sha256(apk)).append('\n');
        }
        List<String> interesting = new ArrayList<>();
        if (info.activities != null) for (android.content.pm.ActivityInfo item : info.activities)
          addInteresting(interesting, item.name + " exported=" + item.exported
              + " permission=" + String.valueOf(item.permission));
        if (info.providers != null) for (android.content.pm.ProviderInfo item : info.providers)
          addInteresting(interesting, item.name + " authority=" + item.authority
              + " exported=" + item.exported
              + " readPermission=" + String.valueOf(item.readPermission)
              + " writePermission=" + String.valueOf(item.writePermission));
        if (info.services != null) for (android.content.pm.ServiceInfo item : info.services)
          addInteresting(interesting, item.name + " exported=" + item.exported
              + " permission=" + String.valueOf(item.permission));
        for (String item : interesting) out.append("component=").append(item).append('\n');
      } catch (Throwable missing) {
        out.append("missing=true\n");
      }
      out.append('\n');
    }
    return out.toString();
  }

  private static void addInteresting(List<String> out, String value) {
    if (value == null || out.size() >= 160) return;
    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.contains("car") || lower.contains("vehicle") || lower.contains("shelf")
        || lower.contains("scene") || lower.contains("account") || lower.contains("pantanal")
        || lower.contains("card") || lower.contains("quickapp") || lower.contains("device"))
      out.add(value.replace('\n',' ').replace('\r',' '));
  }

  private static String rootHookLog() {
    Process process = null;
    try {
      process = new ProcessBuilder("su", "-c", "logcat -d -v threadtime -t 2500")
          .redirectErrorStream(true).start();
      StringBuilder out = new StringBuilder();
      try (java.io.BufferedReader reader = new java.io.BufferedReader(
          new java.io.InputStreamReader(process.getInputStream(),
              java.nio.charset.StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null && out.length() < 320_000) {
          String lower = line.toLowerCase(Locale.ROOT);
          boolean relevant = lower.contains("psoppobridge")
              || lower.contains("psvehiclecard")
              || lower.contains("mydevices_vehicle")
              || lower.contains("quickapp_")
              || lower.contains("shelf_pantanal")
              || lower.contains("scene_account")
              || lower.contains("assistant_host_report")
              || lower.contains("native_home_card");
          if (!relevant) continue;
          if (lower.contains("authorization") || lower.contains("access_token")
              || lower.contains("refresh_token") || lower.contains("password")
              || lower.contains("cookie") || lower.contains("vin=")
              || lower.contains("vin:")) continue;
          out.append(line.replace('\r', ' ')).append('\n');
        }
      }
      boolean done = process.waitFor(6, java.util.concurrent.TimeUnit.SECONDS);
      if (!done) process.destroyForcibly();
      if (out.length() == 0) return "";
      return "# Root-filtered OPPO Bridge logcat; sensitive credential patterns are excluded.\n"
          + out;
    } catch (Throwable unavailable) {
      if (process != null) process.destroyForcibly();
      return "";
    }
  }

  private static String sceneProviderSummary(Context context) {
    StringBuilder out=new StringBuilder();
    out.append("accountAuthority=com.oplus.sceneservice.AccountSDKInitProvider\n");
    try {
      android.content.pm.ProviderInfo owner=context.getPackageManager().resolveContentProvider(
          "com.oplus.sceneservice.AccountSDKInitProvider",0);
      if(owner==null) out.append("owner=unresolved\n");
      else {
        out.append("ownerPackage=").append(owner.packageName).append('\n');
        out.append("providerClass=").append(owner.name).append('\n');
        out.append("exported=").append(owner.exported).append('\n');
        out.append("readPermission=").append(String.valueOf(owner.readPermission)).append('\n');
        out.append("writePermission=").append(String.valueOf(owner.writePermission)).append('\n');
      }
    } catch(Throwable failure) {
      out.append("resolveError=").append(failure.getClass().getSimpleName()).append('\n');
    }
    return out.toString();
  }

  private static boolean hasAny(String text,String... values) {
    if(text==null)return false;
    for(String value:values)if(text.contains(value))return true;
    return false;
  }

  private static boolean eventSummaryHasCar(String text) {
    if(text==null)return false;
    int start=text.indexOf("eventSummary");
    if(start<0)return false;
    int end=Math.min(text.length(),start+5000);
    String window=text.substring(start,end);
    return hasAny(window,"kongtiao","chesuo","shanguang","mingdi");
  }

  private static void extractAssistantScreenAssets(
      Context context, ZipOutputStream out, long[] rawBudget) {
    try {
      ApplicationInfo app = context.getPackageManager()
          .getApplicationInfo("com.coloros.assistantscreen", 0);
      try (ZipFile apk = new ZipFile(app.sourceDir)) {
        String[] entries = {
            "assets/preset/card_config_local_config.json",
            "assets/local_export_oppo_service_info_list_json.json",
            "assets/local_export_oneplus_service_info_list_json.json",
            "assets/local_guaranteed_service_info_list_json.json",
            "assets/serverconfig/preset/card_cloud_config.json"
        };
        for (String entry : entries) {
          String leaf = entry.substring(entry.lastIndexOf('/') + 1);
          copyEntry(apk, entry, out, rawBudget,
              "assistantscreen/" + leaf, MAX_TEXT_ENTRY);
        }
      }
    } catch (Throwable ignored) {
      // Exact assets differ by firmware; package metadata and runtime probes remain useful.
    }
  }

  private static void extractMyDevicesAssets(Context context, ZipOutputStream out, long[] rawBudget) {
    try {
      ApplicationInfo app = context.getPackageManager().getApplicationInfo("com.heytap.mydevices", 0);
      StringBuilder analysis=new StringBuilder();
      try (ZipFile apk = new ZipFile(app.sourceDir)) {
        ZipEntry shortcutEntry=apk.getEntry("assets/support/card_short_cut.json");
        if(shortcutEntry!=null) {
          byte[] shortcutBytes;
          try(InputStream in=apk.getInputStream(shortcutEntry)) { shortcutBytes=readBounded(in,MAX_TEXT_ENTRY); }
          putBytes(out,rawBudget,"mydevices/card_short_cut.json",shortcutBytes);
          String shortcutText=new String(shortcutBytes,java.nio.charset.StandardCharsets.UTF_8);
          analysis.append("javaShortcutWhitelistHasCarEvents=")
              .append(hasAny(shortcutText,"kongtiao","chesuo","shanguang","mingdi")).append('\n');
        }
        ZipEntry rpk = apk.getEntry("assets/com.heytap.mydevices.quickapp.kdRpk.rpk");
        if (rpk != null && rpk.getSize() > 0 && rpk.getSize() < 8_000_000L) {
          try (ZipInputStream nested = new ZipInputStream(new BufferedInputStream(apk.getInputStream(rpk)))) {
            ZipEntry entry;
            while ((entry = nested.getNextEntry()) != null) {
              String name = entry.getName();
              if (name == null) continue;
              boolean target = name.endsWith("cards/SingleDeviceCard2x2/index.js")
                  || name.endsWith("cards/SingleDeviceCard4x2/index.js")
                  || name.endsWith("manifest.json") || name.endsWith("app.json");
              if (!target) continue;
              byte[] data = readBounded(nested, MAX_TEXT_ENTRY);
              putBytes(out, rawBudget, "mydevices/rpk/" + sanitizePath(name), data);
              String text=new String(data,java.nio.charset.StandardCharsets.UTF_8);
              if(name.endsWith("SingleDeviceCard2x2/index.js")) {
                analysis.append("rpk2x2DefinesCarIcons=")
                    .append(hasAny(text,"carEventIconName","kongtiao","chesuo","shanguang","mingdi")).append('\n');
                analysis.append("rpk2x2EventSummaryHasCarEvents=")
                    .append(eventSummaryHasCar(text)).append('\n');
              } else if(name.endsWith("SingleDeviceCard4x2/index.js")) {
                analysis.append("rpk4x2DefinesCarIcons=")
                    .append(hasAny(text,"carEventIconName","kongtiao","chesuo","shanguang","mingdi")).append('\n');
                analysis.append("rpk4x2EventSummaryHasCarEvents=")
                    .append(eventSummaryHasCar(text)).append('\n');
              }
            }
          }
        }
        if(analysis.length()>0)putText(out,rawBudget,"mydevices-analysis.txt",analysis.toString());
      }
    } catch (Throwable ignored) {
      // Export remains useful even if OEM APK layout changes.
    }
  }

  private static void copyEntry(ZipFile apk, String source, ZipOutputStream out, long[] rawBudget,
      String target, int max) throws IOException {
    ZipEntry entry = apk.getEntry(source);
    if (entry == null) return;
    try (InputStream in = apk.getInputStream(entry)) {
      putBytes(out, rawBudget, target, readBounded(in, max));
    }
  }

  private static byte[] readBounded(InputStream in, int max) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] block = new byte[8192];
    int total = 0, read;
    while ((read = in.read(block)) != -1) {
      int allowed = Math.min(read, max - total);
      if (allowed > 0) buffer.write(block, 0, allowed);
      total += allowed;
      if (total >= max) break;
    }
    return buffer.toByteArray();
  }

  private static String sanitizePath(String name) {
    return name.replace("..", "_").replace('\\','/');
  }

  private static void putText(ZipOutputStream zip, long[] budget, String name, String value)
      throws IOException {
    putBytes(zip, budget, name, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private static void putBytes(ZipOutputStream zip, long[] budget, String name, byte[] data)
      throws IOException {
    if (data == null || data.length == 0) return;
    if (budget[0] + data.length > MAX_RAW_BYTES) return;
    budget[0] += data.length;
    zip.putNextEntry(new ZipEntry(name));
    zip.write(data);
    zip.closeEntry();
  }

  private static String sha256(File file) {
    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] block = new byte[64 * 1024];
      int read;
      while ((read = in.read(block)) != -1) digest.update(block, 0, read);
      StringBuilder out = new StringBuilder();
      for (byte b : digest.digest()) out.append(String.format(Locale.US, "%02x", b & 255));
      return out.toString();
    } catch (Throwable failure) {
      return "unavailable";
    }
  }
}
