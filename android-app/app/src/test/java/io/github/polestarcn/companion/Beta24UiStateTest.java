package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class Beta24UiStateTest {
  private Context c(){return RuntimeEnvironment.getApplication();}
  private VehicleSnapshot state(String lock,String windows,String climate,String air){
    return new VehicleSnapshot("Polestar 2","masked",51,210,12000,"空闲","全部关闭",windows,"已关闭",lock,
        System.currentTimeMillis(),"LIVE",false,climate,air);
  }

  @Test public void minimalModeCanToggleWithoutChangingVehicleState(){
    Context c=c();UiDisplayMode.setMinimal(c,false);assertFalse(UiDisplayMode.minimal(c));
    assertTrue(UiDisplayMode.toggleMinimal(c));assertTrue(UiDisplayMode.minimal(c));
    assertFalse(UiDisplayMode.toggleMinimal(c));assertFalse(UiDisplayMode.minimal(c));
  }

  @Test public void compactControlWordingMatchesBeta24Rules(){
    Context c=c();VehicleSnapshot on=state("已上锁","全部关闭","开启","开启");
    assertEquals("已锁",ControlStatusText.value(c,"lock",on,""));
    assertEquals("已关",ControlStatusText.value(c,"windows",on,""));
    assertEquals("运行中",ControlStatusText.value(c,"climate",on,""));
    assertEquals("运行中",ControlStatusText.value(c,"air",on,""));
    VehicleSnapshot off=state("未锁","未关严","关闭","关闭");
    assertEquals("未锁",ControlStatusText.value(c,"lock",off,""));
    assertEquals("未关",ControlStatusText.value(c,"windows",off,""));
    assertEquals("未运行",ControlStatusText.value(c,"climate",off,""));
    assertEquals("未运行",ControlStatusText.value(c,"air",off,""));
  }

  @Test public void vehicleArtIsScopedPerSkinAndMigratesLegacySelection(){
    Context c=c();AppPreferences.set(c,"theme","light");
    AppPreferences.set(c,"vehicle_art_day","top_white");
    UiSkin.set(c,UiSkin.PS_DEFAULT);assertEquals("top_white",VehicleArt.selection(c,false));
    AppPreferences.set(c,VehicleArt.selectionKey(c,false),"side_official_space");
    UiSkin.set(c,UiSkin.VOLIKE_POLESTAR);assertEquals("top_white",VehicleArt.selection(c,false));
    AppPreferences.set(c,VehicleArt.selectionKey(c,false),"angled_white");
    UiSkin.set(c,UiSkin.PS_DEFAULT);assertEquals("side_official_space",VehicleArt.selection(c,false));
    UiSkin.set(c,UiSkin.VOLIKE_POLESTAR);assertEquals("angled_white",VehicleArt.selection(c,false));
  }
}