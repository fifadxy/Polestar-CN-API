package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.os.*;
import android.database.Cursor;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import io.github.polestarcn.companion.uploadedcarlink.OppoCarLinkCommandMap;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class UploadedCarLinkLogicTest {
 private android.content.Context c;
 @Before public void start()throws Exception{c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();c.getSharedPreferences("vehicle_snapshot_display_v1",0).edit().clear().commit();c.getSharedPreferences("psa_virtual_vehicle_session",0).edit().clear().commit();c.getSharedPreferences("oppo_carlink_bridge_prefs",0).edit().clear().commit();Field f=VehicleCoordinator.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,null);f=io.github.polestarcn.companion.uploadedcarlink.VirtualVehicleSessionManager.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,null);}
 @Test public void originalNullContextAndDefaultBindingRemainTrue(){assertTrue(io.github.polestarcn.companion.uploadedcarlink.OppoOfficialVehicleBindState.hasCarBinding(null));assertTrue(io.github.polestarcn.companion.uploadedcarlink.OppoOfficialVehicleBindState.hasCarBinding(c));assertEquals("LPSVSEDEENL000001",io.github.polestarcn.companion.uploadedcarlink.OppoOfficialVehicleBindState.resolveBoundVin(null));}
 @Test public void originalMissingDirectionStillDefaultsToTrue(){assertEquals(OppoCarLinkCommandMap.MappedAction.LOCK,OppoCarLinkCommandMap.resolve(1001,null));assertEquals(OppoCarLinkCommandMap.MappedAction.CLIMATE_START,OppoCarLinkCommandMap.resolve(2,null));assertEquals(OppoCarLinkCommandMap.MappedAction.WINDOW_CLOSE,OppoCarLinkCommandMap.resolve(1003,null));Bundle b=new Bundle();b.putBoolean("action_target_state",false);assertEquals(OppoCarLinkCommandMap.MappedAction.UNLOCK,OppoCarLinkCommandMap.resolve(1001,b));}
 @Test public void originalSubstringMappingIsPreserved(){assertEquals(OppoCarLinkCommandMap.MappedAction.CLIMATE_START,OppoCarLinkCommandMap.resolveFromSdkCommandName("air_start",null));assertEquals(OppoCarLinkCommandMap.MappedAction.FIND_CAR_FLASH_HORN,OppoCarLinkCommandMap.resolveFromSdkCommandName("horn",null));}
 @Test public void originalHonkSpellingStillRemainsUnknown(){assertEquals(OppoCarLinkCommandMap.MappedAction.UNKNOWN,OppoCarLinkCommandMap.resolveFromSdkCommandName("honk",null));}
 @Test public void originalDefaultSessionValuesAreNotRemoved(){Bundle b=io.github.polestarcn.companion.uploadedcarlink.VirtualVehicleSessionManager.getInstance(c).buildSessionBundle();assertTrue(b.getBoolean("hasCarBinding"));assertEquals(82,b.getInt("soc_percent"));assertEquals(415,b.getInt("range_km"));assertTrue(b.getBoolean("is_locked"));}
 @Test public void originalDirectMethodNamesAreRouted(){for(String name:new String[]{"auth","lock","unlock","climate_on","climate_off","window_close","window_vent","find_car"})assertTrue(name,UploadedCarLinkEndpoint.handles(name));}
 @Test public void originalProviderCursorAndImmediateAckAreRetained(){try(Cursor cursor=UploadedCarLinkEndpoint.query(c,PsAssistantBridgeProvider.URI,new String[]{"vin"},null,null,null)){assertEquals(13,cursor.getColumnCount());assertTrue(cursor.moveToFirst());assertEquals(1,cursor.getInt(cursor.getColumnIndexOrThrow("hasCarBinding")));}Bundle b=new Bundle();b.putString("sdk_cmd_name","zzz");Bundle ack=UploadedCarLinkEndpoint.call(c,"dispatchCarCommand",null,b);assertTrue(ack.getBoolean("exec_success"));assertEquals(0,ack.getInt("exec_code"));assertTrue(ack.getBoolean("uploadedLogic"));assertFalse(ack.getBoolean("realVehicleConfirmed"));}
}
