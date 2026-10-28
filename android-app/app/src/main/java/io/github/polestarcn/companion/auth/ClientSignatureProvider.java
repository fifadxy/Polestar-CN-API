package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * China account client configuration. Only the application's own bundled assets or
 * its own previously encrypted migration data are accepted. Never scans installed apps.
 */
public final class ClientSignatureProvider {
  private static final String MODERN = "client-signing.bin";
  private static final String STANDALONE_V085 = "standalone/account_compat.bin";
  private static final String LEGACY_PREF = "reference_import";
  private static final String LEGACY_KEY = "encrypted";
  public enum Source { BUNDLED_V2, BUNDLED_LEGACY, BUNDLED_STANDALONE, MIGRATED_LEGACY, MISSING }

  private final Context context;
  private volatile String cached;
  private volatile Source source = Source.MISSING;

  public ClientSignatureProvider(Context context) {
    this.context = context.getApplicationContext();
  }

  public boolean isReady() {
    try { resolve(); return true; }
    catch (Exception unavailable) { return false; }
  }

  public Source source() {
    if (cached == null) isReady();
    return source;
  }

  public String resolve() throws Exception {
    String v = cached;
    if (v != null) return v;
    synchronized (this) {
      if (cached != null) return cached;

      byte[] built = readAsset(MODERN);
      if (built != null) {
        String material;
        if (built.length == 48) {
          material = ClientConfigFormat.decodeV2(built);
          source = Source.BUNDLED_V2;
        } else {
          material = ClientConfigFormat.decodePlaintext(built);
          source = Source.BUNDLED_LEGACY;
        }
        ClientConfigFormat.validateCompatibility(material);
        return cached = material;
      }

      byte[] previous = readAsset(STANDALONE_V085);
      if (previous != null) {
        String material = ClientConfigFormat.decodeLegacyStandalone(previous);
        ClientConfigFormat.validateCompatibility(material);
        source = Source.BUNDLED_STANDALONE;
        return cached = material;
      }

      String encrypted = context.getSharedPreferences(LEGACY_PREF, Context.MODE_PRIVATE)
          .getString(LEGACY_KEY, null);
      if (encrypted != null && !encrypted.isEmpty()) {
        try {
          String material = new String(
              SecureSessionStore.decrypt(Base64.decode(encrypted, Base64.NO_WRAP)),
              StandardCharsets.US_ASCII);
          ClientConfigFormat.validateCompatibility(material);
          source = Source.MIGRATED_LEGACY;
          return cached = material;
        } catch (Exception invalid) {
          // An old installation may no longer have its Keystore key; retain the value.
        }
      }

      source = Source.MISSING;
      throw new IllegalStateException(
          "This is a public verification APK without China account client configuration. "
              + "Use the complete PsAssistant build with its original signing identity.");
    }
  }

  private byte[] readAsset(String name) throws Exception {
    try (InputStream in = context.getAssets().open(name)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream(64);
      byte[] buffer = new byte[64];
      int n;
      while ((n = in.read(buffer)) != -1) {
        if (n == 0) continue;
        if (out.size() + n > 128) throw new IllegalArgumentException("Client configuration resource too large");
        out.write(buffer, 0, n);
      }
      return out.toByteArray();
    } catch (IOException missing) {
      return null;
    }
  }
}
