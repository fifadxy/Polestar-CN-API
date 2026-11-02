package io.github.polestarcn.oppobridge;

import android.app.Application;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Read-only provider catalog observation for the ColorOS vehicle/account binding UI. */
final class AccountProviderCatalogProbe {
  private static final String COROUTINE=
      "com.oplus.advice.settings.ui.viewmodel.AdviceSettingCompatViewModel$queryProviderInfoListFromDB$1";
  private AccountProviderCatalogProbe() {}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Application app=currentApplication();
    String version="";try{if(app!=null)version=app.getPackageManager().getPackageInfo("com.coloros.assistantscreen",0).versionName;}catch(Throwable ignored){}
    Class<?> coroutine=loader.loadClass(COROUTINE);
    Method invoke=coroutine.getDeclaredMethod("invokeSuspend",Object.class);invoke.setAccessible(true);
    final String observedVersion=version;
    module.hook(invoke).setId("psassistant_account_provider_catalog_v2")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain->{
          Object result=chain.proceed();
          try {
            Object continuation=chain.getThisObject();Object vm=outer(continuation);
            List<?> list=findProviderList(vm);
            if(list==null&&result instanceof List<?> && looksLikeProviderList((List<?>)result))list=(List<?>)result;
            if(list!=null)report(module,list);
            else module.info("account_provider_catalog_probe no_provider_list hostVersion="+observedVersion);
          } catch(Throwable failure) {module.warn("account_provider_catalog_probe failed="+failure.getClass().getSimpleName());}
          return result;
        });
    module.info("account_provider_catalog_probe ready=true hostVersion="+version);
  }

  private static Object outer(Object continuation)throws Exception {
    for(Class<?> c=continuation.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField("this$0");f.setAccessible(true);return f.get(continuation);}catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException("this$0");
  }

  private static List<?> findProviderList(Object owner) {
    if(owner==null)return null;
    for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())for(Field f:c.getDeclaredFields())try{
      f.setAccessible(true);Object v=f.get(owner);if(v instanceof List<?> && looksLikeProviderList((List<?>)v))return (List<?>)v;
    }catch(Throwable ignored){}
    return null;
  }

  private static boolean looksLikeProviderList(List<?> list) {
    for(Object item:list)if(item!=null)try{item.getClass().getMethod("getSpCode");item.getClass().getMethod("getName");return true;}catch(Throwable ignored){}
    return false;
  }

  private static void report(OppoBridgeModule module,List<?> items)throws Exception {
    Application app=currentApplication();if(app==null)return;List<String> out=new ArrayList<>();
    for(Object item:items){if(item==null||out.size()>=64)continue;try{Method getCode=item.getClass().getMethod("getSpCode");Method getName=item.getClass().getMethod("getName");String code=safe(String.valueOf(getCode.invoke(item)),32);String name=safe(String.valueOf(getName.invoke(item)),48);if(!code.isEmpty())out.add(code+"="+name);}catch(Throwable ignored){}}
    SceneAccountBindProbe.reportCatalog(app,String.join("|",out));
    module.info("account_provider_catalog observed="+out.size()+" polestar="+out.stream().anyMatch(x->x.toUpperCase(java.util.Locale.ROOT).startsWith("POLESTAR=")));
  }

  private static String safe(String value,int max){if(value==null||"null".equals(value))return "";String clean=value.replace('|',' ').replace('=',' ').replace('\n',' ').replace('\r',' ').trim();return clean.substring(0,Math.min(clean.length(),max));}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
}