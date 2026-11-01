package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Exact-build native AssistantScreen vehicle-control adapter for 17.14.60.
 *
 * <p>This avoids mutating MyDevices' signed QuickApp RPK. Only PsAssistant rows are adapted, and
 * the original OEM list is returned unchanged on any shape/version mismatch.</p>
 */
final class AssistantScreenNativeVehicleHook {
  private static final String HOST="com.coloros.assistantscreen";
  private static final String VERSION="17.14.60";
  private static final String MANAGER="com.oplus.assistantscreen.card.mydevices.data.a";
  private static final String DEVICE_INFO=
      "com.oplus.assistantscreen.card.mydevices.domain.model.DeviceInfo";
  private static final String DEVICE_TYPE=
      "com.oplus.assistantscreen.card.mydevices.domain.model.DeviceType";
  private static final String CONNECT_STATE=
      "com.oplus.assistantscreen.card.mydevices.domain.model.ConnectState";
  private static final String TEMPLATE_TYPE=
      "com.oplus.assistantscreen.card.mydevices.domain.model.TemplateType";
  private static final String SWITCH_MENU=
      "com.oplus.assistantscreen.card.mydevices.domain.model.SwitchMenu";

  private AssistantScreenNativeVehicleHook() {}

  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Application app=currentApplication();
    if(app!=null) {
      String version=app.getPackageManager().getPackageInfo(HOST,0).versionName;
      if(!VERSION.equals(version)) {
        module.info("assistant_native_vehicle skipped hostVersion="+version);
        return;
      }
    }

    Class<?> manager=loader.loadClass(MANAGER);
    Class<?> deviceInfo=loader.loadClass(DEVICE_INFO);
    Class<?> deviceType=loader.loadClass(DEVICE_TYPE);
    Class<?> connectState=loader.loadClass(CONNECT_STATE);
    Class<?> templateType=loader.loadClass(TEMPLATE_TYPE);
    Class<?> switchMenu=loader.loadClass(SWITCH_MENU);

    Method load=manager.getDeclaredMethod("c");
    if(Modifier.isStatic(load.getModifiers())||!ArrayList.class.isAssignableFrom(load.getReturnType())
        ||load.getParameterCount()!=0)
      throw new IllegalStateException("AssistantScreen DeviceInfoManager.c shape changed");
    load.setAccessible(true);

    Method getId=deviceInfo.getMethod("getMDeviceId");
    Method getAuthority=deviceInfo.getMethod("getMAuthority");
    Method setType=deviceInfo.getMethod("setMDeviceType",deviceType);
    Method setConnect=deviceInfo.getMethod("setMConnectState",connectState);
    Method setTemplate=deviceInfo.getMethod("setMTemplateType",templateType);
    Method setTemplateList=deviceInfo.getMethod("setMTemplateList",ArrayList.class);
    Method setSwitches=deviceInfo.getMethod("setSwitchMenuList",List.class);

    Object car=enumConstant(deviceType,"CAR");
    Object connected=enumConstant(connectState,"CONNECTED");
    // Exact 17.14.60: NOISE_CANCELING + non-empty SwitchMenuList selects m50/g, the generic
    // battery + switch-control holder. No user-facing noise wording is emitted by this shim.
    Object switchTemplate=enumConstant(templateType,"NOISE_CANCELING");
    Constructor<?> menuCtor=switchMenu.getConstructor(
        String.class,String.class,String.class,int.class,int.class,boolean.class,String.class);

    module.hook(load)
        .setId("psassistant_assistantscreen_native_vehicle")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain->{
          Object raw=chain.proceed();
          if(!(raw instanceof ArrayList<?>))return raw;
          Application host=currentApplication();
          if(host==null)return raw;
          try {
            Bundle snapshot=BridgeClient.snapshot(host);
            if(!snapshot.getBoolean("available",false)||snapshot.getBoolean("demo",false)
                ||!snapshot.getBoolean("cardEnabled",true))
              return raw;

            @SuppressWarnings("unchecked") ArrayList<Object> list=(ArrayList<Object>)raw;
            int applied=0;
            for(Object device:list) {
              if(device==null||!deviceInfo.isInstance(device))continue;
              String id=safe(getId.invoke(device));
              String authority=safe(getAuthority.invoke(device));
              if(!isOurDevice(id,authority))continue;

              setType.invoke(device,car);
              setConnect.invoke(device,connected);
              setTemplate.invoke(device,switchTemplate);
              setTemplateList.invoke(device,new ArrayList<>());
              setSwitches.invoke(device,menus(menuCtor,snapshot,commandScheme(host)));
              applied++;
            }
            if(applied>0) {
              AssistantScreenHostReporter.report(module,"native-switch-menu",true,
                  "applied="+applied+" template=NOISE_CANCELING-switch-holder");
              module.info("assistant_native_vehicle applied="+applied);
            }
          } catch(Throwable failure) {
            module.warn("assistant_native_vehicle fallback="+failure.getClass().getSimpleName());
          }
          return raw;
        });

    module.info("assistant_native_vehicle ready=true manager="+load.getName());
  }

  private static List<Object> menus(Constructor<?> ctor,Bundle state,String scheme) throws Exception {
    ArrayList<Object> out=new ArrayList<>(4);
    boolean locked=contains(state.getString("lock"),"已上锁","已锁","LOCKED");
    boolean climate=contains(state.getString("climate"),"开启","运行中","ON","ACTIVE","RUNNING");
    boolean air=contains(state.getString("air"),"开启","运行中","ON","ACTIVE","RUNNING");
    boolean trunk=contains(state.getString("tailgate"),"已打开","未关严","OPEN");

    out.add(ctor.newInstance("1","车锁","车锁",0,0,locked,
        scheme+"://command?action=lock-toggle"));
    out.add(ctor.newInstance("2","空调","空调",0,0,climate,
        scheme+"://command?action=climate-toggle"));
    out.add(ctor.newInstance("3","净化","净化",0,0,air,
        scheme+"://command?action=air-toggle"));
    out.add(ctor.newInstance("4","尾门","尾门",0,0,trunk,
        scheme+"://command?action=trunk-toggle"));
    return out;
  }

  private static boolean isOurDevice(String id,String authority) {
    return id.startsWith("psassistant_")
        ||"io.github.polestarcn.oppobridge.devices".equals(authority)
        ||"io.github.polestarcn.companion.devices".equals(authority);
  }

  private static String commandScheme(Application app) {
    // The standalone LSPosed bridge is preferred when installed. The integrated PsAssistant build
    // uses a separate scheme so installing both packages never creates an ACTION_VIEW chooser.
    try {
      app.getPackageManager().getApplicationInfo("io.github.polestarcn."+"oppobridge",0);
      return "psassistant-oppo-bridge";
    } catch(Throwable ignored) {
      return "psassistant-companion";
    }
  }

  @SuppressWarnings({"rawtypes","unchecked"})
  private static Object enumConstant(Class<?> type,String name) {
    return Enum.valueOf((Class<? extends Enum>)type.asSubclass(Enum.class),name);
  }

  private static String safe(Object value){return value==null?"":String.valueOf(value);}

  private static boolean contains(String value,String... expected) {
    String normalized=value==null?"":value.trim().toUpperCase(java.util.Locale.ROOT);
    for(String item:expected)
      if(normalized.contains(item.toUpperCase(java.util.Locale.ROOT)))return true;
    return false;
  }

  private static Application currentApplication() {
    try {
      return (Application)Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch(Throwable ignored) {return null;}
  }
}
