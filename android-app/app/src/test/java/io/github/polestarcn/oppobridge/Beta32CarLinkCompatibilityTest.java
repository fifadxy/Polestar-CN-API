package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.database.MatrixCursor;
import android.os.Bundle;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public final class Beta32CarLinkCompatibilityTest {
  public static final class FakeSession {
    public String vin;public String carId;public String connectState;public List<String> supportFeatures;
    public String lockState;public String climateState;public int soc;public int rangeKm;public long lastUpdateTime;
  }
  @After public void cleanup(){VirtualCarLinkSessionStore.clear();}

  @Test public void cursorOverlayCopiesRowsAndAppendsPolestar(){
    MatrixCursor raw=new MatrixCursor(new String[]{"provider_code","model_id","device_type","car_id"});
    raw.addRow(new Object[]{"BYD","BYD_X","CAR","byd-1"});
    Bundle p=new Bundle();p.putString("providerCode","POLESTAR");p.putString("modelId","POLESTAR_2");p.putString("deviceType","CAR");p.putString("virtualCarId","psassistant_1");
    MatrixCursor out=VehicleCatalogCursorShim.appendPolestarRow(raw,p);assertEquals(2,out.getCount());assertTrue(out.moveToLast());
    assertEquals("POLESTAR",out.getString(out.getColumnIndex("provider_code")));assertEquals("POLESTAR_2",out.getString(out.getColumnIndex("model_id")));
  }

  @Test public void reflectedSessionIsStableAndDynamicallyUpdated() throws Exception {
    Bundle p=new Bundle();p.putString("testVin","TST0123456789ABCD");p.putString("virtualCarId","psassistant_1");p.putString("connectState","COORDINATED");
    p.putString("lock","LOCKED");p.putString("climate","INACTIVE");p.putBoolean("batteryPercentKnown",true);p.putInt("batteryPercent",55);
    p.putBoolean("rangeKmKnown",true);p.putInt("rangeKm",230);p.putStringArray("capabilities",new String[]{"LOCK","UNLOCK","SOC"});
    Object first=VirtualCarLinkSessionStore.getOrCreate(getClass().getClassLoader(),FakeSession.class,p);
    Object second=VirtualCarLinkSessionStore.getOrCreate(getClass().getClassLoader(),FakeSession.class,p);
    assertSame(first,second);FakeSession session=(FakeSession)first;
    assertEquals("TST0123456789ABCD",session.vin);assertEquals("COORDINATED",session.connectState);assertEquals(55,session.soc);assertTrue(session.supportFeatures.contains("LOCK"));
    VirtualCarLinkSessionStore.updateVehicleState("UNLOCKED","ACTIVE");assertEquals("UNLOCKED",session.lockState);assertEquals("ACTIVE",session.climateState);
  }
}
