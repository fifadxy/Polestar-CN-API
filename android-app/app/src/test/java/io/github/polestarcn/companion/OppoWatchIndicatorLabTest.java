package io.github.polestarcn.companion;

import android.app.Notification;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
public final class OppoWatchIndicatorLabTest {
  @Test public void labIsOffByDefaultAndNeverChangesServiceCategory(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    assertFalse(OppoWatchIndicatorLab.enabled(c));
    Notification.Builder original=new Notification.Builder(c,"mock")
        .setSmallIcon(R.drawable.ic_notification).setCategory(Notification.CATEGORY_SERVICE).setOngoing(true);
    Notification n=OppoWatchIndicatorLab.decorateResident(c,original).build();
    assertEquals(Notification.CATEGORY_SERVICE,n.category);
    assertFalse(n.extras.getBoolean("show_heytap_indicator"));
    AppPreferences.set(c,OppoWatchIndicatorLab.PREF_ENABLED,"on");
    AppPreferences.set(c,OppoWatchIndicatorLab.PREF_RESIDENT,"on");
    Notification m=OppoWatchIndicatorLab.decorateResident(c,new Notification.Builder(c,"mock")
        .setSmallIcon(R.drawable.ic_notification).setCategory(Notification.CATEGORY_SERVICE).setOngoing(true)).build();
    assertEquals(Notification.CATEGORY_SERVICE,m.category);
    assertTrue(m.extras.getBoolean("show_heytap_indicator"));
  }
  @Test public void syntheticTestsHaveOfficialFieldsAndAutoExpire(){
    Context c=RuntimeEnvironment.getApplication();
    for(String cat:new String[]{"navigation","workout"}){
      Notification n=OppoWatchIndicatorLab.buildTest(c,cat);
      assertEquals(cat,n.category);
      assertTrue(n.extras.getBoolean("show_heytap_indicator"));
      assertTrue((n.flags&Notification.FLAG_ONGOING_EVENT)!=0);
      assertEquals(OppoWatchIndicatorLab.TEST_WINDOW_MS,n.getTimeoutAfter());
      assertNotNull(n.contentIntent);
    }
  }
}
