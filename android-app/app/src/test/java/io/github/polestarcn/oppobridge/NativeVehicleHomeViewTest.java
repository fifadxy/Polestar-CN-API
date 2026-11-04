package io.github.polestarcn.oppobridge;
import static org.junit.Assert.*;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class NativeVehicleHomeViewTest {
  private List<View> children(View v){List<View> out=new ArrayList<>();out.add(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)out.addAll(children(((ViewGroup)v).getChildAt(i)));return out;}
  @Test public void psAssistantDeviceIdSurvivesMyDevicesPackageAndTypeNormalization(){
    assertTrue(NativeVehicleHomeView.isOurCard("io.github.polestarcn.companion","CAR","psassistant_abc"));
    assertTrue(NativeVehicleHomeView.isOurCard("com.heytap.mydevices","UNKNOWN","psassistant_abc"));
    assertTrue(NativeVehicleHomeView.isOurCard("","3","psassistant_abc"));
    assertFalse(NativeVehicleHomeView.isOurCard("com.other.car","CAR","other_vehicle"));
    assertFalse(NativeVehicleHomeView.isOurCard("io.github.polestarcn.companion","WATCH","other_vehicle"));
    assertTrue(NativeVehicleHomeView.isOurCard("io.github.polestarcn.companion","CAR",null));
  }
  @Test public void nativeHomeShowsRealCacheAndAllFourDirectControls(){
    NativeVehicleHomeView view=new NativeVehicleHomeView(RuntimeEnvironment.getApplication());
    Bundle b=new Bundle();b.putBoolean("available",true);b.putBoolean("batteryPercentKnown",true);b.putInt("batteryPercent",64);b.putBoolean("rangeKmKnown",true);b.putInt("rangeKm",270);b.putString("lock","已上锁");b.putString("climate","开启");b.putString("air","关闭");b.putString("tailgate","已关闭");b.putLong("capturedAt",System.currentTimeMillis());view.bindSnapshot(b);
    List<String> labels=new ArrayList<>();StringBuilder text=new StringBuilder();
    for(View v:children(view)){if(v instanceof Button){labels.add(((Button)v).getText().toString());assertTrue(v.isEnabled());}if(v instanceof TextView)text.append(((TextView)v).getText());}
    assertEquals(6,labels.size());assertTrue(labels.contains("解锁"));assertTrue(labels.contains("关闭空调"));assertTrue(labels.contains("净化"));assertTrue(labels.contains("开启尾门"));assertTrue(labels.contains("刷新"));assertTrue(labels.contains("更多控制"));assertTrue(text.toString().contains("64%"));assertTrue(text.toString().contains("270 km"));assertTrue(text.toString().contains("缓存"));
    b.putBoolean("demo",true);view.bindSnapshot(b);
    for(View v:children(view))if(v instanceof Button){String label=((Button)v).getText().toString();if(!label.equals("刷新")&&!label.equals("更多控制"))assertFalse(v.isEnabled());}
  }
  @Test public void controlsFitTheOem203DpCardHeight(){
    NativeVehicleHomeView view=new NativeVehicleHomeView(RuntimeEnvironment.getApplication());
    Bundle b=new Bundle();b.putBoolean("available",true);b.putBoolean("batteryPercentKnown",true);b.putInt("batteryPercent",64);b.putBoolean("rangeKmKnown",true);b.putInt("rangeKm",270);b.putLong("capturedAt",System.currentTimeMillis());view.bindSnapshot(b);
    float density=view.getResources().getDisplayMetrics().density;
    view.measure(View.MeasureSpec.makeMeasureSpec(Math.round(160*density),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
    StringBuilder sizes=new StringBuilder();for(View v:children(view))sizes.append(v.getClass().getSimpleName()).append(":"+v.getMeasuredWidth()+"x"+v.getMeasuredHeight()).append(v instanceof TextView?((TextView)v).getText()+" font="+((TextView)v).getTextSize()+" line="+((TextView)v).getLineHeight():"").append(";");
    assertTrue("Native controls exceed OEM card height: "+view.getMeasuredHeight()+" "+sizes,view.getMeasuredHeight()<=Math.round(203*density));
  }
  @Test public void staleControlsAreDisabledButRefreshIsAvailable(){
    NativeVehicleHomeView view=new NativeVehicleHomeView(RuntimeEnvironment.getApplication());
    Bundle b=new Bundle();b.putBoolean("available",true);
    b.putLong("capturedAt",System.currentTimeMillis()-120*60_000L);
    b.putString("lock","已上锁");b.putString("climate","关闭");
    b.putString("air","关闭");b.putString("tailgate","已关闭");
    view.bindSnapshot(b);
    int disabled=0;boolean refreshEnabled=false;
    for(View v:children(view))if(v instanceof Button){
      String label=((Button)v).getText().toString();
      if("刷新".equals(label))refreshEnabled=v.isEnabled();
      else if(!"更多控制".equals(label)){assertFalse(v.isEnabled());disabled++;}
    }
    assertEquals(4,disabled);assertTrue(refreshEnabled);
  }

  @Test public void mainAppUnlockedValueKeepsLockControlAvailable(){
    NativeVehicleHomeView view=new NativeVehicleHomeView(RuntimeEnvironment.getApplication());
    Bundle b=new Bundle();b.putBoolean("available",true);b.putLong("capturedAt",System.currentTimeMillis());b.putString("lock","未锁");view.bindSnapshot(b);
    boolean found=false;for(View child:children(view))if(child instanceof Button&&"上锁".equals(((Button)child).getText().toString())){found=true;assertTrue(child.isEnabled());}
    assertTrue(found);
  }
  @Test public void missingValuesNeverBecomeZero(){
    NativeVehicleHomeView view=new NativeVehicleHomeView(RuntimeEnvironment.getApplication());StringBuilder text=new StringBuilder();for(View v:children(view))if(v instanceof TextView)text.append(((TextView)v).getText());assertFalse(text.toString().contains("0%"));assertFalse(text.toString().contains("0 km"));assertTrue(text.toString().contains("时间未知"));
  }
}
