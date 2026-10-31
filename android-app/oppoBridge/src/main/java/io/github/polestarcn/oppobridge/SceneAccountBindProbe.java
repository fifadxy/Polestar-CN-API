package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.net.Uri;
import android.os.Bundle;
import android.content.pm.ProviderInfo;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Read-only SceneService account-binding state probe.
 *
 * <p>AssistantScreen 17.14.60 uses the same provider/method to decide whether an account-bind
 * dialog is needed. We retain only short provider-code-shaped tokens. No account identifier,
 * credential, URL or arbitrary provider payload is persisted.</p>
 */
final class SceneAccountBindProbe {
  private static final Uri URI =
      Uri.parse("content://com.oplus.sceneservice.AccountSDKInitProvider");
  private static final String METHOD = "method_get_account_bind_list";
  private static final String HOST = "com.coloros.assistantscreen";
  private static final String TARGET_VERSION = "17.14.60";
  private static final Pattern SAFE_CODE = Pattern.compile("[A-Za-z0-9_-]{2,32}");

  private SceneAccountBindProbe() {}

  static void install(OppoBridgeModule module) {
    new Handler(Looper.getMainLooper()).post(() -> attempt(module, 0));
  }

  private static void attempt(OppoBridgeModule module, int retry) {
    Application app = currentApplication();
    if (app == null) {
      if (retry < 5) new Handler(Looper.getMainLooper())
          .postDelayed(() -> attempt(module, retry + 1), 900L);
      return;
    }
    try {
      String version = app.getPackageManager().getPackageInfo(HOST, 0).versionName;
      if (!TARGET_VERSION.equals(version)) {
        report(app, false, "host-version-mismatch:" + version, 0, "", "", "", false);
        return;
      }
      ProviderInfo owner=app.getPackageManager().resolveContentProvider(
          "com.oplus.sceneservice.AccountSDKInitProvider",0);
      String ownerPackage=owner==null?"":safeMeta(owner.packageName,96);
      String ownerPermission=owner==null?"":safeMeta(
          owner.readPermission!=null?owner.readPermission:owner.writePermission,128);
      boolean ownerExported=owner!=null&&owner.exported;

      Bundle extras = new Bundle();
      extras.putBoolean("useDB", true);
      Bundle result = app.getContentResolver().call(URI, METHOD, null, extras);
      if (result == null) {
        report(app, false, "null-result", 0, "", "", "", false);
        module.warn("scene_account_bind_probe null_result");
        return;
      }
      int count = Math.max(0, result.getInt("accountBindCount", 0));
      ArrayList<String> raw = result.getStringArrayList("accountBindList");
      Set<String> safe = new LinkedHashSet<>();
      if (raw != null) {
        for (String value : raw) {
          if (value == null) continue;
          String token = value.trim();
          if (SAFE_CODE.matcher(token).matches() && safe.size() < 48)
            safe.add(token.toUpperCase(Locale.ROOT));
        }
      }
      String codes = String.join("|", safe);
      report(app, true, "ok", count, codes, ownerPackage, ownerPermission, ownerExported);
      module.info("scene_account_bind_probe ready=true count=" + count
          + " safeCodes=" + safe.size());
    } catch (SecurityException denied) {
      report(app, false, "permission-denied", 0, "", "", "", false);
      module.warn("scene_account_bind_probe permission_denied");
    } catch (Throwable failure) {
      report(app, false, failure.getClass().getSimpleName(), 0, "", "", "", false);
      module.warn("scene_account_bind_probe failed=" + failure.getClass().getSimpleName());
    }
  }

  static void reportCatalog(Application app, String catalog) {
    try {
      Bundle in = new Bundle();
      in.putBoolean("ready", true);
      in.putString("reason", "provider-catalog");
      in.putString("providerCatalog", catalog == null ? "" : catalog);
      in.putString("hostVersion", TARGET_VERSION);
      in.putString("moduleVersion", "0.9.0");
      BridgeClient.call(app, "account-bind-report", in);
    } catch (Throwable ignored) {}
  }

  private static void report(Application app, boolean ready, String reason, int count, String codes,
      String ownerPackage, String ownerPermission, boolean ownerExported) {
    try {
      Bundle in = new Bundle();
      in.putBoolean("ready", ready);
      in.putString("reason", reason);
      in.putInt("bindCount", count);
      in.putString("boundCodes", codes);
      in.putString("providerOwner", ownerPackage);
      in.putString("providerPermission", ownerPermission);
      in.putBoolean("providerExported", ownerExported);
      in.putString("hostVersion", TARGET_VERSION);
      in.putString("moduleVersion", "0.9.0");
      BridgeClient.call(app, "account-bind-report", in);
    } catch (Throwable ignored) {}
  }

  private static String safeMeta(String value,int max) {
    if(value==null)return "";
    String clean=value.replace('\n',' ').replace('\r',' ').trim();
    return clean.substring(0,Math.min(clean.length(),max));
  }

  private static Application currentApplication() {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch (Throwable ignored) {
      return null;
    }
  }
}
