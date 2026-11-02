package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import org.junit.Test;

public class OppoCarKeyCapabilityTest {
  @Test public void exposesOfficialBluetoothKeyEntryPointsWithoutBindingSideEffects() {
    assertEquals("oppo.intent.action.opluscarlink.BIND_BT_KEY",
        OppoCarKeyCapability.bindIntent().getAction());
    assertEquals("com.nearme.wallet.key.KEY_DETAIL_ACTIVITY",
        OppoCarKeyCapability.walletDetailIntent().getAction());
    assertEquals("6",OppoCarKeyCapability.VALUE_CARD_TYPE_BLUETOOTH_CAR_KEY);
  }
}
