package io.github.polestarcn.oppobridge;

import android.content.Context;
import android.net.Uri;

/** Runtime identity shared by the standalone bridge and the integrated PsAssistant build. */
final class BridgeIdentity {
  static final String STANDALONE_PACKAGE = "io.github.polestarcn.oppobridge";
  static final String INTEGRATED_PACKAGE = "io.github.polestarcn.companion";
  static final String STANDALONE_AUTHORITY = STANDALONE_PACKAGE + ".devices";
  static final String INTEGRATED_AUTHORITY = INTEGRATED_PACKAGE + ".devices";

  private BridgeIdentity() {}

  static String packageName(Context context) {
    String current = context == null ? "" : context.getPackageName();
    if (STANDALONE_PACKAGE.equals(current) || INTEGRATED_PACKAGE.equals(current)) return current;
    if (context != null) {
      try {
        if (context.getPackageManager().resolveContentProvider(INTEGRATED_AUTHORITY, 0) != null)
          return INTEGRATED_PACKAGE;
        if (context.getPackageManager().resolveContentProvider(STANDALONE_AUTHORITY, 0) != null)
          return STANDALONE_PACKAGE;
      } catch (RuntimeException ignored) {}
    }
    return INTEGRATED_PACKAGE;
  }

  static String authority(Context context) {
    return STANDALONE_PACKAGE.equals(packageName(context))
        ? STANDALONE_AUTHORITY : INTEGRATED_AUTHORITY;
  }

  static Uri deviceUri(Context context) {
    return Uri.parse("content://" + authority(context) + "/device_info_table_new");
  }

  static String iconUri(Context context) {
    return "android.resource://" + packageName(context) + "/drawable/ic_vehicle_card";
  }

  static String commandScheme(Context context) {
    return STANDALONE_PACKAGE.equals(packageName(context))
        ? "psassistant-oppo-bridge" : "psassistant-companion";
  }

  static boolean isBridgePackage(String value) {
    return STANDALONE_PACKAGE.equals(value) || INTEGRATED_PACKAGE.equals(value);
  }

  static boolean isDeviceAuthority(String value) {
    return STANDALONE_AUTHORITY.equals(value) || INTEGRATED_AUTHORITY.equals(value);
  }
}
