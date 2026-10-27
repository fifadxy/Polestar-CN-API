package io.github.polestarcn.digitalkeylab;

public final class ProvisioningStateMachineTest {
  public static void main(String[] args) {
    ProvisioningStateMachine model = new ProvisioningStateMachine();
    ProvisioningStateMachine.Stage[] stages = ProvisioningStateMachine.Stage.values();
    if (model.canClaimWorkingDigitalKey()) throw new AssertionError("must not claim before pairing");
    if (model.advance(stages[0], stages[2])) throw new AssertionError("must not skip stages");
    for (int i = 0; i < stages.length - 1; i++) {
      if (!model.advance(stages[i], stages[i + 1])) throw new AssertionError("failed at " + stages[i]);
      if (ProvisioningStateMachine.evidenceFor(stages[i + 1]).isEmpty())
        throw new AssertionError("missing evidence label");
    }
    if (!model.canClaimWorkingDigitalKey()) throw new AssertionError("final stage should be paired");
    System.out.println("PASS: provisioning state model; " + stages.length + " evidence stages");
  }
}
