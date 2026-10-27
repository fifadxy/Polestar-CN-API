package io.github.polestarcn.digitalkeylab;

import io.github.polestarcn.companion.digitalkey.DigitalKeyStatus;
import io.github.polestarcn.companion.digitalkey.ResearchDigitalKeyProvider;

/** Pure-Java safety contract tests; does not need an Android device. */
public final class AndroidProviderContractTest {
  public static void main(String[] args) {
    ResearchDigitalKeyProvider provider =
        new ResearchDigitalKeyProvider("no OEM integration in research builds");
    if (provider.isOperational()) throw new AssertionError("research provider cannot operate");
    if (provider.status().phase != DigitalKeyStatus.Phase.UNAVAILABLE)
      throw new AssertionError("research provider must be unavailable");
    if (provider.status().canClaimWorkingKey())
      throw new AssertionError("research provider must never claim a key");

    DigitalKeyStatus simulatedPaired =
        new DigitalKeyStatus(DigitalKeyStatus.Phase.PAK_PAIRED, "lab only", false);
    if (simulatedPaired.canClaimWorkingKey())
      throw new AssertionError("PAK_PAIRED without vehicle proof is insufficient");
    DigitalKeyStatus fakeVerifiedNotPaired =
        new DigitalKeyStatus(DigitalKeyStatus.Phase.SDK_CONFIGURATION, "lab only", true);
    if (fakeVerifiedNotPaired.canClaimWorkingKey())
      throw new AssertionError("verification alone cannot imply pairing");

    System.out.println("PASS: research provider and digital-key verification gates");
  }
}
