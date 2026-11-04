package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.List;

/** Exact native endpoint used by SingleDeviceCard4x2's resolver.call(clickEvent). */
final class QuickAppCarDispatchShim {
  private static final String PROVIDER="com.oplus.mydevices.quickapp.provider.DeviceQuickAppCardWidgetProvider";
  private static long lastDispatchAt=-1;
  private static String lastDispatchId="";
  private QuickAppCarDispatchShim(){}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Class<?> type=loader.loadClass(PROVIDER);
    Method method=type.getDeclaredMethod("handleClickEvent",String.class,Bundle.class);
    if(method.getReturnType()!=Bundle.class)throw new NoSuchMethodException("handleClickEvent result");
    method.setAccessible(true);
    module.hook(method).setId("psassistant_car_provider_click")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
          List<Object> args=chain.getArgs();
          if(args==null||args.size()!=2||!(args.get(1) instanceof Bundle))return chain.proceed();
          Bundle input=(Bundle)args.get(1);
          String event=input.getString("shortcutId",""),device=input.getString("deviceId","");
          if(!CardShortcutCommand.isOemCarId(event)||device.isEmpty())return chain.proceed();
          Application app=currentApplication();if(app==null)return chain.proceed();
          Bundle session=BridgeClient.virtualSession(app);
          if(!matchesEvent(input,session.getString("virtualCarId","")))return chain.proceed();
          report(app,event,"car-event-received",false,"");
          if(!session.getBoolean("active",false))return rejected(app,event,"vehicle-session-not-ready");
          String request=MyDevicesVehicleHook.shortcutRequest(event);
          Bundle resolved=BridgeClient.resolveGateway(app,request);
          if(!resolved.getBoolean("accepted",false))return rejected(app,event,resolved.getString("reason","gateway-rejected"));
          if(!admit(device+"|"+event,android.os.SystemClock.elapsedRealtime()))return response(true,"duplicate-consumed");
          Bundle result=BridgeClient.command(app,request);
          boolean accepted=result.getBoolean("accepted",false);
          String reason=result.getString("reason","unknown");
          report(app,event,"car-dispatch",accepted,reason);
          if(!accepted)feedback(app,"车控未受理："+reason);
          module.info("quickapp_car_dispatch accepted="+accepted+" shortcut="+event+" reason="+reason);
          return response(accepted,reason);
        });
    module.info("quickapp_car_dispatch ready=true endpoint=handleClickEvent(String,Bundle):Bundle");
  }

  static boolean matchesEvent(Bundle input,String currentCar){
    return input!=null&&currentCar!=null&&!currentCar.isEmpty()
        &&currentCar.equals(input.getString("deviceId",""))
        &&CardShortcutCommand.isOemCarId(input.getString("shortcutId",""));
  }
  static boolean shouldIntercept(String packageName,String deviceType,String eventId){
    return BridgeIdentity.isBridgePackage(packageName)&&"CAR".equalsIgnoreCase(deviceType)&&CardShortcutCommand.isOemCarId(eventId);
  }
  static synchronized boolean admit(String id,long now){
    if(id.equals(lastDispatchId)&&lastDispatchAt>=0&&now>=lastDispatchAt&&now-lastDispatchAt<1200L)return false;
    lastDispatchId=id;lastDispatchAt=now;return true;
  }
  private static Bundle rejected(Application app,String event,String reason){
    report(app,event,"car-dispatch-rejected",false,reason);feedback(app,"车控未就绪："+reason);return response(false,reason);
  }
  private static Bundle response(boolean accepted,String reason){Bundle b=new Bundle();b.putInt("code",accepted?0:-1);b.putBoolean("accepted",accepted);b.putString("reason",reason);return b;}
  private static void report(Application app,String event,String phase,boolean accepted,String reason){
    Bundle b=new Bundle();b.putBoolean("ready",true);b.putString("phase",phase);b.putString("moduleVersion",BridgeBuild.VERSION_NAME);
    b.putString("lastClickShortcut",event);b.putString("reason",reason);b.putBoolean("carEventReceived",true);
    b.putLong("carEventReceivedAt",System.currentTimeMillis());b.putBoolean("carTypeGatePassed",true);
    b.putLong("carTypeGatePassedAt",System.currentTimeMillis());b.putBoolean("carDispatchIntercepted",accepted);
    if(accepted)b.putLong("carDispatchInterceptedAt",System.currentTimeMillis());
    BridgeClient.call(app,"quickapp-runtime-report",b);
  }
  private static void feedback(Application app,String message){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->android.widget.Toast.makeText(app,message,android.widget.Toast.LENGTH_SHORT).show());}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(ReflectiveOperationException unavailable){return null;}}
}
