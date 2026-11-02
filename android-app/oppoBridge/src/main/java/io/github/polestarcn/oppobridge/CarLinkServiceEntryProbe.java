package io.github.polestarcn.oppobridge;
import android.content.Intent;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.*;
import java.util.*;
/** Observe the actual host service entry, independent of obfuscated OEM class names. */
final class CarLinkServiceEntryProbe {
  private static final String HOST="com.heytap.opluscarlink";
  private static final Set<String> seen=Collections.synchronizedSet(new HashSet<>());
  static void install(OppoBridgeModule module,ClassLoader loader,String process)throws Exception{
    Class<?> thread=loader.loadClass("android.app.ActivityThread");Method target=null;
    for(Method m:thread.getDeclaredMethods())if(m.getName().equals("handleBindService")&&m.getParameterCount()==1&&m.getParameterTypes()[0].getName().endsWith("$BindServiceData")){target=m;break;}
    if(target==null)throw new NoSuchMethodException("ActivityThread.handleBindService shape");
    Field intent=target.getParameterTypes()[0].getDeclaredField("intent");intent.setAccessible(true);target.setAccessible(true);
    module.hook(target).setId("psassistant_carlink_service_entry").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
      Object result=chain.proceed();
      try{
        List<Object> args=chain.getArgs();Object data=args.size()==1?args.get(0):null;
        Intent request=data==null?null:(Intent)intent.get(data);
        if(accepts(request)){
          String component=serviceName(request,chain.getThisObject(),data);String action=request.getAction();
          if(action==null||!action.startsWith("oppo.intent.action.opluscarlink."))action="other";
          String key=component+"|"+action;
          if(seen.size()<16&&seen.add(key))CarLinkHostReporter.report(module,process,"binder-service-entry",true,"component="+component+" action="+action+" entry-only; auth-unproven",loader);
        }
      }catch(Throwable ignored){module.warn("carlink_service_entry metadata-unavailable");}
      return result;
    });
    module.info("carlink_service_entry probe-installed; observation-only");
  }
  static boolean accepts(Intent request){
    if(request==null)return false;
    return request.getComponent()!=null
        ?HOST.equals(request.getComponent().getPackageName()):HOST.equals(request.getPackage());
  }
  static String serviceName(Intent request,Object thread,Object data){
    if(!accepts(request))return "unrelated";
    if(request.getComponent()!=null)return request.getComponent().getClassName();
    // The SDK binds with setPackage(), so resolve the service already created by
    // ActivityThread. Read only its class name, never extras or Binder payloads.
    try{
      Object services=field(thread,"mServices"),token=field(data,"token");
      Object service=services instanceof Map?((Map<?,?>)services).get(token):null;
      if(service!=null)return service.getClass().getName();
    }catch(ReflectiveOperationException|RuntimeException ignored){}
    return "implicit";
  }
  private static Object field(Object owner,String name)throws ReflectiveOperationException{
    if(owner==null)throw new NoSuchFieldException(name);
    for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass()){
      try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
      catch(NoSuchFieldException missing){/* Try the next superclass. */}
    }
    throw new NoSuchFieldException(name);
  }
}
