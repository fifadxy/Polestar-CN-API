package io.github.polestarcn.companion;
import android.content.*;
/** Replaces the obsolete demo receiver. Only the protected remote-control initialization is used. */
public final class OppoCarLinkInitReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c,Intent intent){if(intent!=null&&OppoOfficialCarLink.INIT_ACTION.equals(intent.getAction())&&OppoDeviceIntegration.enabled(c)&&FirstUseConsent.accepted(c))OppoCarLinkRuntime.get(c).start();}
}
