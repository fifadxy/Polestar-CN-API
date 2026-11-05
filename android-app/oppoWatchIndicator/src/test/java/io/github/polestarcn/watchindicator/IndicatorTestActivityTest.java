package io.github.polestarcn.watchindicator;

import android.app.Notification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
public class IndicatorTestActivityTest {
  @Test public void watchAppUsesOfficialIndicatorContract(){
    IndicatorTestActivity activity=Robolectric.buildActivity(IndicatorTestActivity.class).setup().get();
    for(String category:new String[]{"navigation","workout"}){
      Notification notification=IndicatorTestActivity.create(activity,category);
      assertEquals(category,notification.category);
      assertTrue(notification.extras.getBoolean("show_heytap_indicator"));
      assertTrue((notification.flags&Notification.FLAG_ONGOING_EVENT)!=0);
      assertEquals(90_000L,notification.getTimeoutAfter());
      assertNotNull(notification.contentIntent);
    }
  }
}
