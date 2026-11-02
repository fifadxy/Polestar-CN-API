package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import com.oplus.ocs.carlink.model.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class CarLinkBeta26Test {
  private Context c;
  @Before public void clear()throws Exception{c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();c.getSharedPreferences("oppo-carlink-identity",0).edit().clear().commit();c.getSharedPreferences("legal",0).edit().clear().commit();java.lang.reflect.Field f=VehicleCoordinator.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,null);}
  private VehicleSnapshot state(String lock,String air){return new VehicleSnapshot("Polestar 2","masked",64,270,115442,"空闲","全部关闭","全部关闭","已关闭",lock,System.currentTimeMillis(),"LIVE",false,"开启",air);}
  @Test public void officialCarInfoHasAllMandatoryFieldsAndNoRawVin(){
    OppoCarLinkConfig conf=new OppoCarLinkConfig(123,"test-issued-code","TEST-P2","Polestar 2","https://example.test/p2.png");assertTrue(conf.missing().isEmpty());
    OppoCarLinkModels.account(c,"test-account");CarInfo a=OppoCarLinkModels.info(c,conf,state("已上锁","关闭"),"PRIVATE-FIXTURE-VIN");
    assertNotNull(a.getOpenId());assertEquals("TEST-P2",a.getTypeCode());assertEquals("123",a.getCompanyId());assertEquals("https://example.test/p2.png",a.getImage());assertTrue(a.isCurrentCar());assertEquals("2",a.getPowerType());
    assertFalse(a.getCarId().contains("PRIVATE-FIXTURE"));assertNull(a.getVin());assertEquals(a.getCarId(),OppoCarLinkModels.info(c,conf,state("已上锁","关闭"),"PRIVATE-FIXTURE-VIN").getCarId());
    String first=a.getOpenId();OppoCarLinkModels.account(c,"another-account");assertNotEquals(first,OppoCarLinkModels.info(c,conf,state("已上锁","关闭"),"PRIVATE-FIXTURE-VIN").getOpenId());
  }
  @Test public void missingOfficialConfigIsExplicitAndNotReplacedWithDemoValues(){
    assertTrue(OppoCarLinkConfig.load(c).missing().size()>=3);
    OppoCarLinkConfig conf=new OppoCarLinkConfig(0,"PSASSISTANT_UNCONFIGURED","","Polestar 2","android.resource://local/car");assertEquals(4,conf.missing().size());
    assertFalse(OppoCarLinkConfig.httpsImage("https://user:secret@example.test/car.png"));
  }
  @Test public void reportedSocIsRatioAndSkillsFollowKnownState(){
    CarStatus s=OppoCarLinkModels.status(state("已上锁","关闭"),"fixture");assertEquals(.64f,s.getBatteryLeftPercent(),.0001f);assertEquals(270f,s.getBatteryEnduranceMileage(),0f);
    Map<String,Integer> skills=new HashMap<>();for(ControlInstruction i:s.getAvailableControlInstructions())skills.put(i.getInstruction(),i.getStatus());
    assertEquals(Integer.valueOf(0),skills.get("CAR-DOOR"));assertEquals(Integer.valueOf(1),skills.get("AC-TEMP"));assertFalse(skills.containsKey("CAR-ENGINE"));assertEquals(Integer.valueOf(1),skills.get("LOCK-CAR"));assertTrue(s.isAllDoorStatus());
    VehicleSnapshot unknown=new VehicleSnapshot("Polestar","masked",null,null,null,"未知",null,null,null,null,System.currentTimeMillis(),"LIVE",false,null,null);
    assertFalse(OppoCarLinkModels.canReport(unknown));assertTrue(OppoCarLinkModels.canReport(state("已上锁","关闭")));
    for(ControlInstruction i:OppoCarLinkModels.status(unknown,"fixture").getAvailableControlInstructions())assertTrue(Arrays.asList("CAR-HORN","DOUBLE-FLASH","CAR-HORN&&DOUBLE-FLASH").contains(i.getInstruction()));
  }
  @Test public void sdkCommandsNeverInferTemperatureOrExecuteResetValues(){
    assertEquals("climate-start",OppoCarLinkRuntime.actionForInstruction("AC_TEMP","OPEN"));
    assertEquals("climate-stop",OppoCarLinkRuntime.actionForInstruction("AC-TEMP","CLOSE"));
    for(String value:new String[]{"22","bad","",null})assertNull(OppoCarLinkRuntime.actionForInstruction("AC-TEMP",value));
    assertEquals("honk-flash",OppoCarLinkRuntime.actionForInstruction("CAR_HORN_AND_FLASH","OPEN"));
    assertNull(OppoCarLinkRuntime.actionForInstruction("CAR-HORN","CLOSE"));assertEquals("lock",OppoCarLinkRuntime.actionForInstruction("LOCK-CAR","OPEN"));assertNull(OppoCarLinkRuntime.actionForInstruction("LOCK-CAR","CLOSE"));assertNull(OppoCarLinkRuntime.actionForInstruction("CAR-ENGINE","OPEN"));
  }
  @Test public void serverAcceptanceAndTimeoutNeverBecomeVehicleSuccess(){
    for(OperationState r:OperationState.values())assertEquals(r==OperationState.VEHICLE_CONFIRMED?ControlMessage.CONTROL_SUCCESS:ControlMessage.CONTROL_FAILED,OppoCarLinkRuntime.messageCode(r));
  }
  @Test public void rejectedCoordinatorCommandCompletesOnceWithoutSending(){
    AtomicInteger calls=new AtomicInteger();VehicleCoordinator.get(c).command("unlock",result->{assertEquals(OperationState.NOT_SENT,result);calls.incrementAndGet();});assertEquals(1,calls.get());assertFalse(VehicleCoordinator.get(c).commandPending);
  }
  @Test public void rootLauncherUsesOnlyVerifiedFixedCarLinkComponent(){
    Bundle b=new Bundle();b.putString("bindActivityPackage",OppoOfficialCarLink.PACKAGE);b.putString("bindActivityName","com.heytap.opluscarlink.BindActivity");String command=OppoCarLinkRoot.launchCommand(b);assertTrue(command.contains("--user current"));assertTrue(command.contains("card://carlink.heytap.com/control?operation=bind"));assertFalse(command.contains("pm grant"));
    b.putString("bindActivityName","Bind'; touch /bad");try{OppoCarLinkRoot.launchCommand(b);fail();}catch(IllegalArgumentException expected){}
    b.putString("bindActivityPackage","com.other");try{OppoCarLinkRoot.launchCommand(b);fail();}catch(IllegalArgumentException expected){}
    assertEquals("'a'\\''b'",OppoCarLinkRoot.quote("a'b"));
  }
  @Test public void dexSelectionExcludesUnrelatedApps(){assertTrue(CarLinkDexContract.relevant("Lcom/heytap/opluscarlink/server/CarControlService;"));assertFalse(CarLinkDexContract.relevant("Lcom/other/AccountToken;"));}
  @Test public void repeatedSdkWritesAreBoundedWithoutBlockingOppositeDirection(){
    OppoCarLinkCommandWindow w=new OppoCarLinkCommandWindow();assertTrue(w.admit("car|lock",1000));assertFalse(w.admit("car|lock",5999));assertTrue(w.admit("car|unlock",1001));assertTrue(w.admit("car|lock",6000));assertFalse(w.admit("car|lock",10));
  }

  @Test public void corruptColdStartSelectionNeverFallsBackToPlaintextVin(){
    c.getSharedPreferences("vehicle_selection_secure",0).edit().putString("selection","PRIVATE-PLAINTEXT-VIN").commit();
    assertNull(io.github.polestarcn.companion.auth.SecureVehicleSelection.load(c,"wrong-hash"));
    assertFalse(c.getSharedPreferences("vehicle_selection_secure",0).contains("selection"));
  }

}
