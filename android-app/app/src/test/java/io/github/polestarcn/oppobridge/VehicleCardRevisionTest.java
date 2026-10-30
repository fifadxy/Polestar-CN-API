package io.github.polestarcn.oppobridge;
import static org.junit.Assert.*;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class VehicleCardRevisionTest {
  @Test public void serializationPreservesSourceTimeAndDoesNotInventFreshness() {
    Bundle state=new Bundle();state.putLong("capturedAt",123456L);state.putString("charging","未充电");
    assertEquals(123456L,VehicleDeviceCardProvider.deviceData(state,"test","02:00:00:00:00:01").optLong("revision"));
    assertEquals(123456L,VehicleDeviceCardProvider.deviceData(state,"test","02:00:00:00:00:01").optLong("timestamp"));
    state.putLong("capturedAt",0L);
    assertEquals(0L,VehicleDeviceCardProvider.deviceData(state,"test","02:00:00:00:00:01").optLong("revision"));
  }
}
