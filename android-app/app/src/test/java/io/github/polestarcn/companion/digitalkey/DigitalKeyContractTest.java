package io.github.polestarcn.companion.digitalkey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

  @Test public void v21StopsAtOemIdentityWithoutAuthorizedIdentity() {
    AuthorizedFlowAssessment flow =
        AuthorizedFlowAssessment.observed(true, true, false, true);
    assertTrue(flow.completed(AuthorizedFlowAssessment.Stage.ACCOUNT_LOGIN));
    assertTrue(flow.completed(AuthorizedFlowAssessment.Stage.VEHICLE_DISCOVERY));
    assertFalse(flow.completed(AuthorizedFlowAssessment.Stage.OEM_IDENTITY));
    assertEquals(
        AuthorizedFlowAssessment.Stage.OEM_IDENTITY,
        flow.firstBlockedStage());
    assertFalse(flow.protectedProvisioningAllowed());
    assertFalse(flow.canClaimWorkingDigitalKey());
  }

  @Test public void nativeSdkCannotBypassMissingVehicleOrIdentity() {
    AuthorizedFlowAssessment noVehicle =
        AuthorizedFlowAssessment.observed(true, false, true, true);
    assertEquals(
        AuthorizedFlowAssessment.Stage.VEHICLE_DISCOVERY,
        noVehicle.firstBlockedStage());
    assertFalse(noVehicle.completed(AuthorizedFlowAssessment.Stage.SDK_CONFIGURATION));

    AuthorizedFlowAssessment noAccount =
        AuthorizedFlowAssessment.observed(false, true, true, true);
    assertEquals(
        AuthorizedFlowAssessment.Stage.ACCOUNT_LOGIN,
        noAccount.firstBlockedStage());
    assertFalse(noAccount.protectedProvisioningAllowed());
  }
}
