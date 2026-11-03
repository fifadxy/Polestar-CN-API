package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class Beta25NativeRouteTest {
  private static final String IDS="'chesuo','kongtiao','shanguang','mingdi'";
  @Test public void nearbyCarIconsCannotQualifyMissingCarEventsOrDefaults(){
    String js="const eventSummary = exports.eventSummary = ['1','2','NOISE_REDUCTION_CLOSE'];"
        +"const smartHomeDefault = exports.smartHomeDefault = {LAMP:[{id:'power'}],AIR_CONDITIONER:[]};"
        +"const carEventIconName={CAR:["+IDS+"]};";
    assertFalse(QuickAppCarCapability.hasCarEvents(js));assertFalse(QuickAppCarCapability.hasCarDefaults(js));
    assertFalse(new QuickAppCarCapability.Result("17.4.15",true,true,false,false,"").routeSafe());
  }
  @Test public void requiresAllFourIdsInsideTheActualInitializer(){
    assertTrue(QuickAppCarCapability.hasCarEvents("const eventSummary = exports.eventSummary = ["+IDS+"];"));
    assertTrue(QuickAppCarCapability.hasCarDefaults("const smartHomeDefault = exports.smartHomeDefault = {CAR:["+IDS+"]};"));
    assertFalse(QuickAppCarCapability.hasCarEvents("const eventSummary = ['chesuo']; // "+IDS));
    assertFalse(QuickAppCarCapability.hasCarEvents("const eventSummary = ["+IDS));
    assertFalse(QuickAppCarCapability.hasCarDefaults("const smartHomeDefault = {CAR:['chesuo'],OTHER:["+IDS+"]};"));
  }
  public static class DeviceFixture {
    final String id,name,authority;final List<?> switches;final ArrayList<Integer> batteries;
    public DeviceFixture(String mac,String id,String name,Object type,Object connect,Object template,int icon,String url,String authority,Object use,int priority,long at,boolean audio,boolean multi,ArrayList<?> templates,boolean supportsSwitch,List<?> switches,ArrayList<?> actions,ArrayList<Integer> batteries,ArrayList<?> batteryInfo){
      this.id=id;this.name=name;this.authority=authority;this.switches=switches;this.batteries=batteries;
    }
  }
  @Test public void nativeFallbackBuildsAnOwnRowWithoutInventingUnknownBattery()throws Exception{
    Bundle s=new Bundle();s.putString("name","Polestar 2");ArrayList<Object> menus=new ArrayList<>();menus.add("fixture-switch");
    java.lang.reflect.Constructor<?> ctor=DeviceFixture.class.getConstructors()[0];
    DeviceFixture row=(DeviceFixture)AssistantScreenNativeVehicleHook.newDevice(ctor,s,"CAR","CONNECTED","NOISE_CANCELING",menus);
    assertEquals("psassistant_shelf_polestar",row.id);assertTrue(BridgeIdentity.isDeviceAuthority(row.authority));
    assertSame(menus,row.switches);assertTrue(row.batteries.isEmpty());
    s.putInt("batteryPercent",64);s.putBoolean("batteryPercentKnown",true);
    row=(DeviceFixture)AssistantScreenNativeVehicleHook.newDevice(ctor,s,"CAR","CONNECTED","NOISE_CANCELING",menus);
    assertEquals(Integer.valueOf(64),row.batteries.get(0));assertTrue(row.name.contains("64%"));
  }
}
