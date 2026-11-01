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
    for (String method : new String[]{"capabilities","snapshot","host-status","shelf-status","account-bind-status","discovery-baseline-diff"}) {
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
          addInteresting(interesting, item.name);
        if (info.providers != null) for (android.content.pm.ProviderInfo item : info.providers)
          addInteresting(interesting, item.name + " authority=" + item.authority);
        if (info.services != null) for (android.content.pm.ServiceInfo item : info.services)
          addInteresting(interesting, item.name);
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

  private static void extractMyDevicesAssets(Context context, ZipOutputStream out, long[] rawBudget) {
    try {
      ApplicationInfo app = context.getPackageManager().getApplicationInfo("com.heytap.mydevices", 0);
      try (ZipFile apk = new ZipFile(app.sourceDir)) {
        copyEntry(apk, "assets/support/card_short_cut.json", out, rawBudget,
            "mydevices/card_short_cut.json", MAX_TEXT_ENTRY);
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
            }
          }
        }
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
