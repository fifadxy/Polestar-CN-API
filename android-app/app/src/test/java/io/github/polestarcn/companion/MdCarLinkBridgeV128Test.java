package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class MdCarLinkBridgeV128Test {
  private Bundle input(String value){
    Bundle b=new Bundle();b.putString("sdk_cmd_name",value);return b;
  }
  @Test public void allConfirmedOemFourIdsUseUnifiedDispatch(){
    assertEquals("lock-toggle",OppoCarLinkCommandMap.bridgeAction(-1,input("chesuo")));
    assertEquals("climate-toggle",OppoCarLinkCommandMap.bridgeAction(-1,input("kongtiao")));
    assertEquals("flash",OppoCarLinkCommandMap.bridgeAction(-1,input("shanguang")));
    assertEquals("honk",OppoCarLinkCommandMap.bridgeAction(-1,input("mingdi")));
    assertEquals("honk-flash",OppoCarLinkCommandMap.bridgeAction(1004,null));
  }
  @Test public void missingDirectionNeverBecomesAnImplicitUnlock(){
    assertNull(OppoCarLinkCommandMap.bridgeAction(1001,null));
    assertNull(OppoCarLinkCommandMap.bridgeAction(-1,input("car-door")));
    Bundle b=input("car-door");b.putBoolean("action_target_state",true);
    assertEquals("unlock",OppoCarLinkCommandMap.bridgeAction(-1,b));
    b.putBoolean("action_target_state",false);
    assertEquals("lock",OppoCarLinkCommandMap.bridgeAction(-1,b));
  }
  @Test public void aClickMayRequestOneRefreshOnlyUntilCooldownExpires(){
    assertTrue(BridgeDemandRefresh.admit("vehicle-fixture",100000L));
    assertFalse(BridgeDemandRefresh.admit("vehicle-fixture",100001L));
    assertFalse(BridgeDemandRefresh.admit("vehicle-fixture",119999L));
    assertTrue(BridgeDemandRefresh.admit("vehicle-fixture",120000L));
    assertTrue(BridgeDemandRefresh.admit("another-vehicle",120001L));
  }
}
