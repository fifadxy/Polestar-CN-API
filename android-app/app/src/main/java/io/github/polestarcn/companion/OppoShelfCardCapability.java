package io.github.polestarcn.companion;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

/**
 * Read-only capability model for OPPO Shelf / Seedling plugin cards.
 *
 * <p>Document 11733 and the related Seedling documentation describe the official card route.
 * This public build intentionally does not invent SDK classes or bypass OPPO whitelisting. It
 * exposes only host/package readiness and the already observed Pantanal Shelf metadata.</p>
 */
public final class OppoShelfCardCapability {
  public static final String ASSISTANT_SCREEN = "com.coloros.assistantscreen";
  public static final String PANTANAL_UMS = "com.oplus.pantanal.ums";
  public static final String QUICK_APP_PLATFORM = "com.nearme.instant.platform";
  public static final String DOC_ID = "11733";

  private OppoShelfCardCapability() {}

  public static Bundle status(Context context) {
    Bundle out = new Bundle();
    out.putString("documentId", DOC_ID);
    out.putBoolean("officialSdkRequired", true);
    out.putBoolean("oppoWhitelistingMayBeRequired", true);
    putPackage(context, out, "assistantScreen", ASSISTANT_SCREEN);
    putPackage(context, out, "pantanal", PANTANAL_UMS);
    putPackage(context, out, "quickAppPlatform", QUICK_APP_PLATFORM);

    Bundle cardWidget = OppoCardWidgetRuntimeCapability.status(context);
    out.putString("cardWidgetSdkVersionCode", cardWidget.getString("sdkVersionCode", ""));
    out.putBoolean("cardWidgetRuntimeSupported", cardWidget.getBoolean("runtimeCardWidgetSupported", false));
    out.putBoolean("assistantCardWidgetFlag", cardWidget.getBoolean("assistantCardWidgetFlag", false));
    out.putBoolean("smartEngineInstalled", cardWidget.getBoolean("smartEngineInstalled", false));
    out.putLong("smartEngineVersionCode", cardWidget.getLong("smartEngineVersionCode", -1L));
    out.putBoolean("cardServiceInstalled", cardWidget.getBoolean("cardServiceInstalled", false));
    out.putLong("cardServiceVersionCode", cardWidget.getLong("cardServiceVersionCode", -1L));
    out.putString("uiEngineVersion", cardWidget.getString("resolvedUiEngineVersion", ""));

    Bundle shelf = OppoShelfDiagnostics.status(context);
    out.putBoolean("shelfMetadataReady", shelf.getBoolean("ready", false));
    out.putLong("shelfSeenAt", shelf.getLong("seenAt", 0L));
    out.putInt("shelfServiceRows", shelf.getInt("rowCount", 0));
    out.putString("shelfCandidates", shelf.getString("candidates", ""));
    out.putString("shelfServices", shelf.getString("services", ""));

    Bundle host = OppoShelfHostDiagnostics.status(context);
    out.putBoolean("pantanalHookSeen", host.getLong("pantanal_seenAt", 0L) > 0L);
    out.putBoolean("assistantShelfHookSeen", host.getLong("assistant_seenAt", 0L) > 0L);
    out.putString("pantanalVersion", host.getString("pantanal_versionName", ""));
    out.putString("assistantShelfVersion", host.getString("assistant_versionName", ""));
    out.putString("pantanalComponents", host.getString("pantanal_components", ""));
    out.putString("assistantShelfComponents", host.getString("assistant_components", ""));
    out.putBoolean("smartEngineHookSeen", host.getLong("smartengine_seenAt", 0L) > 0L);
    out.putBoolean("cardServiceHookSeen", host.getLong("cardservice_seenAt", 0L) > 0L);
    out.putString("smartEngineHookVersion", host.getString("smartengine_versionName", ""));
    out.putString("cardServiceHookVersion", host.getString("cardservice_versionName", ""));

    boolean hostVisible = out.getBoolean("pantanalInstalled", false)
        || out.getBoolean("assistantScreenInstalled", false);
    out.putBoolean("cardHostVisible", hostVisible);
    out.putBoolean("officialCardReadyForPrivateAdapter",
        hostVisible && out.getBoolean("cardWidgetRuntimeSupported", false)
        && (out.getBoolean("shelfMetadataReady", false)
        || out.getBoolean("pantanalHookSeen", false)
        || out.getBoolean("cardServiceInstalled", false)));
    return out;
  }

  public static String describe(Context context) {
    Bundle s = status(context);
    return "OPPO 负一屏插件卡: host=" + s.getBoolean("cardHostVisible", false)
        + " · Shelf metadata=" + s.getBoolean("shelfMetadataReady", false)
        + " · CardWidget=" + s.getBoolean("cardWidgetRuntimeSupported", false)
        + " / SDK " + s.getString("cardWidgetSdkVersionCode", "")
        + " · UIEngine=" + s.getString("uiEngineVersion", "")
        + " · Pantanal Hook=" + s.getBoolean("pantanalHookSeen", false)
        + " (" + s.getString("pantanalVersion", "") + ")"
        + "\n正式 Seedling/插件卡发布仍需 OPPO SDK、事件/模板配置及可能的白名单；当前只做真实数据映射和宿主探测。";
  }

  private static void putPackage(Context context, Bundle out, String prefix, String pkg) {
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(pkg, 0);
      out.putBoolean(prefix + "Installed", true);
      out.putString(prefix + "VersionName", info.versionName == null ? "" : info.versionName);
      out.putLong(prefix + "VersionCode", Build.VERSION.SDK_INT >= 28
          ? info.getLongVersionCode() : info.versionCode);
    } catch (PackageManager.NameNotFoundException | SecurityException missing) {
      out.putBoolean(prefix + "Installed", false);
      out.putString(prefix + "VersionName", "");
      out.putLong(prefix + "VersionCode", -1L);
    }
  }
}