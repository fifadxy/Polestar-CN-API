package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class Beta49HealthCheckTest {
  private VehicleSnapshot car(){
    return new VehicleSnapshot("Polestar 2","test",88,370,22000,"空闲",
        "全部关闭","全部关闭","已关闭","已上锁",System.currentTimeMillis(),
        "LIVE",false,"关闭","关闭");
  }
  private VehicleSnapshot washer(){
    return car().withHealth(new VehicleHealthSnapshot(
        VehicleHealthSnapshot.OK,VehicleHealthSnapshot.WARNING,
        VehicleHealthSnapshot.OK,System.currentTimeMillis()));
  }
  @Test public void allGoodWarningNamesActualReportedIssue(){
    Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppPreferences.set(c,"language","zh");
    VehicleSnapshot warning=washer();
    assertTrue(VehicleCondition.problem(warning,true));
    assertEquals("请检查 玻璃水",ConditionStyle.message(c,true,warning,true));
    assertEquals("请检查 玻璃水",VehicleCondition.summary(c,warning,true));
    assertTrue(ConditionStyle.summary(c,ConditionStyle.message(c,true,warning,true)));
    assertEquals("All Good！",VehicleCondition.summary(c,car().withHealth(new VehicleHealthSnapshot(
        VehicleHealthSnapshot.OK,VehicleHealthSnapshot.OK,VehicleHealthSnapshot.OK,System.currentTimeMillis())),true));
    VehicleSnapshot battery=car().withHealth(new VehicleHealthSnapshot(VehicleHealthSnapshot.WARNING,
        VehicleHealthSnapshot.WARNING,VehicleHealthSnapshot.OK,System.currentTimeMillis()));
    assertEquals("请检查 12V 蓄电池",VehicleCondition.summary(c,battery,true));
  }
  @Test public void customWarningCopyCanOverrideButUnknownDataIsNotAnIssue(){
    Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppPreferences.set(c,"language","zh");
    assertFalse(VehicleCondition.problem(car(),true));
    assertEquals("请检查车辆",ConditionStyle.message(c,true,car(),true));
    AppPreferences.set(c,"condition_problem_text","需要检修");
    assertEquals("需要检修",ConditionStyle.message(c,true,washer(),true));
    AppPreferences.set(c,"condition_problem_text","请检查车辆");
    assertEquals("请检查 玻璃水",ConditionStyle.message(c,true,washer(),true));
  }
  @Test public void issueTypographyNeverExceedsNavigationFourteenSp(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    TextView view=new TextView(c);view.setText("请检查 玻璃水");view.setTextSize(38);
    ConditionStyle.apply(view);
    float actualSp=view.getTextSize()/c.getResources().getDisplayMetrics().scaledDensity;
    assertTrue("Actual "+actualSp,actualSp<=14.1f&&actualSp>=9f);
    assertEquals(1,view.getMaxLines());
  }
  @Test public void widgetCheckIsOnByDefaultAndCanBeDisabled(){
    Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppPreferences.set(c,"language","zh");
    assertTrue(WidgetCheckHint.visible(c,washer()));
    assertFalse(WidgetCheckHint.visible(c,car()));
    assertTrue(SettingsBackup.allowed(WidgetCheckHint.PREF));
    AppPreferences.set(c,WidgetCheckHint.PREF,"off");assertFalse(WidgetCheckHint.visible(c,washer()));
    AppPreferences.set(c,WidgetCheckHint.PREF,"on");assertTrue(WidgetCheckHint.visible(c,washer()));
  }
  @Test public void bothOemWidgetLayoutsProvideMatchingWarningIconAndLabel(){
    Context c=RuntimeEnvironment.getApplication();
    for(int layoutId:new int[]{R.layout.widget_native_summary,R.layout.widget_summary_content}){
      View root=android.view.LayoutInflater.from(c).inflate(layoutId,new FrameLayout(c),false);
      View icon=root.findViewById(R.id.widget_check_icon),label=root.findViewById(R.id.widget_check_text);
      assertNotNull(icon);assertNotNull(label);
      assertEquals(View.GONE,icon.getVisibility());
      assertEquals(View.GONE,label.getVisibility());
      assertEquals("Check",((TextView)label).getText().toString());
    }
  }
  @Test public void compactWidgetTimestampKeepsActualTimeAndAvoidsHiddenCount(){
    java.util.Calendar today=java.util.Calendar.getInstance();
    String md=String.format(java.util.Locale.US,"%02d-%02d",today.get(java.util.Calendar.MONTH)+1,today.get(java.util.Calendar.DAY_OF_MONTH));
    assertEquals("05:18",WidgetRenderer.compactCheckTimestamp("Upd "+md+" 05:18 · +2",today));
    assertEquals("05:18",WidgetRenderer.compactCheckTimestamp("更新 "+md+" 05:18",today));
    assertEquals("--",WidgetRenderer.compactCheckTimestamp("更新时间未知",today));
    assertEquals("04-19",WidgetRenderer.compactCheckTimestamp("Upd 04-19 09:30 · +2",today));
  }
  @Test public void confirmedCommandLabelsAreSpecificAndNeverClaimUnverifiedHorns(){
    Context c=RuntimeEnvironment.getApplication();
    AppPreferences.set(c,"language","zh");
    assertEquals("已上锁",CommandResultLabel.confirmed(c,"lock"));
    assertEquals("净化已开启",CommandResultLabel.confirmed(c,"air-start"));
    assertEquals("尾门已关闭",CommandResultLabel.confirmed(c,"trunk-close"));
    assertEquals("执行成功",CommandResultLabel.confirmed(c,"unsupported"));
  }
}
