package io.github.polestarcn.companion;

import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.os.Bundle;
import java.util.List;
import org.json.JSONObject;

/** Cache-only provider self-test. Export booleans/reasons, never device_data or vehicle values. */
final class OppoProviderDiagnostics {
  static JSONObject inspect(Context c) {
    JSONObject out=new JSONObject();
    try {
      PackageManager pm=c.getPackageManager();
      ComponentName name=new ComponentName(c.getPackageName(),OppoDeviceIntegration.PROVIDER_CLASS);
      ProviderInfo info=pm.getProviderInfo(name,PackageManager.GET_META_DATA|PackageManager.MATCH_DISABLED_COMPONENTS);
      boolean enabled=enabled(pm.getComponentEnabledSetting(name),info.enabled);
      out.put("authority",info.authority).put("componentEnabled",enabled).put("exported",info.exported)
          .put("integrationEnabled",OppoDeviceIntegration.enabled(c));
      Bundle catalog=OppoAccountBindDiagnostics.status(c);
      out.put("catalogObservationAvailable",catalog.getLong("catalogSeenAt")>0)
          .put("catalogSeenAt",catalog.getLong("catalogSeenAt")).put("catalogSource",catalog.getString("catalogSource",""))
          .put("catalogContainsPolestar",catalog.getString("providerCatalog","").toUpperCase(java.util.Locale.ROOT).contains("POLESTAR="));
      List<ResolveInfo> found=pm.queryIntentContentProviders(new Intent("oplus.devicecards.action.DEVICE_INFO").setPackage(c.getPackageName()),PackageManager.GET_META_DATA);
      boolean discovered=false;
      if(found!=null)for(ResolveInfo row:found)if(row.providerInfo!=null&&name.getClassName().equals(row.providerInfo.name))discovered=true;
      out.put("discoveredByAction",discovered).put("upstreamResolved",pm.resolveContentProvider(PsAssistantBridgeProvider.AUTHORITY,0)!=null)
          .put("standaloneProviderVisible",pm.resolveContentProvider("io.github.polestarcn.oppobridge.devices",0)!=null);
      // This call only reads local cache. Never refresh, delete or show/unhide a card during a test.
      if(enabled)try(Cursor rows=c.getContentResolver().query(OppoDeviceIntegration.CARD_URI,null,null,null,null)) {
        out.put("querySucceeded",rows!=null).put("rowCount",rows==null?0:rows.getCount());
        if(rows!=null)out.put("queryReason",rows.getExtras().getString("reason","unknown"));
      } else out.put("querySucceeded",false).put("queryReason","component-disabled");
      VehicleCoordinator model=VehicleCoordinator.get(c);
      out.put("signedIn",model.hasSession()).put("selectedVehiclePresent",model.selected!=null)
          .put("snapshotAvailable",model.snapshot!=null).put("snapshotDemo",model.snapshot!=null&&model.snapshot.isDemo);
      Bundle runtime=enabled?c.getContentResolver().call(OppoDeviceIntegration.CARD_URI,"ps_vehicle_diagnostics",null,null):null;
      if(runtime!=null)for(String key:new String[]{"lastQueryAt","lastQueryReason","lastRows","hidden",
          "lastHostQueryAt","lastHostQueryPackage","hostQueryCount","hostQueryObserved",
          "lastHostCallAt","lastHostCallPackage","lastHostCallMethod","hostCallCount","lastSelfTestAt"})out.put(key,runtime.get(key));
    } catch(Exception error) {try{out.put("error",error.getClass().getSimpleName());}catch(Exception ignored){}}
    return out;
  }
  static boolean enabled(int override,boolean declared) {
    return override==PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        ||override==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT&&declared;
  }
}
