package io.github.polestarcn.companion;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Narrow local IPC used by the optional ColorOS bridge.
 *
 * <p>No account credential, token, VIN, raw API response or private signing material is exposed.
 * Calls from other UIDs are accepted only when every resolved caller package is in the explicit
 * ColorOS/bridge allow-list. Vehicle writes are posted onto the app main thread and then use the
 * existing VehicleCoordinator command/readback path.</p>
 */
public final class PsAssistantBridgeProvider extends ContentProvider {
  public static final String AUTHORITY = "io.github.polestarcn.companion.bridge";
  public static final Uri URI = Uri.parse("content://" + AUTHORITY);
  public static final int PROTOCOL_VERSION = 1;

  private static final Set<String> ALLOWED_CALLERS = new HashSet<>(Arrays.asList(
      "io.github.polestarcn.oppobridge",
      "com.heytap.mydevices",
      "com.coloros.assistantscreen",
      "com.oplus.pantanal.ums",
      "com.heytap.speechassist",
      "com.oplus.ocar"
  ));

  private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
      "lock", "unlock",
      "trunk-open", "trunk-close",
      "climate-start", "climate-stop",
      "air-start", "air-stop",
      "windows-open", "windows-close",
      "honk", "flash", "honk-flash"
  ));

  private Handler main;

  @Override public boolean onCreate() {
    main = new Handler(Looper.getMainLooper());
    return true;
  }

  @Override public Bundle call(String method, String arg, Bundle extras) {
    enforceCaller();
    Context context = contextOrThrow();
    Bundle out = new Bundle();
    out.putInt("protocolVersion", PROTOCOL_VERSION);
    out.putBoolean("cardEnabled", OppoDeviceIntegration.enabled(context));
    if (!OppoDeviceIntegration.enabled(context)) {
      out.putBoolean("available", false);
      out.putBoolean("accepted", false);
      out.putString("reason", "integration-disabled");
      return out;
    }

    if ("capabilities".equals(method)) {
      out.putStringArray("commands", ALLOWED_COMMANDS.toArray(new String[0]));
      out.putBoolean("state", true);
      out.putBoolean("refresh", true);
      return out;
    }

    VehicleCoordinator coordinator = VehicleCoordinator.get(context);

    if ("snapshot".equals(method)) {
      VehicleSnapshot state = coordinator.snapshot;
      if (state == null) state = VehicleSnapshotStore.loadChina(context);
      writeSnapshot(out, state);
      return out;
    }

    if ("refresh".equals(method)) {
      if (!coordinator.hasSession()) {
        out.putBoolean("accepted", false);
        out.putString("reason", "not-signed-in");
        return out;
      }
      main.post(coordinator::refreshQuiet);
      out.putBoolean("accepted", true);
      out.putString("reason", "queued");
      return out;
    }

    if ("command".equals(method)) {
      String action = extras == null ? null : extras.getString("action");
      if (action == null || !ALLOWED_COMMANDS.contains(action)) {
        out.putBoolean("accepted", false);
        out.putString("reason", "unsupported-command");
        return out;
      }
      if (!coordinator.canCommand(action)) {
        out.putBoolean("accepted", false);
        out.putString("reason", coordinator.hasSession() ? "vehicle-not-ready-or-busy" : "not-signed-in");
        return out;
      }
      main.post(() -> coordinator.command(action));
      out.putBoolean("accepted", true);
      out.putString("reason", "queued");
      out.putString("action", action);
      return out;
    }

    throw new IllegalArgumentException("Unknown bridge method: " + method);
  }

  private static void writeSnapshot(Bundle out, VehicleSnapshot state) {
    if (state == null) {
      out.putBoolean("available", false);
      return;
    }
    out.putBoolean("available", true);
    out.putString("name", state.name);
    out.putString("source", state.source);
    out.putBoolean("demo", state.isDemo);
    putInt(out, "batteryPercent", state.batteryPercent);
    putInt(out, "rangeKm", state.rangeKm);
    putInt(out, "odometerKm", state.odometerKm);
    out.putString("charging", state.charging);
    out.putString("doors", state.doors);
    out.putString("windows", state.windows);
    out.putString("tailgate", state.tailgate);
    out.putString("lock", state.lock);
    out.putString("climate", state.climate);
    out.putString("air", state.air);
    out.putLong("capturedAt", state.timestampMillis);
  }

  private static void putInt(Bundle out, String key, Integer value) {
    if (value != null) {
      out.putBoolean(key + "Known", true);
      out.putInt(key, value);
    } else {
      out.putBoolean(key + "Known", false);
    }
  }

  private Context contextOrThrow() {
    Context context = getContext();
    if (context == null) throw new IllegalStateException("Bridge provider has no context");
    return context;
  }

  private void enforceCaller() {
    Context context = contextOrThrow();
    int uid = Binder.getCallingUid();
    if (uid == Process.myUid()) return;

    PackageManager pm = context.getPackageManager();
    String[] packages = pm.getPackagesForUid(uid);
    if (packages == null || packages.length == 0) {
      throw new SecurityException("Bridge caller has no package identity");
    }

    for (String pkg : packages) {
      if (ALLOWED_CALLERS.contains(pkg)) return;
    }
    throw new SecurityException("Bridge caller is not allowed");
  }

  @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
  @Override public String getType(Uri uri) { return null; }
  @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
  @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
  @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
