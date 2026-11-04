package io.github.polestarcn.oppobridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Root-assisted schema inventory only. It never reads or writes a table row. */
final class CarLinkDatabaseInventory {
  private static final int MAX_CHARS = 32_000;

  private CarLinkDatabaseInventory() {}

  static String capture() {
    String command =
        "for d in /data/user/0/com.heytap.opluscarlink/databases /data/data/com.heytap.opluscarlink/databases; do "
        + "[ -d \"$d\" ] || continue; "
        + "for f in \"$d\"/*; do [ -f \"$f\" ] || continue; "
        + "case \"$f\" in *-wal|*-shm|*.journal) continue;; esac; "
        + "b=$(basename \"$f\"); s=$(stat -c %s \"$f\" 2>/dev/null || echo 0); "
        + "echo \"database=$b bytes=$s\"; "
        + "if command -v sqlite3 >/dev/null 2>&1; then "
        + "sqlite3 -readonly \"$f\" \"select 'schemaObject='||type||':'||name from sqlite_master where type in ('table','view') and name not like 'sqlite_%' limit 200;\" 2>/dev/null; "
        + "else echo sqlite3=unavailable; fi; done; done";
    Process process = null;
    try {
      process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
      StringBuilder out = new StringBuilder("CarLink database schema inventory (read-only; no rows)\n");
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(
          process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null && out.length() < MAX_CHARS)
          out.append(sanitize(line)).append('\n');
      }
      boolean done = process.waitFor(6, TimeUnit.SECONDS);
      if (!done) process.destroyForcibly();
      if (out.indexOf("database=") < 0) out.append("databaseInventory=unavailable-or-empty\n");
      return out.toString();
    } catch (Throwable unavailable) {
      if (process != null) process.destroyForcibly();
      return "CarLink database schema inventory unavailable: "
          + unavailable.getClass().getSimpleName() + "\n";
    }
  }

  private static String sanitize(String line) {
    if (line == null) return "";
    String value = line.replace('\r', ' ').replace('\n', ' ');
    value = value.replaceAll("(?i)(token|password|cookie|secret|vin)[^\\s]*", "[redacted]");
    value = value.replaceAll("[A-Fa-f0-9]{24,}", "[long-id-redacted]");
    return value.substring(0, Math.min(value.length(), 500));
  }
}
