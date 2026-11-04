package io.github.polestarcn.companion.auth;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import org.junit.Test;

public final class ClientConfigFormatTest {
  private static final String SYNTHETIC = "0123456789ABCDEF0123456789ABCDEF";

  @Test public void v2RoundTripAndChecksum() throws Exception {
    byte[] encoded = ClientConfigFormat.encodeV2(SYNTHETIC);
    assertEquals(48, encoded.length);
    assertEquals(SYNTHETIC, ClientConfigFormat.decodeV2(encoded));
    encoded[12] ^= 1;
    try {
      ClientConfigFormat.decodeV2(encoded);
      fail("Tampering must invalidate container");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("checksum"));
    }
  }

  @Test public void legacyStandaloneFromPreviousVersion() throws Exception {
    byte[] clear = SYNTHETIC.getBytes(StandardCharsets.US_ASCII);
    byte[] mask = MessageDigest.getInstance("SHA-256").digest(
        ClientConfigFormat.LEGACY_LABEL.getBytes(StandardCharsets.UTF_8));
    byte[] oldAsset = new byte[32];
    for (int i = 0; i < 32; i++) oldAsset[i] = (byte) (clear[i] ^ mask[i]);
    assertEquals(SYNTHETIC, ClientConfigFormat.decodeLegacyStandalone(oldAsset));
  }

  @Test public void oldPlaintextAndBadInputsAreHandledWithoutNetwork() throws Exception {
    assertEquals(SYNTHETIC, ClientConfigFormat.decodePlaintext(
        SYNTHETIC.getBytes(StandardCharsets.US_ASCII)));
    try { ClientConfigFormat.decodeV2(new byte[32]); fail(); }
    catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("length")); }
    try { ClientConfigFormat.decodePlaintext(new byte[0]); fail(); }
    catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("length")); }
    try { ClientConfigFormat.validateCompatibility(SYNTHETIC); fail(); }
    catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("previous version")); }
  }
}
