package io.github.polestarcn.companion.auth;
import android.content.Context;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
/** Optional map key stays encrypted on this phone, never bundled or logged. */
public final class SecureMapPreferences {
  private SecureMapPreferences(){}
  public static String key(Context c){
    String blob=c.getSharedPreferences("map_provider_private",0).getString("key","");if(blob.isEmpty())return "";
    try{return new String(SecureSessionStore.decrypt(Base64.decode(blob,Base64.NO_WRAP)),StandardCharsets.UTF_8);}catch(Exception e){return "";}
  }
  public static void save(Context c,String key)throws Exception {
    if(key==null||key.trim().isEmpty()){c.getSharedPreferences("map_provider_private",0).edit().remove("key").apply();return;}
    c.getSharedPreferences("map_provider_private",0).edit().putString("key",Base64.encodeToString(SecureSessionStore.encrypt(key.trim().getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP)).apply();
  }
}
