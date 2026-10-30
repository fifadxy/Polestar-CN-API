package io.github.polestarcn.oppobridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ColorOS My Devices provider for the currently selected Polestar.
 *
 * <p>My Devices discovers providers with the DEVICE_INFO action, queries
 * device_info_table_new and parses device_data into its private DeviceModel.
 * The provider never holds Polestar credentials; state and commands are forwarded to
 * PsAssistantBridgeProvider.</p>
 */
public final class VehicleDeviceCardProvider extends ContentProvider {
  public static final String AUTHORITY = "io.github.polestarcn.oppobridge.devices";
  public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/device_info_table_new");
  public static final String METHOD_COMMAND = "ps_vehicle_command";
  public static final String METHOD_REFRESH = "ps_vehicle_refresh";

  private static final String TAG = "PsVehicleCard";
  private static final String[] COLUMNS = {"device_id", "device_mac", "device_data", "authority"};
  private static final Set<String> HOSTS = new HashSet<>(Arrays.asList(
      "com.heytap.mydevices",
      "com.oplus.wirelesssettings"
  ));
  private static final String CALLBACK_DESCRIPTOR = "com.heytap.deviceinfo.DeviceAppCallbackInterface";
  private static final int CALLBACK_RESULT = 196611;
  private ContentObserver upstream;

  @Override public boolean onCreate() {
    Context context = getContext();
    if (context != null) {
      upstream = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) {
          context.getContentResolver().notifyChange(URI, null);
        }
      };
      try {
        context.getContentResolver().registerContentObserver(BridgeClient.URI, false, upstream);
      } catch (RuntimeException unavailable) {
        Log.w(TAG, "PsAssistant observer unavailable", unavailable);
      }
    }
    return true;
  }

  @Override public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    requireHost();
    if (!URI.equals(uri)) throw new IllegalArgumentException("Unsupported vehicle card URI");
    if (sortOrder != null) throw new IllegalArgumentException("Sort order not supported");

    String[] columns = projection == null ? COLUMNS : projection.clone();
    MatrixCursor cursor = new MatrixCursor(columns, 1);
    cursor.setNotificationUri(context().getContentResolver(), URI);

    Bundle state;
    try {
      state = BridgeClient.snapshot(context());
    } catch (RuntimeException unavailable) {
      return cursor;
    }
    if (!state.getBoolean("available", false) || state.getBoolean("demo", false)) return cursor;
    if (context().getSharedPreferences("vehicle-card", Context.MODE_PRIVATE)
        .getBoolean("hidden", false)) return cursor;

    String id = deviceId(state);
    String pseudoMac = "02:50:53:41:00:01";
    if (selection != null) {
      String arg = selectionArgs != null && selectionArgs.length > 0 ? selectionArgs[0] : null;
      if (arg == null || !(arg.equals(id) || arg.equals(pseudoMac) || arg.equals(AUTHORITY))) return cursor;
    }

    JSONObject data = deviceData(state, id, pseudoMac);
    MatrixCursor.RowBuilder row = cursor.newRow();
    for (String column : columns) {
      switch (column) {
        case "device_id": row.add(id); break;
        case "device_mac": row.add(pseudoMac); break;
        case "device_data": row.add(data.toString()); break;
        case "authority": row.add(AUTHORITY); break;
        default: row.add(null);
      }
    }
    return cursor;
  }

  @Override public Bundle call(String method, String arg, Bundle extras) {
    requireHost();
    Log.i(TAG, "call " + method);
    android.os.IBinder callback = extras == null ? null : extras.getBinder("callBinder");
    boolean ok = true;
    Bundle result = new Bundle();
    try {
      if (METHOD_COMMAND.equals(method)) {
        String action = extras == null ? null : extras.getString("extra");
        Bundle response = BridgeClient.command(context(), action);
        ok = response.getBoolean("accepted", false);
        result.putAll(response);
      } else if (METHOD_REFRESH.equals(method)) {
        Bundle response = BridgeClient.refresh(context());
        ok = response.getBoolean("accepted", false);
        result.putAll(response);
      } else if ("delete_device".equals(method)) {
        context().getSharedPreferences("vehicle-card", Context.MODE_PRIVATE)
            .edit().putBoolean("hidden", true).apply();
        context().getContentResolver().notifyChange(URI, null);
      } else if ("connect".equals(method) || "card_show".equals(method)) {
        context().getSharedPreferences("vehicle-card", Context.MODE_PRIVATE)
            .edit().remove("hidden").apply();
        try { BridgeClient.refresh(context()); } catch (RuntimeException ignored) {}
      } else if ("disconnect".equals(method) || "card_hide".equals(method)) {
        // Vehicle cloud connectivity is managed by PsAssistant; no local disconnect is needed.
      }
    } catch (Throwable failure) {
      ok = false;
      result.putString("reason", failure.getClass().getSimpleName() + ": " + failure.getMessage());
      Log.w(TAG, "card call failed", failure);
    }
    result.putBoolean("result", ok);
    if (callback != null) reply(callback, ok);
    return result;
  }

  static JSONObject deviceData(Bundle state, String id, String pseudoMac) {
    try {
      int soc = state.getBoolean("batteryPercentKnown", false) ? state.getInt("batteryPercent") : -1;
      JSONArray batteries = new JSONArray();
      JSONArray levels = new JSONArray();
      if (soc >= 0 && soc <= 100) {
        boolean charging = !"未充电".equals(state.getString("charging", ""))
            && !"Not charging".equalsIgnoreCase(state.getString("charging", ""));
        batteries.put(new JSONObject().put("batteryType", "SINGLE").put("value", soc).put("charge", charging));
        levels.put(soc);
      }
      long now = Math.max(state.getLong("capturedAt", 0L), System.currentTimeMillis());
      JSONArray shortcuts = shortcuts(state);
      String name = state.getString("name", "Polestar");
      return new JSONObject()
          .put("mDeviceId", id)
          .put("mDeviceName", name == null || name.isEmpty() ? "Polestar" : name)
          .put("mMacAddress", pseudoMac)
          .put("modelId", "POLESTAR")
          .put("mDeviceType", "CAR")
          .put("iconUrl", "android.resource://io.github.polestarcn.oppobridge/drawable/ic_vehicle_card")
          .put("mIconUrl", "android.resource://io.github.polestarcn.oppobridge/drawable/ic_vehicle_card")
          .put("mBatteryInfoList", batteries)
          .put("mBatteryList", levels)
          .put("connection", new JSONObject()
              .put("connectState", "CONNECTED")
              .put("coordinationState", "DEFAULT")
              .put("lastConnectTime", now)
              .put("lastDisconnectTime", 0))
          .put("mConnectState", "CONNECTED")
          .put("mAuthority", AUTHORITY)
          .put("isActive", true)
          .put("cardStyle", 1)
          .put("timestamp", now)
          .put("versionCode", 1L)
          .put("feature", 0)
          .put("linkageVersion", 0)
          .put("autoSwitch", false)
          .put("shortcuts", shortcuts)
          .put("revision", now);
    } catch (Exception e) {
      throw new IllegalStateException("Vehicle card serialization failed", e);
    }
  }

  private static JSONArray shortcuts(Bundle state) throws Exception {
    JSONArray out = new JSONArray();
    String lock = state.getString("lock", "");
    addShortcut(out, ("已上锁".equals(lock) || "已锁".equals(lock)) ? "解锁" : "上锁",
        ("已上锁".equals(lock) || "已锁".equals(lock)) ? "unlock" : "lock");

    String climate = state.getString("climate", "");
    addShortcut(out, "开启".equals(climate) ? "关闭空调" : "开启空调",
        "开启".equals(climate) ? "climate-stop" : "climate-start");

    String tailgate = state.getString("tailgate", "");
    addShortcut(out, ("已打开".equals(tailgate) || "未关严".equals(tailgate)) ? "关闭尾门" : "开启尾门",
        ("已打开".equals(tailgate) || "未关严".equals(tailgate)) ? "trunk-close" : "trunk-open");

    String air = state.getString("air", "");
    addShortcut(out, "开启".equals(air) ? "关闭净化" : "空气净化",
        "开启".equals(air) ? "air-stop" : "air-start");
    return out;
  }

  private static void addShortcut(JSONArray out, String label, String action) throws Exception {
    JSONObject extra = new JSONObject()
        .put("authority", AUTHORITY)
        .put("method", METHOD_COMMAND)
        .put("extra", action);
    out.put(new JSONObject()
        .put("shortcutId", "ps_" + action)
        .put("shortcutName", label)
        .put("action", "")
        .put("links", "")
        .put("componentType", 2)
        .put("componentExtra", extra.toString())
        .put("style", 0)
        .put("state", 0));
  }

  private static String deviceId(Bundle state) {
    String seed = state.getString("name", "Polestar") + "|" + state.getInt("protocolVersion", 1);
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder("psassistant_");
      for (int i = 0; i < 6; i++) out.append(String.format("%02x", digest[i]));
      return out.toString();
    } catch (Exception ignored) {
      return "psassistant_polestar";
    }
  }

  private void reply(android.os.IBinder binder, boolean ok) {
    Parcel data = Parcel.obtain();
    Parcel out = Parcel.obtain();
    try {
      data.writeInterfaceToken(CALLBACK_DESCRIPTOR);
      data.writeInt(CALLBACK_RESULT);
      data.writeInt(1);
      Bundle response = new Bundle();
      response.putInt("result_code", ok ? 100 : 1);
      response.putString("result_msg", ok ? "ok" : "failed");
      response.writeToParcel(data, 0);
      binder.transact(1, data, out, 0);
      out.readException();
    } catch (Exception e) {
      Log.w(TAG, "callback failed", e);
    } finally {
      out.recycle();
      data.recycle();
    }
  }

  private void requireHost() {
    int uid = Binder.getCallingUid();
    if (uid == Process.myUid() || uid == 0 || uid == Process.SHELL_UID) return;
    String[] packages = context().getPackageManager().getPackagesForUid(uid);
    if (packages != null) for (String pkg : packages) if (HOSTS.contains(pkg)) return;
    throw new SecurityException("Not a ColorOS vehicle-card host");
  }

  private Context context() {
    Context c = getContext();
    if (c == null) throw new IllegalStateException("No provider context");
    return c;
  }

  @Override public String getType(Uri uri) {
    return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".device_info";
  }
  @Override public Uri insert(Uri uri, ContentValues values) { return null; }
  @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
  @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
