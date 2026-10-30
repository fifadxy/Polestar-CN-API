package io.github.polestarcn.oppobridge;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Structural ColorOS host probe.
 *
 * <p>ColorOS 16 and 17 rename/obfuscate private classes aggressively. The probe intentionally
 * verifies class/method shape before any future hook is enabled. Missing or ambiguous targets are
 * logged and left untouched.</p>
 */
final class HostProbe {
  private HostProbe() {}

  static void install(OppoBridgeModule module, String pkg, ClassLoader loader) {
    switch (pkg) {
      case "com.heytap.mydevices":
        probeMyDevices(module, loader);
        return;
      case "com.coloros.assistantscreen":
        module.info("host_ready package=com.coloros.assistantscreen role=shelf candidate=true");
        return;
      case "com.oplus.pantanal.ums":
        module.info("host_ready package=com.oplus.pantanal.ums role=pantanal candidate=true");
        return;
      case "com.heytap.speechassist":
        module.info("host_ready package=com.heytap.speechassist role=xiaobu candidate=true");
        return;
      case "com.oplus.ocar":
        module.info("host_ready package=com.oplus.ocar role=carplus container=true");
        return;
      default:
        module.warn("unexpected_scope package=" + pkg);
    }
  }

  private static void probeMyDevices(OppoBridgeModule module, ClassLoader loader) {
    Class<?> manager = load(loader, "com.heytap.mydevices.core.config.DeviceAppConfigManager");
    Class<?> agent = load(loader, "com.heytap.mydevices.core.agent.AppAgentManager");
    Class<?> deviceApp = load(loader, "com.oplus.mydevices.domain.entities.config.DeviceApp");

    if (manager == null || agent == null || deviceApp == null) {
      module.warn("mydevices_probe missing manager=" + (manager != null)
          + " agent=" + (agent != null) + " deviceApp=" + (deviceApp != null));
      return;
    }

    Method cache = null;
    Method namedSupport = null;
    int stringBooleanMethods = 0;
    for (Method method : manager.getDeclaredMethods()) {
      if (!Modifier.isStatic(method.getModifiers())
          && method.getParameterCount() == 0
          && CopyOnWriteArrayList.class.isAssignableFrom(method.getReturnType())) {
        if (cache != null) {
          module.warn("mydevices_probe cache=ambiguous");
          return;
        }
        cache = method;
      }
      if ("getSupportDeviceApplications".equals(method.getName()) && method.getParameterCount() == 0) {
        namedSupport = method;
      }
      if (!Modifier.isStatic(method.getModifiers())
          && method.getReturnType() == boolean.class
          && method.getParameterCount() == 1
          && method.getParameterTypes()[0] == String.class) {
        stringBooleanMethods++;
      }
    }

    Method register = null;
    Method unregister = null;
    for (Method method : agent.getDeclaredMethods()) {
      Class<?>[] p = method.getParameterTypes();
      if (!Modifier.isStatic(method.getModifiers())
          && method.getReturnType() == void.class
          && p.length == 2 && p[0] == String.class && p[1].isInterface()) {
        if ("addDeviceObserver".equals(method.getName()) || register == null) register = method;
      }
      if (!Modifier.isStatic(method.getModifiers())
          && method.getReturnType() == void.class
          && p.length == 1 && p[0] == String.class
          && !Modifier.isFinal(method.getModifiers())) {
        if ("removeDeviceObserver".equals(method.getName()) || unregister == null) unregister = method;
      }
    }

    module.info("mydevices_probe ready=true"
        + " cache=" + name(cache)
        + " support=" + name(namedSupport)
        + " register=" + name(register)
        + " unregister=" + name(unregister)
        + " supportBooleanCount=" + stringBooleanMethods);
  }

  private static Class<?> load(ClassLoader loader, String name) {
    try { return Class.forName(name, false, loader); }
    catch (Throwable ignored) { return null; }
  }

  private static String name(Method method) {
    return method == null ? "missing" : method.getName();
  }
}
