package io.github.polestarcn.companion;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import java.util.List;

/**
 * Read-only discovery and user-visible entry points for OPPO's official CarLink route.
 *
 * <p>No demo AUTH_CODE/companyId is bundled here. Full OCS SDK initialization must use
 * credentials issued by OPPO for PsAssistant and its production signing identity.</p>
 */
public final class OppoOfficialCarLink {
  public static final String PACKAGE = "com.heytap.opluscarlink";
  public static final String SERVICE_ACTION = "oppo.intent.action.opluscarlink.OPLUS_SDK_SERVICE";
  public static final String INIT_ACTION = "oppo.intent.action.opluscarlink.INIT_OPLUS_SDK";
  public static final String PROVIDER_AUTHORITY = "com.heytap.opluscarlink.CarAppProvider";
  public static final Uri BIND_URI = Uri.parse("card://carlink.heytap.com/control?operation=bind");
  public static final String BT_KEY_BIND_ACTION = "oppo.intent.action.opluscarlink.BIND_BT_KEY";
  public static final String LOCAL_BIND_ACTION = "io.github.polestarcn.oppobridge.ACTION_OFFICIAL_BIND";
  public static final long MIN_CONTROL_HOST_VERSION_CODE = 130000L;

  private OppoOfficialCarLink() {}

  public static Intent bindIntent() {
    return new Intent(Intent.ACTION_VIEW, BIND_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  public static Intent localBindIntent(Context context) {
    return localBindIntentForPackage(context.getPackageName());
  }

  static Intent localBindIntentForPackage(String packageName) {
    return new Intent(LOCAL_BIND_ACTION)
        .setPackage(packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  static boolean directLaunchAllowed(boolean exported, String permission, int permissionStatus) {
    if (!exported) return false;
    return permission == null || permission.isEmpty()
        || permissionStatus == PackageManager.PERMISSION_GRANTED;
  }

  public static Bundle status(Context context) {
    PackageManager pm=context.getPackageManager();
    Bundle out=new Bundle();
    out.putString("packageName",PACKAGE);
    out.putString("serviceAction",SERVICE_ACTION);
    out.putString("providerAuthority",PROVIDER_AUTHORITY);
    out.putString("bindUri",BIND_URI.toString());
    out.putBoolean("developerAuthorizationRequired",true);
    long version=-1L;
    try {
      PackageInfo info=pm.getPackageInfo(PACKAGE,0);
      version=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
      out.putBoolean("installed",true);
      out.putString("versionName",info.versionName==null?"":info.versionName);
      out.putLong("versionCode",version);
    } catch(PackageManager.NameNotFoundException|SecurityException missing) {
      out.putBoolean("installed",false);out.putString("versionName","");out.putLong("versionCode",-1L);
    }
    try {
      List<ResolveInfo> services=pm.queryIntentServices(new Intent(SERVICE_ACTION).setPackage(PACKAGE),0);
      out.putInt("serviceCount",services==null?0:services.size());
      out.putBoolean("serviceAvailable",services!=null&&!services.isEmpty());
    } catch(RuntimeException failure) {
      out.putInt("serviceCount",0);out.putBoolean("serviceAvailable",false);
    }
    try {
      ProviderInfo provider=pm.resolveContentProvider(PROVIDER_AUTHORITY,0);
      out.putBoolean("providerAvailable",provider!=null);
      if(provider!=null) {
        out.putString("providerPackage",provider.packageName==null?"":provider.packageName);
        out.putBoolean("providerExported",provider.exported);
      }
    } catch(RuntimeException failure) { out.putBoolean("providerAvailable",false); }

    try {
      ResolveInfo resolved=pm.resolveActivity(bindIntent(),PackageManager.MATCH_DEFAULT_ONLY);
      ActivityInfo activity=resolved==null?null:resolved.activityInfo;
      out.putBoolean("bindRouteAvailable",activity!=null);
      if(activity!=null) {
        String permission=activity.permission==null?"":activity.permission;
        int permissionStatus=permission.isEmpty()?PackageManager.PERMISSION_GRANTED:
            pm.checkPermission(permission,context.getPackageName());
        out.putString("bindActivityPackage",activity.packageName==null?"":activity.packageName);
        out.putString("bindActivityName",activity.name==null?"":activity.name);
        out.putBoolean("bindActivityExported",activity.exported);
        out.putString("bindActivityPermission",permission);
        out.putBoolean("bindActivityPermissionGranted",
            permissionStatus==PackageManager.PERMISSION_GRANTED);
        out.putBoolean("bindRouteDirectLaunchAllowed",
            directLaunchAllowed(activity.exported,permission,permissionStatus));
        out.putBoolean("bindRouteProtected",
            !directLaunchAllowed(activity.exported,permission,permissionStatus));
      } else {
        out.putBoolean("bindActivityExported",false);
        out.putString("bindActivityPermission","");
        out.putBoolean("bindActivityPermissionGranted",false);
        out.putBoolean("bindRouteDirectLaunchAllowed",false);
        out.putBoolean("bindRouteProtected",false);
      }
    } catch(RuntimeException failure) {
      out.putBoolean("bindRouteAvailable",false);
      out.putBoolean("bindRouteDirectLaunchAllowed",false);
      out.putBoolean("bindRouteProtected",false);
      out.putString("bindRouteProbeError",failure.getClass().getSimpleName());
    }

    try {
      Intent keyBind=new Intent(BT_KEY_BIND_ACTION);
      List<ResolveInfo> handlers=pm.queryIntentActivities(keyBind,PackageManager.MATCH_DEFAULT_ONLY);
      out.putInt("bluetoothKeyBindHandlerCount",handlers==null?0:handlers.size());
      boolean ours=false;
      if(handlers!=null)for(ResolveInfo handler:handlers)
        if(handler.activityInfo!=null&&context.getPackageName().equals(handler.activityInfo.packageName))ours=true;
      out.putBoolean("psAssistantBluetoothKeyHandlerVisible",ours);
    } catch(RuntimeException failure) {
      out.putInt("bluetoothKeyBindHandlerCount",0);out.putBoolean("psAssistantBluetoothKeyHandlerVisible",false);
    }
    out.putBoolean("controlHostVersionEligible",version>=MIN_CONTROL_HOST_VERSION_CODE);
    out.putBoolean("officialRouteAvailable",out.getBoolean("installed")&&out.getBoolean("serviceAvailable"));
    return out;
  }

  /**
   * Opens the official route only when the manifest contract says the caller may launch it.
   * Protected OPPO routes fall back to PsAssistant's local bind/status UI instead of raising
   * SecurityException. This does not elevate privileges or bypass OPPO authorization.
   */
  public static Bundle launchBind(Activity activity) {
    Bundle route=status(activity);
    Bundle result=new Bundle(route);
    if(route.getBoolean("bindRouteDirectLaunchAllowed",false)) {
      try {
        activity.startActivity(bindIntent());
        result.putString("launchPath","official");
        result.putString("launchReason","");
        return result;
      } catch(SecurityException denied) {
        result.putString("officialLaunchError","SecurityException");
      } catch(ActivityNotFoundException missing) {
        result.putString("officialLaunchError","ActivityNotFoundException");
      } catch(RuntimeException failure) {
        result.putString("officialLaunchError",failure.getClass().getSimpleName());
      }
    } else if(route.getBoolean("bindRouteAvailable",false)) {
      result.putString("officialLaunchError","protected-entry");
    } else {
      result.putString("officialLaunchError","unavailable");
    }

    try {
      activity.startActivity(localBindIntent(activity));
      result.putString("launchPath","local-fallback");
      result.putString("launchReason",result.getString("officialLaunchError","protected-entry"));
    } catch(RuntimeException failure) {
      result.putString("launchPath","failed");
      result.putString("launchReason",failure.getClass().getSimpleName());
    }
    return result;
  }

  public static String describe(Context context) {
    Bundle s=status(context);
    if(!s.getBoolean("installed",false)) return "OPPO CarLink 官方通道：未检测到／当前不可见";
    String bind;
    if(!s.getBoolean("bindRouteAvailable",false)) bind="未发现";
    else if(s.getBoolean("bindRouteDirectLaunchAllowed",false)) bind="可直接启动";
    else bind="受系统权限保护";
    String permission=s.getString("bindActivityPermission","");
    return "OPPO CarLink 官方通道："+s.getString("versionName","")+" ("+s.getLong("versionCode",-1L)+")"
        +"\nOCS service="+s.getBoolean("serviceAvailable",false)
        +" · CarAppProvider="+s.getBoolean("providerAvailable",false)
        +" · 我的车辆绑定入口="+bind
        +"\n目标="+s.getString("bindActivityPackage","")+"/"+s.getString("bindActivityName","")
        +" · exported="+s.getBoolean("bindActivityExported",false)
        +(permission.isEmpty()?"":" · permission="+permission+" · granted="+s.getBoolean("bindActivityPermissionGranted",false))
        +"\n正式 OCS SDK 初始化仍需 OPPO 为 PsAssistant 分配独立 AUTH_CODE/companyId；受保护入口不会通过 Hook 绕过权限。";
  }
}
