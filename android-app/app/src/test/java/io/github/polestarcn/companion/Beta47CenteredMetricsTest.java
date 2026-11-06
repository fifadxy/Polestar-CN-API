package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class Beta47CenteredMetricsTest {
 @Test public void inactiveAirHasRoomForBothSensorLines(){
   Context c=RuntimeEnvironment.getApplication();
   CenteredStatusButton button=new CenteredStatusButton(c);
   button.setText("净化");
   button.contents("PM2.5·12.5\nAQI·43",null);
   assertEquals(3,button.getMaxLines());
   String shown=button.getText().toString();
   assertTrue(shown.contains("PM2.5·12.5"));
   assertTrue(shown.contains("AQI·43"));
   assertFalse(shown.contains("已关闭"));
 }
 @Test public void shortStatusesRemainTwoLines(){
   CenteredStatusButton button=new CenteredStatusButton(RuntimeEnvironment.getApplication());
   button.setText("空调");
   button.contents("运行中",null);
   assertEquals(2,button.getMaxLines());
 }
}
