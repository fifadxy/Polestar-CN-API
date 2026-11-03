package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Exact-gated diagnostics and CAR shortcut allow-list adapter for MyDevices 17.4.15.
 *
 * <p>The host has two independent shortcut gates: QuickCardDeviceDataConverter first filters
 * DeviceShortcut IDs through assets/support/card_short_cut.json, and the bundled 2x2/4x2 RPK
 * filters the resulting list again through config.eventSummary. The adapter widens the first gate
 * only for the four CAR IDs already present in the verified RPK; QuickAppRpkPatch widens the second
 * gate in memory. Unknown builds remain unchanged.</p>
 */
final class QuickAppContractProbe {
  private static final String PACKAGE = "io.github.polestarcn.oppobridge";
  private static final Set<String> OWN_IDS = new LinkedHashSet<>(Arrays.asList(
      "ps_lock", "ps_unlock", "ps_climate-start", "ps_climate-stop",
      "ps_trunk-open", "ps_trunk-close", "ps_air-start", "ps_air-stop"
  ));
  private static final Set<String> OEM_CAR_IDS = new LinkedHashSet<>(Arrays.asList(
      "kongtiao", "chesuo", "shanguang", "mingdi"
  ));
  private static final AtomicBoolean whitelistReported = new AtomicBoolean();
  private static final AtomicBoolean conversionReported = new AtomicBoolean();
  private static final AtomicBoolean appliedReported = new AtomicBoolean();

  private QuickAppContractProbe() {}

  static void install(OppoBridgeModule module, ClassLoader loader) throws Exception {
    Application app = currentApplication();
    if (app != null) {
      String version = app.getPackageManager().getPackageInfo("com.heytap.mydevices", 0).versionName;
      if (!"17.4.15".equals(version)) {
        module.info("quickapp_probe skipped hostVersion=" + version);
        return;
      }
    }

    Class<?> converter = loader.loadClass("com.oplus.mydevices.quickapp.data.QuickCardDeviceDataConverter");
    Method whitelist = findWhitelist(converter);
    Method convert = findConvert(converter);

    whitelist.setAccessible(true);
    module.hook(whitelist)
        .setId("psassistant_quickapp_whitelist_probe")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain -> {
          Object result = chain.proceed();
          boolean applied=false;
          if(result instanceof Set<?>) {
            Application current=currentApplication();
            if(current!=null&&experimentEnabled(current)) {
              QuickAppCarCapability.Result capability=QuickAppCarCapability.detect(current);
              if(capability.exactPatchSupported) {
                LinkedHashSet<Object> copy=new LinkedHashSet<>((Set<?>)result);
                copy.addAll(OEM_CAR_IDS);result=copy;applied=true;
                reportApplied(current,module,capability);
              }
            }
          }
          if (result instanceof Set<?> && whitelistReported.compareAndSet(false, true)) {
            Set<?> set = (Set<?>) result;
            List<String> missing = new ArrayList<>();
            for (String id : OWN_IDS) if (!set.contains(id)) missing.add(id);
            List<String> missingOem = new ArrayList<>();
            for (String id : OEM_CAR_IDS) if (!set.contains(id)) missingOem.add(id);
            String reason = "converterWhitelistOwn=" + (missing.isEmpty() ? "all" : "blocked")
                + " missing=" + missing.size()
                + " converterWhitelistOemCar=" + (missingOem.isEmpty() ? "all" : "blocked")
                + " oemMissing=" + missingOem.size()
                + " size=" + set.size()
                + " exactConverterPatch="+applied
                + " rpkEventSummaryOwn=blocked rpkEventSummaryOemCar="+(applied?"memory-patch":"blocked")
                + " nativeHomeFallback="+(!applied);
            module.info("quickapp_gate " + reason);
            HostReporter.report("com.heytap.mydevices", "com.heytap.mydevices:cards",
                "quickapp-gates", true, reason);
          }
          return result;
        });

    convert.setAccessible(true);
    module.hook(convert)
        .setId("psassistant_quickapp_conversion_probe")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain -> {
          Object result = chain.proceed();
          try {
            if (!conversionReported.get() && isOurCar(chain.getArgs())) {
              conversionReported.set(true);
              List<String> ids = shortcutIds(result);
              String reason = "ourCarConvertedIds=" + ids
                  + " expectedPsIds=" + OWN_IDS.size()
                  + " rpkEventSummaryOwn=blocked nativeHomeFallback=true";
              module.info("quickapp_conversion " + reason);
              HostReporter.report("com.heytap.mydevices", "com.heytap.mydevices:cards",
                  "quickapp-converted", true, reason);
              Application runtimeApp=currentApplication();
              if(runtimeApp!=null) {
                Bundle report=new Bundle();report.putBoolean("ready",true);
                report.putString("phase","quickapp-converted");report.putString("moduleVersion",BridgeBuild.VERSION_NAME);
                report.putInt("selectedShortcutCount",ids.size());
                report.putString("selectedShortcutIds",ids.toString());
                BridgeClient.call(runtimeApp,"quickapp-runtime-report",report);
              }
            }
          } catch (Throwable failure) {
            module.warn("quickapp_conversion_probe_failed " + failure.getClass().getSimpleName());
          }
          return result;
        });

    module.info("quickapp_contract_probe ready=true whitelist=" + whitelist.getName()
        + " convert=" + convert.getName());
  }

  private static boolean experimentEnabled(Application app) {
    try{return BridgeClient.call(app,"oppo-oem-car-status",null).getBoolean("enabled",false);}
    catch(Throwable ignored){return false;}
  }

  private static void reportApplied(Application app,OppoBridgeModule module,
      QuickAppCarCapability.Result capability) {
    if(!appliedReported.compareAndSet(false,true))return;
    try {
      Bundle in=new Bundle();in.putBoolean("ready",true);
      in.putString("phase","converter-whitelist");in.putString("moduleVersion",BridgeBuild.VERSION_NAME);
      in.putString("hostVersion",capability.hostVersion);in.putBoolean("converterWhitelistApplied",true);
      in.putLong("converterWhitelistAt",System.currentTimeMillis());
      in.putString("rpkEntrySha256",capability.indexSha256);
      BridgeClient.call(app,"quickapp-runtime-report",in);
      module.info("quickapp_converter_whitelist applied=true ids="+OEM_CAR_IDS);
    } catch(Throwable ignored) {}
  }

  private static Method findWhitelist(Class<?> type) {
    Method named = null;
    Method shape = null;
    for (Method m : type.getDeclaredMethods()) {
      if (Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 0
          || !Set.class.isAssignableFrom(m.getReturnType())) continue;
      if ("r".equals(m.getName())) named = m;
      if (shape != null && named == null)
        throw new IllegalStateException("QuickApp whitelist method ambiguous");
      shape = m;
    }
    Method result = named != null ? named : shape;
    if (result == null) throw new IllegalStateException("QuickApp whitelist method missing");
    return result;
  }

  private static Method findConvert(Class<?> type) {
    Method named = null;
    Method shape = null;
    for (Method m : type.getDeclaredMethods()) {
      Class<?>[] p = m.getParameterTypes();
      if (Modifier.isStatic(m.getModifiers()) || p.length != 3
          || !List.class.isAssignableFrom(m.getReturnType())) continue;
      if (!p[0].isInterface() || !p[1].isEnum() || !p[2].isEnum()) continue;
      if ("q".equals(m.getName())) named = m;
      if (shape != null && named == null)
        throw new IllegalStateException("QuickApp converter method ambiguous");
      shape = m;
    }
    Method result = named != null ? named : shape;
    if (result == null) throw new IllegalStateException("QuickApp converter method missing");
    return result;
  }

  private static boolean isOurCar(List<Object> args) throws Exception {
    if (args == null || args.size() < 2 || args.get(0) == null || args.get(1) == null) return false;
    Object device = args.get(0);
    Method pkg = device.getClass().getMethod("getDeviceAppPackage");
    String packageName = String.valueOf(pkg.invoke(device));
    String type = String.valueOf(args.get(1));
    return (PACKAGE.equals(packageName) || RuntimeBridgeClient.PSASSISTANT.equals(packageName))
        && "CAR".equals(type);
  }

  private static List<String> shortcutIds(Object result) throws Exception {
    List<String> ids = new ArrayList<>();
    if (!(result instanceof List<?>)) return ids;
    for (Object item : (List<?>) result) {
      if (item == null) continue;
      Method id = item.getClass().getMethod("getId");
      ids.add(String.valueOf(id.invoke(item)));
    }
    return ids;
  }

  private static Application currentApplication() {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch (Throwable ignored) {
      return null;
    }
  }
}
