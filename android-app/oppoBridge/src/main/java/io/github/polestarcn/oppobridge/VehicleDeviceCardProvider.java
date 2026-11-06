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
  public static final String AUTHORITY = BridgeIdentity.STANDALONE_AUTHORITY;
  public static final String INTEGRATED_AUTHORITY = BridgeIdentity.INTEGRATED_AUTHORITY;
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
  public static final String METHOD_DIAGNOSTICS = "ps_vehicle_diagnostics";
  private static volatile long lastQueryAt;
  private static volatile String lastQueryReason="not-queried";
  private static volatile int lastRows;
  private static volatile long lastHostQueryAt;
  private static volatile String lastHostQueryPackage="";
  private static volatile int hostQueryCount;
  private static volatile long lastHostCallAt;
  private static volatile String lastHostCallPackage="";
  private static volatile String lastHostCallMethod="";
  private static volatile int hostCallCount;
  private static volatile long lastSelfTestAt;
  private ContentObserver upstream;

  @Override public boolean onCreate() {
    Context context = getContext();
    if (context != null) {
      upstream = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) {
          context.getContentResolver().notifyChange(BridgeIdentity.deviceUri(context), null);
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
    Caller caller=requireHost();
    long identity=Binder.clearCallingIdentity();
    try { return queryLocal(uri,projection,selection,selectionArgs,sortOrder,caller); }
    finally { Binder.restoreCallingIdentity(identity); }
  }

  private Cursor queryLocal(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder,Caller caller) {
    requireUri(uri);
    if(projection!=null&&projection.length>32)throw new IllegalArgumentException("Projection limit");
    String[] columns = projection == null ? COLUMNS : projection.clone();
    MatrixCursor cursor = new MatrixCursor(columns, 1);
    cursor.setNotificationUri(context().getContentResolver(), BridgeIdentity.deviceUri(context()));

    Bundle state;
    try {
      state = BridgeClient.snapshot(context());
    } catch (RuntimeException unavailable) {
      return observed(cursor,"upstream-unavailable",caller);
    }
    if (!state.getBoolean("cardEnabled", false)) return observed(cursor,"integration-disabled-or-upstream-unavailable",caller);
    if (!state.getBoolean("available", false)) return observed(cursor,"no-snapshot",caller);
    if (state.getBoolean("demo", false)) return observed(cursor,"demo-excluded",caller);
    if (context().getSharedPreferences("vehicle-card", Context.MODE_PRIVATE)
        .getBoolean("hidden", false)) return observed(cursor,"hidden-by-user",caller);

    Bundle session;
    try {session=BridgeClient.virtualSession(context());}catch(RuntimeException unavailable){session=Bundle.EMPTY;}
    String id=session.getString("virtualCarId","");if(id.isEmpty())id=deviceId(state);
    String pseudoMac=session.getString("testMac","");if(pseudoMac.isEmpty())pseudoMac="02:50:53:41:00:01";
    if (!matchesSelection(selection,selectionArgs,id,pseudoMac)) return observed(cursor,"selection-miss",caller);

    JSONObject data = deviceData(state, id, pseudoMac, context(),session);
    MatrixCursor.RowBuilder row = cursor.newRow();
    for (String column : columns) {
      switch (column) {
        case "device_id": row.add(id); break;
        case "device_mac": row.add(pseudoMac); break;
        case "device_data": row.add(data.toString()); break;
        case "authority": row.add(BridgeIdentity.authority(context())); break;
        default: row.add(null);
      }
    }
    return observed(cursor,"row-ready",caller);
  }

  static void requireUri(Uri uri) {
    if(uri==null||!"content".equals(uri.getScheme())||!BridgeIdentity.isDeviceAuthority(uri.getAuthority())
        ||uri.getFragment()!=null||!uri.getPathSegments().equals(java.util.Collections.singletonList("device_info_table_new")))
      throw new IllegalArgumentException("Unsupported vehicle card URI");
  }
  static boolean matchesSelection(String selection,String[] args,String id,String mac) {
    if(selection==null||selection.trim().isEmpty()) {
      if(args!=null&&args.length>0)throw new IllegalArgumentException("Arguments without selection");
      return true;
    }
    if(args==null||args.length!=1)throw new IllegalArgumentException("Expected one selection argument");
    String key=selection.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    if("device_id=?".equals(key))return id.equals(args[0]);
    if("device_mac=?".equals(key))return mac.equals(args[0]);
    if("authority=?".equals(key))return BridgeIdentity.isDeviceAuthority(args[0]);
    throw new IllegalArgumentException("Unsupported vehicle selection");
  }
  private static Cursor observed(MatrixCursor cursor,String reason,Caller caller) {
    lastQueryAt=System.currentTimeMillis();lastQueryReason=reason;lastRows=cursor.getCount();
    if(caller.host) {
      lastHostQueryAt=lastQueryAt;lastHostQueryPackage=caller.packageName;hostQueryCount++;
    } else lastSelfTestAt=lastQueryAt;
    Bundle status=new Bundle();status.putString("reason",reason);status.putInt("rows",lastRows);cursor.setExtras(status);return cursor;
  }
  static Bundle runtimeStatus(Context context) {
    Bundle b=new Bundle();b.putString("authority",BridgeIdentity.authority(context));b.putString("lastQueryReason",lastQueryReason);
    b.putLong("lastQueryAt",lastQueryAt);b.putInt("lastRows",lastRows);
    b.putLong("lastHostQueryAt",lastHostQueryAt);b.putString("lastHostQueryPackage",lastHostQueryPackage);
    b.putInt("hostQueryCount",hostQueryCount);b.putBoolean("hostQueryObserved",hostQueryCount>0);
    b.putLong("lastHostCallAt",lastHostCallAt);b.putString("lastHostCallPackage",lastHostCallPackage);
    b.putString("lastHostCallMethod",lastHostCallMethod);b.putInt("hostCallCount",hostCallCount);
    b.putLong("lastSelfTestAt",lastSelfTestAt);
    b.putBoolean("hidden",context.getSharedPreferences("vehicle-card",Context.MODE_PRIVATE).getBoolean("hidden",false));return b;
  }

  @Override public Bundle call(String method, String arg, Bundle extras) {
    Caller caller=requireHost();
    if(caller.host){lastHostCallAt=System.currentTimeMillis();lastHostCallPackage=caller.packageName;
      lastHostCallMethod=method==null?"":method;hostCallCount++;}
    long identity=Binder.clearCallingIdentity();
    try { return callLocal(method,arg,extras); }
    finally { Binder.restoreCallingIdentity(identity); }
  }
  private Bundle callLocal(String method,String arg,Bundle extras) {
    if(METHOD_DIAGNOSTICS.equals(method))return runtimeStatus(context());
    if (!BridgeClient.capabilities(context()).getBoolean("cardEnabled", false)) {
      Bundle disabled = new Bundle();
      disabled.putBoolean("result", false);
      disabled.putBoolean("accepted", false);
      disabled.putString("reason", "integration-disabled");
      android.os.IBinder callback = extras == null ? null : extras.getBinder("callBinder");
      if (callback != null) reply(callback, false);
      return disabled;
    }
    Log.i(TAG, "call " + method);
    android.os.IBinder callback = extras == null ? null : extras.getBinder("callBinder");
    boolean ok = true;
    Bundle result = new Bundle();
    try {
      if (METHOD_COMMAND.equals(method)) {
        String action = extras == null ? null : extras.getString("extra");
        Bundle state=BridgeClient.snapshot(context());
        if(!state.getBoolean("available")||state.getBoolean("demo")||!VehicleShortcutActivity.fresh(state.getLong("capturedAt"),System.currentTimeMillis())) {
          result.putBoolean("result",false);result.putBoolean("accepted",false);result.putString("reason","fresh-vehicle-state-required");
          if(callback!=null)reply(callback,false);return result;
        }
        Bundle response = BridgeClient.command(context(), action);
        ok = response.getBoolean("accepted", false);
        result.putAll(response);
        result.putBoolean("vehicleConfirmed",false);
      } else if (METHOD_REFRESH.equals(method)) {
        Bundle response = BridgeClient.refresh(context());
        ok = response.getBoolean("accepted", false);
        result.putAll(response);
      } else if ("delete_device".equals(method)) {
        context().getSharedPreferences("vehicle-card", Context.MODE_PRIVATE)
            .edit().putBoolean("hidden", true).apply();
        context().getContentResolver().notifyChange(BridgeIdentity.deviceUri(context()), null);
      } else if ("connect".equals(method) || "card_show".equals(method)) {
        context().getSharedPreferences("vehicle-card", Context.MODE_PRIVATE)
            .edit().remove("hidden").apply();
        context().getContentResolver().notifyChange(BridgeIdentity.deviceUri(context()),null);
        result.putString("reason","cached-card-only");
      } else if ("disconnect".equals(method) || "card_hide".equals(method)) {
        result.putString("reason","host-event-acknowledged");
      } else {
        ok=false;
        result.putBoolean("accepted",false);
        result.putString("reason","unsupported-host-method");
      }
    } catch (Throwable failure) {
      ok = false;
      result.putString("reason", failure.getClass().getSimpleName());
      Log.w(TAG, "card call failed", failure);
    }
    result.putBoolean("result", ok);
    if (callback != null) reply(callback, ok);
    return result;
  }

  static JSONObject deviceData(Bundle state, String id, String pseudoMac) {
    return deviceData(state,id,pseudoMac,null);
  }

  static JSONObject deviceData(Bundle state, String id, String pseudoMac, Context context) {
    Bundle session=Bundle.EMPTY;
    if(context!=null)try{session=BridgeClient.virtualSession(context);}catch(RuntimeException ignored){}
    return deviceData(state,id,pseudoMac,context,session);
  }

  static JSONObject deviceData(Bundle state, String id, String pseudoMac, Context context,Bundle session) {
    try {
      int soc = state.getBoolean("batteryPercentKnown", false) ? state.getInt("batteryPercent") : -1;
      JSONArray batteries = new JSONArray();
      JSONArray levels = new JSONArray();
      if (soc >= 0 && soc <= 100) {
        boolean charging = isCharging(state.getString("charging", ""));
        batteries.put(new JSONObject().put("batteryType", "SINGLE").put("value", soc).put("charge", charging));
        levels.put(soc);
      }
      long now = Math.max(0L, state.getLong("capturedAt", 0L));
      JSONArray shortcuts = shortcuts(state,context);
      return new JSONObject()
          .put("mDeviceId", id)
          .put("mDeviceName", cardDisplayName(state))
          .put("mMacAddress", pseudoMac)
          .put("modelId", session.getString("modelId","POLESTAR_2"))
          .put("modelName", session.getString("modelName","Polestar 2"))
          .put("brand", session.getString("brand","Polestar"))
          .put("manufacturer", session.getString("manufacturer","Polestar"))
          .put("vehicleKey", state.getString("vehicleKey", id))
          .put("mDeviceType", session.getString("deviceType","CAR"))
          .put("iconUrl", BridgeIdentity.iconUri(context))
          .put("mIconUrl", BridgeIdentity.iconUri(context))
          .put("mBatteryInfoList", batteries)
          .put("mBatteryList", levels)
          .put("connection", new JSONObject()
              .put("connectState", session.getString("connectState","COORDINATED"))
              .put("coordinationState", session.getString("connectState","COORDINATED"))
              .put("lastConnectTime", now)
              .put("lastDisconnectTime", 0))
          .put("mConnectState", session.getString("connectState","COORDINATED"))
          .put("mAuthority", BridgeIdentity.authority(context))
          .put("packageName", BridgeIdentity.packageName(context))
          .put("deviceAppPackage", BridgeIdentity.packageName(context))
          .put("isActive", true)
          .put("cardStyle", 1)
          .put("timestamp", now)
          .put("versionCode", 2L)
          .put("feature", 0)
          .put("linkageVersion", 0)
          .put("autoSwitch", false)
          .put("data", carStatusData(state))
          .put("shortcuts", shortcuts)
          .put("revision", now);
    } catch (Exception e) {
      throw new IllegalStateException("Vehicle card serialization failed", e);
    }
  }

  // Retain the cached summary for compact templates; native CAR templates also
  // read CarStatusInfo from the JSON string in DeviceModel.data (17.4.15).
  // Keep the stable device ID independent of this cached-state display name.
  static String cardDisplayName(Bundle state) {
    // OEM cards read this field as a narrow single-line title. No auto "cache"
    // suffix, model name or duplicated data that would trigger an ellipsis.
    return OppoCardSummary.format(state);
  }

  static boolean isCharging(String value) {
    return "充电中".equals(value)||"正在充电".equals(value)||"智能充电".equals(value)
        ||"Charging".equalsIgnoreCase(value)||"CHARGING_IN_PROGRESS".equalsIgnoreCase(value);
  }

  static String carStatusData(Bundle state) throws Exception {
    int range = state.getInt("rangeKm", -1);
    // The OEM renderer displays this integer directly. Never turn an unknown
    // range into 0 km or -1 km; keep the existing cached-name fallback instead.
    if (!state.getBoolean("rangeKmKnown") || range < 0) return "";
    // Keep real CarStatus range in the structured property, and use the
    // same user-selected compact text in the native CAR message slot.
    String summary=OppoCardSummary.format(state);
    return new JSONObject()
        .put("batteryEnduranceMileage", range)
        .put("oilEnduranceMileage", -1)
        .put("totalEnduranceMileage", range)
        .put("isRefreshing", false)
        .put("msg", new JSONObject().put("code", 0).put("text", summary))
        .toString();
  }

  private static JSONArray shortcuts(Bundle state,Context context) throws Exception {
    JSONArray out = new JSONArray();
    if(!VehicleShortcutActivity.fresh(state.getLong("capturedAt",0L),System.currentTimeMillis()))return out;
    String lock = state.getString("lock", "");
    if("已上锁".equals(lock)||"已锁".equals(lock)||"未锁".equals(lock))addShortcut(out, ("已上锁".equals(lock) || "已锁".equals(lock)) ? "解锁" : "上锁",
        ("已上锁".equals(lock) || "已锁".equals(lock)) ? "unlock" : "lock",context);

    String climate = state.getString("climate", "");
    if("开启".equals(climate)||"关闭".equals(climate))addShortcut(out, "开启".equals(climate) ? "关闭空调" : "开启空调",
        "开启".equals(climate) ? "climate-stop" : "climate-start",context);

    String tailgate = state.getString("tailgate", "");
    if("已打开".equals(tailgate)||"未关严".equals(tailgate)||"已关闭".equals(tailgate))addShortcut(out, ("已打开".equals(tailgate) || "未关严".equals(tailgate)) ? "关闭尾门" : "开启尾门",
        ("已打开".equals(tailgate) || "未关严".equals(tailgate)) ? "trunk-close" : "trunk-open",context);

    String air = state.getString("air", "");
    if("开启".equals(air)||"关闭".equals(air))addShortcut(out, "开启".equals(air) ? "关闭净化" : "净化",
        "开启".equals(air) ? "air-stop" : "air-start",context);
    return out;
  }

  private static void addShortcut(JSONArray out, String label, String action,Context context) throws Exception {
    JSONObject extra = new JSONObject()
        .put("authority", BridgeIdentity.authority(context))
        .put("method", METHOD_COMMAND)
        .put("extra", action);
    out.put(new JSONObject()
        .put("shortcutId", "ps_" + action)
        .put("shortcutName", label)
        .put("action", "")
        .put("links", "")
        .put("componentType", 2)
        .put("componentExtra", extra.toString())
        // ShortcutMenuKt preserves style; Shortcut.isSupportShowOnHomeCards()
        // accepts bit 8. A CAR shortcut with style=0 is filtered on 17.4.15.
        .put("style", 8)
        .put("state", 0));
  }

  private static String deviceId(Bundle state) {
    String seed = state.getString("vehicleKey", "");
    if(seed.isEmpty())seed=state.getString("name", "Polestar") + "|" + state.getInt("protocolVersion", 1);
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

  private Caller requireHost() {
    int uid = Binder.getCallingUid();
    if (uid == Process.myUid()) return new Caller(false,context().getPackageName());
    if (uid == 0 || uid == Process.SHELL_UID) return new Caller(false,uid==0?"root":"shell");
    String[] packages = context().getPackageManager().getPackagesForUid(uid);
    if (packages != null) for (String pkg : packages) if (HOSTS.contains(pkg)) return new Caller(true,pkg);
    throw new SecurityException("Not a ColorOS vehicle-card host");
  }

  private static final class Caller {
    final boolean host;final String packageName;
    Caller(boolean host,String packageName){this.host=host;this.packageName=packageName==null?"":packageName;}
  }

  private Context context() {
    Context c = getContext();
    if (c == null) throw new IllegalStateException("No provider context");
    return c;
  }

  @Override public void shutdown() {
    if(upstream!=null&&getContext()!=null){context().getContentResolver().unregisterContentObserver(upstream);upstream=null;}
    super.shutdown();
  }
  @Override public String getType(Uri uri) {
    requireUri(uri);
    return "vnd.android.cursor.dir/vnd." + BridgeIdentity.authority(context()) + ".device_info";
  }
  @Override public Uri insert(Uri uri, ContentValues values) { return null; }
  @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
  @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
