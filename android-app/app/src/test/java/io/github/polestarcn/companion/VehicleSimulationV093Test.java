package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class VehicleSimulationV093Test {
  private int switches(View v){
    int found=v instanceof Switch?1:0;
    if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;
      for(int i=0;i<group.getChildCount();i++)found+=switches(group.getChildAt(i));}
    return found;
  }
  @Test public void offlineAllScenarioScreenNeverCreatesRealCoordinator()throws Exception{
    Field instance=VehicleCoordinator.class.getDeclaredField("instance");
    instance.setAccessible(true);instance.set(null,null);
    ActivityController<VehicleSimulationActivity> controller=Robolectric.buildActivity(VehicleSimulationActivity.class).setup();
    VehicleSimulationActivity activity=controller.get();
    View root=activity.getWindow().getDecorView();
    assertTrue("switches for charge, HVAC, door, lock, trunk, windows, camp, warnings and location",
        switches(root)>=12);
    assertNull("simulation cannot initialize real vehicle coordinator",instance.get(null));
    controller.pause().stop().destroy();
  }
}
