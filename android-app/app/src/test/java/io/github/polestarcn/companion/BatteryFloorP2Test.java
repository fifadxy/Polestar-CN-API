package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class BatteryFloorP2Test {
  @Test public void defaultIsOffAndOnlyShowsValidObservedSoc(){
    Context c=RuntimeEnvironment.getApplication();BatteryFloor.reset(c);
    BatteryFloor.Settings floor=new BatteryFloor.Settings(c);
    assertFalse(floor.enabled);assertFalse(floor.idle);assertTrue(floor.status);
    assertFalse(floor.visible(true,81));
    AppPreferences.set(c,BatteryFloor.PREFIX+"enabled","on");floor=new BatteryFloor.Settings(c);
    assertTrue(floor.visible(true,81));assertFalse(floor.visible(false,81));
    assertTrue("Charging floor remains visible as an empty guide when SOC is unknown",floor.visible(true,null));
    assertTrue(floor.visible(true,-1));assertFalse(floor.visible(false,null));
    AppPreferences.set(c,BatteryFloor.PREFIX+"idle","on");
    assertTrue(new BatteryFloor.Settings(c).visible(false,81));
  }
  @Test public void eachCornerPersistsIndependentlyAndNeverInvertsTheFloor(){
    Context c=RuntimeEnvironment.getApplication();BatteryFloor.reset(c);
    AppPreferences.set(c,BatteryFloor.corner(0,"x"),"-25");
    AppPreferences.set(c,BatteryFloor.corner(1,"x"),"25");
    AppPreferences.set(c,BatteryFloor.corner(2,"y"),"-25");
    AppPreferences.set(c,BatteryFloor.corner(3,"y"),"25");
    BatteryFloor.Settings settings=new BatteryFloor.Settings(c);
    float[][] p=BatteryFloor.geometry(new RectF(20,12,320,220),settings,240,1f);
    assertTrue(p[0][0]<p[1][0]);assertTrue(p[2][0]<p[3][0]);
    assertTrue(Math.min(p[2][1],p[3][1])>Math.max(p[0][1],p[1][1]));
    assertEquals(-25,settings.x[0]);assertEquals(25,settings.x[1]);
    assertEquals(-25,settings.y[2]);assertEquals(25,settings.y[3]);
    BatteryFloor.reset(c);assertEquals(0,new BatteryFloor.Settings(c).x[0]);
  }
  @Test public void presetAndCustomColorAreValidated(){
    Context c=RuntimeEnvironment.getApplication();BatteryFloor.reset(c);
    assertEquals(Color.rgb(255,107,0),new BatteryFloor.Settings(c).color);
    AppPreferences.set(c,BatteryFloor.PREFIX+"color_preset","green");
    assertEquals(BatteryFloor.parseColor(BatteryFloor.GREEN),new BatteryFloor.Settings(c).color);
    AppPreferences.set(c,BatteryFloor.PREFIX+"custom_color","#1234AB");
    AppPreferences.set(c,BatteryFloor.PREFIX+"color_preset","custom");
    assertEquals(Color.rgb(0x12,0x34,0xAB),new BatteryFloor.Settings(c).color);
    AppPreferences.set(c,BatteryFloor.PREFIX+"custom_color","not a color");
    assertEquals(Color.rgb(255,107,0),new BatteryFloor.Settings(c).color);
    BatteryFloor.reset(c);
  }
  @Test public void actualDashboardSnapshotReachesFloorWithoutCoordinator(){
    Context c=RuntimeEnvironment.getApplication();BatteryFloor.reset(c);
    AppPreferences.set(c,BatteryFloor.PREFIX+"enabled","on");
    VehicleHeroView car=new VehicleHeroView(c);
    car.refreshFloorSettings();
    long now=System.currentTimeMillis();
    VehicleSnapshot charging=new VehicleSnapshot("car","masked",73,200,10000,
        "充电中","全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"关闭","关闭");
    car.setSnapshot(charging);
    assertTrue("Snapshot delivered by MainActivity must actually control the floor",car.hasFloor());
    VehicleSnapshot notCharging=new VehicleSnapshot("car","masked",73,200,10000,
        "空闲","全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"关闭","关闭");
    car.setSnapshot(notCharging);assertFalse(car.hasFloor());
    AppPreferences.set(c,BatteryFloor.PREFIX+"idle","on");car.refreshFloorSettings();
    assertTrue(car.hasFloor());
    BatteryFloor.reset(c);
  }
  @Test public void settingsPreviewIsPinnedAboveAnIndependentScrollView(){
    org.robolectric.android.controller.ActivityController<BatteryFloorSettingsActivity> controller=
        org.robolectric.Robolectric.buildActivity(BatteryFloorSettingsActivity.class).setup();
    BatteryFloorSettingsActivity activity=controller.get();
    android.view.ViewGroup screen=activity.findViewById(android.R.id.content);
    android.view.View pinned=screen.findViewWithTag("battery_floor_pinned_preview");
    android.widget.ScrollView scroller=screen.findViewWithTag("battery_floor_settings_scroll");
    View preview=screen.findViewWithTag("battery_floor_preview");
    assertNotNull(pinned);assertNotNull(scroller);assertNotNull(preview);
    assertEquals("Pinned preview and scroll must be siblings",pinned.getParent(),scroller.getParent());
    assertTrue("Preview belongs to fixed pane",isDescendant(preview,pinned));
    assertFalse("Preview must not scroll away with settings",isDescendant(preview,scroller));
    androidx.appcompat.widget.SwitchCompat enable=scroller.findViewWithTag(BatteryFloor.PREFIX+"enabled");
    assertNotNull(enable);enable.setChecked(true);
    assertTrue(new BatteryFloor.Settings(activity).enabled);
    android.widget.Button green=scroller.findViewWithTag(BatteryFloor.PREFIX+"color_preset_1");
    assertNotNull(green);green.performClick();
    assertEquals(BatteryFloor.parseColor(BatteryFloor.GREEN),new BatteryFloor.Settings(activity).color);
    controller.pause().stop().destroy();
  }
  private boolean isDescendant(android.view.View child,android.view.View ancestor){
    android.view.ViewParent p=child.getParent();
    while(p!=null){if(p==ancestor)return true;p=p.getParent();}
    return false;
  }

}
