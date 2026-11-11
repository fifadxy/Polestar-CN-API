package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import org.junit.Test;

/** The one-tap button cannot target arbitrary processes or inject shell text. */
public final class BridgeHostRestarterV271Test {
 @Test public void onlyAllowlistedHostPackagesMayBeTouched(){
   for(String pkg:BridgeHostRestarter.targets())assertTrue(BridgeHostRestarter.approved(pkg));
   for(String pkg:new String[]{"com.android.systemui","com.android.launcher",
       "io.github.polestarcn.companion","org.lsposed.manager",
       "com.topjohnwu.magisk","com.heytap.mydevices;reboot"})
     assertFalse(BridgeHostRestarter.approved(pkg));
 }
 @Test public void exactLauncherOwnershipIsRequired(){
   assertEquals("am kill --user current 'com.heytap.mydevices'",
       BridgeHostRestarter.command("com.heytap.mydevices",null));
   String valid=BridgeHostRestarter.command("com.heytap.mydevices",
       "com.heytap.mydevices/com.heytap.mydevices.MainActivity");
   assertTrue(valid.contains("am force-stop --user current"));
   assertTrue(valid.contains("am start --user current -n"));
   try{BridgeHostRestarter.command("com.heytap.mydevices","com.android.systemui/.Main");
       fail("unrelated launcher");}catch(IllegalArgumentException expected){}
   try{BridgeHostRestarter.command("com.android.systemui",null);
       fail("systemui forbidden");}catch(IllegalArgumentException expected){}
 }
}
