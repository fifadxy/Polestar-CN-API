package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Self-contained China-account client configuration provider.
 *
 * Complete distribution builds package a versioned opaque asset generated from
 * private build input. The installed app never scans, imports or depends on
 * another APK. Older encrypted PsAssistant configuration is accepted only as
 * an upgrade fallback.
 *
 * The asset transformation is not presented as strong secret protection: any
 * client-side configuration can ultimately be extracted from a distributed
 * APK. It primarily prevents accidental plaintext exposure and provides format
 * and integrity checks.
 */
public final class ClientSignatureProvider {
  private static final String ASSET = "client-signing.bin";
  private static final String LEGACY_PREF = "reference_import";
  private static final String LEGACY_KEY = "encrypted";
  private static final byte[] MAGIC = new byte[] {'P','S','A','C','F','G','2',0};
  private static final String MASK_LABEL = "PsAssistant-Standalone-ClientConfig-v2";
  private static final String EXPECTED_SHA256 =
      "59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274";

  public enum Source {
    BUNDLED_V2,
    BUNDLED_LEGACY,
    MIGRATED_LEGACY,
    MISSING
  }

  private final Context context;
  private volatile String cached;
  private volatile Source source = Source.MISSING;

  public ClientSignatureProvider(Context context) {
    this.context = context.getApplicationContext();
  }

  public boolean isReady() {
    try {
      resolve();
      return true;
    } catch (Exception missing) {
      return false;
    }
  }

  public Source source() {
    if (cached == null) {
      try {
        resolve();
      } catch (Exception ignored) {
        source = Source.MISSING;
      }
    }
    return source;
  }

  public String resolve() throws Exception {
    String current = cached;
    if (current != null) return current;

    synchronized (this) {
      if (cached != null) return cached;

      String bundled = readBundled();
      if (bundled != null) {
        cached = bundled;
        return cached;
      }

      String migrated = readLegacyMigration();
      if (migrated != null) {
        source = Source.MIGRATED_LEGACY;
        cached = migrated;
        return cached;
      }

      source = Source.MISSING;
      throw new IllegalStateException(
          "This verification build has no account client configuration. Install the complete signed APK.");
    }
  }

  private String readBundled() throws Exception {
    byte[] encoded;
    try (InputStream in = context.getAssets().open(ASSET)) {
      encoded = readAll(in);
    } catch (java.io.IOException missing) {
      return null;
    }

    // Backward compatibility with the first standalone build format.
    if (encoded.length == 32) {
      String value = new String(encoded, StandardCharsets.US_ASCII).trim();
      validate(value);
      source = Source.BUNDLED_LEGACY;
      return value;
    }

    if (encoded.length != 48) {
      throw new IllegalStateException("Bundled client configuration has an invalid length.");
    }
    for (int i = 0; i < MAGIC.length; i++) {
      if (encoded[i] != MAGIC[i]) {
        throw new IllegalStateException("Bundled client configuration has an invalid header.");
      }
    }

    byte[] mask = MessageDigest.getInstance("SHA-256")
        .digest(MASK_LABEL.getBytes(StandardCharsets.UTF_8));
    byte[] plain = new byte[32];
    for (int i = 0; i < plain.length; i++) {
      plain[i] = (byte) (encoded[MAGIC.length + i] ^ mask[i]);
    }

    byte[] digest = MessageDigest.getInstance("SHA-256").digest(plain);
    byte[] storedCheck = Arrays.copyOfRange(encoded, 40, 48);
    if (!MessageDigest.isEqual(Arrays.copyOf(digest, 8), storedCheck)) {
      Arrays.fill(plain, (byte) 0);
      throw new IllegalStateException("Bundled client configuration integrity check failed.");
    }

    String value = new String(plain, StandardCharsets.US_ASCII);
    Arrays.fill(plain, (byte) 0);
    validate(value);
    source = Source.BUNDLED_V2;
    return value;
  }

  private String readLegacyMigration() {
    String old =
        context.getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE).getString(LEGACY_KEY, null);
    if (old == null || old.isEmpty()) return null;
    try {
      String value =
          new String(
              SecureSessionStore.decrypt(Base64.decode(old, Base64.NO_WRAP)),
              StandardCharsets.UTF_8);
      validate(value);
      return value;
    } catch (Exception unavailable) {
      return null;
    }
  }

  private static void validate(String value) throws Exception {
    if (value == null || !value.matches("[0-9A-F]{32}")) {
      throw new IllegalStateException("Account client configuration format is invalid.");
    }
    String fingerprint = sha256(value.getBytes(StandardCharsets.US_ASCII));
    if (!EXPECTED_SHA256.equals(fingerprint)) {
      throw new IllegalStateException("Account client configuration fingerprint mismatch.");
    }
  }

  private static byte[] readAll(InputStream in) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[256];
    int n;
    int total = 0;
    while ((n = in.read(buffer)) != -1) {
      if (n <= 0) continue;
      total += n;
      if (total > 128) throw new IllegalStateException("Client configuration asset is too large.");
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }

  private static String sha256(byte[] value) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
    StringBuilder out = new StringBuilder(digest.length * 2);
    for (byte b : digest) out.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
    return out.toString();
  }
}
