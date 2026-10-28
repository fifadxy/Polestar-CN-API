package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import io.github.polestarcn.companion.api.ChinaPolestarApi;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
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
  public boolean progressOnly;
  public boolean commandPending; public int readbackPercent; public int readbackChecks;
  private long readbackStarted;
  private final Map<String, PendingCommand> pendingCommands = new LinkedHashMap<>();
  private final ExecutorService commandIO = Executors.newFixedThreadPool(2);
  private boolean exclusive;
  private static final class PendingCommand {
    final String action; final ChinaPolestarApi.VehicleInfo vehicle; final String group;
    final long issued; final Float target; boolean accepted; int checks; long started;
    OperationState state=OperationState.SENDING;
    PendingCommand(String a, ChinaPolestarApi.VehicleInfo v, long issued, Float target) {
      this.action=a;this.vehicle=v;this.group=CommandSlots.group(a);this.issued=issued;this.target=target;
    }
  }
  public boolean isCommandPending(String control) { return pendingCommands.containsKey(CommandSlots.group(control)); }
  public boolean canCommand(String action) { return signedIn && selected != null && !exclusive && !isCommandPending(action); }
  public boolean canControl(String control) { return canCommand(control); }
  public OperationState controlOperation(String control) {
    PendingCommand ticket=pendingCommands.get(CommandSlots.group(control));
    return ticket==null?operation:ticket.state;
  }
  private void syncCommandStatus() {
    commandPending=!pendingCommands.isEmpty();
    if(commandPending){PendingCommand recent=null;for(PendingCommand p:pendingCommands.values())recent=p;
      activeAction=recent.action;readbackChecks=recent.checks;
      readbackPercent=recent.started==0?0:Math.min(98,(int)((android.os.SystemClock.elapsedRealtime()-recent.started)*98/26_000L));
      operation=recent.state;
    } else {activeAction="";readbackChecks=0;readbackPercent=100;main.removeCallbacks(progressTick);}
  }
  public LocationSnapshot parkedLocation;
  private final Runnable progressTick=new Runnable(){public void run(){if(pendingCommands.isEmpty())return;
    syncCommandStatus(); progressOnly=true;for(Runnable r:new ArrayList<>(listeners))r.run();progressOnly=false;
    main.postDelayed(this,1000);
  }};

  private long vehiclesDiscoveredAt;
  private boolean signedIn;

  public boolean locationBusy;
  private int locationGeneration;
  private long parkingFetchedAt;
  private final List<java.util.function.BiConsumer<LocationSnapshot,String>> locationCompletions=new ArrayList<>();
  public void readLastParkedLocation(java.util.function.BiConsumer<LocationSnapshot,String> complete){readLastParkedLocation(true,complete);}
  public void readLastParkedLocation(boolean force,java.util.function.BiConsumer<LocationSnapshot,String> complete) {
    if(!FirstUseConsent.accepted(context)||!signedIn||selected==null||selected.vin==null||selected.vin.isEmpty()){complete.accept(null,tr("请先连接车辆","Connect a vehicle first"));return;}
    int cacheMinutes=RefreshPolicy.map(context);
    if(!force&&cacheMinutes>0&&parkedLocation!=null&&!parkedLocation.stale&&android.os.SystemClock.elapsedRealtime()-parkingFetchedAt<cacheMinutes*60_000L){complete.accept(parkedLocation,"");return;}
    locationCompletions.add(complete);if(locationBusy)return;
    String vin=selected.vin;int generation=locationGeneration;locationBusy=true;changed();
    io.execute(()->{
      LocationSnapshot result=null;String error="";
      try{result=api.readLastParkedLocation(vin);}catch(Exception e){DiagnosticLog.add("Last parked location unavailable; vehicle session unchanged");error=tr("暂时无法读取停车位置，请稍后重试","Parking location is unavailable. Try again later.");}
      LocationSnapshot location=result;String message=error;
      main.post(()->{
        if(generation!=locationGeneration)return;
        locationBusy=false;
        List<java.util.function.BiConsumer<LocationSnapshot,String>> callbacks=new ArrayList<>(locationCompletions);locationCompletions.clear();
        if(signedIn&&selected!=null&&vin.equals(selected.vin)){
          if(location!=null){parkedLocation=location;parkingFetchedAt=android.os.SystemClock.elapsedRealtime();}
          changed();for(java.util.function.BiConsumer<LocationSnapshot,String> callback:callbacks)callback.accept(location,message);
        }
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
    quietRefresh=false;
    busy = false; exclusive=false;
    if(!commandPending)activeAction = "";
    changed();
    for (Runnable r : new ArrayList<>(completions)) r.run();
    completions.clear();
  }

  public void login(String phone, String password) {
    if(!FirstUseConsent.accepted(context))return;
    if (busy || commandPending) return;
    busy = true; exclusive=true;
    message = tr("正在登录…", "Signing in…");
    changed();
    io.execute(
        () -> {
          try {
            api.login(phone, password);
            main.post(
                () -> {
                  signedIn = true;
                  busy = false;exclusive=false;
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

  public boolean quietRefresh;
  public void refreshQuiet(){if(busy||commandPending)return;quietRefresh=true;refresh(null);}
  public void refresh(Runnable done) {
    if(!FirstUseConsent.accepted(context)){if(done!=null)done.run();return;}
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
            VehicleSnapshotStore.saveChina(context, live);EnergyLog.record(context,identity(picked.vin),live);
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
    if (busy || commandPending) return;
    parkedLocation=null;parkingFetchedAt=0;locationGeneration++;locationBusy=false;locationCompletions.clear();
    AppPreferences.set(context, "vehicle", identity(car.vin));
    selected = null;
    snapshot = null;
    VehicleSnapshotStore.clearChina(context);
    WidgetRenderer.refreshAll(context);
    refresh(null);
  }

  /** Different command families can progress while another family is under readback or a read refresh is in flight. */
  public void command(String action) {
    if (!FirstUseConsent.accepted(context) || !canCommand(action)) return;
    final ChinaPolestarApi.VehicleInfo vehicle=selected;
    final Float target="climate-start".equals(action)&&ClimateTarget.enabled(context)?ClimateTarget.request(context):null;
    final PendingCommand ticket=new PendingCommand(action,vehicle,System.currentTimeMillis(),target);
    pendingCommands.put(ticket.group,ticket);syncCommandStatus();
    message=tr("正在发送指令 · ","Sending command · ")+action;
    if(pendingCommands.size()==1){main.removeCallbacks(progressTick);main.post(progressTick);}
    changed();
    commandIO.execute(()->{
      try {
        switch(action){
          case "unlock":api.unlock(vehicle.vin);break;
          case "lock":api.lock(vehicle.vin);break;
          case "trunk-open":api.tailgate(vehicle.vin,true);break;
          case "trunk-close":api.tailgate(vehicle.vin,false);break;
          case "climate-start":api.climateStart(vehicle.vin,ClimateTarget.request(context));break;
          case "climate-stop":api.climateStop(vehicle.vin);break;
          case "air-start":api.purify(vehicle.vin,true);break;
          case "air-stop":api.purify(vehicle.vin,false);break;
          case "windows-open":api.windows(vehicle.vin,true);break;
          case "windows-close":api.windows(vehicle.vin,false);break;
          case "honk":api.honkFlash(vehicle.vin,1);break;
          case "flash":api.honkFlash(vehicle.vin,2);break;
          case "honk-flash":api.honkFlash(vehicle.vin,0);break;
          default:throw new IllegalArgumentException("Unsupported action");
        }
        main.post(()->{if(pendingCommands.get(ticket.group)!=ticket)return;
          ticket.accepted=true;ticket.state=OperationState.SERVER_ACCEPTED;
          message=tr("指令已发送 · 正在独立回读 ","Command sent · readback ")+action;
          syncCommandStatus();changed();confirmCommand(ticket);
        });
      } catch(Exception e){main.post(()->{
        if(pendingCommands.get(ticket.group)!=ticket)return;
        if(e instanceof IllegalArgumentException){pendingCommands.remove(ticket.group);syncCommandStatus();operation=OperationState.FAILED;
          message=tr("指令参数不受支持：","Invalid command: ")+safe(e);changed();return;}
        ticket.state=OperationState.UNCONFIRMED;
        message=tr("发送结果未知，正在只读核实：","Delivery uncertain; read-only verification: ")+action;
        syncCommandStatus();changed();confirmCommand(ticket);
      });}
    });
  }

  private void confirmCommand(PendingCommand ticket){
    ticket.started=android.os.SystemClock.elapsedRealtime();syncCommandStatus();changed();
    new CommandReadback(()->api.readVehicle(ticket.vehicle),io,(delay,task)->main.postDelayed(task,delay),new CommandReadback.Listener(){
      public void progress(VehicleSnapshot updated,int count){
        if(pendingCommands.get(ticket.group)!=ticket||selected==null||!ticket.vehicle.vin.equals(selected.vin))return;
        ticket.checks=count;syncCommandStatus();
        message=tr("正在核实 ","Verifying ")+ticket.action+" · "+count+"/5";
        applyReadback(ticket.vehicle,updated);changed();
      }
      public void finished(VehicleSnapshot updated,boolean reached){
        if(pendingCommands.get(ticket.group)!=ticket||selected==null||!ticket.vehicle.vin.equals(selected.vin))return;
        pendingCommands.remove(ticket.group);syncCommandStatus();
        boolean verified=reached&&(ticket.target==null||ClimateTarget.confirmed(updated,ticket.target,ticket.issued));
        OperationState result=momentary(ticket.action)
            ?(ticket.accepted?OperationState.SERVER_ACCEPTED:OperationState.UNCONFIRMED)
            :verified?OperationState.VEHICLE_CONFIRMED:OperationState.UNCONFIRMED;
        if(!commandPending)operation=result;
        if(reached&&"climate-start".equals(ticket.action))ClimateRuntime.started(context,identity(ticket.vehicle.vin),System.currentTimeMillis());
        if(reached&&"climate-stop".equals(ticket.action))ClimateRuntime.stopped(context);
        message=momentary(ticket.action)?tr("寻车已完成只读回读，声光结果不可远程确认","Find-car readback finished; sound/light unverified")
            :verified?tr("车辆状态已连续三次确认：","Three matching vehicle states confirmed: ")+ticket.action
            :tr("五次只读回读已完成，执行结果待确认：","Five read-only checks finished; result uncertain: ")+ticket.action;
        changed();
      }
    }).start(ticket.action);
  }

  private void applyReadback(ChinaPolestarApi.VehicleInfo vehicle,VehicleSnapshot updated){
    if(updated==null||selected==null||!vehicle.vin.equals(selected.vin))return;
    // A delayed callback cannot overwrite a newer refresh / another family's readback.
    if(snapshot!=null&&updated.timestampMillis<snapshot.timestampMillis)return;
    snapshot=updated;VehicleSnapshotStore.saveChina(context,updated);
    EnergyLog.record(context,identity(vehicle.vin),updated);protectCamp(vehicle.vin,updated);WidgetRenderer.refreshAll(context);
  }

  public void logout() {
    if (busy || commandPending) return;
    parkedLocation=null;parkingFetchedAt=0;locationGeneration++;locationBusy=false;locationCompletions.clear();
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
    if(!FirstUseConsent.accepted(context))return;
    if (busy || commandPending || selected == null || hours < 1 || hours > 24) return;
    final ChinaPolestarApi.VehicleInfo vehicle = selected;
    final float requestedTarget=ClimateTarget.request(context);final boolean customTarget=ClimateTarget.enabled(context);
    busy = true;exclusive=true;
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
            fresh.name, now + hours * 3_600_000L, CampModeManager.interval(context), minimum, keepAir,requestedTarget,customTarget);
        CampModeManager.start(context, camping);
        VehicleSnapshotStore.saveChina(context, fresh);EnergyLog.record(context,identity(vehicle.vin),fresh);
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
    busy = true;exclusive=true;
    activeAction = "";
    message = tr("正在检查露营状态…", "Checking camping status…");
    changed();
    io.execute(() -> {
      final VehicleSnapshot[] read = {null};
      final boolean[] wrote = {false};final long[] climateIssuedAt={0};
      ChinaPolestarApi.VehicleInfo vehicle = new ChinaPolestarApi.VehicleInfo();
      vehicle.vin = camping.vin; vehicle.modelName = camping.vehicleName;
      CampEngine.Backend backend = new CampEngine.Backend() {
        public VehicleSnapshot read(CampSession s) throws Exception {
          return read[0] = api.readVehicle(vehicle);
        }
        public void write(CampSession s, String action) throws Exception {
          wrote[0] = true;
          switch (action) {
            case "climate-start": climateIssuedAt[0]=System.currentTimeMillis();api.climateStart(s.vin, s.targetTemperatureC); break;
            case "climate-stop": api.climateStop(s.vin); break;
            case "air-start": api.purify(s.vin, true); break;
            case "air-stop": api.purify(s.vin, false); break;
            default: throw new IllegalArgumentException("Unsupported camping action");
          }
        }
        public boolean confirm(CampSession s, String action) throws Exception {
          int consecutive=0;
          long previous=0;
          for (long at:CommandReadback.CHECKPOINT_MS) {
            if (cancelled.get() || !CampModeManager.current(context, s.id)) return false;
            Thread.sleep(at-previous);previous=at;
            if (cancelled.get() || !CampModeManager.current(context, s.id)) return false;
            boolean reached=false;
            try {
              if(action.equals("climate-start")&&s.verifyClimateTarget){
                read[0]=api.readVehicle(vehicle);
                reached=ClimateTarget.confirmed(read[0],s.targetTemperatureC,climateIssuedAt[0]);
              } else reached=api.commandReached(s.vin, action);
            } catch(Exception unavailable) { DiagnosticLog.add("Camping readback unavailable; waiting for next slot"); }
            consecutive=reached?consecutive+1:0;
          }
          if(consecutive>=3) {
            if("climate-start".equals(action)) ClimateRuntime.started(context,identity(s.vin),System.currentTimeMillis());
            if("climate-stop".equals(action)) ClimateRuntime.stopped(context);
            return true;
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
          snapshot = read[0]; VehicleSnapshotStore.saveChina(context, snapshot);EnergyLog.record(context,identity(camping.vin),snapshot); WidgetRenderer.refreshAll(context);
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
