package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Read-only exact-build probe for MyDevices 17.4.15 QuickApp widget replacement state.
 *
 * <p>The target provider runs in com.heytap.mydevices:cards. The probe records only widget-code
 * routing metadata. It never changes the OEM Bundle, RPK path, card binding, or provider result.</p>
 */
final class QuickAppReplacementProbe {
  private static final String PROVIDER =
      "com.oplus.mydevices.quickapp.provider.DeviceQuickAppCardWidgetProvider";

  private QuickAppReplacementProbe() {}

  static void install(OppoBridgeModule module, ClassLoader loader) throws Exception {
    Application app=currentApplication();
    if(app!=null) {
      String version=app.getPackageManager().getPackageInfo("com.heytap.mydevices",0).versionName;
      if(!"17.4.15".equals(version)) {
        module.info("quickapp_replacement_probe skipped hostVersion="+version);
        return;
      }
    }

    Class<?> type=loader.loadClass(PROVIDER);
    int hooked=0;
    for(Method method:type.getDeclaredMethods()) {
      Class<?>[] p=method.getParameterTypes();
      if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=Bundle.class
          ||p.length!=2||p[0]!=String.class||p[1]!=Bundle.class)continue;
      if(!"handleGetCardData".equals(method.getName())&&!"handleCardShow".equals(method.getName()))continue;
      method.setAccessible(true);
      final String methodName=method.getName();
      module.hook(method)
          .setId("psassistant_quickapp_replace_"+methodName)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            List<Object> args=chain.getArgs();
            String widget=args!=null&&!args.isEmpty()?safe(args.get(0)):"";
            Bundle extras=args!=null&&args.size()>1&&args.get(1) instanceof Bundle?(Bundle)args.get(1):null;
            Object result=chain.proceed();
            try {
              report(module,methodName,widget,extras,result instanceof Bundle?(Bundle)result:null);
            } catch(Throwable failure) {
              module.warn("quickapp_replacement_probe_failed "+failure.getClass().getSimpleName());
            }
            return result;
          });
      hooked++;
    }
    if(hooked==0)throw new IllegalStateException("QuickApp replacement methods missing");
    module.info("quickapp_replacement_probe ready=true methods="+hooked);
  }

  private static void report(OppoBridgeModule module,String method,String widget,Bundle extras,Bundle result) {
    Application app=currentApplication();
    if(app==null)return;

    String oldCode=value(extras,"oldWidgetCode");
    String replaced=value(extras,"replacedWidgetCode");
    if(replaced.isEmpty()&&result!=null)replaced=value(result,"replacedWidgetCode");
    if(oldCode.isEmpty()&&result!=null)oldCode=value(result,"oldWidgetCode");

    String effective=!replaced.isEmpty()?replaced:!oldCode.isEmpty()?oldCode:widget;
    String path=cardPath(effective);

    Bundle in=new Bundle();
    in.putBoolean("ready",true);
    in.putString("phase","widget-routing");
    in.putString("method",method);
    in.putString("widgetCode",bounded(widget,500));
    in.putString("oldWidgetCode",bounded(oldCode,500));
    in.putString("replacedWidgetCode",bounded(replaced,500));
    in.putString("cardPath",path);
    in.putString("reason","read-only exact MyDevices 17.4.15 routing observation");
    in.putString("moduleVersion","1.2.0");
    try {in.putString("hostVersion",app.getPackageManager().getPackageInfo("com.heytap.mydevices",0).versionName);}
    catch(Throwable ignored){in.putString("hostVersion","");}

    in.putBoolean("single4x2CarRegistry",true);
    in.putBoolean("single2x2CarRegistry",false);
    in.putBoolean("single4x2EventSummaryCar",false);
    in.putBoolean("single2x2EventSummaryCar",false);

    try {
      app.getContentResolver().call(BridgeClient.URI,"quickapp-runtime-report",null,in);
      module.info("quickapp_replacement method="+method+" cardPath="+path
          +" replaced="+(!replaced.isEmpty()));
    } catch(Throwable ignored) {}
  }

  private static String value(Bundle bundle,String key) {
    if(bundle==null||!bundle.containsKey(key))return "";
    Object value=bundle.get(key);
    return value==null?"":String.valueOf(value);
  }

  private static String cardPath(String code) {
    if(code==null)return "";
    for(String size:new String[]{"SingleDeviceCard4x2","SingleDeviceCard2x2","SingleDeviceCard2x1",
        "PackDeviceCard4x2","PackDeviceCard2x2","PackDeviceCard2x1",
        "LockScreenSingleCard2x1","LockScreenSingleCard1x1"})
      if(code.contains(size))return size;
    return "";
  }

  private static String safe(Object value){return value==null?"":String.valueOf(value);}
  private static String bounded(String value,int max) {
    if(value==null)return "";
    return value.substring(0,Math.min(value.length(),max));
  }

  private static Application currentApplication() {
    try {
      return (Application)Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch(Throwable ignored) {return null;}
  }
}
