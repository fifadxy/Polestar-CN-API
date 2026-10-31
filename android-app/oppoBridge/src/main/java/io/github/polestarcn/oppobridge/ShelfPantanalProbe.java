package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Read-only Shelf service discovery from inside the AssistantScreen process.
 *
 * <p>The Pantanal ShelfProvider is exported behind com.oplus.permission.safe.ASSISTANT.
 * Running in AssistantScreen's process lets us observe the same service metadata the OEM Shelf
 * is allowed to query, without bypassing the provider permission. No card content, account data
 * or authorization token is read or stored.</p>
 */
final class ShelfPantanalProbe {
  private static final Uri SERVICES =
      Uri.parse("content://com.oplus.pantanal.ums.card.support.shelf/shelf_services");
  private static final String HOST = "com.coloros.assistantscreen";
  private static final String TARGET_VERSION = "17.14.60";
  private static final int MAX_ROWS = 512;
  private static final int MAX_CANDIDATES = 12;
  private static final int MAX_SERVICE_INDEX = 80;

  private ShelfPantanalProbe() {}

  static void install(OppoBridgeModule module) {
    new Handler(Looper.getMainLooper()).post(() -> attempt(module, 0));
  }

  private static void attempt(OppoBridgeModule module, int retry) {
    Application app = currentApplication();
    if (app == null) {
      if (retry < 5) new Handler(Looper.getMainLooper()).postDelayed(
          () -> attempt(module, retry + 1), 800L);
      return;
    }
    try {
      String version = app.getPackageManager().getPackageInfo(HOST, 0).versionName;
      if (!TARGET_VERSION.equals(version)) {
        report(app, false, "host-version-mismatch:" + version, 0, "", "", "");
        module.info("shelf_pantanal_probe skipped hostVersion=" + version);
        return;
      }

      int rows = 0;
      List<String> candidates = new ArrayList<>();
      List<String> services = new ArrayList<>();
      String columns;
      try (Cursor cursor = app.getContentResolver().query(SERVICES, null, null, null, null)) {
        if (cursor == null) {
          report(app, false, "null-cursor", 0, "", "", "");
          module.warn("shelf_pantanal_probe null_cursor");
          return;
        }
        columns = String.join(",", cursor.getColumnNames());
        while (rows < MAX_ROWS && cursor.moveToNext()) {
          rows++;
          String service = first(cursor, "serviceId", "service_id", "serviceid");
          String score = first(cursor, "score");
          if (!service.isEmpty() && services.size() < MAX_SERVICE_INDEX)
            services.add(safe(service) + "@" + safe(score));
          String pkg = first(cursor, "packageName", "package_name", "bizPkgName",
              "biz_pkg_name", "host_package_name", "hostPackageName");
          String component = first(cursor, "componentName", "component_name",
              "host_component_name", "hostComponentName");
          String name = first(cursor, "name", "label", "serviceName", "service_name",
              "groupTitle", "group_title");
          String joined = (service + " " + pkg + " " + component + " " + name)
              .toLowerCase(Locale.ROOT);
          if (looksVehicleRelated(joined, name) && candidates.size() < MAX_CANDIDATES) {
            candidates.add("id=" + safe(service) + " pkg=" + safe(pkg)
                + " component=" + safe(component) + " name=" + safe(name));
          }
        }
      }
      String candidateText = String.join(" | ", candidates);
      String serviceIndex = String.join("|", services);
      report(app, true, "ok", rows, columns, candidateText, serviceIndex);
      module.info("shelf_pantanal_probe ready=true rows=" + rows
          + " services=" + services.size() + " candidates=" + candidates.size()
          + " columns=" + columns);
      if (!candidateText.isEmpty()) module.info("shelf_pantanal_candidates " + candidateText);
    } catch (SecurityException denied) {
      report(app, false, "permission-denied", 0, "", "", "");
      module.warn("shelf_pantanal_probe permission_denied");
    } catch (Throwable failure) {
      report(app, false, failure.getClass().getSimpleName(), 0, "", "", "");
      module.warn("shelf_pantanal_probe failed=" + failure.getClass().getSimpleName());
    }
  }

  private static String first(Cursor cursor, String... names) {
    for (String name : names) {
      int index = cursor.getColumnIndex(name);
      if (index >= 0 && !cursor.isNull(index)) {
        String value = cursor.getString(index);
        if (value != null) return value;
      }
    }
    return "";
  }

  private static boolean looksVehicleRelated(String lower, String rawName) {
    return lower.contains("car") || lower.contains("vehicle") || lower.contains("auto")
        || lower.contains("icar") || lower.contains("motor")
        || rawName.contains("车") || rawName.contains("汽车") || rawName.contains("车辆")
        || lower.contains("polestar");
  }

  private static String safe(String value) {
    if (value == null || value.isEmpty()) return "-";
    String flat = value.replace('\n', ' ').replace('\r', ' ').trim();
    return flat.substring(0, Math.min(flat.length(), 96));
  }

  private static void report(Application app, boolean ready, String reason, int rows,
      String columns, String candidates, String services) {
    try {
      Bundle in = new Bundle();
      in.putBoolean("ready", ready);
      in.putString("reason", reason);
      in.putInt("rowCount", rows);
      in.putString("columns", columns);
      in.putString("candidates", candidates);
      in.putString("services", services);
      in.putString("hostVersion", TARGET_VERSION);
      in.putString("moduleVersion", "0.9.0");
      BridgeClient.call(app, "shelf-report", in);
    } catch (Throwable ignored) {
      // Diagnostic reporting must not affect AssistantScreen.
    }
  }

  private static Application currentApplication() {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch (Throwable ignored) {
      return null;
    }
  }
}
