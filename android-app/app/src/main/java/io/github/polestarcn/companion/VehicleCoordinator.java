package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import io.github.polestarcn.companion.api.ChinaPolestarApi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One network queue shared by UI and background jobs. Never replays write operations. */
public final class VehicleCoordinator {
  private static VehicleCoordinator instance;

  public static synchronized VehicleCoordinator get(Context c) {
    if (instance == null) instance = new VehicleCoordinator(c.getApplicationContext());
    return instance;
  }

  public final ChinaPolestarApi api;
  public final List<ChinaPolestarApi.VehicleInfo> cars = new ArrayList<>();
  public ChinaPolestarApi.VehicleInfo selected;
  public VehicleSnapshot snapshot;
  public boolean busy;
  private boolean signedIn;

  public boolean hasSession() {
    return signedIn;
  }

  public String message = "";
  public OperationState operation = OperationState.NOT_SENT;
  private final Context context;
  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final List<Runnable> listeners = new ArrayList<>(), completions = new ArrayList<>();

  private VehicleCoordinator(Context c) {
    context = c;
    api = new ChinaPolestarApi(c);
    snapshot = VehicleSnapshotStore.loadChina(c);
    signedIn = api.session() != null;
  }

  public void listen(Runnable r) {
    listeners.add(r);
  }

  public void remove(Runnable r) {
    listeners.remove(r);
  }

  private String tr(String zh, String en) {
    return AppPreferences.tr(context, zh, en);
  }

  private void changed() {
    for (Runnable r : new ArrayList<>(listeners)) r.run();
    ResidentService.update(context);
  }

  private void finish() {
    busy = false;
    changed();
    for (Runnable r : new ArrayList<>(completions)) r.run();
    completions.clear();
  }

  public void login(String phone, String password) {
    if (busy) return;
    busy = true;
    message = tr("正在登录…", "Signing in…");
    changed();
    io.execute(
        () -> {
          try {
            api.login(phone, password);
            main.post(
                () -> {
                  signedIn = true;
                  busy = false;
                  refresh(null);
                });
          } catch (Exception e) {
            main.post(
                () -> {
                  message = tr("登录失败：", "Sign-in failed: ") + safe(e);
                  finish();
                });
          }
        });
  }

  public void refresh(Runnable done) {
    if (done != null) completions.add(done);
    if (busy) return;
    if (!signedIn) {
      message = tr("请先登录", "Sign in to connect");
      finish();
      return;
    }
    busy = true;
    message = tr("正在更新车辆…", "Updating vehicle…");
    changed();
    final String wanted = AppPreferences.get(context, "vehicle", "");
    io.execute(
        () -> {
          try {
            List<ChinaPolestarApi.VehicleInfo> found = api.discoverVehicles();
            ChinaPolestarApi.VehicleInfo picked = found.get(0);
            for (ChinaPolestarApi.VehicleInfo car : found)
              if (identity(car.vin).equals(wanted)) picked = car;
            VehicleSnapshot live = api.readVehicle(picked);
            VehicleSnapshotStore.saveChina(context, live);
            final ChinaPolestarApi.VehicleInfo pick = picked;
            main.post(
                () -> {
                  cars.clear();
                  cars.addAll(found);
                  selected = pick;
                  AppPreferences.set(context, "vehicle", identity(pick.vin));
                  snapshot = live;
                  message = tr("车辆状态已更新", "Vehicle updated");
                  WidgetRenderer.refreshAll(context);
                  finish();
                });
          } catch (Exception e) {
            main.post(
                () -> {
                  message = tr("更新失败：", "Update failed: ") + safe(e);
                  finish();
                });
          }
        });
  }

  public void choose(ChinaPolestarApi.VehicleInfo car) {
    if (busy) return;
    AppPreferences.set(context, "vehicle", identity(car.vin));
    selected = null;
    snapshot = null;
    VehicleSnapshotStore.clearChina(context);
    WidgetRenderer.refreshAll(context);
    refresh(null);
  }

  public void command(String action) {
    if (busy || selected == null) return;
    final ChinaPolestarApi.VehicleInfo vehicle = selected;
    busy = true;
    operation = OperationState.SENDING;
    message = tr("正在发送指令…", "Sending command…");
    changed();
    io.execute(
        () -> {
          try {
            switch (action) {
              case "unlock":
                api.unlock(vehicle.vin);
                break;
              case "lock":
                api.lock(vehicle.vin);
                break;
              case "trunk-open":
                api.tailgate(vehicle.vin, true);
                break;
              case "trunk-close":
                api.tailgate(vehicle.vin, false);
                break;
              case "climate-start":
                api.climateStart(vehicle.vin, 22f);
                break;
              case "climate-stop":
                api.climateStop(vehicle.vin);
                break;
              case "air-start":
                api.purify(vehicle.vin, true);
                break;
              case "air-stop":
                api.purify(vehicle.vin, false);
                break;
              default:
                throw new IllegalArgumentException("Unsupported action");
            }
            main.post(
                () -> {
                  operation = OperationState.SERVER_ACCEPTED;
                  message = tr("服务器已接受，正在回读…", "Accepted; checking vehicle…");
                  changed();
                });
            boolean confirmed = false;
            for (int i = 0; i < 4; i++) {
              Thread.sleep(3000);
              try {
                if (api.commandReached(vehicle.vin, action)) {
                  confirmed = true;
                  break;
                }
              } catch (Exception e) {
                DiagnosticLog.add("Readback unavailable; command will not be replayed");
              }
            }
            VehicleSnapshot live = null;
            try {
              live = api.readVehicle(vehicle);
              VehicleSnapshotStore.saveChina(context, live);
            } catch (Exception ignored) {
            }
            final boolean reached = confirmed;
            final VehicleSnapshot updated = live;
            main.post(
                () -> {
                  operation =
                      reached ? OperationState.VEHICLE_CONFIRMED : OperationState.UNCONFIRMED;
                  if (reached && "climate-start".equals(action))
                    ClimateRuntime.started(context, identity(vehicle.vin), System.currentTimeMillis());
                  if (reached && "climate-stop".equals(action)) ClimateRuntime.stopped(context);
                  message =
                      reached
                          ? tr("车辆状态已确认", "Vehicle state confirmed")
                          : tr("已发送，车辆动作尚未确认", "Sent; vehicle action unconfirmed");
                  if (updated != null) snapshot = updated;
                  WidgetRenderer.refreshAll(context);
                  finish();
                });
          } catch (Exception e) {
            main.post(
                () -> {
                  operation = uncertain(e) ? OperationState.UNCONFIRMED : OperationState.FAILED;
                  message = tr("操作结果：", "Command result: ") + safe(e);
                  finish();
                });
          }
        });
  }

  public void logout() {
    if (busy) return;
    signedIn = false;
    api.logout();
    selected = null;
    cars.clear();
    snapshot = null;
    VehicleSnapshotStore.clearChina(context);
    ClimateRuntime.stopped(context);
    AppPreferences.set(context, "vehicle", "");
    operation = OperationState.NOT_SENT;
    message = "";
    WidgetRenderer.refreshAll(context);
    changed();
  }

  private static boolean uncertain(Exception e) {
    String s = String.valueOf(e.getMessage());
    return s.contains("DEADLINE")
        || s.contains("UNAVAILABLE")
        || s.contains("TIMEOUT")
        || s.contains("reset");
  }

  public static String safe(Exception e) {
    String s = e.getMessage();
    if (s == null) s = e.getClass().getSimpleName();
    s =
        s.replaceAll("(?i)Bearer\\s+[^\\s]+", "[redacted]")
            .replaceAll("[A-HJ-NPR-Z0-9]{17}", "[VIN]")
            .replaceAll("(?<![0-9])1[3-9][0-9]{9}(?![0-9])", "[phone]");
    return s.length() > 150 ? s.substring(0, 150) : s;
  }

  public static String identity(String vin) {
    try {
      byte[] b =
          java.security.MessageDigest.getInstance("SHA-256")
              .digest(vin.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder s = new StringBuilder();
      for (byte x : b) s.append(String.format(java.util.Locale.US, "%02x", x & 255));
      return s.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
