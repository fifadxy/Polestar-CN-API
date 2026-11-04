package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.RectF;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class Beta37AppearanceTest {
 private Context c;
 @Before public void start(){c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();}
 @Test public void expandedRangesAndSmoothEntryPreserveSavedValues(){AppPreferences.set(c,"ui_icon_percent","250");AppPreferences.set(c,"ui_button_text_percent","220");AppPreferences.set(c,UiTypography.key(c),"200");AppPreferences.set(c,"fx_charge_field_range","800");assertEquals(250,ButtonAppearance.iconPercent(c));assertEquals(220,ButtonAppearance.textPercent(c));assertEquals(200,UiTypography.percent(c,UiTypography.key(c)));assertEquals(800,new EffectSettings(c).chargeField.range);assertEquals(0f,EffectSettings.entryAlpha(0,100),0f);assertEquals(1f,EffectSettings.entryAlpha(.5f,100),0f);assertTrue(EffectSettings.entryAlpha(.2f,100)<EffectSettings.entryAlpha(.3f,100));}
 @Test public void distributedOriginsDoNotShareOneRectangularBoundary(){ParticleField field=new EffectSettings(c).chargeField;RectF bounds=new RectF(0,0,100,100);float[] a=new float[2],b=new float[2];field.start(bounds,0,.1f,.5f,a);field.start(bounds,1,.1f,.5f,b);assertNotEquals(a[1],b[1],.001f);}
 @Test public void previewAndServiceWidgetUseReadOnlyComponents(){SettingsPreviewView preview=new SettingsPreviewView(c);preview.previewIcons(250);preview.previewLabels(220);preview.previewFont(200);preview.previewWidget(60);assertNotNull(preview.findViewWithTag("settings_live_preview"));assertEquals("service",WidgetButtons.intentAction("status"));AppPreferences.set(c,"widget_language","zh");assertEquals("保养",WidgetButtons.label(c,"status"));}
 @Test public void everySkinOpensCompactServiceAndExplicitMenusWithoutSending()throws Exception{
  AppRegressionTest base=new AppRegressionTest();base.start();
  java.lang.reflect.Field f=AppRegressionTest.class.getDeclaredField("activity");f.setAccessible(true);MainActivity a=(MainActivity)f.get(base);
  VehicleCoordinator model=VehicleCoordinator.get(a);f=VehicleCoordinator.class.getDeclaredField("signedIn");f.setAccessible(true);f.setBoolean(model,true);model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();model.selected.vin="BETA37-FIXTURE";model.snapshot=new VehicleSnapshot("Polestar 2","masked",63,270,12000,"空闲","全部关闭","全部关闭","已关闭","已上锁",System.currentTimeMillis(),"LIVE",false,"关闭","关闭");AppPreferences.set(a,"language","zh");
  java.lang.reflect.Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);
  try{
   for(String skin:UiSkin.pickerChoices()){UiSkin.set(a,skin);render.invoke(a);android.view.View root=a.getWindow().getDecorView();android.widget.TextView button=root.findViewWithTag("exterior_button");assertNotNull(skin,button);assertTrue(skin,button.getText().toString().contains("保养"));save(root,"home-"+skin);button.performClick();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());assertEquals("保养",org.robolectric.Shadows.shadowOf(dialog).getTitle());assertNotNull(dialog.findViewById(android.R.id.button2));assertNotNull(dialog.getWindow().getDecorView().findViewWithTag("maintenance_popup"));save(dialog.getWindow().getDecorView(),"service-"+skin);dialog.dismiss();}
   java.lang.reflect.Method menu=MainActivity.class.getDeclaredMethod("controlChoiceMenu",String.class);menu.setAccessible(true);
   String[][] cases={{"lock","lock","unlock"},{"climate","climate-start","climate-stop"},{"windows","windows-open","windows-close"},{"trunk","trunk-open","trunk-close"},{"air","air-start","air-stop"}};
   for(String[] row:cases){menu.invoke(a,row[0]);android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.view.View root=dialog.getWindow().getDecorView();assertNotNull(root.findViewWithTag("explicit_action_"+row[1]));assertNotNull(root.findViewWithTag("explicit_action_"+row[2]));save(root,"menu-"+row[0]);dialog.dismiss();}
   assertEquals(OperationState.NOT_SENT,model.operation);
  }finally{android.app.AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();if(d!=null)d.dismiss();base.stop();}
 }
 private void clearScrollIndicators(android.view.View v){v.setScrollIndicators(0);if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)clearScrollIndicators(g.getChildAt(i));}}
 private void save(android.view.View root,String name)throws Exception{
  clearScrollIndicators(root);int w=360;root.measure(android.view.View.MeasureSpec.makeMeasureSpec(w,1073741824),android.view.View.MeasureSpec.makeMeasureSpec(780,android.view.View.MeasureSpec.AT_MOST));int h=Math.max(1,root.getMeasuredHeight());root.layout(0,0,w,h);
  android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap));java.io.File target=new java.io.File("build/reports/previews/beta37/"+name+".png");target.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(target)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
 }
}
