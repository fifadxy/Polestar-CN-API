package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Makes PsAssistant visible in local ColorOS vehicle/app catalogs.
 *
 * <p>This is a local LSPosed compatibility layer. It does not fabricate an OPPO cloud
 * authorization, company identity or digital-key enrollment.</p>
 */
final class PsAssistantCarAppCatalogHook {
  private static final String PSA="io.github.polestarcn.companion";

  private static final String[] CATALOG_CLASSES={
      "com.oplus.carlink.utils.WhitelistManager",
      "com.oppo.carlink.utils.WhitelistManager",
      "com.heytap.opluscarlink.utils.WhitelistManager",
      "com.oplus.carlink.config.CarSupportConfigManager",
      "com.oppo.carlink.config.CarSupportConfigManager",
      "com.heytap.opluscarlink.config.CarSupportConfigManager",
      "com.oplus.carlink.config.CarAppConfigManager",
      "com.oppo.carlink.config.CarAppConfigManager",
      "com.heytap.opluscarlink.config.CarAppConfigManager",
      "com.oplus.carlink.manager.CarAppManager",
      "com.oppo.carlink.manager.CarAppManager",
      "com.heytap.opluscarlink.manager.CarAppManager",
      "com.oplus.sceneservice.car.CarAppWhitelistManager",
      "com.coloros.sceneservice.car.CarAppWhitelistManager",
      "com.oplus.sceneservice.car.CarProviderManager",
      "com.coloros.sceneservice.car.CarProviderManager",
      "com.oplus.smartengine.car.CarAppManager",
      "com.oplus.cardservice.car.CarAppManager"
  };

  private PsAssistantCarAppCatalogHook(){}

  static void install(OppoBridgeModule module,String pkg,ClassLoader loader){
    int hooks=0;
    for(String className:CATALOG_CLASSES)hooks+=hookCatalogClass(module,loader,className);
    hooks+=hookPackageEligibility(module,pkg,loader);
    if("com.oplus.ocar".equals(pkg))hooks+=hookCarPlus(module,loader);
    module.info("psassistant_car_app_catalog package="+pkg+" hooks="+hooks);
  }

  private static int hookCatalogClass(OppoBridgeModule module,ClassLoader loader,String className){
    Class<?> type;
    try{type=Class.forName(className,false,loader);}catch(Throwable missing){return 0;}
    int hooks=0;
    for(Method method:type.getDeclaredMethods()){
      if(method.getParameterCount()>4)continue;
      Class<?> result=method.getReturnType();
      String n=method.getName().toLowerCase(Locale.ROOT);
      boolean named=n.contains("support")||n.contains("white")||n.contains("certif")||n.contains("car")
          ||n.contains("vehicle")||n.contains("provider")||n.contains("brand")||n.contains("app")||n.contains("package");
      if(!named)continue;

      if((result==boolean.class||result==Boolean.class)&&method.getParameterCount()>0){
        method.setAccessible(true);
        final String id="psassistant_car_support_"+safe(type.getSimpleName())+"_"+safe(method.getName())+"_"+method.getParameterCount();
        module.hook(method).setId(id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
          if(active()&&targetsPsAssistant(chain.getArgs())){
            module.info("psassistant_car_support allowed="+className+"."+method.getName());
            return Boolean.TRUE;
          }
          return chain.proceed();
        });
        hooks++;
        continue;
      }

      boolean collection=List.class.isAssignableFrom(result)||Set.class.isAssignableFrom(result)
          ||(result.isArray()&&result.getComponentType()==String.class);
      if(!collection)continue;
      method.setAccessible(true);
      final String id="psassistant_car_catalog_"+safe(type.getSimpleName())+"_"+safe(method.getName())+"_"+method.getParameterCount();
      module.hook(method).setId(id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
        Object raw=chain.proceed();
        if(!active())return raw;
        try{
          Object updated=append(raw,n);
          if(updated!=raw)module.info("psassistant_car_catalog injected="+className+"."+method.getName());
          return updated;
        }catch(Throwable failure){
          module.warn("psassistant_car_catalog fallback="+className+"."+method.getName()+":"+failure.getClass().getSimpleName());
          return raw;
        }
      });
      hooks++;
    }
    return hooks;
  }

  private static Object append(Object raw,String methodName){
    String marker=stringMarker(raw,methodName);
    if(raw instanceof List<?>){
      List<?> list=(List<?>)raw;
      if(isStringCollection(list)){
        ArrayList<Object> copy=new ArrayList<>(list);
        if(!containsString(copy,marker))copy.add(marker);
        return copy;
      }
      List<?> updated=PolestarProviderCatalogShim.appendCopy(list,null);
      return updated==null?raw:updated;
    }
    if(raw instanceof Set<?>){
      Set<?> set=(Set<?>)raw;
      if(!isStringCollection(set))return raw;
      LinkedHashSet<Object> copy=new LinkedHashSet<>(set);if(!containsString(copy,marker))copy.add(marker);return copy;
    }
    if(raw instanceof String[]){
      String[] values=(String[])raw;
      for(String value:values)if(marker.equalsIgnoreCase(value)||PSA.equals(value)||"POLESTAR".equalsIgnoreCase(value))return raw;
      String[] copy=java.util.Arrays.copyOf(values,values.length+1);copy[values.length]=marker;return copy;
    }
    return raw;
  }

  private static String stringMarker(Object raw,String methodName){
    String n=methodName==null?"":methodName.toLowerCase(Locale.ROOT);
    if(n.contains("provider")||n.contains("brand"))return "POLESTAR";
    Iterable<?> values=raw instanceof Iterable<?>?(Iterable<?>)raw:null;
    if(values!=null)for(Object value:values)if(value instanceof String){
      String text=(String)value;
      if(text.contains(".")||text.startsWith("com.")||text.startsWith("io."))return PSA;
    }
    return n.contains("app")||n.contains("package")||n.contains("white")||n.contains("certif")?PSA:"POLESTAR";
  }

  private static boolean containsString(Iterable<?> values,String marker){
    for(Object value:values)if(value instanceof String){
      String text=(String)value;
      if(marker.equalsIgnoreCase(text)||PSA.equals(text)||"POLESTAR".equalsIgnoreCase(text)||text.toUpperCase(Locale.ROOT).contains("POLESTAR"))return true;
    }
    return false;
  }

  static boolean targetsPsAssistant(List<Object> args){
    if(args==null)return false;
    for(Object arg:args)if(targetValue(arg,0))return true;
    return false;
  }

  private static boolean targetValue(Object value,int depth){
    if(value==null||depth>2)return false;
    if(value instanceof String){
      String text=((String)value).trim();
      String upper=text.toUpperCase(Locale.ROOT);
      return PSA.equals(text)||upper.contains("POLESTAR")||text.contains("极星");
    }
    if(value instanceof android.os.Bundle){
      android.os.Bundle b=(android.os.Bundle)value;
      for(String key:b.keySet())try{if(targetValue(b.get(key),depth+1))return true;}catch(Throwable ignored){}
      return false;
    }
    if(value instanceof java.util.Map<?,?>){
      for(Object item:((java.util.Map<?,?>)value).values())if(targetValue(item,depth+1))return true;
      return false;
    }
    for(String getter:new String[]{"getPackageName","getAppPackageName","getPkgName","getProviderCode","getSpCode","getBrand","getBrandName","getName"}){
      try{Method m=value.getClass().getMethod(getter);if(m.getParameterCount()==0&&targetValue(m.invoke(value),depth+1))return true;}catch(Throwable ignored){}
    }
    for(String field:new String[]{"packageName","appPackageName","pkgName","providerCode","spCode","brand","brandName","name"}){
      for(Class<?> c=value.getClass();c!=null;c=c.getSuperclass())try{
        Field f=c.getDeclaredField(field);f.setAccessible(true);if(targetValue(f.get(value),depth+1))return true;break;
      }catch(Throwable ignored){}
    }
    return false;
  }

  private static boolean isStringCollection(Iterable<?> values){
    boolean seen=false;
    for(Object value:values){
      if(value==null)continue;
      seen=true;
      if(!(value instanceof String))return false;
    }
    return seen;
  }

  /**
   * Car+ has a separate certified launcher-app catalog. This only exposes PsAssistant as a launchable
   * app in Car+; remote vehicle control continues to use the PsAssistant vehicle bridge.
   *
   * <p>The constructor/field contract mirrors the currently verified ColorOS Car+ implementation.
   * Unknown builds fail open without changing the OEM list.</p>
   */
  private static int hookPackageEligibility(OppoBridgeModule module,String hostPackage,ClassLoader loader){
    Class<?> pm;
    try{pm=Class.forName("android.app.ApplicationPackageManager",false,loader);}
    catch(Throwable unavailable){return 0;}
    int hooks=0;
    for(Method method:pm.getDeclaredMethods()){
      Class<?>[] p=method.getParameterTypes();
      if("checkPermission".equals(method.getName())&&p.length==2&&p[0]==String.class&&p[1]==String.class
          &&(method.getReturnType()==int.class||method.getReturnType()==Integer.class)){
        method.setAccessible(true);
        module.hook(method).setId("psassistant_carlink_pkg_permission")
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
              List<Object> args=chain.getArgs();
              String permission=args!=null&&args.size()>0?String.valueOf(args.get(0)):"";
              String pkg=args!=null&&args.size()>1?String.valueOf(args.get(1)):"";
              if(active()&&PSA.equals(pkg)&&carPermission(permission)){
                module.info("psassistant_carlink_pkg_permission granted="+permission+" host="+hostPackage);
                return PackageManager.PERMISSION_GRANTED;
              }
              return chain.proceed();
            });
        hooks++;
      }else if("checkSignatures".equals(method.getName())&&p.length==2&&p[0]==String.class&&p[1]==String.class
          &&(method.getReturnType()==int.class||method.getReturnType()==Integer.class)){
        method.setAccessible(true);
        module.hook(method).setId("psassistant_carlink_pkg_signature")
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
              List<Object> args=chain.getArgs();
              String a=args!=null&&args.size()>0?String.valueOf(args.get(0)):"";
              String b=args!=null&&args.size()>1?String.valueOf(args.get(1)):"";
              if(active()&&((PSA.equals(a)&&hostIdentity(b,hostPackage))||(PSA.equals(b)&&hostIdentity(a,hostPackage)))){
                module.info("psassistant_carlink_pkg_signature local-match host="+hostPackage);
                return PackageManager.SIGNATURE_MATCH;
              }
              return chain.proceed();
            });
        hooks++;
      }
    }
    return hooks;
  }

  static boolean carPermission(String permission){
    if(permission==null)return false;
    String p=permission.toLowerCase(Locale.ROOT);
    return p.contains("permission.safe.iot")||p.contains("carlink.permission")||p.contains("vehicle.control");
  }

  static boolean hostIdentity(String candidate,String hostPackage){
    if(candidate==null)return false;
    return candidate.equals(hostPackage)||candidate.equals("com.heytap.opluscarlink")
        ||candidate.equals("com.oplus.ocar")||candidate.equals("com.oplus.sceneservice")
        ||candidate.equals("com.coloros.sceneservice");
  }

  private static int hookCarPlus(OppoBridgeModule module,ClassLoader loader){
    try{
      Class<?> apps=loader.loadClass("com.oplus.ocar.carcontrol.response.CertifiedApps");
      Method getList=apps.getDeclaredMethod("getList");getList.setAccessible(true);
      module.hook(getList).setId("psassistant_carplus_certified_apps")
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
            Object raw=chain.proceed();
            if(!(raw instanceof List<?>)||!active())return raw;
            List<?> original=(List<?>)raw;
            if(containsPackage(original,PSA))return raw;
            Object item=carPlusItem(loader);
            if(item==null)return raw;
            ArrayList<Object> copy=new ArrayList<>(original);copy.add(item);
            module.info("psassistant_carplus injected=true");
            return copy;
          });
      return 1;
    }catch(Throwable unavailable){
      module.info("psassistant_carplus unavailable="+unavailable.getClass().getSimpleName());
      return 0;
    }
  }

  private static Object carPlusItem(ClassLoader loader){
    Application app=currentApplication();if(app==null)return null;
    try{
      PackageManager pm=app.getPackageManager();
      ApplicationInfo ai=pm.getApplicationInfo(PSA,0);
      PackageInfo pi=pm.getPackageInfo(PSA,0);
      String label=String.valueOf(ai.loadLabel(pm));
      Class<?> type=loader.loadClass("com.oplus.ocar.carcontrol.CertifiedAppInfo");
      Constructor<?> ctor=type.getDeclaredConstructor(String.class,String.class,String.class,int.class,int.class,String.class,String.class);
      ctor.setAccessible(true);
      Object item=ctor.newInstance(label,PSA,"2600",4,0,"","");
      set(item,"appType","media");set(item,"secondaryAppType","media");set(item,"appName",label);
      set(item,"appVersion",String.valueOf(android.os.Build.VERSION.SDK_INT>=28?pi.getLongVersionCode():pi.versionCode));
      set(item,"verName",pi.versionName==null?"":pi.versionName);
      set(item,"doubleOpenMinVersion","");
      set(item,"displayParams","{\"supportPortrait\":true,\"supportLandscape\":true}");
      set(item,"launchParams","{\"actionName\":\"android.intent.action.MAIN\",\"categoryName\":\"android.intent.category.LAUNCHER\",\"continuable\":true}");
      set(item,"supportCarFusion",true);set(item,"supportCarPlusMode",true);set(item,"supportDoubleOpen",true);set(item,"experimentalApp",false);
      return item;
    }catch(Throwable failure){return null;}
  }

  private static boolean containsPackage(List<?> list,String pkg){
    for(Object item:list){
      if(item==null)continue;
      String value=string(item,new String[]{"getAppPackageName","getPackageName"},new String[]{"appPackageName","packageName","pkgName"});
      if(pkg.equals(value))return true;
    }
    return false;
  }

  private static void set(Object target,String field,Object value){
    for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{
      Field f=c.getDeclaredField(field);f.setAccessible(true);
      if(value instanceof Boolean&&(f.getType()==boolean.class||f.getType()==Boolean.class))f.set(target,value);
      else if(value instanceof String&&f.getType()==String.class)f.set(target,value);
      return;
    }catch(Throwable ignored){}
  }

  private static String string(Object target,String[] getters,String[] fields){
    for(String getter:getters)try{Object v=target.getClass().getMethod(getter).invoke(target);if(v!=null)return String.valueOf(v);}catch(Throwable ignored){}
    for(String name:fields)for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);Object v=f.get(target);if(v!=null)return String.valueOf(v);}catch(Throwable ignored){}
    return "";
  }

  private static boolean active(){
    Application app=currentApplication();if(app==null)return false;
    try{return BridgeClient.virtualSession(app).getBoolean("active",false);}catch(Throwable ignored){return false;}
  }
  private static String safe(String value){return value.replaceAll("[^A-Za-z0-9_]","_");}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
}
