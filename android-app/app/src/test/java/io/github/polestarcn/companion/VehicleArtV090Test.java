package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
@RunWith(RobolectricTestRunner.class)
public class VehicleArtV090Test {
 @Test public void categoryThenColorDoesNotLoseOriginalImageRatio(){
   Context c=RuntimeEnvironment.getApplication();
   assertEquals(5,VehicleArt.TYPES.length);
   assertEquals(5,VehicleArt.variants("overhead").length);
   assertEquals(5,VehicleArt.variants("three_quarter").length);
   assertEquals(7,VehicleArt.variants("side").length);
   for(String type:VehicleArt.TYPES)for(String variant:VehicleArt.variants(type)){
     assertEquals(type,VehicleArt.type(variant));
     assertNotEquals(0,VehicleArt.drawable(variant,false,true,false));
   }
   assertEquals(R.drawable.vehicle_hero,VehicleArt.drawable("angled_original",false,true,false));
   assertEquals(2,VehicleArt.MODELS.length);
   assertEquals(2,VehicleArt.types(VehicleArt.POLESTAR_5).length);
   assertEquals(4,VehicleArt.variants("side",VehicleArt.POLESTAR_5).length);
   assertEquals("side",VehicleArt.type("p5_side_snow"));
   assertEquals(R.drawable.official_p5_angle_snow,VehicleArt.drawable("p5_side_snow",false,true,false));
 }
 @Test public void independentDayNightSettingsAreAppliedToDashboard(){
   Context c=RuntimeEnvironment.getApplication();
   AppPreferences.set(c,"ui_skin",UiSkin.SIMPLE);
   AppPreferences.set(c,"vehicle_art_day","top_white");
   AppPreferences.set(c,"vehicle_art_night","top_dark");
   AppPreferences.set(c,"theme","light");
   assertEquals(R.drawable.psa_vehicle_top_white,VehicleArt.dashboard(c));
   assertEquals(0,VehicleArt.portraitRotation(c));
   AppPreferences.set(c,"theme","dark");
   assertEquals(R.drawable.psa_vehicle_top_dark,VehicleArt.dashboard(c));
   assertEquals(0,VehicleArt.portraitRotation(c));
 }
 @Test public void originalBlackWhiteAutoDefaultRestoredWithoutRotatingTopView(){
   Context c=RuntimeEnvironment.getApplication();
   AppPreferences.set(c,"ui_skin",UiSkin.PS_DEFAULT);
   AppPreferences.set(c,VehicleArt.selectionKey(false),"auto");
   AppPreferences.set(c,VehicleArt.selectionKey(true),"auto");
   AppPreferences.set(c,VehicleArt.modelKey(false),VehicleArt.POLESTAR_2);
   AppPreferences.set(c,VehicleArt.modelKey(true),VehicleArt.POLESTAR_2);
   AppPreferences.set(c,"theme","light");
   assertEquals(R.drawable.vehicle_top,VehicleArt.dashboard(c));
   assertEquals(0,VehicleArt.portraitRotation(c));
   AppPreferences.set(c,"theme","dark");
   assertEquals(R.drawable.vehicle_top,VehicleArt.dashboard(c));
   assertEquals(0,VehicleArt.portraitRotation(c));
 }
 @Test public void polestarFiveModelOnlyShowsVerifiedOfficialViews(){
   Context c=RuntimeEnvironment.getApplication();AppPreferences.set(c,"ui_skin",UiSkin.SIMPLE);
   AppPreferences.set(c,"theme","light");
   AppPreferences.set(c,VehicleArt.modelKey(false),VehicleArt.POLESTAR_5);
   AppPreferences.set(c,VehicleArt.selectionKey(false),"auto");
   assertEquals(R.drawable.official_p5_angle_snow,VehicleArt.dashboard(c));
   AppPreferences.set(c,VehicleArt.selectionKey(false),"top_white");
   assertEquals("auto",VehicleArt.key(c)); // No Polestar 2 roof passed off as Polestar 5
 }
 @Test public void widgetImagesAndColorsAreSelectable(){
   Context c=RuntimeEnvironment.getApplication();
   for(String selection:VehicleArt.WIDGET_CHOICES){
     AppPreferences.set(c,"widget_visual",selection);
     assertTrue(VehicleArt.widgetDrawable(c)>0);
   }
   for(String color:WidgetColors.KEYS){
     AppPreferences.set(c,"widget_ink",color);
     assertNotEquals(0,WidgetColors.ink(c));
   }
   assertTrue(WidgetColors.validHex("#FF6B00"));
   assertFalse(WidgetColors.validHex("#GG9900"));
 }
}
