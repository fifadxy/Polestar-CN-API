package io.github.polestarcn.oppobridge;

import android.os.Bundle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class VirtualCarLinkSessionStore {
  private static volatile Object session;private static volatile Class<?> sessionClass;private VirtualCarLinkSessionStore(){}
  static synchronized Object getOrCreate(ClassLoader loader,Class<?> preferred,Bundle profile)throws Exception{Class<?> type=resolveSessionClass(loader,preferred,profile);if(session==null||sessionClass!=type){session=create(type);sessionClass=type;}updateFromProfile(profile);return session;}
  static synchronized void updateFromProfile(Bundle p){if(session==null||p==null)return;
    setAny(session,new String[]{"vin","mVin","vehicleVin"},p.getString("testVin",""));setAny(session,new String[]{"carId","mCarId","deviceId","mDeviceId","vehicleId"},p.getString("virtualCarId",""));
    setAny(session,new String[]{"mac","mMac","deviceMac"},p.getString("testMac",""));setAny(session,new String[]{"providerCode","spCode","mProviderCode"},p.getString("providerCode","POLESTAR"));
    setAny(session,new String[]{"brand","brandName","mBrand"},p.getString("brand","Polestar"));setAny(session,new String[]{"modelId","mModelId"},p.getString("modelId","POLESTAR_2"));setAny(session,new String[]{"modelName","mModelName","name"},p.getString("modelName","Polestar 2"));
    setAnyFlexible(session,new String[]{"connectState","connectionState","mConnectState"},p.getString("connectState","COORDINATED"));setAnyFlexible(session,new String[]{"online","isOnline","mOnline"},p.getBoolean("online",false));
    setAnyFlexible(session,new String[]{"lockState","doorLockState","mLockState"},p.getString("lock",""));setAnyFlexible(session,new String[]{"climateState","airConditionState","mClimateState"},p.getString("climate",""));
    if(p.getBoolean("batteryPercentKnown",false))setAnyFlexible(session,new String[]{"soc","batteryPercent","battery","mSoc"},p.getInt("batteryPercent"));
    if(p.getBoolean("rangeKmKnown",false))setAnyFlexible(session,new String[]{"rangeKm","range","remainingRange","mRange"},p.getInt("rangeKm"));
    if(p.getBoolean("odometerKmKnown",false))setAnyFlexible(session,new String[]{"odometerKm","odometer","mOdometer"},p.getInt("odometerKm"));
    setCapabilities(session,p.getStringArray("capabilities"));setAnyFlexible(session,new String[]{"lastUpdateTime","updateTime","timestamp","mTimestamp"},System.currentTimeMillis());
  }
  static synchronized void updateVehicleState(String lock,String climate){if(session==null)return;setAnyFlexible(session,new String[]{"lockState","doorLockState","mLockState"},lock);setAnyFlexible(session,new String[]{"climateState","airConditionState","mClimateState"},climate);setAnyFlexible(session,new String[]{"lastUpdateTime","updateTime","timestamp","mTimestamp"},System.currentTimeMillis());}
  static synchronized void clear(){session=null;sessionClass=null;}
  static Class<?> resolveSessionClass(ClassLoader loader,Class<?> preferred,Bundle p)throws ClassNotFoundException{
    if(preferred!=null&&preferred!=Object.class&&!preferred.isInterface()&&!preferred.isPrimitive()&&!preferred.isArray())return preferred;
    LinkedHashSet<String> names=new LinkedHashSet<>();if(p!=null){String configured=p.getString("sessionClassName","");if(!configured.isEmpty())names.add(configured);}
    names.add("com.oplus.carlink.session.CarDeviceSession");names.add("com.heytap.opluscarlink.session.CarDeviceSession");names.add("com.oplus.ocs.carlink.session.CarDeviceSession");
    for(String n:names)try{return loader.loadClass(n);}catch(Throwable ignored){}throw new ClassNotFoundException("CarDeviceSession");
  }
  private static Object create(Class<?> type)throws Exception{
    try{Constructor<?> c=type.getDeclaredConstructor();c.setAccessible(true);return c.newInstance();}catch(NoSuchMethodException ignored){}
    for(Constructor<?> c:type.getDeclaredConstructors()){if(c.getParameterCount()>10)continue;c.setAccessible(true);Class<?>[] types=c.getParameterTypes();Object[] args=new Object[types.length];for(int i=0;i<types.length;i++)args[i]=defaultValue(types[i]);try{return c.newInstance(args);}catch(Throwable ignored){}}
    throw new IllegalStateException("No usable CarDeviceSession constructor: "+type.getName());
  }
  private static Object defaultValue(Class<?> t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==byte.class)return (byte)0;if(t==short.class)return (short)0;if(t==int.class)return 0;if(t==long.class)return 0L;if(t==float.class)return 0F;if(t==double.class)return 0D;if(t==char.class)return '\0';return null;}
  private static void setCapabilities(Object target,String[] values){if(values==null)return;Field f=findAny(target.getClass(),new String[]{"supportFeatures","supportedFeatures","capabilities","features","mSupportFeatures"});if(f==null)return;try{f.setAccessible(true);Class<?> t=f.getType();if(t.isArray()&&t.getComponentType()==String.class)f.set(target,values.clone());else if(Set.class.isAssignableFrom(t))f.set(target,new LinkedHashSet<>(Arrays.asList(values)));else if(List.class.isAssignableFrom(t))f.set(target,new ArrayList<>(Arrays.asList(values)));else if(t==String.class)f.set(target,String.join(",",values));}catch(Throwable ignored){}}
  private static void setAny(Object target,String[] names,Object value){Field f=findAny(target.getClass(),names);if(f==null)return;try{f.setAccessible(true);if(value==null||f.getType().isInstance(value)||f.getType()==String.class)f.set(target,value==null?null:String.valueOf(value));}catch(Throwable ignored){}}
  private static void setAnyFlexible(Object target,String[] names,Object value){Field f=findAny(target.getClass(),names);if(f==null||value==null)return;try{f.setAccessible(true);Class<?> t=f.getType();if(t.isEnum()&&value instanceof String){Object e=enumValue(t,(String)value);if(e!=null)f.set(target,e);return;}if(t==String.class){f.set(target,String.valueOf(value));return;}if((t==boolean.class||t==Boolean.class)&&value instanceof Boolean){f.set(target,value);return;}if(value instanceof Number){Number n=(Number)value;if(t==int.class||t==Integer.class)f.set(target,n.intValue());else if(t==long.class||t==Long.class)f.set(target,n.longValue());else if(t==float.class||t==Float.class)f.set(target,n.floatValue());else if(t==double.class||t==Double.class)f.set(target,n.doubleValue());return;}if((t==int.class||t==Integer.class)&&value instanceof String){f.set(target,1);return;}if(t.isInstance(value))f.set(target,value);}catch(Throwable ignored){}}
  static Object connectedValue(Class<?> t,String configured){if(t==boolean.class||t==Boolean.class)return Boolean.TRUE;if(t==int.class||t==Integer.class)return 1;if(t==long.class||t==Long.class)return 1L;if(t==String.class)return configured==null||configured.isEmpty()?"COORDINATED":configured;if(t.isEnum())return enumValue(t,configured);return null;}
  @SuppressWarnings({"rawtypes","unchecked"}) private static Object enumValue(Class<?> type,String requested){if(!type.isEnum())return null;String wanted=requested==null?"":requested;for(Object c:type.getEnumConstants()){String n=((Enum)c).name();if(n.equalsIgnoreCase(wanted)||n.equalsIgnoreCase("CONNECTED")||n.equalsIgnoreCase("COORDINATED")||n.equalsIgnoreCase("ACTIVE"))return c;}return null;}
  private static Field findAny(Class<?> type,String[] names){for(String n:names)for(Class<?> c=type;c!=null;c=c.getSuperclass())try{return c.getDeclaredField(n);}catch(NoSuchFieldException ignored){}return null;}
}
