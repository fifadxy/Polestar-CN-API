package io.github.polestarcn.companion;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

/**
 * Runtime capability model derived from OPPO CardWidget SDK 1.2.3 public signatures.
 *
 * <p>The supplied official SDK checks AssistantScreen metadata key
 * {@code oplus.feature.support.flags}; bit 0 means CardWidget support. It falls back to the
 * {@code com.oplus.smartengine} package version and reads {@code com.oplus.uiengine.version}.
 * This class mirrors only those public capability checks and never claims a card is registered.</p>
 */
public final class OppoCardWidgetRuntimeCapability {
  public static final String ASSISTANT_SCREEN = "com.coloros.assistantscreen";
  public static final String ALT_ASSISTANT_SCREEN = "com.oplus.assistantscreen";
  public static final String SMART_ENGINE = "com.oplus.smartengine";
  public static final String CARD_SERVICE = "com.oplus.cardservice";
  public static final String SUPPORT_FLAGS = "oplus.feature.support.flags";
  public static final String UI_ENGINE_VERSION = "com.oplus.uiengine.version";
  public static final int CARD_WIDGET_FLAG = 1;
  public static final String SDK_VERSION_CODE = "10203";
  public static final String SDK_BUILD_TYPE = "external";
  public static final String SDK_BUILD_TIME = "2022-06-14 14:17:24";
  public static final String CARD_SERVICE_SERVER_PROVIDER =
      "com.oplus.cardservice.repository.provider.CardServiceServerProvider";

  private OppoCardWidgetRuntimeCapability() {}

  public static Bundle status(Context context) {
    Bundle out = new Bundle();
    out.putString("sdkVersionCode", SDK_VERSION_CODE);
    out.putString("sdkBuildType", SDK_BUILD_TYPE);
    out.putString("sdkBuildTime", SDK_BUILD_TIME);
    out.putString("cardServiceServerProvider", CARD_SERVICE_SERVER_PROVIDER);

    Bundle metadata = assistantMetadata(context);
    int flags = metadata.getInt(SUPPORT_FLAGS, 0);
    String metadataUi = metadata.getString(UI_ENGINE_VERSION, "");
    out.putInt("assistantSupportFlags", flags);
    out.putBoolean("assistantCardWidgetFlag", supportedByFlag(flags));
    out.putString("assistantUiEngineVersion", metadataUi == null ? "" : metadataUi);

    long smartVersion = packageVersion(context, SMART_ENGINE);
    long cardServiceVersion = packageVersion(context, CARD_SERVICE);
    long assistantVersion = packageVersion(context, ASSISTANT_SCREEN);
    long altAssistantVersion = packageVersion(context, ALT_ASSISTANT_SCREEN);
    out.putLong("smartEngineVersionCode", smartVersion);
    out.putLong("cardServiceVersionCode", cardServiceVersion);
    out.putLong("assistantVersionCode", assistantVersion);
    out.putLong("altAssistantVersionCode", altAssistantVersion);
    out.putBoolean("smartEngineInstalled", smartVersion > 0L);
    out.putBoolean("cardServiceInstalled", cardServiceVersion > 0L);
    out.putBoolean("assistantInstalled", assistantVersion > 0L || altAssistantVersion > 0L);
    out.putBoolean("runtimeCardWidgetSupported",
        supportedByFlag(flags) || smartVersion > 0L);
    out.putString("resolvedUiEngineVersion", resolveUiEngineVersion(smartVersion, metadataUi));
    return out;
  }

  static boolean supportedByFlag(int flags) {
    return (flags & CARD_WIDGET_FLAG) == CARD_WIDGET_FLAG;
  }

  static String resolveUiEngineVersion(long smartEngineVersion, String metadataVersion) {
    if (smartEngineVersion > 0L) return String.valueOf(smartEngineVersion);
    return metadataVersion == null ? "" : metadataVersion;
  }

  private static Bundle assistantMetadata(Context context) {
    for (String pkg : new String[]{ASSISTANT_SCREEN, ALT_ASSISTANT_SCREEN}) {
      try {
        ApplicationInfo info = context.getPackageManager().getApplicationInfo(
            pkg, PackageManager.GET_META_DATA);
        if (info.metaData != null) return new Bundle(info.metaData);
      } catch (PackageManager.NameNotFoundException | SecurityException ignored) {}
    }
    return new Bundle();
  }

  private static long packageVersion(Context context, String pkg) {
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(pkg, 0);
      return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    } catch (PackageManager.NameNotFoundException | SecurityException missing) {
      return -1L;
    }
  }
}