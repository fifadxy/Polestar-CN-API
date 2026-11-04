package io.github.polestarcn.oppobridge;

import android.content.ComponentName;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class CarLinkServiceEntryProbeTest {
  private static final String HOST="com.heytap.opluscarlink";
  static class BindData { private final Object token=new Object(); }
  static class ThreadState { private final Map<Object,Object> mServices=new HashMap<>(); }
  @Test public void sdkPackageIntentResolvesTheActuallyBoundService(){
    Intent request=new Intent("oppo.intent.action.opluscarlink.OPLUS_SDK_SERVICE").setPackage(HOST);
    BindData data=new BindData();ThreadState thread=new ThreadState();thread.mServices.put(data.token,this);
    assertTrue(CarLinkServiceEntryProbe.accepts(request));
    assertEquals(getClass().getName(),CarLinkServiceEntryProbe.serviceName(request,thread,data));
    assertEquals("implicit",CarLinkServiceEntryProbe.serviceName(request,new Object(),data));
  }
  @Test public void explicitComponentTakesPrecedenceOverPackage(){
    Intent request=new Intent().setComponent(new ComponentName(HOST,HOST+".ControlService"));
    assertTrue(CarLinkServiceEntryProbe.accepts(request));
    assertEquals(HOST+".ControlService",CarLinkServiceEntryProbe.serviceName(request,null,null));
    request.setPackage(HOST).setComponent(new ComponentName("com.other","com.other.Service"));
    assertFalse(CarLinkServiceEntryProbe.accepts(request));
    assertFalse(CarLinkServiceEntryProbe.accepts(new Intent().setPackage("com.other")));
    assertFalse(CarLinkServiceEntryProbe.accepts(new Intent()));assertFalse(CarLinkServiceEntryProbe.accepts(null));
  }
}
