package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

final class CarLinkVirtualSessionShim {
  private static final String[] MANAGER_FALLBACKS={"com.oplus.carlink.session.SessionManager","com.heytap.opluscarlink.session.SessionManager","com.oplus.ocs.carlink.session.SessionManager","com.heytap.opluscarlink.service.SessionManager"};
  private static final Set<String> SESSION_GETTERS=new LinkedHashSet<>();
  static{SESSION_GETTERS.add("getCurrentSession");SESSION_GETTERS.add("getCurrentCarSession");SESSION_GETTERS.add("currentSession");}
  private CarLinkVirtualSessionShim(){}

  static void install(OppoBridgeModule module,ClassLoader loader){
    Application app=currentApplication();Bundle initial=profile(app);LinkedHashSet<String> managers=new LinkedHashSet<>();
    String configured=initial.getString("sessionManagerClassName","");if(!configured.isEmpty())managers.add(configured);for(String f:MANAGER_FALLBACKS)managers.add(f);
    LinkedHashSet<Class<?>> sessionTypes=new LinkedHashSet<>();int hooks=0;
    for(String name:managers){Class<?> manager;try{manager=loader.loadClass(name);}catch(Throwable missing){continue;}hooks+=hookManager(module,loader,manager,sessionTypes);}
    String configuredSession=initial.getString("sessionClassName","");if(!configuredSession.isEmpty())try{sessionTypes.add(loader.loadClass(configuredSession));}catch(Throwable ignored){}
    for(Class<?> type:sessionTypes)hooks+=hookSessionGetters(module,type);
    if(hooks==0)module.warn("carlink_virtual_session no-compatible-methods");else module.info("carlink_virtual_session hooks="+hooks+" sessionTypes="+sessionTypes.size());
  }

  private static int hookManager(OppoBridgeModule module,ClassLoader loader,Class<?> manager,Set<Class<?>> sessionTypes){
    int hooks=0;for(Method method:manager.getDeclaredMethods()){String name=method.getName();
      if(SESSION_GETTERS.contains(name)&&method.getParameterCount()==0&&!method.getReturnType().isPrimitive()&&method.getReturnType()!=Void.TYPE){
        method.setAccessible(true);Class<?> returnType=method.getReturnType();if(returnType!=Object.class)sessionTypes.add(returnType);
        module.hook(method).setId("psassistant_virtual_session_"+safe(manager.getSimpleName())+"_"+safe(name)).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
          Bundle p=profile(currentApplication());if(!p.getBoolean("active",false))return chain.proceed();
          try{Object virtual=VirtualCarLinkSessionStore.getOrCreate(loader,returnType,p);if(returnType==Object.class||returnType.isInstance(virtual))return virtual;}
          catch(Throwable failure){module.warn("virtual_session fallback="+failure.getClass().getSimpleName());}
          return chain.proceed();
        });hooks++;
      } else if("getConnectState".equals(name)&&method.getParameterCount()==0){
        method.setAccessible(true);Class<?> returnType=method.getReturnType();
        module.hook(method).setId("psassistant_virtual_connect_state_"+safe(manager.getSimpleName())).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
          Bundle p=profile(currentApplication());if(!p.getBoolean("active",false))return chain.proceed();Object value=VirtualCarLinkSessionStore.connectedValue(returnType,p.getString("connectState","COORDINATED"));return value==null?chain.proceed():value;
        });hooks++;
      } else if(("isConnected".equals(name)||"hasActiveSession".equals(name))&&method.getParameterCount()==0&&(method.getReturnType()==boolean.class||method.getReturnType()==Boolean.class)){
        method.setAccessible(true);
        module.hook(method).setId("psassistant_virtual_active_"+safe(manager.getSimpleName())+"_"+safe(name)).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
          return profile(currentApplication()).getBoolean("active",false)?Boolean.TRUE:chain.proceed();
        });hooks++;
      }
    }return hooks;
  }

  private static int hookSessionGetters(OppoBridgeModule module,Class<?> type){
    int hooks=0;for(Method method:type.getDeclaredMethods()){if(method.getParameterCount()!=0)continue;String key=sessionKey(method.getName());if(key==null)continue;method.setAccessible(true);Class<?> returnType=method.getReturnType();
      module.hook(method).setId("psassistant_virtual_session_getter_"+safe(type.getSimpleName())+"_"+safe(method.getName())).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
        Bundle p=profile(currentApplication());if(!p.getBoolean("active",false))return chain.proceed();Object value=valueForGetter(key,returnType,p);return value==UNSET?chain.proceed():value;
      });hooks++;
    }return hooks;
  }

  private static final Object UNSET=new Object();
  private static String sessionKey(String name){
    if("getVin".equals(name))return "testVin";if("getCarId".equals(name)||"getDeviceId".equals(name)||"getVehicleId".equals(name))return "virtualCarId";
    if("isBound".equals(name)||"getBound".equals(name))return "bound";if("getBindState".equals(name)||"getBindingState".equals(name))return "bindState";
    if("getConnectState".equals(name))return "connectState";if("getLockState".equals(name)||"getDoorLockState".equals(name))return "lock";
    if("getClimateState".equals(name)||"getAirConditionState".equals(name))return "climate";if("getSoc".equals(name)||"getBatteryPercent".equals(name))return "batteryPercent";
    if("getRangeKm".equals(name)||"getRange".equals(name))return "rangeKm";if("getOdometerKm".equals(name)||"getOdometer".equals(name))return "odometerKm";
    if("getSupportFeatures".equals(name)||"getSupportedFeatures".equals(name)||"getCapabilities".equals(name))return "capabilities";return null;
  }
  private static Object valueForGetter(String key,Class<?> type,Bundle p){
    if("connectState".equals(key)){Object v=VirtualCarLinkSessionStore.connectedValue(type,p.getString(key,"COORDINATED"));return v==null?UNSET:v;}
    if("bound".equals(key)){if(type==boolean.class||type==Boolean.class)return p.getBoolean("active",false);if(type==String.class)return p.getBoolean("active",false)?"BOUND":"UNBOUND";return UNSET;}
    if("bindState".equals(key)){Object v=VirtualCarLinkSessionStore.connectedValue(type,p.getBoolean("active",false)?"BOUND":"UNBOUND");return v==null?UNSET:v;}
    if("capabilities".equals(key)){String[] values=p.getStringArray("capabilities");if(values==null)return UNSET;if(type.isArray()&&type.getComponentType()==String.class)return values.clone();if(java.util.List.class.isAssignableFrom(type))return new java.util.ArrayList<>(java.util.Arrays.asList(values));if(java.util.Set.class.isAssignableFrom(type))return new java.util.LinkedHashSet<>(java.util.Arrays.asList(values));if(type==String.class)return String.join(",",values);return UNSET;}
    if("batteryPercent".equals(key)||"rangeKm".equals(key)||"odometerKm".equals(key)){if(!p.getBoolean(key+"Known",false))return UNSET;int value=p.getInt(key);if(type==int.class||type==Integer.class)return value;if(type==long.class||type==Long.class)return (long)value;if(type==String.class)return String.valueOf(value);return UNSET;}
    String value=p.getString(key,"");if(type==String.class)return value;return UNSET;
  }

  static void updateVehicleState(String lock,String climate){VirtualCarLinkSessionStore.updateVehicleState(lock,climate);}
  private static Bundle profile(Application app){if(app==null)return Bundle.EMPTY;try{Bundle p=BridgeClient.virtualSession(app);if(p.getBoolean("active",false))VirtualCarLinkSessionStore.updateFromProfile(p);return p;}catch(Throwable ignored){return Bundle.EMPTY;}}
  private static String safe(String value){return value.replaceAll("[^A-Za-z0-9_]","_");}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
}
