package io.github.polestarcn.oppobridge;

import android.os.Bundle;
import android.app.Application;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only probe for the exact MyDevices QuickApp ContentProvider protocol.
 *
 * <p>MyDevices 17.4.15 RPK calls DeviceQuickAppCardWidgetProvider.call() with methods such as
 * com.oplus.mydevices.getCardData and com.oplus.mydevices.clickEvent. This probe never changes
 * the provider result; it only reports whether PsAssistant's device reached that OEM path.</p>
 */
final class QuickAppProviderProbe {
  private static final String PROVIDER =
      "com.oplus.mydevices.quickapp.provider.DeviceQuickAppCardWidgetProvider";
  private static final AtomicBoolean CARD_DATA_REPORTED = new AtomicBoolean();
  private static final AtomicBoolean CLICK_REPORTED = new AtomicBoolean();

  private QuickAppProviderProbe() {}

  static void install(OppoBridgeModule module, ClassLoader loader) throws Exception {
    Class<?> type = loader.loadClass(PROVIDER);
    Method call = null;
    for (Method method : type.getDeclaredMethods()) {
      Class<?>[] p = method.getParameterTypes();
      if (!"call".equals(method.getName()) || Modifier.isStatic(method.getModifiers())
          || p.length != 3 || p[0] != String.class || p[1] != String.class
          || p[2] != Bundle.class || method.getReturnType() != Bundle.class) continue;
      if (call != null) throw new IllegalStateException("QuickApp provider call ambiguous");
      call = method;
    }
    if (call == null) throw new IllegalStateException("QuickApp provider call missing");
    call.setAccessible(true);

    module.hook(call)
        .setId("psassistant_quickapp_provider_protocol")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain -> {
          List<Object> args = chain.getArgs();
          String method = args == null || args.isEmpty() ? "" : String.valueOf(args.get(0));
          Bundle extras = args != null && args.size() > 2 && args.get(2) instanceof Bundle
              ? (Bundle) args.get(2) : null;
          Object result = chain.proceed();

          try {
            if ("com.oplus.mydevices.getCardData".equals(method)
                && CARD_DATA_REPORTED.compareAndSet(false, true)) {
              String payload = result instanceof Bundle
                  ? ((Bundle) result).getString("deviceList", "") : "";
              boolean contains = payload != null && payload.contains("psassistant_");
              boolean coordinated = contains && payload.contains("COORDINATED");
              String reason = "oemGetCardDataSeen=true psassistantDevice=" + contains
                  + " coordinated=" + coordinated;
              module.info("quickapp_provider " + reason);
              HostReporter.report("com.heytap.mydevices",
                  processName(),
                  "quickapp-provider", true, reason);
            } else if ("com.oplus.mydevices.clickEvent".equals(method)
                && extras != null) {
              String deviceId = extras.getString("deviceId", "");
              if (deviceId != null && deviceId.startsWith("psassistant_")
                  && CLICK_REPORTED.compareAndSet(false, true)) {
                String shortcutId = extras.getString("shortcutId", "");
                String valueType = extras.containsKey("valueBoolean") ? "bool"
                    : extras.containsKey("valueInt") ? "int" : "none";
                String value = extras.containsKey("valueBoolean")
                    ? String.valueOf(extras.getBoolean("valueBoolean"))
                    : extras.containsKey("valueInt")
                        ? String.valueOf(extras.getInt("valueInt")) : "";
                String reason = "oemClickSeen=true shortcutId=" + bounded(shortcutId)
                    + " valueType=" + valueType + " value=" + bounded(value);
                module.info("quickapp_provider " + reason);
                HostReporter.report("com.heytap.mydevices",
                    processName(),
                    "quickapp-provider", true, reason);
                Application app=currentApplication();
                if(app!=null) {
                  Bundle previous=BridgeClient.call(app,"quickapp-runtime-status",null);
                  Bundle report=new Bundle();report.putBoolean("ready",true);
                  report.putString("phase","oem-click");report.putString("moduleVersion","1.14.0");
                  report.putBoolean("oemClickSeen",true);report.putLong("oemClickAt",System.currentTimeMillis());
                  report.putInt("oemClickCount",previous.getInt("oemClickCount",0)+1);
                  report.putString("lastClickShortcut",bounded(shortcutId));
                  BridgeClient.call(app,"quickapp-runtime-report",report);
                }
              }
            }
          } catch (Throwable failure) {
            module.warn("quickapp_provider_probe_failed " + failure.getClass().getSimpleName());
          }
          return result;
        });

    module.info("quickapp_provider_probe ready=true method=" + call.getName());
  }

  private static String processName() {
    try {
      Object value=Class.forName("android.app.ActivityThread")
          .getMethod("currentProcessName").invoke(null);
      return value == null ? "com.heytap.mydevices" : String.valueOf(value);
    } catch (Throwable ignored) {
      return "com.heytap.mydevices";
    }
  }

  private static Application currentApplication() {
    try {return (Application)Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }

  private static String bounded(String value) {
    if (value == null) return "";
    return value.substring(0, Math.min(value.length(), 80));
  }
}
