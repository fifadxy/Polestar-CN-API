package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-memory Polestar provider entry for ColorOS vehicle/account provider catalogs.
 *
 * <p>The old implementation only touched one coroutine and one obfuscated field on AssistantScreen
 * 17.14.60. ColorOS can hand the list to the UI through another method/field before that hook runs,
 * which explains devices where the module is loaded but no Polestar row appears. This generation
 * keeps the same no-database safety boundary, but injects into every provider-shaped list reachable
 * from the known view-model and into provider-list method results.</p>
 */
final class PolestarProviderCatalogShim {
  static final String PROVIDER_CODE="POLESTAR";
  static final String DISPLAY_NAME="Polestar 极星";
  private static final String COROUTINE=
      "com.oplus.advice.settings.ui.viewmodel.AdviceSettingCompatViewModel$queryProviderInfoListFromDB$1";
  private static final String VIEW_MODEL=
      "com.oplus.advice.settings.ui.viewmodel.AdviceSettingCompatViewModel";
  private static volatile Object cachedTemplate;

  private PolestarProviderCatalogShim() {}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Application app=currentApplication();
    String hostVersion="unknown";
    try { if(app!=null)hostVersion=app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName; }
    catch(Throwable ignored) {}

    int hooks=0;
    try { hooks+=hookCoroutine(module,loader); }
    catch(Throwable failure) { module.warn("polestar_provider_catalog coroutine="+failure.getClass().getSimpleName()); }
    try { hooks+=hookViewModelMethods(module,loader); }
    catch(Throwable failure) { module.warn("polestar_provider_catalog viewmodel="+failure.getClass().getSimpleName()); }
    try { hooks+=hookProviderSinks(module,loader); }
    catch(Throwable failure) { module.warn("polestar_provider_catalog sinks="+failure.getClass().getSimpleName()); }

    // The class fingerprint is the safety gate. We intentionally do not hard-fail only because the
    // minor AssistantScreen version changed; if the known class/method shape still exists, the hook
    // remains useful. Unknown shapes simply install zero hooks and leave the OEM UI untouched.
    module.info("polestar_provider_catalog generation=3 hostVersion="+hostVersion+" hooks="+hooks);
  }

  private static int hookCoroutine(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Class<?> coroutine=loader.loadClass(COROUTINE);
    Method invoke=coroutine.getDeclaredMethod("invokeSuspend",Object.class);
    invoke.setAccessible(true);
    module.hook(invoke)
        .setId("psassistant_polestar_provider_catalog_coroutine_v3")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain->{
          Object result=chain.proceed();
          try {
            Object continuation=chain.getThisObject();
            Object vm=outerViewModel(continuation);
            int changed=injectEveryProviderList(vm);
            if(result instanceof List<?>)changed+=injectList((List<?>)result);
            if(changed>0)module.info("polestar_provider_catalog injected coroutine changed="+changed);
            scheduleRecheck(module,vm);
          } catch(Throwable failure) {
            module.warn("polestar_provider_catalog coroutine_fallback="+failure.getClass().getSimpleName());
          }
          return result;
        });
    return 1;
  }

  private static int hookViewModelMethods(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Class<?> vm=loader.loadClass(VIEW_MODEL);
    int hooks=0;
    for(Method method:vm.getDeclaredMethods()) {
      if(Modifier.isStatic(method.getModifiers())||Modifier.isAbstract(method.getModifiers()))continue;
      String name=method.getName().toLowerCase(Locale.ROOT);
      boolean providerNamed=name.contains("provider")||name.contains("account")||name.contains("vehicle");
      boolean listResult=List.class.isAssignableFrom(method.getReturnType());
      if(!providerNamed&&!listResult)continue;
      if(method.getParameterTypes().length>4)continue;
      method.setAccessible(true);
      final String id="psassistant_polestar_provider_vm_v3_"+safeId(method.getName())+"_"+method.getParameterTypes().length+"_"+Integer.toHexString(method.toGenericString().hashCode());
      module.hook(method).setId(id)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            Object result=chain.proceed();
            try {
              int changed=0;
              if(result instanceof List<?>)changed+=injectList((List<?>)result);
              Object owner=chain.getThisObject();
              changed+=injectEveryProviderList(owner);
              if(changed>0)module.info("polestar_provider_catalog injected method="+method.getName()+" changed="+changed);
            } catch(Throwable failure) {
              module.warn("polestar_provider_catalog method_fallback="+method.getName()+":"+failure.getClass().getSimpleName());
            }
            return result;
          });
      hooks++;
    }
    return hooks;
  }

  private static int hookProviderSinks(OppoBridgeModule module,ClassLoader loader) {
    int hooks=0;
    hooks+=hookListSinkClass(module,loader,"androidx.lifecycle.MutableLiveData",new String[]{"setValue","postValue"});
    hooks+=hookListSinkClass(module,loader,"androidx.recyclerview.widget.ListAdapter",new String[]{"submitList"});
    hooks+=hookListSinkClass(module,loader,"androidx.recyclerview.widget.AsyncListDiffer",new String[]{"submitList"});
    return hooks;
  }

  private static int hookListSinkClass(OppoBridgeModule module,ClassLoader loader,String className,String[] names) {
    Class<?> type;
    try { type=Class.forName(className,false,loader); } catch(Throwable missing) { return 0; }
    int hooks=0;
    for(Method method:type.getDeclaredMethods()) {
      boolean named=false;for(String name:names)if(name.equals(method.getName()))named=true;
      if(!named||Modifier.isStatic(method.getModifiers())||method.getParameterTypes().length==0)continue;
      Class<?> first=method.getParameterTypes()[0];
      if(first!=Object.class&&!List.class.isAssignableFrom(first))continue;
      method.setAccessible(true);
      final String id="psassistant_polestar_provider_sink_v3_"+safeId(type.getSimpleName())+"_"+safeId(method.getName())+"_"+method.getParameterTypes().length;
      module.hook(method).setId(id)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            try {
              List<Object> args=chain.getArgs();
              if(args!=null&&!args.isEmpty()&&args.get(0) instanceof List<?>) {
                List<?> original=(List<?>)args.get(0);
                if(looksLikeProviderList("sink",original)&&!hasCode(original,PROVIDER_CODE)) {
                  int changed=injectList(original);
                  if(changed==0) {
                    ArrayList<Object> copy=new ArrayList<>();copy.addAll(original);
                    if(injectList(copy)>0) { args.set(0,copy);changed=1; }
                  }
                  if(changed>0)module.info("polestar_provider_catalog sink="+type.getSimpleName()+"."+method.getName());
                }
              }
            } catch(Throwable failure) {
              module.warn("polestar_provider_catalog sink_fallback="+type.getSimpleName()+":"+failure.getClass().getSimpleName());
            }
            return chain.proceed();
          });
      hooks++;
    }
    return hooks;
  }

  private static void scheduleRecheck(OppoBridgeModule module,Object owner) {
    if(owner==null)return;
    Handler handler=new Handler(Looper.getMainLooper());
    handler.postDelayed(()->{try{int n=injectEveryProviderList(owner);if(n>0)module.info("polestar_provider_catalog delayed=350 changed="+n);}catch(Throwable ignored){}},350L);
    handler.postDelayed(()->{try{int n=injectEveryProviderList(owner);if(n>0)module.info("polestar_provider_catalog delayed=1200 changed="+n);}catch(Throwable ignored){}},1200L);
  }

  private static Object outerViewModel(Object continuation) throws Exception {
    Class<?> type=continuation.getClass();
    for(Class<?> c=type;c!=null;c=c.getSuperclass()) {
      try {
        Field outer=c.getDeclaredField("this$0");outer.setAccessible(true);return outer.get(continuation);
      } catch(NoSuchFieldException ignored) {}
    }
    throw new NoSuchFieldException("this$0");
  }

  private static int injectEveryProviderList(Object owner) {
    if(owner==null)return 0;
    int changed=0;
    for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass()) {
      for(Field field:c.getDeclaredFields()) {
        if(Modifier.isStatic(field.getModifiers()))continue;
        try {
          field.setAccessible(true);
          Object value=field.get(owner);
          if(!(value instanceof List<?>))continue;
          List<?> list=(List<?>)value;
          if(!looksLikeProviderList(field.getName(),list))continue;
          int one=injectList(list);
          changed+=one;
          if(one==0&&!hasCode(list,PROVIDER_CODE)&&!list.isEmpty()) {
            // Some Kotlin collections are immutable snapshots. Replace only a field that already
            // looks like a provider list; no database or system provider is modified.
            ArrayList<Object> copy=new ArrayList<>();
            copy.addAll((List<?>)list);
            if(injectList(copy)>0) {
              field.set(owner,copy);
              changed++;
            }
          }
        } catch(Throwable ignored) {}
      }
    }
    return changed;
  }

  private static boolean looksLikeProviderList(String fieldName,List<?> list) {
    String n=fieldName==null?"":fieldName.toLowerCase(Locale.ROOT);
    if(n.contains("provider")||n.contains("account")||n.contains("catalog"))return true;
    for(Object item:list)if(isProviderItem(item))return true;
    return false;
  }

  private static boolean isProviderItem(Object item) {
    if(item==null)return false;
    try { item.getClass().getMethod("getSpCode");item.getClass().getMethod("getName");return true; }
    catch(Throwable ignored) {return false;}
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  static int injectList(List<?> raw) {
    if(raw==null||hasCode(raw,PROVIDER_CODE))return 0;
    Object sample=firstProvider(raw);
    if(sample!=null)cachedTemplate=sample;
    if(sample==null)sample=cachedTemplate;
    if(sample==null)return 0;
    Object injected=copyProvider(sample);
    if(injected==null)return 0;
    try {
      ((List)raw).add(injected);
      return 1;
    } catch(Throwable ignored) {return 0;}
  }

  static boolean hasCode(List<?> list,String code) {
    for(Object item:list)if(item!=null)try {
      Object value=item.getClass().getMethod("getSpCode").invoke(item);
      if(code.equalsIgnoreCase(String.valueOf(value)))return true;
    } catch(Throwable ignored) {}
    return false;
  }

  private static Object firstProvider(List<?> list) {
    for(Object item:list)if(isProviderItem(item))return item;
    return null;
  }

  private static Object copyProvider(Object sample) {
    try {
      Class<?> type=sample.getClass();
      Method getCode=type.getMethod("getSpCode");
      Method getName=type.getMethod("getName");
      Method copyDefault=null;
      for(Method m:type.getDeclaredMethods()) {
        Class<?>[] p=m.getParameterTypes();
        if("copy$default".equals(m.getName())&&Modifier.isStatic(m.getModifiers())
            &&m.getReturnType()==type&&p.length>=4&&p[0]==type
            &&p[p.length-2]==int.class&&p[p.length-1]==Object.class) {copyDefault=m;break;}
      }
      if(copyDefault==null)return constructFallback(type,getCode,getName);
      copyDefault.setAccessible(true);
      Class<?>[] p=copyDefault.getParameterTypes();
      int dataCount=p.length-3;
      if(dataCount<=0||dataCount>31)return constructFallback(type,getCode,getName);

      int codeIndex=-1,nameIndex=-1;
      for(int i=0;i<dataCount;i++) {
        if(p[i+1]!=String.class)continue;
        String sentinel="__PS_PROBE_"+i+"__";
        Object probe=invokeCopy(copyDefault,sample,dataCount,i,sentinel);
        if(probe==null)continue;
        if(sentinel.equals(String.valueOf(getCode.invoke(probe))))codeIndex=i;
        if(sentinel.equals(String.valueOf(getName.invoke(probe))))nameIndex=i;
      }
      if(codeIndex<0||nameIndex<0)return constructFallback(type,getCode,getName);

      Object[] args=copyArgs(copyDefault,sample,dataCount);
      int mask=(1<<dataCount)-1;
      args[1+codeIndex]=PROVIDER_CODE;mask&=~(1<<codeIndex);
      args[1+nameIndex]=DISPLAY_NAME;mask&=~(1<<nameIndex);
      // Keep the OEM sample icon/flags/feature metadata through the default mask. An empty icon can
      // cause some ColorOS list renderers to drop the row entirely.
      args[1+dataCount]=mask;
      Object result=copyDefault.invoke(null,args);
      return verify(result,getCode,getName)?result:null;
    } catch(Throwable ignored) {return null;}
  }

  private static Object invokeCopy(Method copy,Object sample,int dataCount,int index,String value) {
    try {
      Object[] args=copyArgs(copy,sample,dataCount);
      args[1+index]=value;
      int mask=(1<<dataCount)-1;mask&=~(1<<index);args[1+dataCount]=mask;
      return copy.invoke(null,args);
    } catch(Throwable ignored) {return null;}
  }

  private static Object[] copyArgs(Method copy,Object sample,int dataCount) {
    Class<?>[] types=copy.getParameterTypes();
    Object[] args=new Object[types.length];args[0]=sample;
    for(int i=0;i<dataCount;i++)args[1+i]=defaultValue(types[1+i]);
    args[1+dataCount]=(1<<dataCount)-1;args[2+dataCount]=null;return args;
  }

  private static Object constructFallback(Class<?> type,Method getCode,Method getName) {
    try {
      Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);Object out=ctor.newInstance();
      set(out,"spCode",PROVIDER_CODE);set(out,"name",DISPLAY_NAME);
      return verify(out,getCode,getName)?out:null;
    } catch(Throwable ignored) {return null;}
  }

  private static void set(Object target,String property,String value) {
    String setter="set"+Character.toUpperCase(property.charAt(0))+property.substring(1);
    try {target.getClass().getMethod(setter,String.class).invoke(target,value);return;}catch(Throwable ignored) {}
    for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try {
      Field f=c.getDeclaredField(property);f.setAccessible(true);f.set(target,value);return;
    } catch(Throwable ignored) {}
  }

  private static boolean verify(Object value,Method getCode,Method getName) {
    if(value==null)return false;
    try {
      return PROVIDER_CODE.equalsIgnoreCase(String.valueOf(getCode.invoke(value)))
          &&String.valueOf(getName.invoke(value)).toLowerCase(Locale.ROOT).contains("polestar");
    } catch(Throwable ignored) {return false;}
  }

  private static Object defaultValue(Class<?> type) {
    if(!type.isPrimitive())return null;
    if(type==boolean.class)return false;if(type==byte.class)return (byte)0;if(type==short.class)return (short)0;
    if(type==int.class)return 0;if(type==long.class)return 0L;if(type==float.class)return 0f;
    if(type==double.class)return 0d;if(type==char.class)return (char)0;return null;
  }

  private static String safeId(String name) {return name.replaceAll("[^A-Za-z0-9_]","_");}

  private static Application currentApplication() {
    try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }
}