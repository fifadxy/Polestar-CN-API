package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Compatibility signer for the user-verified reference app family. Imported compatibility data is
 * encrypted with the Android Keystore; never included in source or APK.
 *
 * Identity is anchored to the package name and verified signing certificate instead of requiring
 * byte-for-byte equality of the whole APK. This allows legitimately rebuilt APK variants signed by
 * the same key while still rejecting third-party re-signed packages.
 */
public final class ReferenceSignatureProvider {
  public static final String REFERENCE_PACKAGE = "app.c3garage.companion";
  public static final String REFERENCE_CERT_SHA256 =
      "a64bcf2456d3a2e920a7620e899e7da74df7bcc39d53f517f43461eaf1166bc7";

  private final Context context;
  private volatile String inMemorySecret;

  public ReferenceSignatureProvider(Context context) {
    this.context = context.getApplicationContext();
  }

  public boolean isReferenceInstalled() {
    try {
      ApplicationInfo info =
          context.getPackageManager().getApplicationInfo(REFERENCE_PACKAGE, 0);
      validateIdentity(new File(info.sourceDir));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isReady() {
    return isReferenceInstalled()
        || context.getSharedPreferences("reference_import", 0).contains("encrypted");
  }

  public synchronized void importApk(Uri uri) throws Exception {
    File temporary = File.createTempFile("reference-", ".apk", context.getCacheDir());
    try {
      try (InputStream in = context.getContentResolver().openInputStream(uri);
          java.io.FileOutputStream out = new java.io.FileOutputStream(temporary)) {
        if (in == null) throw new Exception("Cannot read selected APK");
        byte[] buffer = new byte[32768];
        int count;
        long total = 0;
        while ((count = in.read(buffer)) != -1) {
          total += count;
          if (total > 20 * 1024 * 1024) throw new Exception("Reference APK exceeds 20 MB");
          out.write(buffer, 0, count);
        }
      }
      String material = extract(temporary);
      String encrypted =
          Base64.encodeToString(
              SecureSessionStore.encrypt(material.getBytes(StandardCharsets.UTF_8)),
              Base64.NO_WRAP);
      context
          .getSharedPreferences("reference_import", 0)
          .edit()
          .putString("encrypted", encrypted)
          .apply();
      inMemorySecret = material;
    } finally {
      temporary.delete();
    }
  }

  public String resolve() throws Exception {
    String cached = inMemorySecret;
    if (cached != null) return cached;

    String encrypted =
        context.getSharedPreferences("reference_import", 0).getString("encrypted", null);
    if (encrypted != null) {
      try {
        inMemorySecret =
            new String(
                SecureSessionStore.decrypt(Base64.decode(encrypted, Base64.NO_WRAP)),
                StandardCharsets.UTF_8);
        return inMemorySecret;
      } catch (Exception e) {
        context.getSharedPreferences("reference_import", 0).edit().clear().apply();
      }
    }
    ApplicationInfo info;
    try {
      info = context.getPackageManager().getApplicationInfo(REFERENCE_PACKAGE, 0);
    } catch (PackageManager.NameNotFoundException e) {
      throw new IllegalStateException("未检测到已验证模板 APK；请先安装模板或在设置中导入 APK");
    }
    inMemorySecret = extract(new File(info.sourceDir));
    return inMemorySecret;
  }

  private String extract(File apk) throws Exception {
    validateIdentity(apk);

    byte[] dex;
    try (ZipFile zip = new ZipFile(apk)) {
      ZipEntry entry = zip.getEntry("classes.dex");
      if (entry == null) throw new IllegalStateException("模板 APK 缺少 classes.dex");
      try (InputStream in = zip.getInputStream(entry)) {
        dex = readAll(in);
      }
    }

    Set<String> candidates = new LinkedHashSet<>();
    for (int i = 0; i + 32 <= dex.length; i++) {
      if (!isUpperHex(dex[i])) continue;
      boolean ok = true;
      for (int j = 1; j < 32; j++) {
        if (!isUpperHex(dex[i + j])) {
          ok = false;
          break;
        }
      }
      if (!ok) continue;
      boolean left = i > 0 && isUpperHex(dex[i - 1]);
      boolean right = i + 32 < dex.length && isUpperHex(dex[i + 32]);
      if (!left && !right) {
        candidates.add(new String(dex, i, 32, StandardCharsets.US_ASCII));
        i += 31;
      }
    }
    if (candidates.size() != 1) {
      throw new IllegalStateException("模板兼容签名材料定位失败：候选数=" + candidates.size());
    }
    return candidates.iterator().next();
  }

  private void validateIdentity(File apk) throws Exception {
    PackageManager pm = context.getPackageManager();
    int flags =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            ? PackageManager.GET_SIGNING_CERTIFICATES
            : PackageManager.GET_SIGNATURES;
    PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
    if (info == null) throw new IllegalStateException("无法读取模板 APK 包信息");
    if (!REFERENCE_PACKAGE.equals(info.packageName)) {
      throw new IllegalStateException("模板 APK 包名不匹配，已拒绝导入");
    }

    Signature[] signatures;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
      signatures =
          info.signingInfo.hasMultipleSigners()
              ? info.signingInfo.getApkContentsSigners()
              : info.signingInfo.getSigningCertificateHistory();
    } else {
      signatures = info.signatures;
    }
    if (signatures == null || signatures.length == 0) {
      throw new IllegalStateException("模板 APK 未读取到签名证书");
    }

    boolean trusted = false;
    for (Signature signature : signatures) {
      String certSha = sha256(signature.toByteArray());
      if (REFERENCE_CERT_SHA256.equalsIgnoreCase(certSha)) {
        trusted = true;
        break;
      }
    }
    if (!trusted) {
      throw new IllegalStateException(
          "检测到模板 APK，但签名证书与已验证样本不一致，已拒绝提取兼容签名材料");
    }
  }

  private static boolean isUpperHex(byte b) {
    int c = b & 0xff;
    return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
  }

  private static String sha256(byte[] data) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(data);
    StringBuilder out = new StringBuilder();
    for (byte b : md.digest()) out.append(String.format(Locale.US, "%02x", b & 0xff));
    return out.toString();
  }

  private static byte[] readAll(InputStream in) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[32 * 1024];
    int n;
    while ((n = in.read(buf)) >= 0) if (n > 0) out.write(buf, 0, n);
    return out.toByteArray();
  }
}
