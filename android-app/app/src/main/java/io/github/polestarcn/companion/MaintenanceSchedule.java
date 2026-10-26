package io.github.polestarcn.companion;

import android.content.Context;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Local service reminder. Vehicle Health data may replace/augment this only after China verification. */
public final class MaintenanceSchedule {
  public static final int DEFAULT_INTERVAL_DAYS = 365;
  public static final int DEFAULT_INTERVAL_KM = 20_000;

  private MaintenanceSchedule() {}

  private static String vehicle(String key) {
    return key == null || key.trim().isEmpty() ? "unselected" : key.trim();
  }

  private static String key(String vehicleKey, String field) {
    return "maintenance_" + vehicle(vehicleKey) + "_" + field;
  }

  private static int positive(Context c, String key, int fallback) {
    try {
      int value = Integer.parseInt(AppPreferences.get(c, key, String.valueOf(fallback)));
      return value > 0 ? value : fallback;
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  public static int intervalDays(Context c) {
    return positive(c, "maintenance_interval_days", DEFAULT_INTERVAL_DAYS);
  }

  public static int intervalKm(Context c) {
    return positive(c, "maintenance_interval_km", DEFAULT_INTERVAL_KM);
  }

  public static void save(Context c, String vehicleKey, LocalDate date, Integer odometerKm) {
    if (date == null || date.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("Invalid service date");
    if (odometerKm != null && odometerKm < 0)
      throw new IllegalArgumentException("Invalid service odometer");
    AppPreferences.set(c, key(vehicleKey, "date"), date.toString());
    AppPreferences.set(c, key(vehicleKey, "odometer_km"),
        odometerKm == null ? "" : String.valueOf(odometerKm));
  }

  public static void clear(Context c, String vehicleKey) {
    AppPreferences.set(c, key(vehicleKey, "date"), "");
    AppPreferences.set(c, key(vehicleKey, "odometer_km"), "");
  }

  public static LocalDate lastDate(Context c, String vehicleKey) {
    String raw = AppPreferences.get(c, key(vehicleKey, "date"), "");
    if (raw.isEmpty()) return null;
    try {
      return LocalDate.parse(raw);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  public static Integer lastOdometerKm(Context c, String vehicleKey) {
    String raw = AppPreferences.get(c, key(vehicleKey, "odometer_km"), "");
    if (raw.isEmpty()) return null;
    try {
      int value = Integer.parseInt(raw);
      return value >= 0 ? value : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  public static Status status(Context c, String vehicleKey, VehicleSnapshot snapshot) {
    return evaluate(lastDate(c, vehicleKey), lastOdometerKm(c, vehicleKey),
        LocalDate.now(), snapshot == null ? null : snapshot.odometerKm,
        intervalDays(c), intervalKm(c));
  }

  public static Status evaluate(LocalDate lastDate, Integer lastOdometerKm, LocalDate today,
      Integer currentOdometerKm, int intervalDays, int intervalKm) {
    if (lastDate == null || today == null)
      return Status.unconfigured(intervalDays, intervalKm);
    long elapsedDays = Math.max(0L, ChronoUnit.DAYS.between(lastDate, today));
    Integer elapsedKm = null;
    if (lastOdometerKm != null && currentOdometerKm != null
        && currentOdometerKm >= lastOdometerKm)
      elapsedKm = currentOdometerKm - lastOdometerKm;
    long overdueDays = Math.max(0L, elapsedDays - intervalDays);
    Integer overdueKm = elapsedKm == null ? null : Math.max(0, elapsedKm - intervalKm);
    boolean due = elapsedDays >= intervalDays || (elapsedKm != null && elapsedKm >= intervalKm);
    return new Status(true, due, lastDate, lastOdometerKm, intervalDays, intervalKm,
        elapsedDays, elapsedKm, overdueDays, overdueKm);
  }

  public static String summary(Context c, String vehicleKey, VehicleSnapshot snapshot) {
    return status(c, vehicleKey, snapshot).summary(AppPreferences.english(c));
  }

  public static final class Status {
    public final boolean configured, due;
    public final LocalDate lastDate;
    public final Integer lastOdometerKm, elapsedKm, overdueKm;
    public final int intervalDays, intervalKm;
    public final long elapsedDays, overdueDays;

    private Status(boolean configured, boolean due, LocalDate lastDate, Integer lastOdometerKm,
        int intervalDays, int intervalKm, long elapsedDays, Integer elapsedKm,
        long overdueDays, Integer overdueKm) {
      this.configured = configured;
      this.due = due;
      this.lastDate = lastDate;
      this.lastOdometerKm = lastOdometerKm;
      this.intervalDays = intervalDays;
      this.intervalKm = intervalKm;
      this.elapsedDays = elapsedDays;
      this.elapsedKm = elapsedKm;
      this.overdueDays = overdueDays;
      this.overdueKm = overdueKm;
    }

    static Status unconfigured(int days, int km) {
      return new Status(false, false, null, null, days, km, 0, null, 0, null);
    }

    private static String km(int value) {
      return String.format(Locale.US, "%,d", value);
    }

    public String summary(boolean english) {
      if (!configured)
        return english
            ? "Service not set · " + intervalDays + " d / " + km(intervalKm) + " km"
            : "保养未设置 · " + intervalDays + "天 / " + km(intervalKm) + "km";
      boolean overdue = overdueDays > 0 || (overdueKm != null && overdueKm > 0);
      if (overdue) {
        StringBuilder s = new StringBuilder(english ? "Service overdue" : "保养已超期");
        if (overdueDays > 0)
          s.append(english ? " · " + overdueDays + " d" : " · 时间超 " + overdueDays + "天");
        if (overdueKm != null && overdueKm > 0)
          s.append(english ? " · " + km(overdueKm) + " km" : " · 里程超 " + km(overdueKm) + "km");
        return s.toString();
      }
      if (due) return english ? "Service due now" : "保养已到期";
      String s = english ? "Since service " + elapsedDays + " d"
          : "距上次保养 " + elapsedDays + "天";
      if (elapsedKm != null)
        s += english ? " · " + km(elapsedKm) + " km" : " · " + km(elapsedKm) + "km";
      return s;
    }

    public long remainingDays() { return Math.max(0L, intervalDays - elapsedDays); }
    public Integer remainingKm() { return elapsedKm == null ? null : Math.max(0, intervalKm - elapsedKm); }
    public int remainingTimePercent() {
      if (!configured || intervalDays <= 0) return -1;
      return (int)Math.max(0, Math.min(100, Math.round(100.0 * remainingDays() / intervalDays)));
    }
    public int remainingDistancePercent() {
      Integer remain = remainingKm();
      if (!configured || remain == null || intervalKm <= 0) return -1;
      return (int)Math.max(0, Math.min(100, Math.round(100.0 * remain / intervalKm)));
    }

    public String detail(boolean english) {
      if (!configured) return summary(english);
      StringBuilder s = new StringBuilder();
      s.append(english ? "Last service: " : "上次保养：").append(lastDate);
      if (lastOdometerKm != null)
        s.append(english ? " · " : " · ").append(km(lastOdometerKm)).append(" km");
      s.append("\n").append(english ? "Interval: " : "周期：")
          .append(intervalDays).append(english ? " days / " : "天 / ")
          .append(km(intervalKm)).append(" km");
      s.append("\n").append(summary(english));
      if (!due) {
        s.append("\n").append(english ? "Remaining: " : "距离到期：")
            .append(Math.max(0, intervalDays - elapsedDays))
            .append(english ? " d" : "天");
        if (elapsedKm != null)
          s.append(" · ").append(km(Math.max(0, intervalKm - elapsedKm))).append(" km");
      }
      return s.toString();
    }
  }
}
