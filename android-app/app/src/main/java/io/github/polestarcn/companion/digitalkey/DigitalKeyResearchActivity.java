package io.github.polestarcn.companion.digitalkey;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.github.polestarcn.companion.VehicleCoordinator;
import io.github.polestarcn.companion.auth.SecureSessionStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Research-only diagnostics. No credential access, vehicle discovery,
 * key enrolment, BLE scanning/connection, or remote vehicle commands.
 */
public final class DigitalKeyResearchActivity extends Activity {
  private static final int NEARBY_PERMISSION_REQUEST = 319;
  private static final int INK = Color.rgb(240, 240, 238);
  private static final int MUTED = Color.rgb(164, 168, 171);
  private static final int ORANGE = Color.rgb(255, 107, 0);
  private final ResearchDigitalKeyProvider provider =
      new ResearchDigitalKeyProvider("OEM SDK and real-vehicle enrolment not verified");

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + .5f);
  }

  private TextView text(String value, int size, int color) {
    TextView v = new TextView(this);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setPadding(0, dp(7), 0, dp(7));
    v.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    return v;
  }

  private Button button(String label, Runnable run) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setMinHeight(dp(48));
    b.setTextColor(INK);
    b.setTextSize(14);
    GradientDrawable surface = new GradientDrawable();
    surface.setColor(Color.rgb(42, 45, 49));
    surface.setCornerRadius(dp(8));
    b.setBackground(surface);
    b.setPadding(dp(10), dp(10), dp(10), dp(10));
    b.setOnClickListener(view -> run.run());
    return b;
  }

  private String yesNo(boolean ok) { return ok ? "YES / 是" : "NO / 否"; }

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(15, 17, 19));
    getWindow().setNavigationBarColor(Color.rgb(15, 17, 19));
    getWindow().getDecorView().setSystemUiVisibility(0);
    render();
  }

  private void render() {
    DigitalKeyEnvironment.Result result = DigitalKeyEnvironment.inspect(this);
    NativeLibraryProbe.Result nativeState = NativeLibraryProbe.inspect(this, false);

    boolean accountSession = SecureSessionStore.hasSession(this);
    VehicleCoordinator coordinator = VehicleCoordinator.get(this);
    boolean vehicleSelected =
        accountSession && coordinator.selected != null && coordinator.selected.vin != null;
    boolean nativePrerequisites = nativeState.glAlgorithmBridgePresent
        && nativeState.passiveSchemaCount == 3;
    for (NativeLibraryProbe.Item item : nativeState.items) {
      nativePrerequisites = nativePrerequisites && item.packaged;
    }
    AuthorizedFlowAssessment flow =
        AuthorizedFlowAssessment.observed(
            accountSession,
            vehicleSelected,
            false,
            nativePrerequisites);

    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(Color.rgb(15, 17, 19));

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(22), dp(22), dp(22), dp(28));
    scroll.addView(root);

    root.addView(text("DIGITAL KEY / 数字钥匙", 25, INK));
    root.addView(text("RESEARCH ONLY · 仅环境检查", 13, ORANGE));
    root.addView(text(
        "当前没有启用数字钥匙配对或车辆控制。本页不扫描车辆、不读取证书或钥匙，也不连接官方服务。\n"
        + "Pairing and key operations are disabled. No keys, certificates or account data are read.",
        13, MUTED));

    root.addView(text("Android API / 系统: " + Build.VERSION.SDK_INT, 15, INK));
    root.addView(text("BLE hardware / 蓝牙低功耗: " + yesNo(result.bleSupported), 15, INK));
    root.addView(text("Bluetooth state readable / 状态可读取: "
        + yesNo(result.bluetoothStateReadable), 15, INK));
    root.addView(text("Bluetooth ON / 蓝牙开启: "
        + (result.bluetoothStateReadable ? yesNo(result.bluetoothEnabled) : "UNKNOWN / 未授权"),
        15, INK));
    root.addView(text("Battery optimization exempt / 电池白名单: "
        + yesNo(result.batteryOptimizationIgnored), 15, INK));

    if (result.missingPermissions.isEmpty()) {
      root.addView(text("Nearby permissions / 附近设备权限: GRANTED / 已授权", 15, INK));
    } else {
      root.addView(text("Permissions pending / 待授权权限: "
          + result.missingPermissions.size(), 15, ORANGE));
      for (String permission : result.missingPermissions) {
        root.addView(text("· " + permission.substring(permission.lastIndexOf('.') + 1),
            13, MUTED));
      }
    }

    root.addView(text("P319 environment check / 基础环境: "
        + (result.p319EnvironmentReady() ? "PASS" : "NOT READY"), 16, INK));

    root.addView(text("NATIVE SDK V2 / 原厂原生库", 17, INK));
    for (NativeLibraryProbe.Item item : nativeState.items) {
      root.addView(text(item.library + " packaged / 已打包: "
          + yesNo(item.packaged), 14, item.packaged ? INK : ORANGE));
    }
    root.addView(text("VOCMO Java bridge / Java 绑定类: "
        + yesNo(nativeState.vocmoJavaBridgePresent), 14, MUTED));
    root.addView(text("GLBLE Java bridge / JNI 注册类: "
        + yesNo(nativeState.glAlgorithmBridgePresent), 14,
        nativeState.glAlgorithmBridgePresent ? INK : ORANGE));
    root.addView(text("Passive schemas / PassiveEntry 资源: "
        + nativeState.passiveSchemaCount + "/3", 14,
        nativeState.passiveSchemaCount == 3 ? INK : MUTED));
    root.addView(text("V2.1 AUTHORIZED FLOW / 授权链检查", 17, INK));
    root.addView(text("Account session / 账号会话: "
        + yesNo(flow.completed(AuthorizedFlowAssessment.Stage.ACCOUNT_LOGIN)), 14,
        flow.completed(AuthorizedFlowAssessment.Stage.ACCOUNT_LOGIN) ? INK : ORANGE));
    root.addView(text("Vehicle selected / 已发现车辆: "
        + yesNo(flow.completed(AuthorizedFlowAssessment.Stage.VEHICLE_DISCOVERY)), 14,
        flow.completed(AuthorizedFlowAssessment.Stage.VEHICLE_DISCOVERY) ? INK : ORANGE));
    root.addView(text("OEM key identity / 厂商钥匙身份: "
        + yesNo(flow.completed(AuthorizedFlowAssessment.Stage.OEM_IDENTITY)), 14, ORANGE));
    root.addView(text("Native prerequisites / Native 前置资源: "
        + yesNo(nativePrerequisites), 14, nativePrerequisites ? INK : ORANGE));
    root.addView(text("First blocked stage / 首个阻塞阶段: "
        + AuthorizedFlowAssessment.label(flow.firstBlockedStage()), 14, ORANGE));
    root.addView(text(
        "本页只读取本应用现有会话/车辆选择和本地资源状态；不会自动登录、发现车辆或推进受保护的数字钥匙阶段。\n"
        + "This page only reads existing app state and local resources; protected key stages are not advanced.",
        12, MUTED));

    root.addView(text("OEM integration / 厂商 SDK: NOT CONNECTED / 未接入", 15, ORANGE));
    root.addView(text("Vehicle pairing / 车辆配对: NOT VERIFIED / 未验证", 15, ORANGE));
    root.addView(text("Working digital key / 可用数字钥匙: "
        + yesNo(provider.isOperational() && provider.status().canClaimWorkingKey()),
        15, ORANGE));
    root.addView(text("模拟器支持 BLE 只说明系统接口存在，不证明可以与极星 2 配对。\n"
        + "Emulator BLE support does not validate OEM credentials or a vehicle key.",
        12, MUTED));

    if (!result.missingPermissions.isEmpty()) {
      root.addView(button("申请附近设备权限 / Request nearby permissions",
          this::requestNearbyPermissions));
    }
    root.addView(button("加载原厂 Native SDK / Load bundled native SDK", this::loadNativeSdk));
    root.addView(button("重新检查 / Refresh", this::render));
    root.addView(button("返回 / Back", this::finish));
    setContentView(scroll);
  }

  private void loadNativeSdk() {
    NativeLibraryProbe.Result result = NativeLibraryProbe.inspect(this, true);
    StringBuilder message = new StringBuilder();
    for (NativeLibraryProbe.Item item : result.items) {
      message.append(item.library).append(": ")
          .append(item.loaded ? "LOADED" : (item.packaged ? "LOAD FAILED" : "NOT PACKAGED"));
      if (!item.error.isEmpty()) message.append("\n").append(item.error);
      message.append("\n\n");
    }
    message.append("VOCMO Java bridge: ")
        .append(result.vocmoJavaBridgePresent ? "PRESENT" : "MISSING");
    new android.app.AlertDialog.Builder(this)
        .setTitle("Native SDK V2")
        .setMessage(message.toString().trim())
        .setPositiveButton("OK", null)
        .show();
  }

  private void requestNearbyPermissions() {
    List<String> requested = new ArrayList<>();
    if (Build.VERSION.SDK_INT >= 31) {
      requested.add(Manifest.permission.BLUETOOTH_SCAN);
      requested.add(Manifest.permission.BLUETOOTH_CONNECT);
      requested.add(Manifest.permission.BLUETOOTH_ADVERTISE);
    } else {
      requested.add(Manifest.permission.ACCESS_FINE_LOCATION);
    }
    List<String> pending = new ArrayList<>();
    for (String permission : requested) {
      if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
        pending.add(permission);
    }
    if (!pending.isEmpty()) {
      requestPermissions(pending.toArray(new String[0]), NEARBY_PERMISSION_REQUEST);
    } else {
      render();
    }
  }

  @Override public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (requestCode == NEARBY_PERMISSION_REQUEST) render();
  }
}
