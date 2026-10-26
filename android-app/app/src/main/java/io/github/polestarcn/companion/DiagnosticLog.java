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
        MESSAGES.addLast(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "  " + event);
    }
    public static synchronized String export() {
        StringBuilder out = new StringBuilder("Polestar Android Companion dev diagnostics\n");
        out.append("networkApiEnabled=false; remoteCommandsEnabled=false; data=demo\n");
        for (String e : MESSAGES) out.append(e).append('\n');
        return out.toString();
    }
}
