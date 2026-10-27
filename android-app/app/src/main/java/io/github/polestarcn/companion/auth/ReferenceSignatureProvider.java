package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Standalone China-account compatibility material provider.
 *
 * Runtime APK import and dependency on another installed APK are intentionally removed.
 * A final standalone build may contain one build-time generated opaque asset at
 * assets/standalone/account_compat.bin. The raw material is never committed to source control.
 *
 * Existing users upgrading from an older PsAssistant build can still migrate the encrypted
 * compatibility blob already stored by that earlier build.
 */
public final class ReferenceSignatureProvider {
  private static final String PREF = "reference_import";
  private static final String PREF_KEY = "encrypted";
  private static final String ASSET = "standalone/account_compat.bin";
  private static final byte[] MASK = mask();

  private final Context context;
  private volatile String inMemorySecret;

  public ReferenceSignatureProvider(Context context) {
    this.context = context.getApplicationContext();
  }

  /** True when this APK is self-contained or an older installed version already provisioned it. */
  public boolean isReady() {
    if (context.getSharedPreferences(PREF, 0).contains(PREF_KEY)) return true;
    try (InputStream in = context.getAssets().open(ASSET)) {
      return in.read() >= 0;
    } catch (Exception ignored) {
      return false;
    }
  }

  /**
   * Resolve compatibility material without opening, scanning or depending on any other APK.
   */
  public String resolve() throws Exception {
    String cached = inMemorySecret;
    if (cached != null) return cached;

    String encrypted = context.getSharedPreferences(PREF, 0).getString(PREF_KEY, null);
    if (encrypted != null) {
      try {
        String migrated =
            new String(
                SecureSessionStore.decrypt(Base64.decode(encrypted, Base64.NO_WRAP)),
                StandardCharsets.UTF_8);
        validate(migrated);
        inMemorySecret = migrated;
        return migrated;
      } catch (Exception ignored) {
        context.getSharedPreferences(PREF, 0).edit().remove(PREF_KEY).apply();
      }
    }

    byte[] encoded;
    try (InputStream in = context.getAssets().open(ASSET)) {
      encoded = readAll(in);
    } catch (Exception e) {
      throw new IllegalStateException(
          "当前安装包未包含独立账号兼容配置；请安装完整 Standalone 构建版本");
    }
    if (encoded.length != 32) {
      throw new IllegalStateException("独立账号兼容配置格式异常");
    }
    byte[] plain = new byte[encoded.length];
    for (int i = 0; i < encoded.length; i++) plain[i] = (byte) (encoded[i] ^ MASK[i]);
    String material = new String(plain, StandardCharsets.US_ASCII);
    validate(material);
    inMemorySecret = material;
    return material;
  }

  private static void validate(String material) {
    if (material == null || material.length() != 32)
      throw new IllegalStateException("账号兼容配置长度异常");
    for (int i = 0; i < material.length(); i++) {
      char c = material.charAt(i);
      if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')))
        throw new IllegalStateException("账号兼容配置校验失败");
    }
  }

  private static byte[] readAll(InputStream in) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[4096];
    int n;
    while ((n = in.read(buf)) >= 0) if (n > 0) out.write(buf, 0, n);
    return out.toByteArray();
  }

  private static byte[] mask() {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest("PsAssistant-Standalone-Account-Compatibility-v1".getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
