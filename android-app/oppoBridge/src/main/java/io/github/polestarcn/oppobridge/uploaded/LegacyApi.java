package io.github.polestarcn.oppobridge.uploaded;
import android.app.Application;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.polestarcn.oppobridge.OppoBridgeModule;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
/** API102 transport for the uploaded before/after hooks. No replacement policy in this adapter. */
final class LegacyApi {
 private static OppoBridgeModule module;private static final AtomicInteger sequence=new AtomicInteger();
 static void bind(OppoBridgeModule value){module=value;}
 static final class AndroidAppHelper {static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Exception unavailable){return null;}}}
 static final class XC_LoadPackage {static final class LoadPackageParam {String packageName;ClassLoader classLoader;}}
 static class XC_MethodHook {
  protected void beforeHookedMethod(MethodHookParam p)throws Throwable{}
  protected void afterHookedMethod(MethodHookParam p)throws Throwable{}
  static final class MethodHookParam {
   java.lang.reflect.Member method;Object[] args;private Object result;private boolean changed;private Throwable failure;
   Object getResult(){return result;}void setResult(Object value){result=value;failure=null;changed=true;}
  }
 }
 static final class XposedBridge {
  static void log(String message){if(module!=null)module.log(Log.INFO,"PsUploaded",message);}
  static void hookMethod(Method method,XC_MethodHook hook){method.setAccessible(true);module.hook(method).setId("uploaded_"+sequence.incrementAndGet()).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
   XC_MethodHook.MethodHookParam p=new XC_MethodHook.MethodHookParam();p.method=method;p.args=chain.getArgs().toArray();
   hook.beforeHookedMethod(p);
   if(!p.changed)try{p.result=chain.proceed();}catch(Throwable error){p.failure=error;}
   hook.afterHookedMethod(p);if(p.failure!=null)throw p.failure;return p.result;
  });}
  static void hookAllMethods(Class<?> type,String name,XC_MethodHook hook){for(Method m:type.getDeclaredMethods())if(m.getName().equals(name))hookMethod(m,hook);}
 }
 static final class XposedHelpers {
  static Class<?> findClassIfExists(String name,ClassLoader loader){try{return Class.forName(name,false,loader);}catch(ClassNotFoundException absent){return null;}}
  static void findAndHookMethod(Class<?> type,String name,Object... args){Class<?>[] signature=new Class<?>[args.length-1];for(int i=0;i<signature.length;i++)signature[i]=(Class<?>)args[i];try{XposedBridge.hookMethod(type.getDeclaredMethod(name,signature),(XC_MethodHook)args[args.length-1]);}catch(NoSuchMethodException absent){throw new IllegalArgumentException(absent);}}
 }
}
