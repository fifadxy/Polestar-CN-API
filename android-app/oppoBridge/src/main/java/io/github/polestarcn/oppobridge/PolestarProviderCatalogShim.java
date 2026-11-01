package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Exact-build, in-memory provider catalog shim for AssistantScreen 17.14.60.
 *
 * <p>No SceneService database is written. The shim appends a synthetic POLESTAR ProviderInfo only
 * to the already-loaded AssistantScreen list. Kotlin data-class copy$default is preferred so all
 * OEM fields from a real provider are preserved while only spCode/name/icon are replaced.</p>
 */
final class PolestarProviderCatalogShim {
  private static final String HOST="com.coloros.assistantscreen";
  private static final String VERSION="17.14.60";
  private static final String COROUTINE=
      "com.oplus.advice.settings.ui.viewmodel.AdviceSettingCompatViewModel$queryProviderInfoListFromDB$1";

  private PolestarProviderCatalogShim() {}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Application app=currentApplication();
    if(app!=null&&!VERSION.equals(app.getPackageManager().getPackageInfo(HOST,0).versionName))return;
    Class<?> coroutine=loader.loadClass(COROUTINE);
    Method invoke=coroutine.getDeclaredMethod("invokeSuspend",Object.class);
    invoke.setAccessible(true);
    module.hook(invoke)
        .setId("psassistant_polestar_provider_catalog")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain->{
          Object result=chain.proceed();
          try {
            Object continuation=chain.getThisObject();
            Field outer=continuation.getClass().getDeclaredField("this$0");
            outer.setAccessible(true);
            Object vm=outer.get(continuation);
            Field catalog=vm.getClass().getDeclaredField("o");
            catalog.setAccessible(true);
            tryInject(module,vm,catalog,0);
          } catch(Throwable failure) {
            module.warn("polestar_provider_catalog fallback="+failure.getClass().getSimpleName());
          }
          return result;
        });
  }


  private static void tryInject(OppoBridgeModule module,Object viewModel,Field catalog,int attempt) {
    try {
      Object value=catalog.get(viewModel);
      if(!(value instanceof List<?>))return;
      @SuppressWarnings("unchecked") List<Object> list=(List<Object>)value;
      if(hasCode(list,"POLESTAR"))return;
      if(list.isEmpty()) {
        if(attempt<8) {
          int next=attempt+1;
          new Handler(Looper.getMainLooper()).postDelayed(
              ()->tryInject(module,viewModel,catalog,next),350L+attempt*150L);
        } else {
          module.info("polestar_provider_catalog source_empty=true retries_exhausted");
        }
        return;
      }
      Object sample=firstSample(list);
      if(sample==null) {
        module.warn("polestar_provider_catalog sample_missing=true");
        return;
      }
      Object injected=copyProvider(sample);
      if(injected==null) {
        module.warn("polestar_provider_catalog construct_failed class="+sample.getClass().getName());
        return;
      }
      list.add(injected);
      module.info("polestar_provider_catalog injected=true size="+list.size()+" attempt="+attempt);
    } catch(Throwable failure) {
      module.warn("polestar_provider_catalog retry_fallback="+failure.getClass().getSimpleName());
    }
  }

  private static boolean hasCode(List<?> list,String code) {
    for(Object item:list) if(item!=null) try {
      Object value=item.getClass().getMethod("getSpCode").invoke(item);
      if(code.equalsIgnoreCase(String.valueOf(value)))return true;
    } catch(Throwable ignored) {}
    return false;
  }

  private static Object firstSample(List<?> list) {
    for(Object item:list)if(item!=null)return item;
    return null;
  }

  private static Object copyProvider(Object sample) {
    try {
      Class<?> type=sample.getClass();
      Method getCode=type.getMethod("getSpCode");
      Method getName=type.getMethod("getName");
      Method getIcon=null;
      try {getIcon=type.getMethod("getIconUrl");} catch(Throwable ignored) {}
      Method copyDefault=null;
      for(Method m:type.getDeclaredMethods()) {
        Class<?>[] p=m.getParameterTypes();
        if("copy$default".equals(m.getName())&&Modifier.isStatic(m.getModifiers())
            &&m.getReturnType()==type&&p.length>=4&&p[0]==type
            &&p[p.length-2]==int.class&&p[p.length-1]==Object.class) {
          copyDefault=m;break;
        }
      }
      if(copyDefault==null)return constructFallback(type,getCode,getName,getIcon);

      copyDefault.setAccessible(true);
      Class<?>[] p=copyDefault.getParameterTypes();
      int dataCount=p.length-3;
      if(dataCount<=0||dataCount>31)return constructFallback(type,getCode,getName,getIcon);

      int codeIndex=-1,nameIndex=-1,iconIndex=-1;
      for(int i=0;i<dataCount;i++) {
        if(p[i+1]!=String.class)continue;
        Object probe=invokeCopy(copyDefault,sample,dataCount,i,"__PS_PROBE_"+i+"__");
        if(probe==null)continue;
        String sentinel="__PS_PROBE_"+i+"__";
        if(sentinel.equals(String.valueOf(getCode.invoke(probe))))codeIndex=i;
        if(sentinel.equals(String.valueOf(getName.invoke(probe))))nameIndex=i;
        if(getIcon!=null&&sentinel.equals(String.valueOf(getIcon.invoke(probe))))iconIndex=i;
      }
      if(codeIndex<0||nameIndex<0)return constructFallback(type,getCode,getName,getIcon);

      Object[] args=copyArgs(copyDefault,sample,dataCount);
      int mask=(1<<dataCount)-1;
      args[1+codeIndex]="POLESTAR";mask&=~(1<<codeIndex);
      args[1+nameIndex]="Polestar 极星";mask&=~(1<<nameIndex);
      if(iconIndex>=0) {
        args[1+iconIndex]="";mask&=~(1<<iconIndex);
      }
      args[1+dataCount]=mask;
      Object result=copyDefault.invoke(null,args);
      return verify(result,getCode,getName)?result:null;
    } catch(Throwable ignored) {
      return null;
    }
  }

  private static Object invokeCopy(Method copy,Object sample,int dataCount,int index,String value) {
    try {
      Object[] args=copyArgs(copy,sample,dataCount);
      args[1+index]=value;
      int mask=(1<<dataCount)-1;
      mask&=~(1<<index);
      args[1+dataCount]=mask;
      return copy.invoke(null,args);
    } catch(Throwable ignored) {return null;}
  }

  private static Object[] copyArgs(Method copy,Object sample,int dataCount) {
    Class<?>[] types=copy.getParameterTypes();
    Object[] args=new Object[types.length];
    args[0]=sample;
    for(int i=0;i<dataCount;i++)args[1+i]=defaultValue(types[1+i]);
    args[1+dataCount]=(1<<dataCount)-1;
    args[2+dataCount]=null;
    return args;
  }

  private static Object constructFallback(Class<?> type,Method getCode,Method getName,Method getIcon) {
    try {
      Constructor<?> ctor=type.getDeclaredConstructor();
      ctor.setAccessible(true);
      Object out=ctor.newInstance();
      set(out,"spCode","POLESTAR");
      set(out,"name","Polestar 极星");
      set(out,"iconUrl","");
      return verify(out,getCode,getName)?out:null;
    } catch(Throwable ignored) {return null;}
  }

  private static void set(Object target,String property,String value) {
    String setter="set"+Character.toUpperCase(property.charAt(0))+property.substring(1);
    try {target.getClass().getMethod(setter,String.class).invoke(target,value);return;}
    catch(Throwable ignored) {}
    try {
      Field f=target.getClass().getDeclaredField(property);f.setAccessible(true);f.set(target,value);
    } catch(Throwable ignored) {}
  }

  private static boolean verify(Object value,Method getCode,Method getName) {
    if(value==null)return false;
    try {
      return "POLESTAR".equalsIgnoreCase(String.valueOf(getCode.invoke(value)))
          &&String.valueOf(getName.invoke(value)).contains("Polestar");
    } catch(Throwable ignored) {return false;}
  }

  private static Object defaultValue(Class<?> type) {
    if(!type.isPrimitive())return null;
    if(type==boolean.class)return false;
    if(type==byte.class)return (byte)0;
    if(type==short.class)return (short)0;
    if(type==int.class)return 0;
    if(type==long.class)return 0L;
    if(type==float.class)return 0f;
    if(type==double.class)return 0d;
    if(type==char.class)return (char)0;
    return null;
  }

  private static Application currentApplication() {
    try{return (Application)Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }
}
