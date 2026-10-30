package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.database.ContentObserver;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONObject;

/**
 * Registers PsAssistant as a ColorOS My Devices provider.
 *
 * <p>Targets are resolved by class/method shape so ColorOS 16 R8 names and ColorOS 17
 * restored names can share one APK. Ambiguous shapes are rejected rather than guessed.</p>
 */
final class MyDevicesVehicleHook {
  private static final String PACKAGE = "io.github.polestarcn.oppobridge";
  private static final String HOST = "com.heytap.mydevices";

  private MyDevicesVehicleHook() {}

  static void installMain(OppoBridgeModule module, ClassLoader loader) throws Exception {
    Class<?> deviceApp = loader.loadClass("com.oplus.mydevices.domain.entities.config.DeviceApp");
    Class<?> deletion = loader.loadClass("com.oplus.mydevices.domain.entities.config.DelDeviceMethod");
    Class<?> manager = loader.loadClass("com.heytap.mydevices.core.config.DeviceAppConfigManager");
    Class<?> agent = loader.loadClass("com.heytap.mydevices.core.agent.AppAgentManager");

    Object none = deletion.getField("NONE").get(null);
    Object ours = deviceApp.getConstructor(String.class, String.class, long.class, int.class,
        boolean.class, boolean.class, Boolean.class, deletion)
        .newInstance("Polestar", PACKAGE, 0L, 0, true, true, Boolean.TRUE, none);
    Method packageOf = deviceApp.getMethod("getPackageName");

    Method cache = uniqueCache(manager);
    Method supportList = HostMethodResolver.supportList(manager);
    Method register = HostMethodResolver.register(agent);
    Method unregister = HostMethodResolver.unregister(agent);
    Class<?> observerType = register.getParameterTypes()[1];
    Method removed = uniqueObserverRemoved(observerType);
    List<Method> oneString = observerOneString(observerType);
    Method added = namedOrFirst(oneString, "onDeviceAdd", 0);
    Method updated = namedOrFirst(oneString, "onDeviceUpdated", 1);

    ResultTransform include = (args, result) -> {
      if (!(result instanceof List<?>)) return result;
      List<?> original = (List<?>) result;
      boolean enabled = providerEnabled();
      if (!enabled) {
        List<Object> filtered = result instanceof CopyOnWriteArrayList<?> ? new CopyOnWriteArrayList<>() : new ArrayList<>();
        for (Object item : original) {
          if (!deviceApp.isInstance(item) || !PACKAGE.equals(String.valueOf(packageOf.invoke(item)))) filtered.add(item);
        }
        return filtered;
      }
      boolean present = false;
      for (Object item : original) {
        if (deviceApp.isInstance(item) && PACKAGE.equals(String.valueOf(packageOf.invoke(item)))) {
          present = true;
          break;
        }
      }
      if (present) return result;
      if (result instanceof CopyOnWriteArrayList<?>) {
        CopyOnWriteArrayList<Object> copy = new CopyOnWriteArrayList<>();
        copy.addAll((List<?>) result);
        copy.add(ours);
        return copy;
      }
      ArrayList<Object> copy = new ArrayList<>();
      copy.addAll((List<?>) result);
      copy.add(ours);
      return copy;
    };
    after(module, cache, "mydevices_cache", include);
    if(supportList!=null&&!supportList.equals(cache))after(module,supportList,"mydevices_supported_apps",include);

    int supportHooks = 0;
    for (Method method : manager.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class) continue;
      Class<?>[] p = method.getParameterTypes();
      if (p.length != 1 || p[0] != String.class) continue;
      supportHooks++;
      after(module, method, "mydevices_support_" + method.getName(), (args, result) ->
          PACKAGE.equals(args.get(0)) ? Boolean.valueOf(providerEnabled()) : result);
    }


    after(module, register, "mydevices_observer_register", (args, result) -> {
      if (PACKAGE.equals(args.get(0)) && observerType.isInstance(args.get(1))) {
        CardBridge.attach(args.get(1), removed, added, updated, module);
      }
      return result;
    });
    after(module, unregister, "mydevices_observer_unregister", (args, result) -> {
      if (PACKAGE.equals(args.get(0))) CardBridge.detach();
      return result;
    });

    module.info("mydevices_vehicle_hook ready=true cache=" + cache.getName()
        + " register=" + register.getName()
        + " unregister=" + unregister.getName()
        + " supportHooks=" + supportHooks
        + " observer=" + observerType.getName());
  }

  static void installCardProcess(OppoBridgeModule module, ClassLoader loader) throws Exception {
    Class<?> action = loader.loadClass("com.oplus.mydevices.quickapp.action.QuickAppCardAction");
    Class<?> registry = null;
    for (java.lang.reflect.Field f : action.getDeclaredFields()) {
      for (Method m : f.getType().getDeclaredMethods()) {
        if (isLookup(m)) { registry = f.getType(); break; }
      }
      if (registry != null) break;
    }
    if (registry == null) throw new NoSuchMethodException("QuickApp card registry missing");

    List<Method> lookups = new ArrayList<>();
    for (Method m : registry.getDeclaredMethods()) if (isLookup(m)) lookups.add(m);
    if (lookups.isEmpty()) throw new NoSuchMethodException("QuickApp lookup missing");
    Class<?> controllerType = lookups.get(0).getReturnType();

    Object controller = Proxy.newProxyInstance(loader, new Class<?>[]{controllerType}, (proxy, method, args) -> {
      if (method.getReturnType() == Map.class) return Collections.emptyMap();
      if (method.getParameterCount() == 1) {
        openPanel(args == null ? null : args[0], module);
        return null;
      }
      if ("toString".equals(method.getName())) return "PsAssistantVehicleCardController";
      return null;
    });

    for (Method lookup : lookups) {
      after(module, lookup, "mydevices_card_lookup_" + lookup.getName(), (args, result) -> {
        if (result == null && PACKAGE.equals(args.get(0))) return controller;
        return result;
      });
    }
    module.info("mydevices_card_router ready=true lookups=" + lookups.size());
  }

  private static void openPanel(Object device, OppoBridgeModule module) {
    try {
      Application app = currentApplication();
      if (app == null) return;
      String id = null;
      if (device != null) {
        try {
          Method m = device.getClass().getMethod("getMyDeviceId");
          id = String.valueOf(m.invoke(device));
        } catch (Throwable ignored) {}
      }
      Intent intent = new Intent("com.oplus.mydevices.ACTION_DEVICE_DETAILED_PANEL")
          .setPackage(PACKAGE)
          .putExtra("device_id", id)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      app.startActivity(intent);
      module.info("mydevices_card_opened id=" + id);
    } catch (Throwable t) {
      module.warn("mydevices_card_open_failed " + t);
    }
  }

  private static boolean isLookup(Method m) {
    return !Modifier.isStatic(m.getModifiers())
        && m.getParameterCount() == 2
        && m.getParameterTypes()[0] == String.class
        && m.getParameterTypes()[1] == String.class
        && m.getReturnType().isInterface()
        && m.getReturnType().getDeclaredMethods().length == 3;
  }

  private static Method uniqueCache(Class<?> manager) {
    Method found = null;
    for (Method method : manager.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) continue;
      if (!CopyOnWriteArrayList.class.isAssignableFrom(method.getReturnType())) continue;
      if (found != null) throw new IllegalStateException("MyDevices cache ambiguous");
      found = method;
    }
    if (found == null) throw new IllegalStateException("MyDevices cache missing");
    return found;
  }

  private static Method uniqueObserverRemoved(Class<?> observer) {
    try {
      Method named=observer.getMethod("onDeviceRemoved",String.class,boolean.class,String.class);
      if(named.getReturnType()==void.class&&!Modifier.isStatic(named.getModifiers()))return named;
    } catch(NoSuchMethodException ignored) {}
    Method found=null;
    for(Method m:observer.getMethods()) {
      Class<?>[] p=m.getParameterTypes();
      if(!Modifier.isStatic(m.getModifiers())&&m.getReturnType()==void.class&&p.length==3&&p[0]==String.class&&p[1]==boolean.class&&p[2]==String.class) {
        if(found!=null)throw new IllegalStateException("Observer remove callback ambiguous");
        found=m;
      }
    }
    if(found==null)throw new IllegalStateException("Observer remove callback missing");
    return found;
  }

  private static List<Method> observerOneString(Class<?> observer) {
    List<Method> result = new ArrayList<>();
    for (Method m : observer.getMethods()) {
      Class<?>[] p = m.getParameterTypes();
      if (m.getReturnType() == void.class && p.length == 1 && p[0] == String.class) result.add(m);
    }
    result.sort(java.util.Comparator.comparing(Method::getName));
    if (result.size() < 2) throw new IllegalStateException("Observer add/update callbacks missing");
    return result;
  }

  private static Method namedOrFirst(List<Method> methods, String name, int fallback) {
    for (Method m : methods) if (name.equals(m.getName())) return m;
    if(methods.size()!=2)throw new IllegalStateException("Observer add/update ambiguous");
    return methods.get(fallback);
  }

  private interface ResultTransform {
    Object apply(List<Object> args, Object result) throws Throwable;
  }

  private static void after(OppoBridgeModule module, Method method, String id, ResultTransform transform) {
    method.setAccessible(true);
    module.hook(method)
        .setId(id)
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain -> {
          Object result = chain.proceed();
          try {
            return transform.apply(chain.getArgs(), result);
          } catch (Throwable t) {
            module.warn("hook_failed id=" + id + " " + t);
            return result;
          }
        });
  }

  private static boolean providerEnabled() {
    Application app=currentApplication();
    return app!=null&&app.getPackageManager().resolveContentProvider(VehicleDeviceCardProvider.AUTHORITY,0)!=null;
  }

  private static Application currentApplication() {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch (Throwable t) {
      return null;
    }
  }

  private static final class CardBridge {
    private static volatile long generation;
    private static HandlerThread thread;
    private static ContentObserver observer;
    private static String lastId;
    private static long lastRevision = Long.MIN_VALUE;

    static synchronized void attach(Object target, Method removed, Method added,
        Method updated, OppoBridgeModule module) {
      detach();
      final long session=generation;
      Application app = currentApplication();
      if (app == null) {
        module.warn("mydevices_card_bridge no_application");
        return;
      }
      HandlerThread t = new HandlerThread("psassistant-vehicle-card");
      t.start();
      Handler handler = new Handler(t.getLooper());
      Runnable refresh = new Runnable() {
        private int failures;
        @Override public void run() {
        if(session!=generation)return;
        try {
          String id = null;
          long revision = Long.MIN_VALUE;
          try (android.database.Cursor c = app.getContentResolver().query(
              VehicleDeviceCardProvider.URI,
              new String[]{"device_id", "device_data"}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
              id = c.getString(0);
              revision = new JSONObject(c.getString(1)).optLong("revision", Long.MIN_VALUE);
            }
          }
          if(session!=generation)return;
          String before = lastId;
          if (id == null && before != null) {
            removed.invoke(target, before, false, PACKAGE);
          } else if (id != null && !id.equals(before)) {
            if (before != null) removed.invoke(target, before, false, PACKAGE);
            added.invoke(target, id);
            HostReporter.report(HOST, HOST, "card-added", true, "");
          } else if (id != null && revision != lastRevision) {
            updated.invoke(target, id);
          }
          if(session!=generation)return;
          failures=0;
          lastId = id;
          lastRevision = revision;
        } catch (Throwable e) {
          if(session!=generation)return;
          if(lastId!=null)try {removed.invoke(target,lastId,false,PACKAGE);}catch(Throwable ignored){}
          lastId=null;lastRevision=Long.MIN_VALUE;
          module.warn("mydevices_card_refresh_failed " + e);
          if(providerEnabled()&&++failures<=3)handler.postDelayed(this,failures*1000L);
        }
        }
      };
      ContentObserver o = new ContentObserver(handler) {
        @Override public void onChange(boolean selfChange) { refresh.run(); }
      };
      thread = t;
      observer = o;
      try {app.getContentResolver().registerContentObserver(VehicleDeviceCardProvider.URI, false, o);}
      catch(RuntimeException failure) {detach();throw failure;}
      handler.post(refresh);
      module.info("mydevices_card_observer attached=true");
    }

    static synchronized void detach() {
      generation++;
      Application app = currentApplication();
      if (observer != null && app != null) {
        try { app.getContentResolver().unregisterContentObserver(observer); } catch (Throwable ignored) {}
      }
      if (thread != null) thread.quitSafely();
      observer = null;
      thread = null;
      lastId = null;
      lastRevision = Long.MIN_VALUE;
    }
  }
}
