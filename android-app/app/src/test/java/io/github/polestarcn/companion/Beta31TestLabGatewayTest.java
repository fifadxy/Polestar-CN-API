package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import org.junit.Test;

public final class Beta31TestLabGatewayTest {
  private static VehicleSnapshot snapshot(String lock,String climate) {
    return new VehicleSnapshot("Polestar 2","id",55,230,10000,"未充电","已关闭","已关闭","已关闭",lock,
        System.currentTimeMillis(),"test",false,climate,"关闭");
  }

  @Test public void gatewayResolvesExactToggleStatesWithoutSubstringMistakes() {
    assertEquals("lock",VehicleProtocolGateway.resolveAction("lock-toggle",snapshot("UNLOCKED","INACTIVE")));
    assertEquals("unlock",VehicleProtocolGateway.resolveAction("lock-toggle",snapshot("LOCKED","INACTIVE")));
    assertEquals("climate-start",VehicleProtocolGateway.resolveAction("climate-toggle",snapshot("LOCKED","INACTIVE")));
    assertEquals("climate-stop",VehicleProtocolGateway.resolveAction("climate-toggle",snapshot("LOCKED","ACTIVE")));
  }

  @Test public void gatewayPassesOnlyKnownDirectCommands() {
    assertEquals("flash",VehicleProtocolGateway.resolveAction("flash",snapshot("LOCKED","INACTIVE")));
    assertEquals("honk",VehicleProtocolGateway.resolveAction("honk",snapshot("LOCKED","INACTIVE")));
    assertEquals("trunk-open",VehicleProtocolGateway.resolveAction("trunk-open",snapshot("LOCKED","INACTIVE")));
    assertNull(VehicleProtocolGateway.resolveAction("foreign",snapshot("LOCKED","INACTIVE")));
    assertNull(VehicleProtocolGateway.resolveAction("lock-toggle",snapshot("UNKNOWN","INACTIVE")));
  }
}
