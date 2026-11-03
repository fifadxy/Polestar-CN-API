package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class VehicleCatalogCursorShim {
  private VehicleCatalogCursorShim(){}
  static void install(OppoBridgeModule module)throws Exception{
    int hooks=0;
    for(Method method:ContentResolver.class.getDeclaredMethods()){
      if(!"query".equals(method.getName())||!Cursor.class.isAssignableFrom(method.getReturnType()))continue;
      Class<?>[] params=method.getParameterTypes();if(params.length==0||params[0]!=Uri.class)continue;
      method.setAccessible(true);final String hookId="psassistant_vehicle_catalog_query_"+params.length;
      module.hook(method).setId(hookId).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
        List<Object> args=chain.getArgs();Uri uri=args!=null&&!args.isEmpty()&&args.get(0) instanceof Uri?(Uri)args.get(0):null;
        Object rawValue=chain.proceed();if(!(rawValue instanceof Cursor)||uri==null)return rawValue;
        Application app=currentApplication();Bundle profile=profile(app);
        if(!profile.getBoolean("active",false)||!isTargetAuthority(uri,profile))return rawValue;
        Cursor raw=(Cursor)rawValue;if(!looksLikeVehicleCatalog(raw.getColumnNames()))return rawValue;
        try{MatrixCursor wrapped=appendPolestarRow(raw,profile);try{raw.close();}catch(Throwable ignored){}
          module.info("vehicle_catalog_cursor authority="+uri.getAuthority()+" rows="+wrapped.getCount());return wrapped;}
        catch(Throwable failure){module.warn("vehicle_catalog_cursor fallback="+failure.getClass().getSimpleName());return rawValue;}
      });hooks++;
    }
    module.info("vehicle_catalog_cursor hooks="+hooks);
  }
  static boolean isTargetAuthority(Uri uri,Bundle profile){
    if(uri==null||uri.getAuthority()==null)return false;String[] configured=profile==null?null:profile.getStringArray("catalogAuthorities");if(configured==null)return false;
    for(String a:configured)if(uri.getAuthority().equals(a))return true;return false;
  }
  static boolean looksLikeVehicleCatalog(String[] columns){
    if(columns==null||columns.length==0)return false;int hits=0;
    for(String c:columns){String k=c==null?"":c.toLowerCase(Locale.ROOT).replace("_","");
      if(k.contains("provider")||k.contains("brand")||k.contains("model")||k.contains("vehicle")||k.contains("carid")||k.contains("deviceid")||k.contains("devicetype")||k.equals("vin")||k.contains("bindstate")||k.contains("connectstate"))hits++;}
    return hits>=1;
  }
  static MatrixCursor appendPolestarRow(Cursor raw,Bundle session){
    String[] columns=raw.getColumnNames();MatrixCursor out=new MatrixCursor(columns,Math.max(1,raw.getCount()+1));Set<String> strings=new HashSet<>();int old=raw.getPosition();
    try{raw.moveToPosition(-1);while(raw.moveToNext()){MatrixCursor.RowBuilder row=out.newRow();for(int i=0;i<columns.length;i++){Object v=valueAt(raw,i);row.add(v);if(v instanceof String)strings.add(((String)v).toLowerCase(Locale.ROOT));}}}
    finally{try{raw.moveToPosition(old);}catch(Throwable ignored){}}
    if(!containsPolestar(strings)){MatrixCursor.RowBuilder row=out.newRow();for(String c:columns)row.add(valueForColumn(c,session));}return out;
  }
  private static Object valueAt(Cursor raw,int i){
    switch(raw.getType(i)){case Cursor.FIELD_TYPE_NULL:return null;case Cursor.FIELD_TYPE_INTEGER:return raw.getLong(i);case Cursor.FIELD_TYPE_FLOAT:return raw.getDouble(i);case Cursor.FIELD_TYPE_BLOB:return raw.getBlob(i);default:return raw.getString(i);}
  }
  private static boolean containsPolestar(Set<String> values){for(String v:values)if(v.contains("polestar")||v.contains("极星")||v.equals("polestar_2"))return true;return false;}
  static Object valueForColumn(String column,Bundle s){
    String raw=column==null?"":column.toLowerCase(Locale.ROOT),k=raw.replace("_","");
    if(raw.equals("_id")||k.equals("rowid"))return Math.abs(s.getString("virtualCarId","psassistant").hashCode());
    if(k.equals("id")||k.equals("carid")||k.equals("deviceid")||k.equals("vehicleid"))return s.getString("virtualCarId","");
    if(k.equals("providercode")||k.equals("spcode"))return s.getString("providerCode","POLESTAR");
    if(k.equals("providername")||k.equals("displayname")||k.equals("name"))return s.getString("displayName","Polestar 极星");
    if(k.equals("brand")||k.equals("brandname"))return s.getString("brand","Polestar");if(k.equals("manufacturer"))return s.getString("manufacturer","Polestar");
    if(k.equals("devicetype")||k.equals("type"))return s.getString("deviceType","CAR");if(k.equals("modelid"))return s.getString("modelId","POLESTAR_2");if(k.equals("modelname"))return s.getString("modelName","Polestar 2");
    if(k.equals("vin"))return s.getString("testVin","");if(k.equals("mac")||k.equals("devicemac"))return s.getString("testMac","");
    if(k.equals("connectstate")||k.equals("connectionstate"))return s.getString("connectState","COORDINATED");
    if(k.equals("bindstate")||k.equals("bound")||k.equals("isbind")||k.equals("isbound")||k.equals("online")||k.equals("isonline"))return 1;
    if(k.equals("sessionid"))return s.getString("sessionId","");return null;
  }
  private static Bundle profile(Application app){if(app==null)return Bundle.EMPTY;try{return BridgeClient.virtualSession(app);}catch(Throwable ignored){return Bundle.EMPTY;}}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
}
