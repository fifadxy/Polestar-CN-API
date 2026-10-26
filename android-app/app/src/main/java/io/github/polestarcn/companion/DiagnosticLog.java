package io.github.polestarcn.companion;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/** Non-sensitive operation messages only; no VIN, password or token values. */
public final class DiagnosticLog {
  private static final ArrayDeque<String> MESSAGES = new ArrayDeque<>();

  private DiagnosticLog() {}

  public static synchronized void add(String event) {
    if (MESSAGES.size() >= 60) MESSAGES.removeFirst();
    event =
        event
            .replaceAll("(?i)Bearer\\s+[^\\s]+", "Bearer [redacted]")
            .replaceAll("[A-HJ-NPR-Z0-9]{17}", "[VIN]")
            .replaceAll("(?<![0-9])1[3-9][0-9]{9}(?![0-9])", "[phone]")
            .replaceAll("eyJ[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+", "[token]");
    MESSAGES.addLast(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "  " + event);
  }

  public static synchronized String export() {
    StringBuilder out = new StringBuilder("Polestar Android Companion diagnostics\n");
    out.append("build=0.4.0-beta1; mainUi=control; chinaRemote=biometric; globalApi=off\n");
    for (String e : MESSAGES) out.append(e).append('\n');
    return out.toString();
  }
}
