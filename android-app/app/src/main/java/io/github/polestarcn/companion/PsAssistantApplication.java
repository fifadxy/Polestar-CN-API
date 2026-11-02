package io.github.polestarcn.companion;
/** Optional SDK registration has no retry loop and never initializes the Bluetooth-key feature. */
public final class PsAssistantApplication extends android.app.Application {
  @Override public void onCreate(){super.onCreate();try{OppoDeviceIntegration.reconcile(this);}catch(RuntimeException ignored){}if(OppoDeviceIntegration.enabled(this))OppoCarLinkRuntime.get(this).start();}
}
