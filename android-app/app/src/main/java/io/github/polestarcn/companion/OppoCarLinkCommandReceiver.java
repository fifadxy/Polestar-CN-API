package io.github.polestarcn.companion;
import android.content.*;
import io.github.polestarcn.bridge.CarLinkBridgeContract;
/** IOT-permission protected optional transport. Request IDs deduplicate against Provider calls. */
public final class OppoCarLinkCommandReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  if(intent!=null&&OppoDeviceIntegration.enabled(context))new io.github.polestarcn.companion.uploadedcarlink.OppoCarLinkInitReceiver().onReceive(context,intent);
 }
}
