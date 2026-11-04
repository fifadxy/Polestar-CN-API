package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import com.oplus.ocs.carlink.*;
import com.oplus.ocs.carlink.control.*;
import com.oplus.ocs.carlink.model.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public final class Beta34MockOcsTest {
  @Test public void mockOcsCompletesInitAuthSubscribeBindAndControlContract() throws Exception {
    if(!BuildConfig.OPPO_SDK_MOCK)return;
    Context context=RuntimeEnvironment.getApplication();
    AtomicInteger auth=new AtomicInteger(),sub=new AtomicInteger(),controls=new AtomicInteger();
    final String carId="psa_mock_test";
    ICarControlCallback callback=new ICarControlCallback(){
      @Override public void onAuthResult(int code){auth.set(code);}
      @Override public void onSubscribeResult(int code){sub.set(code);}
      @Override public void inquireCarInfo(){
        CarInfo info=new CarInfo("mock-open",carId,"Polestar 2 TestLab",true);
        info.setCompanyId("990001");info.setTypeCode("POLESTAR_2_TESTLAB");
        info.setTypeName("Polestar 2 TestLab");info.setImage("https://test.invalid/p2.png");info.setPowerType("2");
        CarControl.sendCarInfo(info);
      }
      @Override public void inquireCarStatus(){
        CarStatus status=new CarStatus(carId);status.setTimestamp(System.currentTimeMillis());status.setActive(true);
        status.setBatteryLeftPercent(.66f);status.setBatteryEnduranceMileage(280f);CarControl.sendCarStatus(status);
      }
      @Override public ControlTask onControl(ControlInstruction instruction){
        controls.incrementAndGet();return new ControlTask("mock-task",60);
      }
    };
    CarControl.registerCallback(callback);
    CarLink.init(context,CarLink.FLAG_CAR_CONTROL,new ICallback(){},false);
    assertTrue(CarLink.isCarControlInitComplete());
    CarControl.auth();assertEquals(1,auth.get());
    CarControl.subscribe();assertEquals(1,sub.get());
    assertTrue(CarControl.supportCarControl());assertTrue(CarControl.hasCarBinding());assertTrue(CarControl.checkCarBind(carId));

    Class<?> server=Class.forName("com.oplus.ocs.carlink.mock.MockCarLinkServer");
    Object task=server.getMethod("issueControl",String.class,String.class,String.class)
        .invoke(null,carId,"LOCK-CAR","OPEN");
    assertNotNull(task);assertEquals(1,controls.get());
    android.os.Bundle status=(android.os.Bundle)server.getMethod("status").invoke(null);
    assertEquals(990001,status.getInt("companyId"));assertEquals("TESTLAB_MOCK_ONLY",status.getString("authCodeLabel"));
    assertTrue(status.getBoolean("bound"));
  }

  @Test public void mockVehicleCommandsRemainLocalDryRun() {
    if(!BuildConfig.OPPO_SDK_MOCK)return;
    OppoCarLinkMockFixture.reset();
    assertEquals("已锁",OppoCarLinkMockFixture.snapshot().lock);
    OppoCarLinkMockFixture.apply("unlock");assertEquals("未锁",OppoCarLinkMockFixture.snapshot().lock);
    OppoCarLinkMockFixture.apply("climate-start");assertEquals("开启",OppoCarLinkMockFixture.snapshot().climate);
    OppoCarLinkMockFixture.apply("trunk-open");assertEquals("已打开",OppoCarLinkMockFixture.snapshot().tailgate);
  }
}
