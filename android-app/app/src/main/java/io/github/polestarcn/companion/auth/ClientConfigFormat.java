package io.github.polestarcn.companion.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

/**
 * Format-only codec for build-time client configuration; never handles user credentials.
 * Compatible with PsAssistant v0.8.5 standalone, v0.8.8 plaintext and v0.8.6+ v2.
 */
public final class ClientConfigFormat {
  static final String EXPECTED_FINGERPRINT =
      "59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274";
  static final String V2_LABEL = "PsAssistant-Standalone-ClientConfig-v2";
  static final String LEGACY_LABEL = "PsAssistant-Standalone-Account-Compatibility-v1";
  private static final byte[] MAGIC = {'P','S','A','C','F','G','2',0};

  private ClientConfigFormat() {}

  static String decodePlaintext(byte[] data) {
    if (data == null || data.length != 32) throw new IllegalArgumentException("Client configuration length");
    return new String(data, StandardCharsets.US_ASCII);
  }

  static String decodeLegacyStandalone(byte[] data) throws Exception {
    if (data == null || data.length != 32) throw new IllegalArgumentException("Legacy client configuration length");
    byte[] mask = MessageDigest.getInstance("SHA-256")
        .digest(LEGACY_LABEL.getBytes(StandardCharsets.UTF_8));
    byte[] clear = new byte[32];
    try {
      for (int i = 0; i < clear.length; i++) clear[i] = (byte) (data[i] ^ mask[i]);
      return new String(clear, StandardCharsets.US_ASCII);
    } finally {
      Arrays.fill(clear, (byte) 0);
    }
  }

  static String decodeV2(byte[] encoded) throws Exception {
    if (encoded == null || encoded.length != 48) throw new IllegalArgumentException("Client configuration container length");
    for (int i = 0; i < MAGIC.length; i++)
      if (MAGIC[i] != encoded[i]) throw new IllegalArgumentException("Client configuration container header");
    byte[] mask = MessageDigest.getInstance("SHA-256")
        .digest(V2_LABEL.getBytes(StandardCharsets.UTF_8));
    byte[] plain = new byte[32];
    try {
      for (int i = 0; i < 32; i++) plain[i] = (byte) (encoded[i + 8] ^ mask[i]);
      byte[] checksum = MessageDigest.getInstance("SHA-256").digest(plain);
      if (!MessageDigest.isEqual(Arrays.copyOf(checksum, 8),
              Arrays.copyOfRange(encoded, 40, 48)))
        throw new IllegalArgumentException("Client configuration container checksum");
      return new String(plain, StandardCharsets.US_ASCII);
    } finally {
      Arrays.fill(plain, (byte) 0);
    }
  }

  static byte[] encodeV2(String value) throws Exception {
    if (value == null || !value.matches("[0-9A-F]{32}")) throw new IllegalArgumentException("Client configuration syntax");
    byte[] plain = value.getBytes(StandardCharsets.US_ASCII);
    try {
      byte[] checksum = MessageDigest.getInstance("SHA-256").digest(plain);
      byte[] mask = MessageDigest.getInstance("SHA-256")
          .digest(V2_LABEL.getBytes(StandardCharsets.UTF_8));
      byte[] encoded = new byte[48];
      System.arraycopy(MAGIC, 0, encoded, 0, MAGIC.length);
      for (int i = 0; i < 32; i++) encoded[i + 8] = (byte) (plain[i] ^ mask[i]);
      System.arraycopy(checksum, 0, encoded, 40, 8);
      return encoded;
    } finally {
      Arrays.fill(plain, (byte) 0);
    }
  }

  static String sha256(String value) throws Exception {
    byte[] b = MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(StandardCharsets.US_ASCII));
    StringBuilder out = new StringBuilder(64);
    for (byte x : b) out.append(String.format(Locale.ROOT, "%02x", x & 255));
    return out.toString();
  }

  static void validateCompatibility(String value) throws Exception {
    if (value == null || !value.matches("[0-9A-F]{32}"))
      throw new IllegalArgumentException("Invalid client configuration syntax");
    if (!EXPECTED_FINGERPRINT.equals(sha256(value)))
      throw new IllegalArgumentException("Client configuration does not match the approved previous version");
  }
}
