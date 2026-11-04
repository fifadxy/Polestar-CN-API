package io.github.polestarcn.companion;

/** Read first; a maintenance decision is never a blind replay of an uncertain remote command. */
public final class CampEngine {
  public interface Backend {
    VehicleSnapshot read(CampSession session) throws Exception;
    void write(CampSession session, String action) throws Exception;
    boolean confirm(CampSession session, String action) throws Exception;
  }
  public interface Control {
    boolean current(String id);
    boolean active(String id);
    boolean cancelled();
    long now();
    void deactivate(String id, String reason);
    void report(String message);
  }
  private final Backend backend;
  private final Control control;
  public CampEngine(Backend backend, Control control) { this.backend = backend; this.control = control; }

  public void check(CampSession session, boolean requestedStop, long now) {
    check(session, requestedStop, now, "MANUAL");
  }
  public void check(CampSession session, boolean requestedStop, long now, String stopReason) {
    if (!usable(session)) return;
    if (requestedStop || now >= session.endsAt) {
      stop(session, requestedStop ? stopReason : "EXPIRED");
      return;
    }
    if (!control.active(session.id)) return;
    try {
      VehicleSnapshot state = backend.read(session);
      if (!usable(session) || !control.active(session.id)) return;
      if (control.now() >= session.endsAt) { stop(session, "EXPIRED"); return; }
      if (state != null && state.batteryPercent != null && state.batteryPercent >= 0
          && state.batteryPercent < session.minimumBattery) {
        stop(session, "LOW_BATTERY");
        return;
      }
      if (state == null || !CampSession.batteryAllows(state.batteryPercent, session.minimumBattery)
          || state.isDemo || !"LIVE".equals(state.source) || state.timestampMillis <= 0
          || now - state.timestampMillis > 10 * 60_000L || state.timestampMillis > now + 300_000L) {
        control.report("WAITING_FOR_BATTERY");
        return;
      }
      if (!maintain(session, state.climate, "climate-start")) return;
      if (session.keepAir && !maintain(session, state.air, "air-start")) return;
      control.report("RUNNING");
    } catch (Exception e) { control.report(CampRecovery.classify(e)); }
  }

  private boolean usable(CampSession s) { return !control.cancelled() && control.current(s.id); }
  private boolean maintain(CampSession s, String state, String action) throws Exception {
    if (!usable(s) || !control.active(s.id)) return false;
    if (control.now() >= s.endsAt) { stop(s, "EXPIRED"); return false; }
    if ("开启".equals(state)) return true;
    if (!"关闭".equals(state)) { control.report("STATE_UNCONFIRMED"); return false; }
    // One write per fresh, known-off state. confirm() performs reads only.
    backend.write(s, action);
    if (!usable(s) || !control.active(s.id)) return false;
    if (!backend.confirm(s, action)) { control.report("COMMAND_UNCONFIRMED"); return false; }
    return true;
  }

  private void stop(CampSession s, String reason) {
    control.deactivate(s.id, reason);
    boolean confirmed = true;
    for (String action : s.keepAir ? new String[]{"climate-stop", "air-stop"} : new String[]{"climate-stop"}) {
      if (!usable(s)) return;
      try {
        backend.write(s, action);
        if (!usable(s)) return;
        confirmed &= backend.confirm(s, action);
      } catch (Exception e) { confirmed = false; }
    }
    control.report(reason + (confirmed ? ":STOPPED" : ":STOP_UNCONFIRMED"));
  }
}
