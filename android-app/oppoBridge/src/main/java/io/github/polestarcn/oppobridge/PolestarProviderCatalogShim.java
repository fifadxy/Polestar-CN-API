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
  private static volatile Class<?> providerType;
  private static long lastCatalogReport=-1;

  private PolestarProviderCatalogShim() {}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Application app=currentApplication();
    String hostVersion="unknown";
    try { if(app!=null)hostVersion=app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName; }
    catch(Throwable ignored) {}

    try { providerType=loader.loadClass("com.oplus.advice.settings.ui.viewmodel.ProviderInfo"); }
    catch(ClassNotFoundException missing){providerType=null;}
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
    module.info("polestar_provider_catalog generation=5 hostVersion="+hostVersion+" hooks="+hooks);
  }

  private static int hookCoroutine(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Class<?> coroutine=loader.loadClass(COROUTINE);
    Method invoke=coroutine.getDeclaredMethod("invokeSuspend",Object.class);
    invoke.setAccessible(true);
    module.hook(invoke)
        .setId("psassistant_polestar_provider_catalog_coroutine_v5")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain->{
          Object result=chain.proceed();
          if(!enabled())return result;
          try {
            Object continuation=chain.getThisObject();
            Object vm=outerViewModel(continuation);
            int changed=injectEveryProviderList(vm);
            if(result instanceof List<?>){List<?> updated=appendCopy((List<?>)result,providerType);if(updated!=result){result=updated;changed++;reportInjected(updated);}}
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
      final String id="psassistant_polestar_provider_vm_v5_"+safeId(method.getName())+"_"+method.getParameterTypes().length+"_"+Integer.toHexString(method.toGenericString().hashCode());
      module.hook(method).setId(id)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            Object result=chain.proceed();
            if(!enabled())return result;
            try {
              int changed=0;
              if(result instanceof List<?> && method.getReturnType().isAssignableFrom(ArrayList.class)){
                Class<?> emptyType=method.getName().toLowerCase(Locale.ROOT).contains("provider")||method.getGenericReturnType().toString().contains("ProviderInfo")?providerType:null;
                List<?> updated=appendCopy((List<?>)result,emptyType);if(updated!=result){result=updated;changed++;reportInjected(updated);}
              }
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
      final String id="psassistant_polestar_provider_sink_v5_"+safeId(type.getSimpleName())+"_"+safeId(method.getName())+"_"+method.getParameterTypes().length;
      module.hook(method).setId(id)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            Object[] replacement=null;
            try {
              List<Object> args=chain.getArgs();
              if(enabled()&&args!=null&&!args.isEmpty()&&args.get(0) instanceof List<?>) {
                List<?> original=(List<?>)args.get(0);
                if(looksLikeProviderList("sink",original)) {
                  List<?> updated=appendCopy(original,null);
                  if(updated!=original){replacement=args.toArray();replacement[0]=updated;reportInjected(updated);module.info("polestar_provider_catalog sink="+type.getSimpleName()+"."+method.getName());}
                }
              }
            } catch(Throwable failure) {
              module.warn("polestar_provider_catalog sink_fallback="+type.getSimpleName()+":"+failure.getClass().getSimpleName());
            }
            return replacement==null?chain.proceed():chain.proceed(replacement);
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

  private static synchronized void reportInjected(List<?> list) {
    long now=android.os.SystemClock.elapsedRealtime();if(lastCatalogReport>=0&&now-lastCatalogReport<2000)return;
    Application app=currentApplication();if(app==null)return;
    ArrayList<String> rows=new ArrayList<>();
    for(Object item:list){String code=providerCode(item),name=providerName(item);if(code.isEmpty()&&BridgeIdentity.isBridgePackage(providerPackage(item)))code=PROVIDER_CODE;
      if(!code.isEmpty()&&rows.size()<64)rows.add(code.replaceAll("[^A-Za-z0-9_-]","")+"="+name.replace('|',' ').replace('=',' '));}
    lastCatalogReport=now;SceneAccountBindProbe.reportCatalog(app,String.join("|",rows),"injected");
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
    if(owner==null||!enabled())return 0;
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
          Class<?> emptyType=field.getGenericType().toString().contains("ProviderInfo")
              ||(VIEW_MODEL.equals(owner.getClass().getName())&&"o".equals(field.getName()))?providerType:null;
          List<?> updated=appendCopy(list,emptyType);
          if(updated!=list){field.set(owner,updated);changed++;reportInjected(updated);}
        } catch(Throwable ignored) {}
      }
    }
    return changed;
  }

  private static boolean looksLikeProviderList(String fieldName,List<?> list) {
    String n=fieldName==null?"":fieldName.toLowerCase(Locale.ROOT);
    if(n.contains("provider")||n.contains("account")||n.contains("catalog")||n.contains("vehicle")||n.contains("car"))return true;
    if(smartVehicleBrandHits(list)>=2)return true;
    for(Object item:list)if(item!=null)try{item.getClass().getMethod("getSpCode");item.getClass().getMethod("getName");return true;}catch(NoSuchMethodException ignored){}
    return false;
  }

  private static int smartVehicleBrandHits(List<?> list) {
    int hits=0;
    String[] known={"比亚迪","长安","东风","日产","极氪","零跑","理想","smart","腾势","tesla","特斯拉","小牛","小鹏","名爵","荣威","深蓝","阿维塔","arcfox","极狐"};
    for(Object item:list) {
      String name=providerName(item).toLowerCase(Locale.ROOT);
      if(name.isEmpty())continue;
      for(String brand:known)if(name.contains(brand.toLowerCase(Locale.ROOT))){hits++;break;}
      if(hits>=2)return hits;
    }
    return hits;
  }

  private static boolean isProviderItem(Object item) {
    if(item==null)return false;
    String name=providerName(item);
    return !name.isEmpty()&&(!providerCode(item).isEmpty()||!providerPackage(item).isEmpty());
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  static int injectList(List<?> raw) {
    if(raw==null||hasCode(raw,PROVIDER_CODE))return 0;
    Object sample=firstProvider(raw);
    if(sample==null)return 0;
    Object injected=copyProvider(sample);
    if(injected==null)return 0;
    try {
      ((List)raw).add(injected);
      return 1;
    } catch(Throwable ignored) {return 0;}
  }

  static List<?> appendCopy(List<?> original,Class<?> emptyType) {
    if(original==null||hasCode(original,PROVIDER_CODE))return original;
    ArrayList<Object> copy=new ArrayList<>();copy.addAll(original);
    if(copy.isEmpty()) {
      Object added=constructFallback(emptyType);if(added==null)return original;copy.add(added);
    }else if(injectList(copy)==0)return original;
    return copy;
  }

  private static boolean enabled(){Application app=currentApplication();return app!=null&&app.getPackageManager().resolveContentProvider(BridgeIdentity.authority(app),0)!=null;}

  static boolean hasCode(List<?> list,String code) {
    for(Object item:list) {
      if(item==null)continue;
      if(code.equalsIgnoreCase(providerCode(item)))return true;
      if(BridgeIdentity.isBridgePackage(providerPackage(item)))return true;
      String name=providerName(item).toLowerCase(Locale.ROOT);
      if(name.contains("polestar")||name.contains("极星"))return true;
    }
    return false;
  }

  private static Object firstProvider(List<?> list) {
    for(Object item:list)if(isProviderItem(item))return item;
    return null;
  }

  private static Object copyProvider(Object sample) {
    try {
      Class<?> type=sample.getClass();
      Method copyDefault=null;
      for(Method m:type.getDeclaredMethods()) {
        Class<?>[] p=m.getParameterTypes();
        if("copy$default".equals(m.getName())&&Modifier.isStatic(m.getModifiers())
            &&m.getReturnType()==type&&p.length>=4&&p[0]==type
            &&p[p.length-2]==int.class&&p[p.length-1]==Object.class) {copyDefault=m;break;}
      }
      if(copyDefault==null)return constructFallback(type);
      copyDefault.setAccessible(true);
      Class<?>[] p=copyDefault.getParameterTypes();
      int dataCount=p.length-3;
      if(dataCount<=0||dataCount>31)return constructFallback(type);

      int codeIndex=-1,nameIndex=-1,packageIndex=-1;
      for(int i=0;i<dataCount;i++) {
        if(p[i+1]!=String.class)continue;
        String sentinel="__PS_PROBE_"+i+"__";
        Object probe=invokeCopy(copyDefault,sample,dataCount,i,sentinel);
        if(probe==null)continue;
        if(sentinel.equals(providerCode(probe)))codeIndex=i;
        if(sentinel.equals(providerName(probe)))nameIndex=i;
        if(sentinel.equals(providerPackage(probe)))packageIndex=i;
      }
      if(nameIndex<0)return constructFallback(type);
      Object[] args=copyArgs(copyDefault,sample,dataCount);
      int mask=(1<<dataCount)-1;
      args[1+nameIndex]=DISPLAY_NAME;mask&=~(1<<nameIndex);
      if(codeIndex>=0){args[1+codeIndex]=PROVIDER_CODE;mask&=~(1<<codeIndex);}
      if(packageIndex>=0){args[1+packageIndex]=bridgePackage();mask&=~(1<<packageIndex);}
      args[1+dataCount]=mask;
      Object result=copyDefault.invoke(null,args);
      return verify(result)?result:null;
    } catch(Throwable ignored) {return constructFallback(sample==null?null:sample.getClass());}
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

  private static Object constructFallback(Class<?> type) {
    if(type==null)return null;
    try {
      // Exact 17.14.60 ProviderInfo has a three-String constructor and no empty constructor.
      if(type.getMethod("getSpCode").getReturnType()==String.class&&type.getMethod("getName").getReturnType()==String.class&&type.getMethod("getIconUrl").getReturnType()==String.class){
        Constructor<?> ctor=type.getDeclaredConstructor(String.class,String.class,String.class);ctor.setAccessible(true);
        Object value=ctor.newInstance(PROVIDER_CODE,DISPLAY_NAME,BridgeIdentity.iconUri(currentApplication()));
        if(verify(value))return value;
      }
    }catch(ReflectiveOperationException ignored){}
    try {
      Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);Object out=ctor.newInstance();
      setAny(out,new String[]{"spCode","providerCode","brandCode","code"},PROVIDER_CODE);
      setAny(out,new String[]{"name","providerName","brandName","appName","label"},DISPLAY_NAME);
      setAny(out,new String[]{"packageName","pkgName","appPackage","package"},bridgePackage());
      return verify(out)?out:null;
    } catch(Throwable ignored) {return null;}
  }

  private static void setAny(Object target,String[] properties,String value) {
    for(String property:properties) {
      String setter="set"+Character.toUpperCase(property.charAt(0))+property.substring(1);
      try {target.getClass().getMethod(setter,String.class).invoke(target,value);return;}catch(Throwable ignored) {}
      for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try {
        Field f=c.getDeclaredField(property);if(f.getType()!=String.class)continue;f.setAccessible(true);f.set(target,value);return;
      } catch(Throwable ignored) {}
    }
  }

  private static String providerCode(Object item) {
    return stringProperty(item,new String[]{"getSpCode","getProviderCode","getBrandCode","getCode"},new String[]{"spCode","providerCode","brandCode","code"});
  }
  private static String providerName(Object item) {
    return stringProperty(item,new String[]{"getName","getProviderName","getBrandName","getAppName","getLabel"},new String[]{"name","providerName","brandName","appName","label"});
  }
  private static String providerPackage(Object item) {
    return stringProperty(item,new String[]{"getPackageName","getPkgName","getAppPackage"},new String[]{"packageName","pkgName","appPackage"});
  }
  private static String stringProperty(Object item,String[] getters,String[] fields) {
    if(item==null)return "";
    for(String getter:getters)try {
      Method m=item.getClass().getMethod(getter);if(m.getParameterTypes().length!=0||m.getReturnType()!=String.class)continue;
      Object value=m.invoke(item);if(value!=null)return String.valueOf(value);
    } catch(Throwable ignored) {}
    for(String field:fields)for(Class<?> c=item.getClass();c!=null;c=c.getSuperclass())try {
      Field f=c.getDeclaredField(field);if(f.getType()!=String.class)continue;f.setAccessible(true);Object value=f.get(item);if(value!=null)return String.valueOf(value);
    } catch(Throwable ignored) {}
    return "";
  }

  private static boolean verify(Object value) {
    if(value==null)return false;
    String name=providerName(value).toLowerCase(Locale.ROOT);
    if(!(name.contains("polestar")||name.contains("极星")))return false;
    String code=providerCode(value),pkg=providerPackage(value);
    return PROVIDER_CODE.equalsIgnoreCase(code)||BridgeIdentity.isBridgePackage(pkg)||(!code.isEmpty()&&name.contains("polestar"));
  }

  private static String bridgePackage(){return BridgeIdentity.packageName(currentApplication());}

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
