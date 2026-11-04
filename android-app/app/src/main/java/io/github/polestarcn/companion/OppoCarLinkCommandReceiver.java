package io.github.polestarcn.companion;
import android.content.*;
import io.github.polestarcn.bridge.CarLinkBridgeContract;
/** IOT-permission protected optional transport. Request IDs deduplicate against Provider calls. */
public final class OppoCarLinkCommandReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  if(intent==null||!CarLinkBridgeContract.COMMAND_ACTION.equals(intent.getAction()))return;
  try{if(OppoDeviceIntegration.enabled(context))UploadedCarLinkEndpoint.call(context,"dispatchCarCommand",null,intent.getExtras());}catch(RuntimeException malformed){android.util.Log.w("PsCarLink","Malformed compatibility broadcast");}
 }
}
