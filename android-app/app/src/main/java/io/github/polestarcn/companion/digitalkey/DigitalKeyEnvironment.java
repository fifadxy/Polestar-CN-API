package io.github.polestarcn.companion.digitalkey;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Non-invasive environment probe based on the P319 start gates recovered from
 * the user-provided Polestar China 4.25.0 APK. It does not scan, advertise,
 * connect to a car, or access any key material.
 */
public final class DigitalKeyEnvironment {
  private DigitalKeyEnvironment() {}

  public static final class Result {
    public final boolean bleSupported;
    public final boolean bluetoothEnabled;
    public final boolean batteryOptimizationIgnored;
    public final List<String> missingPermissions;

    Result(
        boolean bleSupported,
        boolean bluetoothEnabled,
        boolean batteryOptimizationIgnored,
        List<String> missingPermissions) {
      this.bleSupported = bleSupported;
      this.bluetoothEnabled = bluetoothEnabled;
      this.batteryOptimizationIgnored = batteryOptimizationIgnored;
      this.missingPermissions = Collections.unmodifiableList(missingPermissions);
    }

    public boolean p319StartPrerequisitesMet() {
      return bleSupported
          && bluetoothEnabled
          && batteryOptimizationIgnored
          && missingPermissions.isEmpty();
    }
  }

  public static Result inspect(Context context) {
    PackageManager pm = context.getPackageManager();
    boolean ble = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

    BluetoothManager manager =
        (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
    boolean enabled = adapter != null && adapter.isEnabled();

    List<String> missing = new ArrayList<>();
    require(context, missing, Manifest.permission.ACCESS_FINE_LOCATION);
    require(context, missing, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    if (Build.VERSION.SDK_INT >= 31) {
      require(context, missing, Manifest.permission.BLUETOOTH_SCAN);
      require(context, missing, Manifest.permission.BLUETOOTH_CONNECT);
      require(context, missing, Manifest.permission.BLUETOOTH_ADVERTISE);
    }

    PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    boolean battery =
        power != null && power.isIgnoringBatteryOptimizations(context.getPackageName());

    return new Result(ble, enabled, battery, missing);
  }

  private static void require(Context context, List<String> missing, String permission) {
    if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
      missing.add(permission);
    }
  }
}
