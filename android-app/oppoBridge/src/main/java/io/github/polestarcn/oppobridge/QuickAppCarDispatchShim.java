package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pre-dispatch closure for PsAssistant CAR events in MyDevices.
 *
 * <p>Only PsAssistant's CAR identity and the four CAR event IDs already shipped in the exact
 * MyDevices build are intercepted. Every unrelated event continues through the OEM dispatcher.</p>
 */
final class QuickAppCarDispatchShim {
  private static final String ACTION="com.oplus.mydevices.quickapp.action.QuickAppCardAction";
  private static final AtomicLong lastDispatchAt=new AtomicLong();
  private static volatile String lastDispatchId="";

  private QuickAppCarDispatchShim() {}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Class<?> type=loader.loadClass(ACTION);
    int hooked=0;
    for(Method method:type.getDeclaredMethods()) {
      if(Modifier.isAbstract(method.getModifiers())||method.getParameterCount()==0||method.getParameterCount()>8)continue;
      Class<?> ret=method.getReturnType();
      if(!(ret==void.class||ret==boolean.class||ret==Boolean.class))continue;
      method.setAccessible(true);
      final String id="psassistant_car_dispatch_"+safe(method.getName())+"_"+method.getParameterCount();
      module.hook(method).setId(id).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
        List<Object> args=chain.getArgs();Match match=match(args);
        if(match.eventId.isEmpty())return chain.proceed();
        Application app=currentApplication();if(app==null)return chain.proceed();
        report(app,"car-event-received",match,false,false);
        if(!match.ours||!match.car)return chain.proceed();
        report(app,"car-type-gate",match,true,false);
        String request=MyDevicesVehicleHook.shortcutRequest(match.eventId);
        if(request==null||!admit(match.eventId))return chain.proceed();
        Bundle session=BridgeClient.virtualSession(app);
        if(!session.getBoolean("active",false))return chain.proceed();
        Bundle resolved=BridgeClient.resolveGateway(app,request);
        if(!resolved.getBoolean("accepted",false))return chain.proceed();
        Bundle result=BridgeClient.command(app,request);
        boolean accepted=result.getBoolean("accepted",false);
        report(app,"car-dispatch",match,true,accepted);
        if(!accepted)return chain.proceed();
        module.info("quickapp_car_dispatch intercepted id="+match.eventId+" request="+request);
        if(ret==boolean.class||ret==Boolean.class)return Boolean.TRUE;
        return null;
      });
      hooked++;
    }
    if(hooked==0)throw new IllegalStateException("QuickApp CAR dispatcher methods missing");
    module.info("quickapp_car_dispatch ready=true hooks="+hooked);
  }

  static boolean shouldIntercept(String packageName,String deviceType,String eventId) {
    return BridgeIdentity.isBridgePackage(packageName)
        &&"CAR".equalsIgnoreCase(deviceType)&&CardShortcutCommand.isOemCarId(eventId);
  }

  private static boolean admit(String id) {
    long now=android.os.SystemClock.elapsedRealtime();
    synchronized(QuickAppCarDispatchShim.class) {
      long before=lastDispatchAt.get();
      if(id.equals(lastDispatchId)&&before>0&&now-before<1200L)return false;
      lastDispatchId=id;lastDispatchAt.set(now);return true;
    }
  }

  private static Match match(List<Object> args) {
    Match out=new Match();if(args==null)return out;
    for(Object value:args)inspect(value,out);
    if(out.ours&&!out.eventId.isEmpty()&&!out.car)out.car=true;
    return out;
  }

  private static void inspect(Object value,Match out) {
    if(value==null)return;
    if(value instanceof String){inspectString((String)value,out);return;}
    if(value instanceof Bundle) {
      Bundle b=(Bundle)value;
      for(String key:new String[]{"id","shortcutId","eventId","action","packageName","deviceAppPackage","deviceType","type","device_id"})
        if(b.containsKey(key))inspect(b.get(key),out);
      return;
    }
    for(String getter:new String[]{"getId","getShortcutId","getEventId","getPackageName","getDeviceAppPackage","getDeviceType","getType","getMyDeviceId"}) {
      try {Method m=value.getClass().getMethod(getter);if(m.getParameterCount()!=0)continue;Object v=m.invoke(value);if(v!=value)inspect(v,out);}catch(Throwable ignored){}
    }
  }

  private static void inspectString(String raw,Match out) {
    if(raw==null)return;String value=raw.trim();
    if(CardShortcutCommand.isOemCarId(value))out.eventId=value;
    if(BridgeIdentity.isBridgePackage(value)||value.startsWith("psassistant_")||value.startsWith("psa_"))out.ours=true;
    if("CAR".equalsIgnoreCase(value))out.car=true;
  }

  private static void report(Application app,String phase,Match match,boolean gate,boolean intercepted) {
    try {Bundle in=new Bundle();in.putBoolean("ready",true);in.putString("phase",phase);
      in.putString("moduleVersion",BridgeBuild.VERSION_NAME);in.putString("lastClickShortcut",match.eventId);
      in.putBoolean("carEventReceived",true);in.putLong("carEventReceivedAt",System.currentTimeMillis());
      if(gate){in.putBoolean("carTypeGatePassed",true);in.putLong("carTypeGatePassedAt",System.currentTimeMillis());}
      if(intercepted){in.putBoolean("carDispatchIntercepted",true);in.putLong("carDispatchInterceptedAt",System.currentTimeMillis());}
      BridgeClient.call(app,"quickapp-runtime-report",in);
    } catch(Throwable ignored){}
  }

  private static String safe(String value){return value.replaceAll("[^A-Za-z0-9_]","_");}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
  private static final class Match{String eventId="";boolean ours;boolean car;}
}
