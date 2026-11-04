package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * CarLink/SceneService local binding-state compatibility.
 *
 * <p>ColorOS builds do not always read binding state through one manager API. Some versions parse
 * the SceneService response bundle or cached JSON. This shim only rewrites binding-shaped keys
 * while a real PsAssistant vehicle session is active, and only when the call stack belongs to the
 * scoped vehicle host.</p>
 */
final class CarLinkBoundStateShim {
  private static final Set<String> BOOL_KEYS=new HashSet<>();
  private static final Set<String> BIND_STATE_KEYS=new HashSet<>();
  private static final Set<String> CONNECT_STATE_KEYS=new HashSet<>();
  static{
    for(String k:new String[]{"hascarbinding","hasbinding","isbound","bound","iscarbound","vehiclebound","carbound"})
      BOOL_KEYS.add(k);
    for(String k:new String[]{"bindstate","bindingstate","carbindstate","vehiclebindstate"})
      BIND_STATE_KEYS.add(k);
    for(String k:new String[]{"connectstate","connectionstate","sessionstate"})
      CONNECT_STATE_KEYS.add(k);
  }

  private CarLinkBoundStateShim(){}

  static void install(OppoBridgeModule module,ClassLoader loader){
    int hooks=0;
    try{hooks+=hookJson(module,loader);}catch(Throwable x){module.warn("carlink_bound_json unavailable="+x.getClass().getSimpleName());}
    module.info("carlink_bound_state generation=1 hooks="+hooks);
  }

  private static int hookJson(OppoBridgeModule module,ClassLoader loader)throws Exception{
    Class<?> json;
    try{json=Class.forName("org.json.JSONObject",false,loader);}
    catch(Throwable ignored){json=Class.forName("org.json.JSONObject");}
    int hooks=0;
    for(Method method:json.getDeclaredMethods()){
      String name=method.getName();
      Class<?>[] p=method.getParameterTypes();
      if(p.length<1||p[0]!=String.class||p.length>2)continue;
      Class<?> out=method.getReturnType();
      boolean supported=("optBoolean".equals(name)||"getBoolean".equals(name)
          ||"optInt".equals(name)||"getInt".equals(name)
          ||"optLong".equals(name)||"getLong".equals(name)
          ||"optString".equals(name)||"getString".equals(name)
          ||"opt".equals(name)||"get".equals(name));
      if(!supported)continue;
      method.setAccessible(true);
      final String id="psassistant_carlink_bound_json_"+name+"_"+p.length;
      module.hook(method).setId(id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
        List<Object> args=chain.getArgs();
        String key=args!=null&&!args.isEmpty()?String.valueOf(args.get(0)):"";
        String normalized=normalizeKey(key);
        if(!active()||!vehicleStack()||(!BOOL_KEYS.contains(normalized)&&!BIND_STATE_KEYS.contains(normalized)
            &&!CONNECT_STATE_KEYS.contains(normalized)))return chain.proceed();

        Object value=valueFor(out,normalized);
        if(value==UNSET)return chain.proceed();
        module.info("carlink_bound_json key="+safeKey(key)+" via="+name+" value="+String.valueOf(value));
        return value;
      });
      hooks++;
    }
    return hooks;
  }

  static Object valueForTest(Class<?> type,String key){
    return valueFor(type,normalizeKey(key));
  }

  private static final Object UNSET=new Object();

  private static Object valueFor(Class<?> type,String key){
    boolean bool=BOOL_KEYS.contains(key);
    boolean bind=BIND_STATE_KEYS.contains(key);
    boolean connect=CONNECT_STATE_KEYS.contains(key);
    if(!(bool||bind||connect))return UNSET;

    if(type==boolean.class||type==Boolean.class)return Boolean.TRUE;
    if(type==int.class||type==Integer.class)return connect?2:1;
    if(type==long.class||type==Long.class)return connect?2L:1L;
    if(type==String.class)return bool?"true":bind?"BOUND":"CONNECTED";
    if(type==Object.class)return bool?Boolean.TRUE:bind?"BOUND":"CONNECTED";
    return UNSET;
  }

  static boolean bindingKey(String key){
    String n=normalizeKey(key);
    return BOOL_KEYS.contains(n)||BIND_STATE_KEYS.contains(n)||CONNECT_STATE_KEYS.contains(n);
  }

  private static String normalizeKey(String raw){
    return raw==null?"":raw.toLowerCase(Locale.ROOT).replace("_","").replace("-","").replace(".","");
  }

  private static boolean active(){
    Application app=currentApplication();
    if(app==null)return false;
    try{
      Bundle p=BridgeClient.virtualSession(app);
      return p.getBoolean("active",false)&&p.getBoolean("present",true);
    }catch(Throwable ignored){return false;}
  }

  private static boolean vehicleHostProcess(){
    Application app=currentApplication();
    if(app==null)return false;
    String pkg=app.getPackageName();
    return BridgeIdentity.isCarLinkHost(pkg)
        ||"com.oplus.sceneservice".equals(pkg)||"com.coloros.sceneservice".equals(pkg)
        ||"com.oplus.ocar".equals(pkg)||"com.oplus.smartengine".equals(pkg)
        ||"com.oplus.cardservice".equals(pkg);
  }

  private static String safeKey(String raw){
    if(raw==null)return "";
    String v=raw.replaceAll("[^A-Za-z0-9_.-]","_");
    return v.substring(0,Math.min(48,v.length()));
  }

  private static Application currentApplication(){
    try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }
}
