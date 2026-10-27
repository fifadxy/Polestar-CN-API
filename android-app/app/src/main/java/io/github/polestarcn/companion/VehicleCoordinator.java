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
  private long vehiclesDiscoveredAt;
  private boolean signedIn;

  public boolean locationBusy;
  private int locationGeneration;
  public void readLastParkedLocation(java.util.function.BiConsumer<LocationSnapshot,String> complete) {
    if(locationBusy||!signedIn||selected==null||selected.vin==null||selected.vin.isEmpty())return;
    String vin=selected.vin;int generation=locationGeneration;locationBusy=true;changed();
    io.execute(()->{
      LocationSnapshot result=null;String error="";
      try{result=api.readLastParkedLocation(vin);}catch(Exception e){DiagnosticLog.add("Last parked location unavailable; vehicle session unchanged");error=tr("暂时无法读取停车位置，请稍后重试","Parking location is unavailable. Try again later.");}
      LocationSnapshot location=result;String message=error;
      main.post(()->{locationBusy=false;changed();
        if(generation==locationGeneration&&signedIn&&selected!=null&&vin.equals(selected.vin))complete.accept(location,message);
      });
    });
  }

  public boolean hasSession() {
    return signedIn;
  }

  public String message = "";
  public OperationState operation = OperationState.NOT_SENT;
  public String activeAction = "";
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
    if (!listeners.contains(r)) listeners.add(r);
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
    activeAction = "";
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

  public void refreshUser(Runnable done) {
    if (!busy && selected != null) api.invalidateHealth(selected.vin);
    refresh(done);
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
    final List<ChinaPolestarApi.VehicleInfo> known = new ArrayList<>(cars);
    final boolean discover = known.isEmpty() || System.currentTimeMillis() - vehiclesDiscoveredAt >= 15 * 60_000L;
    io.execute(
        () -> {
          try {
            List<ChinaPolestarApi.VehicleInfo> found = discover ? api.discoverVehicles() : known;
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
                  if (discover) vehiclesDiscoveredAt = System.currentTimeMillis();
                  selected = pick;
                  AppPreferences.set(context, "vehicle", identity(pick.vin));
                  snapshot = live;
                  protectCamp(pick.vin, live);
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
    locationGeneration++;
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
    activeAction = action;
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
              case "windows-open":
                api.windows(vehicle.vin, true);
                break;
              case "windows-close":
                api.windows(vehicle.vin, false);
                break;
              case "honk":
                api.honkFlash(vehicle.vin, 1);
                break;
              case "flash":
                api.honkFlash(vehicle.vin, 2);
                break;
              case "honk-flash":
                api.honkFlash(vehicle.vin, 0);
                break;
              default:
                throw new IllegalArgumentException("Unsupported action");
            }
            main.post(
                () -> {
                  operation = OperationState.SERVER_ACCEPTED;
                  if (momentary(action)) {
                    message = tr(
                        "瞬时指令已发送；鸣笛/闪灯无法通过持续车况字段确认",
                        "Momentary command sent; honk/flash cannot be confirmed from persistent vehicle state");
                    finish();
                  } else {
                    message = tr("指令已发送，约 15 秒后回读车况…", "Sent; checking vehicle after about 15 s…");
                    changed();
                    confirmCommand(vehicle, action);
                  }
                });
          } catch (Exception e) {
            main.post(
                () -> {
                  if (uncertain(e)) {
                    operation = OperationState.UNCONFIRMED;
                    if (momentary(action)) {
                      message = tr(
                          "瞬时指令发送结果未确认；未自动重试",
                          "Momentary command delivery unconfirmed; command was not retried");
                      finish();
                    } else {
                      message = tr("发送结果未确认，约 15 秒后回读…", "Delivery uncertain; checking after about 15 s…");
                      changed();
                      confirmCommand(vehicle, action);
                    }
                  } else {
                    operation = OperationState.FAILED;
                    message = tr("操作结果：", "Command result: ") + safe(e);
                    finish();
                  }
                });
          }
        });
  }

  private void confirmCommand(ChinaPolestarApi.VehicleInfo vehicle, String action) {
    new CommandReadback(() -> api.readVehicle(vehicle), io,
        (delay, task) -> main.postDelayed(task, delay), (updated, reached) -> {
          operation = reached ? OperationState.VEHICLE_CONFIRMED : OperationState.UNCONFIRMED;
          if (reached && "climate-start".equals(action))
            ClimateRuntime.started(context, identity(vehicle.vin), System.currentTimeMillis());
          if (reached && "climate-stop".equals(action)) ClimateRuntime.stopped(context);
          message = reached ? tr("车辆状态已确认", "Vehicle state confirmed")
              : tr("车辆状态尚未确认，请稍后刷新；未重复发送", "Vehicle state unconfirmed; refresh later. Command not repeated");
          if (updated != null && selected != null && vehicle.vin.equals(selected.vin)) {
            snapshot = updated;
            VehicleSnapshotStore.saveChina(context, updated);
            protectCamp(vehicle.vin, updated);
          }
          WidgetRenderer.refreshAll(context);
          finish();
        }).start(action);
  }

  public void logout() {
    if (busy) return;
    locationGeneration++;
    signedIn = false;
    CampModeManager.clear(context);
    api.logout();
    selected = null;
    cars.clear();
    vehiclesDiscoveredAt = 0;
    snapshot = null;
    VehicleSnapshotStore.clearChina(context);
    ClimateRuntime.stopped(context);
    AppPreferences.set(context, "vehicle", "");
    operation = OperationState.NOT_SENT;
    message = "";
    WidgetRenderer.refreshAll(context);
    changed();
  }

  public void startCamp(int hours, boolean keepAir) {
    if (busy || selected == null || hours < 1 || hours > 24) return;
    final ChinaPolestarApi.VehicleInfo vehicle = selected;
    busy = true;
    message = tr("正在检查露营电量…", "Checking camping battery…");
    changed();
    io.execute(() -> {
      try {
        VehicleSnapshot fresh = api.readVehicle(vehicle);
        long now = System.currentTimeMillis();
        int minimum = CampModeManager.minimum(context);
        if (!CampSession.batteryAllows(fresh.batteryPercent, minimum))
          throw new IllegalStateException(tr("电量未知或低于下限，未开启露营", "Unknown/below-limit battery; camping not started"));
        if (fresh.timestampMillis <= 0 || now - fresh.timestampMillis > 10 * 60_000L
            || fresh.timestampMillis > now + 300_000L)
          throw new IllegalStateException(tr("电量数据已过期，请刷新后重试", "Battery data is stale. Refresh and try again"));
        CampSession camping = new CampSession(java.util.UUID.randomUUID().toString(), vehicle.vin,
            fresh.name, now + hours * 3_600_000L, CampModeManager.interval(context), minimum, keepAir);
        CampModeManager.start(context, camping);
        VehicleSnapshotStore.saveChina(context, fresh);
        main.post(() -> { snapshot = fresh; message = CampModeManager.status(context); finish(); });
      } catch (Exception e) {
        main.post(() -> { message = tr("露营未开启：", "Camping not started: ") + safe(e); finish(); });
      }
    });
  }

  public void stopCamp() {
    if (CampModeManager.requestStop(context)) {
      checkCamp(new java.util.concurrent.atomic.AtomicBoolean(false), () -> {});
    }
  }

  public void checkCamp(java.util.concurrent.atomic.AtomicBoolean cancelled, Runnable done) {
    if (cancelled.get()) { done.run(); return; }
    if (busy) {
      completions.add(() -> main.post(() -> checkCamp(cancelled, done)));
      return;
    }
    CampSession camping = CampModeManager.session(context);
    String phase = CampModeManager.phase(context);
    if (camping == null || !("ACTIVE".equals(phase) || "STOPPING".equals(phase))) { done.run(); return; }
    if (!signedIn) { CampModeManager.deactivate(context, camping.id, "SIGNED_OUT");
      CampModeManager.finishCheck(context, camping.id); done.run(); return; }
    busy = true;
    activeAction = "";
    message = tr("正在检查露营状态…", "Checking camping status…");
    changed();
    io.execute(() -> {
      final VehicleSnapshot[] read = {null};
      final boolean[] wrote = {false};
      ChinaPolestarApi.VehicleInfo vehicle = new ChinaPolestarApi.VehicleInfo();
      vehicle.vin = camping.vin; vehicle.modelName = camping.vehicleName;
      CampEngine.Backend backend = new CampEngine.Backend() {
        public VehicleSnapshot read(CampSession s) throws Exception {
          return read[0] = api.readVehicle(vehicle);
        }
        public void write(CampSession s, String action) throws Exception {
          wrote[0] = true;
          switch (action) {
            case "climate-start": api.climateStart(s.vin, 22f); break;
            case "climate-stop": api.climateStop(s.vin); break;
            case "air-start": api.purify(s.vin, true); break;
            case "air-stop": api.purify(s.vin, false); break;
            default: throw new IllegalArgumentException("Unsupported camping action");
          }
        }
        public boolean confirm(CampSession s, String action) throws Exception {
          for (int i = 0; i < 4; i++) {
            if (cancelled.get() || !CampModeManager.current(context, s.id)) return false;
            Thread.sleep(i == 0 ? CommandReadback.INITIAL_DELAY_MS : CommandReadback.RETRY_DELAY_MS);
            if (cancelled.get() || !CampModeManager.current(context, s.id)) return false;
            if (api.commandReached(s.vin, action)) {
              if ("climate-start".equals(action)) ClimateRuntime.started(context, identity(s.vin), System.currentTimeMillis());
              if ("climate-stop".equals(action)) ClimateRuntime.stopped(context);
              return true;
            }
          }
          return false;
        }
      };
      CampEngine.Control control = new CampEngine.Control() {
        public boolean current(String id) { return CampModeManager.current(context, id); }
        public boolean active(String id) { return current(id) && CampModeManager.active(context); }
        public boolean cancelled() { return cancelled.get(); }
        public long now() { return System.currentTimeMillis(); }
        public void deactivate(String id, String reason) { CampModeManager.deactivate(context, id, reason); }
        public void report(String result) { CampModeManager.report(context, camping.id, result); }
      };
      new CampEngine(backend, control).check(camping, "STOPPING".equals(phase), System.currentTimeMillis(), CampModeManager.stopReason(context));
      if (wrote[0] && !cancelled.get() && CampModeManager.current(context, camping.id)) {
        try { read[0] = api.readVehicle(vehicle); } catch (Exception ignored) {}
      }
      main.post(() -> {
        if (read[0] != null && selected != null && camping.vin.equals(selected.vin)) {
          snapshot = read[0]; VehicleSnapshotStore.saveChina(context, snapshot); WidgetRenderer.refreshAll(context);
        }
        message = CampModeManager.status(context);
        finish();
        done.run();
        CampModeManager.finishCheck(context, camping.id);
      });
    });
  }

  private void protectCamp(String vin, VehicleSnapshot state) {
    if (CampModeManager.protectBattery(context, vin, state))
      main.post(() -> checkCamp(new java.util.concurrent.atomic.AtomicBoolean(false), () -> {}));
  }

  private static boolean momentary(String action) {
    return "honk".equals(action) || "flash".equals(action) || "honk-flash".equals(action);
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
