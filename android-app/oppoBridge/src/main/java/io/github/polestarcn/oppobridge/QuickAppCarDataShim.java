package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Exact-build final QuickApp data shim.
 *
 * <p>Only PsAssistant's own device row is changed, only after static inspection proves the
 * installed MyDevices 17.4.15 RPK contains a dormant CAR route. Unknown layouts remain untouched
 * and continue through NativeHomeCardHook.</p>
 */
final class QuickAppCarDataShim {
  private static final String PROVIDER=
      "com.oplus.mydevices.quickapp.provider.DeviceQuickAppCardWidgetProvider";
  private static final String BRIDGE_PACKAGE="io.github.polestarcn.oppobridge";
  private static final String PSASSISTANT_PACKAGE="io.github.polestarcn.companion";

  private QuickAppCarDataShim() {}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
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
          .setId("psassistant_oem_car_data_"+methodName)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            Object raw=chain.proceed();
            if(!(raw instanceof Bundle))return raw;
            Application app=currentApplication();
            if(app==null)return raw;
            try {
              Bundle state=BridgeClient.call(app,"oppo-oem-car-status",null);
              if(!state.getBoolean("enabled",false))return raw;

              QuickAppCarCapability.Result capability=QuickAppCarCapability.detect(app);
              if(!capability.routeSafe()) {
                report(app,module,false,capability,"route-not-safe");
                return raw;
              }

              Bundle result=new Bundle((Bundle)raw);
              String json=result.getString("deviceList","");
              if(json==null||json.trim().isEmpty())return raw;
              JSONArray list=new JSONArray(json);
              boolean changed=false;
              for(int i=0;i<Math.min(list.length(),32);i++) {
                JSONObject device=list.optJSONObject(i);
                if(device==null||!isOurDevice(device))continue;
                normalizeCar(device);
                if(capability.eventSummaryCar) {
                  device.put("shortcuts",oemShortcuts());
                  device.put("shortcutList",oemShortcuts());
                } else if(capability.smartHomeDefaultCar) {
                  // Force the exact RPK's built-in CAR default shortcut path instead of feeding
                  // filtered ps_* IDs back into the converter.
                  device.remove("shortcuts");
                  device.remove("shortcutList");
                  device.remove("shortCutList");
                }
                changed=true;
              }
              if(!changed)return raw;
              result.putString("deviceList",list.toString());
              report(app,module,true,capability,capability.mode());
              return result;
            } catch(Throwable failure) {
              module.warn("quickapp_oem_car_data fallback="+failure.getClass().getSimpleName());
              return raw;
            }
          });
      hooked++;
    }
    if(hooked==0)throw new IllegalStateException("QuickApp CAR data methods missing");
    module.info("quickapp_oem_car_data ready=true methods="+hooked);
  }

  static void normalizeCar(JSONObject device) throws Exception {
    device.put("type","CAR");
    device.put("mDeviceType","CAR");
    device.put("deviceType","CAR");
    device.put("modelId","POLESTAR_2");
    device.put("modelName","Polestar 2");
    device.put("brand","Polestar");
    device.put("manufacturer","Polestar");
    device.put("mConnectState","COORDINATED");
    JSONObject connection=device.optJSONObject("connection");
    if(connection==null)connection=new JSONObject();
    connection.put("connectState","COORDINATED");
    connection.put("coordinationState","COORDINATED");
    device.put("connection",connection);
  }

  static JSONArray oemShortcuts() throws Exception {
    JSONArray out=new JSONArray();
    add(out,"chesuo","车锁");
    add(out,"kongtiao","空调");
    add(out,"shanguang","闪灯");
    add(out,"mingdi","鸣笛");
    return out;
  }

  private static void add(JSONArray out,String id,String name) throws Exception {
    out.put(new JSONObject()
        .put("id",id)
        .put("shortcutId",id)
        .put("shortcutName",name)
        .put("style",8)
        .put("state",0));
  }

  private static boolean isOurDevice(JSONObject item) {
    String id=first(item,"id","mDeviceId","deviceId");
    String pkg=first(item,"packageName","deviceAppPackage","mDeviceAppPackage");
    return id.startsWith("psassistant_")||BridgeIdentity.isBridgePackage(pkg);
  }

  private static String first(JSONObject item,String... keys) {
    for(String key:keys) {
      Object value=item.opt(key);
      if(value==null||value==JSONObject.NULL)continue;
      String text=String.valueOf(value).trim();
      if(!text.isEmpty()&&!"null".equalsIgnoreCase(text))return text;
    }
    return "";
  }

  private static void report(Application app,OppoBridgeModule module,boolean applied,
      QuickAppCarCapability.Result capability,String reason) {
    try {
      Bundle in=new Bundle();
      in.putBoolean("ready",true);
      in.putString("phase","oem-car-data-shim");
      in.putString("moduleVersion",BridgeBuild.VERSION_NAME);
      in.putString("hostVersion",capability.hostVersion);
      in.putBoolean("oemShimApplied",applied);
      in.putBoolean("oemCarRegistry",capability.carRegistry);
      in.putBoolean("oemSmartHomeDefaultCar",capability.smartHomeDefaultCar);
      in.putBoolean("oemEventSummaryCar",capability.eventSummaryCar);
      in.putString("oemShimMode",capability.mode());
      in.putString("rpkEntrySha256",capability.indexSha256);
      if(applied)in.putInt("selectedShortcutCount",4);
      in.putString("reason",reason);
      app.getContentResolver().call(BridgeClient.URI,"quickapp-runtime-report",null,in);
      module.info("quickapp_oem_car_data applied="+applied+" mode="+capability.mode());
    } catch(Throwable ignored) {}
  }

  private static Application currentApplication() {
    try {
      return (Application)Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch(Throwable ignored) {return null;}
  }
}
