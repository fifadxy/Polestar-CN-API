package io.github.polestarcn.oppobridge;

import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class OppoBridgeModule extends XposedModule {
  private static final String TAG="PsOppoBridge";
  private volatile String processName="unknown";

  @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
    processName=param.getProcessName();
    log(Log.INFO,TAG,"module_loaded process="+processName+" systemServer="+param.isSystemServer()
        +" api="+getApiVersion()+" framework="+getFrameworkName()+"/"+getFrameworkVersion());
  }

  @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
    String pkg=param.getPackageName(),process=processName;
    if(process==null||!(process.equals(pkg)||process.startsWith(pkg+":")))return;
    try {
      if(!BridgeSettings.enabledForCurrentHost(pkg)) {
        info("hooks_disabled_by_module_settings package="+pkg+" process="+process);
        return;
      }
      if(RuntimeBridgeClient.PSASSISTANT.equals(pkg)) {
        if(RuntimeBridgeClient.PSASSISTANT.equals(process))PsAssistantRuntimeHook.install(this,param.getClassLoader());
        return;
      }

      boolean directControl=BridgeSettings.directControlForCurrentHost();

      HostProbe.install(this,pkg,param.getClassLoader());
      if(directControl&&(pkg.contains("carlink")||pkg.contains("assistantscreen")||pkg.contains("digitalkey")))
        io.github.polestarcn.oppobridge.uploaded.OppoCarLinkBridgeModule.install(this,pkg,param.getClassLoader());
      VehicleSurfaceProbe.install(this,pkg,process,param.getClassLoader());
      ShelfCardHostProbe.install(this,pkg,process);

      if("com.heytap.mydevices".equals(pkg)||"com.oplus.smartengine".equals(pkg)
          ||"com.oplus.cardservice".equals(pkg)||"com.nearme.instant.platform".equals(pkg)) {
        if(!directControl) {
          info("quickapp_direct_control_disabled package="+pkg);
        } else {
          try{QuickAppRpkPatch.install(this);}catch(Throwable x){warn("quickapp_rpk_patch unavailable="+x.getClass().getSimpleName());}
        }
      }

      if(("com.coloros.assistantscreen".equals(pkg)||"com.oplus.assistantscreen".equals(pkg))&&pkg.equals(process)) {
        if("com.coloros.assistantscreen".equals(pkg)) {
          AssistantScreenHostReporter.report(this,"hooks-entered",true,"");
          try{ShelfPantanalProbe.install(this);}catch(Throwable x){warn("shelf_pantanal_probe unavailable="+x.getClass().getSimpleName());}
          try{SceneAccountBindProbe.install(this);}catch(Throwable x){warn("scene_account_bind_probe unavailable="+x.getClass().getSimpleName());}
          try{AccountProviderCatalogProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("account_provider_catalog_probe unavailable="+x.getClass().getSimpleName());}
          try{SceneAccountBindResultShim.install(this);}catch(Throwable x){warn("scene_account_bind_result unavailable="+x.getClass().getSimpleName());}
          if(directControl)
            try{AssistantScreenNativeVehicleHook.install(this,param.getClassLoader());}catch(Throwable x){warn("assistant_native_vehicle unavailable="+x.getClass().getSimpleName());}
        }
        try{PolestarProviderCatalogShim.install(this,param.getClassLoader());}catch(Throwable x){warn("polestar_provider_catalog unavailable="+x.getClass().getSimpleName());}
        try{VehicleCatalogCursorShim.install(this);}catch(Throwable x){warn("vehicle_catalog_cursor unavailable="+x.getClass().getSimpleName());}
        try{PolestarBindIntentShim.install(this);}catch(Throwable x){warn("polestar_bind_redirect unavailable="+x.getClass().getSimpleName());}
      }

      if("com.heytap.opluscarlink".equals(pkg)) {
        CarLinkHostReporter.report(this,process,"hooks-entered",true,"",param.getClassLoader());
        try{CarLinkLocalPanelHook.install(this);}catch(Throwable x){warn("carlink_local_panel unavailable="+x.getClass().getSimpleName());}
        try{CarLinkServiceEntryProbe.install(this,param.getClassLoader(),process);}catch(Throwable x){warn("carlink_service_entry unavailable="+x.getClass().getSimpleName());}
        try{VehicleCatalogCursorShim.install(this);}catch(Throwable x){warn("vehicle_catalog_cursor unavailable="+x.getClass().getSimpleName());}
        try{CarLinkVirtualSessionShim.install(this,param.getClassLoader());}catch(Throwable x){warn("carlink_virtual_session unavailable="+x.getClass().getSimpleName());}
        // TestLab provider/account row is local-only; official cloud authorization remains separate.
        try{PolestarBindIntentShim.install(this);}catch(Throwable x){warn("carlink_polestar_bind_redirect unavailable="+x.getClass().getSimpleName());}
      }

      if("com.heytap.mydevices".equals(pkg)) {
        try{QuickAppProviderProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_provider_probe unavailable="+x.getClass().getSimpleName());}
        if("com.heytap.mydevices".equals(process)) {
          MyDevicesVehicleHook.installMain(this,param.getClassLoader());
          try{NativeHomeCardHook.install(this,param.getClassLoader());}catch(Throwable x){warn("native_home_card unavailable="+x.getClass().getSimpleName());}
          HostReporter.report(pkg,process,"hooks-installed",true,"");
        } else if("com.heytap.mydevices:cards".equals(process)) {
          if(directControl)MyDevicesVehicleHook.installCardProcess(this,param.getClassLoader());
          if(directControl)try{QuickAppCarDispatchShim.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_car_dispatch unavailable="+x.getClass().getSimpleName());}
          try{QuickAppContractProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_contract_probe unavailable="+x.getClass().getSimpleName());}
          try{QuickAppReplacementProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_replacement_probe unavailable="+x.getClass().getSimpleName());}
          if(directControl)
            try{QuickAppCarDataShim.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_oem_car_data unavailable="+x.getClass().getSimpleName());}
          HostReporter.report(pkg,process,"hooks-installed",true,"");
        }
      }
    } catch(Throwable t) {
      HostReporter.report(pkg,process,"install-failed",false,t.getClass().getSimpleName());
      log(Log.ERROR,TAG,"bridge_install_failed package="+pkg+" process="+process,t);
    }
  }

  void info(String message){log(Log.INFO,TAG,message);}
  void warn(String message){log(Log.WARN,TAG,message);}
}
