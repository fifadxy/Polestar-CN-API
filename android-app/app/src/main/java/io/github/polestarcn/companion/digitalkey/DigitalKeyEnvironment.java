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
 * READ-ONLY PAK environment probe for the research branch.
 *
 * This probe never scans, advertises, connects, loads OEM SDKs or requests keys.
 * Nearby Device permission is checked BEFORE reading BluetoothAdapter state:
 * Android 12+ may throw SecurityException without BLUETOOTH_CONNECT.
 *
 * Permission requirements here describe a conservative potential P319 setup,
 * not proof that a real digital key is supported or authorized.
 */
public final class DigitalKeyEnvironment {
  private DigitalKeyEnvironment() {}

  public static final class Result {
    public final boolean bleSupported;
    public final boolean bluetoothStateReadable;
    public final boolean bluetoothEnabled;
    public final boolean batteryOptimizationIgnored;
    public final List<String> missingPermissions;

    Result(
        boolean bleSupported, boolean bluetoothStateReadable,
        boolean bluetoothEnabled, boolean batteryOptimizationIgnored,
        List<String> missingPermissions) {
      this.bleSupported = bleSupported;
      this.bluetoothStateReadable = bluetoothStateReadable;
      this.bluetoothEnabled = bluetoothEnabled;
      this.batteryOptimizationIgnored = batteryOptimizationIgnored;
      this.missingPermissions = Collections.unmodifiableList(new ArrayList<>(missingPermissions));
    }

    /** Hardware/permission readiness ONLY, never an authorized or paired key. */
    public boolean p319EnvironmentReady() {
      return bleSupported && bluetoothStateReadable && bluetoothEnabled
          && batteryOptimizationIgnored && missingPermissions.isEmpty();
    }
  }

  public static Result inspect(Context context) {
    PackageManager pm = context.getPackageManager();
    boolean ble = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    List<String> missing = new ArrayList<>();

    if (Build.VERSION.SDK_INT >= 31) {
      require(context, missing, Manifest.permission.BLUETOOTH_SCAN);
      require(context, missing, Manifest.permission.BLUETOOTH_CONNECT);
      require(context, missing, Manifest.permission.BLUETOOTH_ADVERTISE);
    } else {
      // On Android <= 11, app-controlled BLE scanning requires fine location.
      require(context, missing, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    boolean canReadBluetooth = Build.VERSION.SDK_INT < 31
        || granted(context, Manifest.permission.BLUETOOTH_CONNECT);
    boolean enabled = false;
    if (canReadBluetooth) {
      try {
        BluetoothManager manager =
            (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        enabled = adapter != null && adapter.isEnabled();
      } catch (SecurityException ignored) {
        canReadBluetooth = false;
      }
    }

    PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    boolean battery = power != null && power.isIgnoringBatteryOptimizations(
        context.getPackageName());

    return new Result(ble, canReadBluetooth, enabled, battery, missing);
  }

  private static boolean granted(Context context, String permission) {
    return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }

  private static void require(Context context, List<String> missing, String permission) {
    if (!granted(context, permission)) missing.add(permission);
  }
}
