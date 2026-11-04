package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local ColorOS vehicle-host compatibility for PsAssistant.
 *
 * <p>Only calls that can be attributed to the active PsAssistant/Polestar virtual vehicle are
 * intercepted. Commands are always forwarded to the existing PsAssistant bridge, which keeps the
 * user's per-action authorization policy and live-state readback.</p>
 */
final class PsAssistantVehicleHostHook {
  private static final String PSA="io.github.polestarcn.companion";

  private static final String[] BIND_CLASSES={
      "com.oppo.carlink.auth.CarBindManager",
      "com.oplus.carlink.auth.CarBindManager",
      "com.heytap.opluscarlink.auth.CarBindManager",
      "com.oppo.carlink.manager.CarBindingManager",
      "com.oplus.carlink.manager.CarBindingManager",
      "com.heytap.opluscarlink.manager.CarBindingManager",
      "com.oplus.sceneservice.car.CarBindHelper",
      "com.coloros.sceneservice.car.CarBindHelper",
      "com.oplus.sceneservice.car.CarDataRepository",
      "com.coloros.sceneservice.car.CarDataRepository"
  };

  private static final String[] DISPATCH_CLASSES={
      "com.oppo.carlink.control.CarControlDispatcher",
      "com.oplus.carlink.control.CarControlDispatcher",
      "com.heytap.opluscarlink.control.CarControlDispatcher",
      "com.oplus.sceneservice.car.CarControlService",
      "com.coloros.sceneservice.car.CarControlService",
      "com.oplus.sceneservice.car.CarCommandManager",
      "com.coloros.sceneservice.car.CarCommandManager",
      "com.oplus.smartengine.car.CarCommandManager",
      "com.oplus.cardservice.car.CarCommandManager"
  };

  private PsAssistantVehicleHostHook(){}

  static void install(OppoBridgeModule module,String pkg,ClassLoader loader){
    int bind=0,dispatch=0;
    for(String name:BIND_CLASSES)bind+=hookBindClass(module,loader,name);
    for(String name:DISPATCH_CLASSES)dispatch+=hookDispatchClass(module,loader,name);
    module.info("psassistant_vehicle_host package="+pkg+" bindHooks="+bind+" dispatchHooks="+dispatch);
  }

  private static int hookBindClass(OppoBridgeModule module,ClassLoader loader,String className){
    Class<?> type;
    try{type=Class.forName(className,false,loader);}catch(Throwable missing){return 0;}
    int hooks=0;
    for(Method method:type.getDeclaredMethods()){
      if(Modifier.isStatic(method.getModifiers())||method.getParameterCount()>4)continue;
      String n=method.getName();
      if(!("hasCarBinding".equals(n)||"checkCarBind".equals(n)||"isCarBound".equals(n)
          ||"getCarBindState".equals(n)||"getBindingState".equals(n)||"isBound".equals(n)))continue;
      if(!supportedBindReturn(method.getReturnType()))continue;
      method.setAccessible(true);
      final String id="psassistant_local_bind_"+safe(type.getSimpleName())+"_"+safe(n)+"_"+method.getParameterCount();
      module.hook(method).setId(id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
        Bundle profile=profile();
        if(!profile.getBoolean("active",false))return chain.proceed();
        List<Object> args=chain.getArgs();
        if(args!=null&&!args.isEmpty()&&!matchesTarget(args,profile))return chain.proceed();
        Object value=boundValue(method.getReturnType());
        if(value==UNSET)return chain.proceed();
        module.info("psassistant_local_bind matched="+className+"."+n);
        return value;
      });
      hooks++;
    }
    return hooks;
  }

  private static int hookDispatchClass(OppoBridgeModule module,ClassLoader loader,String className){
    Class<?> type;
    try{type=Class.forName(className,false,loader);}catch(Throwable missing){return 0;}
    int hooks=0;
    for(Method method:type.getDeclaredMethods()){
      if(Modifier.isStatic(method.getModifiers())||method.getParameterCount()==0||method.getParameterCount()>8)continue;
      String n=method.getName().toLowerCase(Locale.ROOT);
      if(!(n.contains("dispatch")||n.contains("executecommand")||n.contains("performcaraction")
          ||n.contains("sendcar")||n.equals("control")||n.contains("carcommand")))continue;
      if(!supportedCommandReturn(method.getReturnType()))continue;
      method.setAccessible(true);
      final String id="psassistant_car_dispatch_"+safe(type.getSimpleName())+"_"+safe(method.getName())+"_"+method.getParameterCount();
      module.hook(method).setId(id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
        Bundle profile=profile();
        if(!profile.getBoolean("active",false))return chain.proceed();
        List<Object> args=chain.getArgs();
        if(args==null||args.isEmpty()||!matchesTarget(args,profile))return chain.proceed();
        String action=extractAction(args,profile);
        if(action==null)return chain.proceed();
        Application app=currentApplication();if(app==null)return chain.proceed();

        Bundle gateway=BridgeClient.resolveGateway(app,action);
        if(!gateway.getBoolean("accepted",false)){
          String reason=gateway.getString("reason","gateway-rejected");
          module.warn("psassistant_car_dispatch rejected action="+action+" reason="+reason);
          return commandResult(method.getReturnType(),false,reason,action);
        }
        Bundle result=BridgeClient.command(app,action);
        boolean accepted=result.getBoolean("accepted",false);
        String reason=result.getString("reason",accepted?"queued":"rejected");
        module.info("psassistant_car_dispatch action="+action+" accepted="+accepted+" reason="+reason);
        return commandResult(method.getReturnType(),accepted,reason,action);
      });
      hooks++;
    }
    return hooks;
  }

  static String extractAction(List<Object> args,Bundle state){
    String direct=null,instruction=null,value=null;
    for(Object arg:args){
      if(arg==null)continue;
      if(arg instanceof String){
        String text=String.valueOf(arg);
        String mapped=mapDirect(text,state);
        if(mapped!=null&&direct==null)direct=mapped;
        if(instruction==null&&looksInstruction(text))instruction=text;
        else if(value==null&&looksState(text))value=text;
      }else if(arg instanceof Boolean||arg instanceof Number){
        if(value==null)value=String.valueOf(arg);
      }else if(arg instanceof Bundle){
        Bundle b=(Bundle)arg;
        String candidate=first(b,"action","command","cmd","shortcutId");
        String mapped=mapDirect(candidate,state);if(mapped!=null&&direct==null)direct=mapped;
        if(instruction==null)instruction=first(b,"instruction","instructionType","type","sdk_cmd_name","controlType");
        if(value==null)value=first(b,"value","state","target","instructionValue","action_target_state");
      }else if(arg instanceof Map<?,?>){
        Map<?,?> m=(Map<?,?>)arg;
        String candidate=firstMap(m,"action","command","cmd","shortcutId");
        String mapped=mapDirect(candidate,state);if(mapped!=null&&direct==null)direct=mapped;
        if(instruction==null)instruction=firstMap(m,"instruction","instructionType","type","sdk_cmd_name","controlType");
        if(value==null)value=firstMap(m,"value","state","target","instructionValue","action_target_state");
      }else{
        String candidate=reflectString(arg,new String[]{"getAction","getCommand","getCmd","getShortcutId"});
        String mapped=mapDirect(candidate,state);if(mapped!=null&&direct==null)direct=mapped;
        if(instruction==null)instruction=reflectString(arg,new String[]{"getInstruction","getInstructionType","getType","getControlType"});
        if(value==null)value=reflectString(arg,new String[]{"getValue","getState","getTarget","getInstructionValue"});
      }
    }
    if(direct!=null)return direct;
    return mapInstruction(instruction,value,state);
  }

  static boolean matchesTarget(List<Object> args,Bundle profile){
    String car=profile==null?"":profile.getString("virtualCarId","");
    for(Object arg:args)if(hasTargetMarker(arg,car,0))return true;
    return false;
  }

  private static boolean hasTargetMarker(Object value,String car,int depth){
    if(value==null||depth>2)return false;
    if(value instanceof String)return marker((String)value,car);
    if(value instanceof Bundle){
      Bundle b=(Bundle)value;
      for(String key:b.keySet())try{if(hasTargetMarker(b.get(key),car,depth+1))return true;}catch(Throwable ignored){}
      return false;
    }
    if(value instanceof Map<?,?>){
      for(Map.Entry<?,?> e:((Map<?,?>)value).entrySet()){
        if(marker(String.valueOf(e.getKey()),car)&&hasTargetMarker(e.getValue(),car,depth+1))return true;
        if(hasTargetMarker(e.getValue(),car,depth+1))return true;
      }
      return false;
    }
    for(String getter:new String[]{"getPackageName","getPkgName","getProviderCode","getSpCode","getBrand","getBrandName",
        "getCarId","getVehicleId","getDeviceId","getName","getVehicleName"}){
      try{Method m=value.getClass().getMethod(getter);if(m.getParameterCount()==0&&hasTargetMarker(m.invoke(value),car,depth+1))return true;}catch(Throwable ignored){}
    }
    for(String field:new String[]{"packageName","pkgName","providerCode","spCode","brand","brandName","carId","vehicleId","deviceId","name"}){
      for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(field);f.setAccessible(true);if(hasTargetMarker(f.get(value),car,depth+1))return true;break;}catch(Throwable ignored){}
    }
    return false;
  }

  private static boolean marker(String raw,String car){
    String text=raw==null?"":raw.trim();
    if(text.isEmpty())return false;
    String upper=text.toUpperCase(Locale.ROOT);
    return PSA.equals(text)||upper.contains("POLESTAR")||text.contains("极星")||(!car.isEmpty()&&text.equals(car));
  }

  static String mapInstruction(String instruction,String rawValue,Bundle state){
    if(instruction==null||instruction.trim().isEmpty())return null;
    String direct=mapDirect(instruction,state);if(direct!=null)return direct;
    String type=normalize(instruction),value=normalize(rawValue);
    boolean open=isOpen(value),close=isClose(value);
    if("LOCK-CAR".equals(type))return open?"lock":null;
    if("CAR-DOOR".equals(type))return open?"unlock":close?"lock":null;
    if("ALL-WINDOW".equals(type))return open?"windows-open":close?"windows-close":null;
    if("CAR-BOOT".equals(type)||"TRUNK".equals(type)||"TAILGATE".equals(type))return open?"trunk-open":close?"trunk-close":null;
    if("AC-TEMP".equals(type)||"CLIMATE".equals(type)||"AIR-CONDITIONER".equals(type))return open?"climate-start":close?"climate-stop":null;
    if("AIR-PURIFICATION".equals(type)||"PURIFICATION".equals(type)||"AIR-CLEAN".equals(type))return open?"air-start":close?"air-stop":null;
    if("CAR-HORN".equals(type))return open||value.isEmpty()?"honk":null;
    if("DOUBLE-FLASH".equals(type))return open||value.isEmpty()?"flash":null;
    if("CAR-HORN&&DOUBLE-FLASH".equals(type)||"HORN-FLASH".equals(type))return open||value.isEmpty()?"honk-flash":null;
    return null;
  }

  static String mapDirect(String raw,Bundle state){
    if(raw==null)return null;
    String value=raw.trim();
    String id=CardShortcutCommand.fromId(value,state);if(id!=null)return id;
    String n=normalize(value);
    switch(n){
      case "LOCK":case "UNLOCK":case "TRUNK-OPEN":case "TRUNK-CLOSE":
      case "CLIMATE-START":case "CLIMATE-STOP":case "AIR-START":case "AIR-STOP":
      case "WINDOWS-OPEN":case "WINDOWS-CLOSE":case "HONK":case "FLASH":case "HONK-FLASH":
        return n.toLowerCase(Locale.ROOT);
      case "CLIMATE-ON":return "climate-start";
      case "CLIMATE-OFF":return "climate-stop";
      case "WINDOW-OPEN":return "windows-open";
      case "WINDOW-CLOSE":return "windows-close";
      case "FIND-CAR":return "honk-flash";
      default:return null;
    }
  }

  private static boolean looksInstruction(String raw){
    String n=normalize(raw);
    return n.startsWith("CAR-")||n.contains("WINDOW")||n.contains("CLIMATE")||n.contains("AC-")
        ||n.contains("PURIFICATION")||n.contains("FLASH")||n.contains("HORN")||n.contains("TRUNK")
        ||n.contains("TAILGATE");
  }
  private static boolean looksState(String raw){String n=normalize(raw);return isOpen(n)||isClose(n);}
  private static boolean isOpen(String n){return "OPEN".equals(n)||"ON".equals(n)||"TRUE".equals(n)||"1".equals(n)||"START".equals(n);}
  private static boolean isClose(String n){return "CLOSE".equals(n)||"CLOSED".equals(n)||"OFF".equals(n)||"FALSE".equals(n)||"0".equals(n)||"STOP".equals(n);}
  private static String normalize(String raw){return raw==null?"":raw.trim().toUpperCase(Locale.ROOT).replace('_','-').replace(' ','-');}

  private static String first(Bundle b,String... keys){
    if(b==null)return null;for(String key:keys)if(b.containsKey(key)){Object v=b.get(key);if(v!=null)return String.valueOf(v);}return null;
  }
  private static String firstMap(Map<?,?> m,String... keys){
    for(String key:keys)if(m.containsKey(key)&&m.get(key)!=null)return String.valueOf(m.get(key));return null;
  }
  private static String reflectString(Object target,String[] getters){
    for(String getter:getters)try{Method m=target.getClass().getMethod(getter);if(m.getParameterCount()==0){Object v=m.invoke(target);if(v!=null)return String.valueOf(v);}}catch(Throwable ignored){}
    return null;
  }

  private static boolean supportedBindReturn(Class<?> type){
    return type==boolean.class||type==Boolean.class||type==int.class||type==Integer.class||type==String.class||type.isEnum();
  }
  private static boolean supportedCommandReturn(Class<?> type){
    return type==void.class||type==boolean.class||type==Boolean.class||type==int.class||type==Integer.class
        ||type==String.class||Bundle.class.isAssignableFrom(type);
  }

  private static final Object UNSET=new Object();
  private static Object boundValue(Class<?> type){
    if(type==boolean.class||type==Boolean.class)return Boolean.TRUE;
    if(type==int.class||type==Integer.class)return 1;
    if(type==String.class)return "BOUND";
    if(type.isEnum()){
      Object[] constants=type.getEnumConstants();
      if(constants!=null)for(Object value:constants){String n=String.valueOf(value).toUpperCase(Locale.ROOT);if(n.contains("BOUND")||n.contains("CONNECTED")||n.contains("READY"))return value;}
    }
    return UNSET;
  }

  private static Object commandResult(Class<?> type,boolean accepted,String reason,String action){
    if(type==void.class)return null;
    if(type==boolean.class||type==Boolean.class)return accepted;
    if(type==int.class||type==Integer.class)return accepted?0:-1;
    if(type==String.class)return accepted?"QUEUED":reason;
    if(Bundle.class.isAssignableFrom(type)){
      Bundle out=new Bundle();out.putInt("code",accepted?0:-1);out.putBoolean("accepted",accepted);
      out.putString("reason",reason);out.putString("action",action);return out;
    }
    return null;
  }

  private static Bundle profile(){Application app=currentApplication();if(app==null)return Bundle.EMPTY;try{return BridgeClient.virtualSession(app);}catch(Throwable ignored){return Bundle.EMPTY;}}
  private static String safe(String value){return value.replaceAll("[^A-Za-z0-9_]","_");}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
}
