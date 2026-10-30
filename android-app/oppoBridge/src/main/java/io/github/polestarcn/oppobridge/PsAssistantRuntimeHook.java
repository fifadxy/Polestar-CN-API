package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Injected into an existing PsAssistant process so the OPPO bridge can reuse its live China
 * account/session without modifying or re-signing the installed PsAssistant APK.
 */
final class PsAssistantRuntimeHook {
  private static final String BRIDGE_PACKAGE = "io.github.polestarcn.oppobridge";
  private static final Set<String> COMMANDS = new HashSet<>(Arrays.asList(
      "lock", "unlock", "trunk-open", "trunk-close", "climate-start", "climate-stop",
      "air-start", "air-stop", "windows-open", "windows-close", "honk", "flash", "honk-flash"
  ));
  private static boolean registered;

  private PsAssistantRuntimeHook() {}

  static void install(OppoBridgeModule module, ClassLoader loader) {
    Application current = currentApplication();
    if (current != null) {
      register(module, current, loader);
      return;
    }
    try {
      Method onCreate = Application.class.getDeclaredMethod("onCreate");
      module.hook(onCreate)
          .setId("psassistant_runtime_application")
          .setExceptionMode(io.github.libxposed.api.XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            if (self instanceof Application) {
              Application app = (Application) self;
              if (RuntimeBridgeClient.PSASSISTANT.equals(app.getPackageName())) register(module, app, loader);
            }
            return result;
          });
      module.info("psassistant_runtime_hook waiting_for_application=true");
    } catch (Throwable t) {
      module.warn("psassistant_runtime_hook application_hook_failed " + t);
    }
  }

  private static synchronized void register(OppoBridgeModule module, Application app, ClassLoader loader) {
    if (registered) return;
    if (Build.VERSION.SDK_INT < 34) {
      module.warn("psassistant_runtime_hook unsupported_api=" + Build.VERSION.SDK_INT);
      return;
    }
    try {
      Class<?> coordinatorType = loader.loadClass("io.github.polestarcn.companion.VehicleCoordinator");
      Method get = coordinatorType.getMethod("get", Context.class);
      Method hasSession = coordinatorType.getMethod("hasSession");
      Method canCommand = coordinatorType.getMethod("canCommand", String.class);
      Method command = coordinatorType.getMethod("command", String.class);
      Method refreshQuiet = coordinatorType.getMethod("refreshQuiet");
      Field snapshot = coordinatorType.getField("snapshot");

      BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
          if (!RuntimeBridgeClient.ACTION.equals(intent.getAction())) return;
          ResultReceiver reply = (ResultReceiver) intent.getParcelableExtra("reply");
          if (!authorizedSender(this, app)) {
            if (reply != null) {
              Bundle denied = new Bundle();
              denied.putBoolean("accepted", false);
              denied.putString("reason", "unauthorized-caller");
              reply.send(403, denied);
            }
            return;
          }
          String method = intent.getStringExtra("method");
          Bundle payload = intent.getBundleExtra("payload");
          Bundle out = new Bundle();
          out.putInt("protocolVersion", 1);
          // Older installed clients have no setting; newer clients own the opt-in.
          try {
            Class<?> gate = loader.loadClass("io.github.polestarcn.companion.OppoDeviceIntegration");
            boolean enabled = Boolean.TRUE.equals(gate.getMethod("enabled", Context.class).invoke(null, app));
            out.putBoolean("cardEnabled", enabled);
            if (!enabled) {
              out.putBoolean("available", false);
              out.putBoolean("accepted", false);
              out.putString("reason", "integration-disabled");
              if (reply != null) reply.send(200, out);
              return;
            }
          } catch (ClassNotFoundException legacyClient) {
            out.putBoolean("cardEnabled", true);
          } catch (Throwable gateFailure) {
            out.putBoolean("accepted", false);
            out.putString("reason", "integration-gate-unavailable");
            if (reply != null) reply.send(200, out);
            return;
          }
          try {
            Object coordinator = get.invoke(null, app);
            if ("capabilities".equals(method)) {
              out.putBoolean("state", true);
              out.putBoolean("refresh", true);
              out.putStringArray("commands", COMMANDS.toArray(new String[0]));
            } else if ("snapshot".equals(method)) {
              writeSnapshot(out, snapshot.get(coordinator));
            } else if ("refresh".equals(method)) {
              if (!Boolean.TRUE.equals(hasSession.invoke(coordinator))) {
                out.putBoolean("accepted", false);
                out.putString("reason", "not-signed-in");
              } else {
                refreshQuiet.invoke(coordinator);
                out.putBoolean("accepted", true);
                out.putString("reason", "queued");
              }
            } else if ("command".equals(method)) {
              String action = payload == null ? null : payload.getString("action");
              if (action == null || !COMMANDS.contains(action)) {
                out.putBoolean("accepted", false);
                out.putString("reason", "unsupported-command");
              } else if (!Boolean.TRUE.equals(canCommand.invoke(coordinator, action))) {
                out.putBoolean("accepted", false);
                out.putString("reason", "vehicle-not-ready-or-busy");
              } else {
                command.invoke(coordinator, action);
                out.putBoolean("accepted", true);
                out.putString("reason", "queued");
                out.putString("action", action);
              }
            } else {
              out.putBoolean("accepted", false);
              out.putString("reason", "unknown-method");
            }
          } catch (Throwable failure) {
            out.putBoolean("accepted", false);
            out.putBoolean("available", false);
            out.putString("reason", failure.getClass().getSimpleName() + ": " + failure.getMessage());
            module.warn("psassistant_runtime_call_failed method=" + method + " " + failure);
          }
          if (reply != null) reply.send(200, out);
        }
      };

      app.registerReceiver(receiver, new IntentFilter(RuntimeBridgeClient.ACTION), Context.RECEIVER_EXPORTED);
      registered = true;
      module.info("psassistant_runtime_hook ready=true package=" + app.getPackageName());
    } catch (Throwable t) {
      module.warn("psassistant_runtime_hook install_failed " + t);
    }
  }

  private static boolean authorizedSender(BroadcastReceiver receiver, Context app) {
    if (Build.VERSION.SDK_INT < 34) return false;
    try {
      int uid = receiver.getSentFromUid();
      String[] packages = app.getPackageManager().getPackagesForUid(uid);
      if (packages != null) for (String pkg : packages) if (BRIDGE_PACKAGE.equals(pkg)) return true;
    } catch (Throwable ignored) {}
    return false;
  }

  private static void writeSnapshot(Bundle out, Object state) throws Exception {
    if (state == null) {
      out.putBoolean("available", false);
      return;
    }
    Class<?> type = state.getClass();
    out.putBoolean("available", true);
    out.putString("name", stringField(type, state, "name"));
    out.putString("source", stringField(type, state, "source"));
    out.putBoolean("demo", booleanField(type, state, "isDemo"));
    putInteger(out, "batteryPercent", field(type, state, "batteryPercent"));
    putInteger(out, "rangeKm", field(type, state, "rangeKm"));
    putInteger(out, "odometerKm", field(type, state, "odometerKm"));
    out.putString("charging", stringField(type, state, "charging"));
    out.putString("doors", stringField(type, state, "doors"));
    out.putString("windows", stringField(type, state, "windows"));
    out.putString("tailgate", stringField(type, state, "tailgate"));
    out.putString("lock", stringField(type, state, "lock"));
    out.putString("climate", stringField(type, state, "climate"));
    out.putString("air", stringField(type, state, "air"));
    Object timestamp = field(type, state, "timestampMillis");
    out.putLong("capturedAt", timestamp instanceof Number ? ((Number) timestamp).longValue() : 0L);
  }

  private static Object field(Class<?> type, Object target, String name) throws Exception {
    return type.getField(name).get(target);
  }
  private static String stringField(Class<?> type, Object target, String name) throws Exception {
    Object value = field(type, target, name);
    return value == null ? "" : String.valueOf(value);
  }
  private static boolean booleanField(Class<?> type, Object target, String name) throws Exception {
    Object value = field(type, target, name);
    return value instanceof Boolean && (Boolean) value;
  }
  private static void putInteger(Bundle out, String key, Object value) {
    if (value instanceof Number) {
      out.putBoolean(key + "Known", true);
      out.putInt(key, ((Number) value).intValue());
    } else {
      out.putBoolean(key + "Known", false);
    }
  }

  private static Application currentApplication() {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch (Throwable t) {
      return null;
    }
  }
}
