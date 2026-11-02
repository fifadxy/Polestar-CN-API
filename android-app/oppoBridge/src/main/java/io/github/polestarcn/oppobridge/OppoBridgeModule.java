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
      if(RuntimeBridgeClient.PSASSISTANT.equals(pkg)) {
        if(RuntimeBridgeClient.PSASSISTANT.equals(process))PsAssistantRuntimeHook.install(this,param.getClassLoader());
        return;
      }

      HostProbe.install(this,pkg,param.getClassLoader());
      VehicleSurfaceProbe.install(this,pkg,process,param.getClassLoader());
      ShelfCardHostProbe.install(this,pkg,process);

      if(("com.coloros.assistantscreen".equals(pkg)||"com.oplus.assistantscreen".equals(pkg))&&pkg.equals(process)) {
        if("com.coloros.assistantscreen".equals(pkg)) {
          AssistantScreenHostReporter.report(this,"hooks-entered",true,"");
          try{ShelfPantanalProbe.install(this);}catch(Throwable x){warn("shelf_pantanal_probe unavailable="+x.getClass().getSimpleName());}
          try{SceneAccountBindProbe.install(this);}catch(Throwable x){warn("scene_account_bind_probe unavailable="+x.getClass().getSimpleName());}
          try{AccountProviderCatalogProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("account_provider_catalog_probe unavailable="+x.getClass().getSimpleName());}
          try{SceneAccountBindResultShim.install(this);}catch(Throwable x){warn("scene_account_bind_result unavailable="+x.getClass().getSimpleName());}
          try{AssistantScreenNativeVehicleHook.install(this,param.getClassLoader());}catch(Throwable x){warn("assistant_native_vehicle unavailable="+x.getClass().getSimpleName());}
        }
        try{PolestarProviderCatalogShim.install(this,param.getClassLoader());}catch(Throwable x){warn("polestar_provider_catalog unavailable="+x.getClass().getSimpleName());}
        try{PolestarBindIntentShim.install(this);}catch(Throwable x){warn("polestar_bind_redirect unavailable="+x.getClass().getSimpleName());}
      }

      if("com.heytap.opluscarlink".equals(pkg)) {
        CarLinkHostReporter.report(this,process,"hooks-entered",true,"",param.getClassLoader());
        // The official host may launch a synthetic provider/account row. Redirect only a payload
        // explicitly marked POLESTAR; do not alter key/auth results.
        try{PolestarBindIntentShim.install(this);}catch(Throwable x){warn("carlink_polestar_bind_redirect unavailable="+x.getClass().getSimpleName());}
      }

      if("com.heytap.mydevices".equals(pkg)) {
        try{QuickAppProviderProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_provider_probe unavailable="+x.getClass().getSimpleName());}
        if("com.heytap.mydevices".equals(process)) {
          MyDevicesVehicleHook.installMain(this,param.getClassLoader());
          try{NativeHomeCardHook.install(this,param.getClassLoader());}catch(Throwable x){warn("native_home_card unavailable="+x.getClass().getSimpleName());}
          HostReporter.report(pkg,process,"hooks-installed",true,"");
        } else if("com.heytap.mydevices:cards".equals(process)) {
          MyDevicesVehicleHook.installCardProcess(this,param.getClassLoader());
          try{QuickAppContractProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_contract_probe unavailable="+x.getClass().getSimpleName());}
          try{QuickAppReplacementProbe.install(this,param.getClassLoader());}catch(Throwable x){warn("quickapp_replacement_probe unavailable="+x.getClass().getSimpleName());}
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