package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import io.github.polestarcn.oppobridge.OppoCardSummary;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class Beta49OppoCardDisplayTest {
  private Bundle car(){
    Bundle s=new Bundle();
    s.putBoolean("available",true);s.putString("name","Polestar 2");
    s.putBoolean("batteryPercentKnown",true);s.putInt("batteryPercent",88);
    s.putBoolean("rangeKmKnown",true);s.putInt("rangeKm",370);
    s.putBoolean("odometerKmKnown",true);s.putInt("odometerKm",25100);
    s.putString("lock","已锁");s.putString("climate","关闭");
    s.putString("air","关闭");s.putString("charging","充电中");
    s.putLong("capturedAt",1762420800000L);return s;
  }
  @Test public void defaultIsExactlyBatteryAndRangeWithoutCacheOrExtraName(){
    String rendered=OppoCardSummary.format(car());
    assertEquals("88% · 370KM",rendered);
    assertFalse(rendered.contains("缓存"));assertFalse(rendered.contains("Polestar"));
    assertFalse(rendered.contains("内容"));assertFalse(rendered.contains("..."));
  }
  @Test public void missingMetricsAreOmittedWithoutInventingZeros(){
    Bundle s=car();
    s.putBoolean("rangeKmKnown",false);assertEquals("88%",OppoCardSummary.format(s));
    s.putBoolean("batteryPercentKnown",false);assertEquals("车况未更新",OppoCardSummary.format(s));
    s.putBoolean("rangeKmKnown",true);s.putInt("rangeKm",-1);
    assertEquals("车况未更新",OppoCardSummary.format(s));
    s.putInt("rangeKm",0);assertEquals("0KM",OppoCardSummary.format(s));
    s.putBoolean("batteryPercentKnown",true);s.putInt("batteryPercent",0);
    assertEquals("0% · 0KM",OppoCardSummary.format(s));
  }
  @Test public void fieldsAndSeparatorsCanBeCustomizedWithoutUpdatingTelemetry(){
    Bundle s=car();
    assertEquals("88% / 370KM",OppoCardSummary.format(s,"{soc}{sep}{range}"," / "));
    assertEquals("电量88% | 续航370KM",OppoCardSummary.format(s,"电量{soc}{sep}续航{range}"," | "));
    assertEquals("25100KM",OppoCardSummary.format(s,"{odometer}",""));
    assertEquals("已锁",OppoCardSummary.format(s,"{lock}",""));
    assertEquals("充电中",OppoCardSummary.format(s,"{charging}",""));
    assertEquals("关闭",OppoCardSummary.format(s,"{climate}",""));
    assertEquals("缓存",OppoCardSummary.format(s,"{source}",""));
    assertTrue(OppoCardSummary.format(s,"{updated}","").startsWith("更新 "));
    assertEquals("88% · 我的极星",OppoCardSummary.format(s,"{soc}{sep}我的极星"," · "));
    assertEquals("88%370KM",OppoCardSummary.format(s,"{soc}{sep}{range}",""));
  }
  @Test public void longTemplatesNeverAddEllipsisOrBreakUnicode(){
    String copy="我的极星车辆欢迎使用我的全新高端定制信息显示卡片以及扩展状态阅读";
    String result=OppoCardSummary.format(car(),copy," · ");
    assertTrue(OppoCardSummary.units(result)<=OppoCardSummary.MAX_UNITS);
    assertFalse(result.contains("..."));assertFalse(result.contains("\ufffd"));
    assertEquals("88%",OppoCardSummary.format(car(),"{soc}{sep}"+copy," · "));
  }
  @Test public void sensitiveOrUnknownTokensAreNotPermitted(){
    for(String input:new String[]{"{vin}","{password}","{token}","{unknown}","{soc}\n{range}","{soc","{SOC}"}){
      try{OppoCardSummary.checkTemplate(input);fail(input);}catch(IllegalArgumentException expected){}
    }
    assertEquals(OppoCardSummary.DEFAULT_TEMPLATE,OppoCardSummary.safeTemplate("{vin}"));
    assertEquals(OppoCardSummary.DEFAULT_SEPARATOR,OppoCardSummary.safeSeparator("\n"));
  }
  @Test public void settingsAreBackedUpAndResetWithoutLoginOrCarChanges() throws Exception{
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    OppoCardSummarySettings.save(c,"{range}{sep}{updated}"," / ");
    assertTrue(SettingsBackup.allowed(OppoCardSummarySettings.TEMPLATE));
    assertTrue(SettingsBackup.allowed(OppoCardSummarySettings.SEPARATOR));
    org.json.JSONObject saved=SettingsBackup.export(c);
    OppoCardSummarySettings.reset(c);
    assertEquals(OppoCardSummary.DEFAULT_TEMPLATE,OppoCardSummarySettings.template(c));
    SettingsBackup.restore(c,saved.getJSONObject("values"));
    Bundle s=car();OppoCardSummarySettings.write(s,c);
    assertTrue(OppoCardSummary.format(s).startsWith("370KM / 更新 "));
  }
  @Test public void editingChangesBothPreviewsBeforeSaving(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    OppoCardDisplaySettingsActivity activity=Robolectric.buildActivity(OppoCardDisplaySettingsActivity.class).setup().get();
    EditText tpl=activity.findViewWithTag("oppo_card_template_editor");
    EditText sep=activity.findViewWithTag("oppo_card_separator_editor");
    TextView device=activity.findViewWithTag("oppo_card_preview_device");
    TextView shelf=activity.findViewWithTag("oppo_card_preview_shelf");
    assertNotNull(tpl);assertNotNull(sep);assertNotNull(device);assertNotNull(shelf);
    tpl.setText("{range}{sep}{soc}");sep.setText(" / ");
    assertEquals(device.getText().toString(),shelf.getText().toString());
    assertFalse(device.getText().toString().contains("{"));
    assertEquals(OppoCardSummary.DEFAULT_TEMPLATE,OppoCardSummarySettings.template(c));
    activity.finish();
  }
}
