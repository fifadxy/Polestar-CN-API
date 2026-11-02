package io.github.polestarcn.companion;

import android.content.Context;
import android.content.Intent;
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
  public static final long MIN_CONTROL_HOST_VERSION_CODE = 130000L;

  private OppoOfficialCarLink() {}

  public static Intent bindIntent() {
    return new Intent(Intent.ACTION_VIEW, BIND_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
      out.putBoolean("bindRouteAvailable",
          pm.resolveActivity(bindIntent(),PackageManager.MATCH_DEFAULT_ONLY)!=null);
    } catch(RuntimeException failure) { out.putBoolean("bindRouteAvailable",false); }
    out.putBoolean("controlHostVersionEligible",version>=MIN_CONTROL_HOST_VERSION_CODE);
    out.putBoolean("officialRouteAvailable",out.getBoolean("installed")&&out.getBoolean("serviceAvailable"));
    return out;
  }

  public static String describe(Context context) {
    Bundle s=status(context);
    if(!s.getBoolean("installed",false)) return "OPPO CarLink 官方通道：未检测到／当前不可见";
    return "OPPO CarLink 官方通道："+s.getString("versionName","")+" ("+s.getLong("versionCode",-1L)+")"
        +"\nOCS service="+s.getBoolean("serviceAvailable",false)
        +" · CarAppProvider="+s.getBoolean("providerAvailable",false)
        +" · 我的车辆绑定入口="+s.getBoolean("bindRouteAvailable",false)
        +"\n正式 OCS SDK 初始化仍需 OPPO 为 PsAssistant 分配独立 AUTH_CODE/companyId；不复用 Demo 凭据。";
  }
}
