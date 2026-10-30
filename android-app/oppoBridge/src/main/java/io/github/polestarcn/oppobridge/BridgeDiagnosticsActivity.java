package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Arrays;

public final class BridgeDiagnosticsActivity extends Activity {
  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    TextView text = new TextView(this);
    int p = (int) (18 * getResources().getDisplayMetrics().density);
    text.setPadding(p, p, p, p);
    text.setTextSize(14f);

    StringBuilder out = new StringBuilder();
    out.append("PsAssistant OPPO Bridge\n\n");
    out.append("Purpose: discover ColorOS 16/17 host structure before enabling any vehicle-binding injection.\n\n");
    try {
      Bundle caps = BridgeClient.capabilities(this);
      out.append("PsAssistant provider: OK\n");
      out.append("Protocol: ").append(caps.getInt("protocolVersion", -1)).append('\n');
      out.append("Commands: ").append(Arrays.toString(caps.getStringArray("commands"))).append("\n\n");
      Bundle snapshot = BridgeClient.snapshot(this);
      out.append("Vehicle available: ").append(snapshot.getBoolean("available", false)).append('\n');
      if (snapshot.getBoolean("available", false)) {
        out.append("Name: ").append(snapshot.getString("name", "--")).append('\n');
        out.append("SOC: ").append(snapshot.getBoolean("batteryPercentKnown") ? snapshot.getInt("batteryPercent") + "%" : "--").append('\n');
        out.append("Range: ").append(snapshot.getBoolean("rangeKmKnown") ? snapshot.getInt("rangeKm") + " km" : "--").append('\n');
        out.append("Lock: ").append(snapshot.getString("lock", "--")).append('\n');
        out.append("Climate: ").append(snapshot.getString("climate", "--")).append('\n');
      }
    } catch (Throwable e) {
      out.append("PsAssistant provider: ERROR\n").append(e.getClass().getSimpleName())
          .append(": ").append(String.valueOf(e.getMessage())).append("\n\n");
    }

    out.append("\nCurrent experiment scope\n");
    out.append("- com.heytap.mydevices: native device-card injection candidate\n");
    out.append("- com.coloros.assistantscreen: Shelf / negative-one-screen host candidate\n");
    out.append("- com.oplus.pantanal.ums: Fluid Cloud / Suggestions governance candidate\n");
    out.append("- com.heytap.speechassist: Xiaobu command-dispatch candidate\n");
    out.append("- com.oplus.ocar: Car+ container only; not treated as the primary vehicle binding layer\n\n");
    out.append("The module is fail-closed: it currently probes host structure and does not alter OEM vehicle state.");
    text.setText(out.toString());

    ScrollView root = new ScrollView(this);
    root.addView(text);
    setContentView(root);
  }
}
