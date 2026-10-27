package io.github.polestarcn.companion.digitalkey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class DigitalKeyContractTest {
  @Test public void researchProviderDoesNotOperate() {
    ResearchDigitalKeyProvider stub = new ResearchDigitalKeyProvider("OEM not authorized");
    assertFalse(stub.isOperational());
    assertEquals(DigitalKeyStatus.Phase.UNAVAILABLE, stub.status().phase);
    assertFalse(stub.status().canClaimWorkingKey());
  }

  @Test public void simulatedPairingDoesNotCreateKey() {
    DigitalKeyStatus pairedWithoutProof =
        new DigitalKeyStatus(DigitalKeyStatus.Phase.PAK_PAIRED, "lab only", false);
    assertFalse(pairedWithoutProof.canClaimWorkingKey());
  }

  @Test public void proofWithoutPairingDoesNotCreateKey() {
    DigitalKeyStatus notPaired =
        new DigitalKeyStatus(DigitalKeyStatus.Phase.SDK_CONFIGURATION, "test", true);
    assertFalse(notPaired.canClaimWorkingKey());
  }
}
