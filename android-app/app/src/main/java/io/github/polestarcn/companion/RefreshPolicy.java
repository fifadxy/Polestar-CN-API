package io.github.polestarcn.companion;
import android.content.Context;

/** Centralized refresh defaults for v1.0.0-beta43. Values are configurable in Settings. */
public final class RefreshPolicy {
 private RefreshPolicy(){}
 public static int minutes(Context c,String key,int fallback,int... allowed) {
   int value;try{value=Integer.parseInt(AppPreferences.get(c,key,String.valueOf(fallback)));}
   catch(NumberFormatException e){return fallback;}
   for(int v:allowed)if(value==v)return value;
   return fallback;
 }
 public static int foreground(Context c){return VehiclePowerPolicy.automaticReads(c)?minutes(c,"foreground_refresh_minutes",10,0,2,5,10,15,30):0;}
 public static int resumeThreshold(Context c){return minutes(c,"resume_refresh_minutes",10,0,5,10,15,30);}
 public static int resumeDelaySeconds(Context c){return minutes(c,"resume_delay_seconds",2,0,2,5);}
 public static int mergeSeconds(Context c){return minutes(c,"refresh_merge_seconds",2,0,2,5,10);}
 public static String mapMode(Context c){
   String mode=AppPreferences.get(c,"map_refresh_mode","smart");
   return mode.equals("fixed")||mode.equals("manual")?mode:"smart";
 }
 public static int map(Context c){
   if(!VehiclePowerPolicy.automaticReads(c)||"manual".equals(mapMode(c)))return 0;
   return "fixed".equals(mapMode(c))?minutes(c,"map_refresh_minutes",15,5,15,30,60,120):15;
 }
 public static int mapResume(Context c){return minutes(c,"map_resume_minutes",10,0,5,10,15,30,60);}
 public static boolean mapVisibleOnly(Context c){return !"always".equals(AppPreferences.get(c,"map_refresh_trigger","visible"));}
 public static boolean mapBackground(Context c){return AppPreferences.visible(c,"map_refresh_background",false)&&VehiclePowerPolicy.automaticReads(c);}
 public static int activeMinutes(Context c,RefreshDomain group,boolean foreground,VehicleSnapshot state){
   switch(group){
     case CORE: return foreground?foreground(c):minutes(c,"app_background_minutes",60,0,15,30,60,120,240);
     case CLIMATE:
       return state!=null&&"开启".equals(state.climate)?
           (foreground?minutes(c,"refresh_climate_foreground",2,0,1,2,5,10,15):minutes(c,"refresh_climate_background",15,0,15,30,60,120)):0;
     case CHARGING:
       return state!=null&&ChargingVisibility.shouldShow(state)?
           (foreground?minutes(c,"refresh_charge_foreground",5,0,2,5,10,15):minutes(c,"refresh_charge_background",30,0,15,30,60,120)):0;
     case AIR:
       if(!"periodic".equals(AppPreferences.get(c,"refresh_air_policy","event")))return 0;
       return state!=null&&"开启".equals(state.air)?(foreground?5:30):0;
     case HEALTH: return foreground?minutes(c,"refresh_health_foreground",30,0,10,30,60,120):minutes(c,"refresh_health_background",120,0,60,120,240,720);
     case MAINTENANCE: return foreground?minutes(c,"refresh_maintenance_foreground",120,0,60,120,360,720):minutes(c,"refresh_maintenance_background",720,0,120,360,720,1440);
     case MILEAGE: return foreground?minutes(c,"refresh_mileage_foreground",10,0,5,10,15,30,60):minutes(c,"refresh_mileage_background",60,0,30,60,120,240);
     default: return 0;
   }
 }
 public static int background(Context c,boolean resident,boolean widgets){
   if(!VehiclePowerPolicy.automaticReads(c))return 0;
   boolean consumer=resident||widgets||OppoDeviceIntegration.enabled(c)||AppPreferences.visible(c,"app_background_enabled",false);
   if(!consumer)return minutes(c,"refresh_idle_minutes",120,0,60,120,240);
   VehicleSnapshot state=VehicleSnapshotStore.loadChina(c);
   int base=activeMinutes(c,RefreshDomain.CORE,false,state);
   for(RefreshDomain d:RefreshDomain.values()){
     if(d==RefreshDomain.CORE)continue;
     int n=activeMinutes(c,d,false,state);
     if(n>0&&(base==0||n<base))base=n;
   }
   return base;
 }
}
