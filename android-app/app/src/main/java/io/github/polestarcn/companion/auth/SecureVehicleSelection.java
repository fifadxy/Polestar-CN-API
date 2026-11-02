package io.github.polestarcn.companion.auth;
import android.content.Context;
import android.util.Base64;
import io.github.polestarcn.companion.VehicleCoordinator;
import io.github.polestarcn.companion.api.ChinaPolestarApi;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
/** Last authorized selection for SDK cold starts; encrypted with the existing Android Keystore key. */
public final class SecureVehicleSelection {
  private static final String PREF="vehicle_selection_secure";
  public static void save(Context c,ChinaPolestarApi.VehicleInfo car){
    if(car==null||car.vin==null||car.vin.isEmpty())return;
    try{JSONObject o=new JSONObject().put("vin",car.vin).put("model",car.model).put("modelName",car.modelName).put("modelYear",car.modelYear);
      byte[] blob=SecureSessionStore.encrypt(o.toString().getBytes(StandardCharsets.UTF_8));
      c.getSharedPreferences(PREF,0).edit().putString("selection",Base64.encodeToString(blob,Base64.NO_WRAP)).commit();
    }catch(Exception unavailable){clear(c);}
  }
  public static ChinaPolestarApi.VehicleInfo load(Context c,String expectedHash){
    String blob=c.getSharedPreferences(PREF,0).getString("selection",null);if(blob==null)return null;
    try{JSONObject o=new JSONObject(new String(SecureSessionStore.decrypt(Base64.decode(blob,Base64.NO_WRAP)),StandardCharsets.UTF_8));
      ChinaPolestarApi.VehicleInfo car=new ChinaPolestarApi.VehicleInfo();car.vin=o.getString("vin");
      if(!VehicleCoordinator.identity(car.vin).equals(expectedHash))return null;
      car.model=o.optString("model","Polestar 2");car.modelName=o.optString("modelName","");car.modelYear=o.optString("modelYear","");return car;
    }catch(Exception unavailable){clear(c);return null;}
  }
  public static void clear(Context c){c.getSharedPreferences(PREF,0).edit().clear().commit();}
}
