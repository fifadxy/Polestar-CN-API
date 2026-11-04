package io.github.polestarcn.oppobridge;
import android.content.Context;
import android.os.*;
import io.github.polestarcn.bridge.CarLinkBridgeContract;
/** Modern LSPosed hosts use the same contract compiled into the main APK. No broadcast retry. */
final class CarLinkProtocolClient {
 private CarLinkProtocolClient(){}
 static boolean supports(String a){return "lock".equals(a)||"unlock".equals(a)||"climate-start".equals(a)||"climate-stop".equals(a)||"windows-open".equals(a)||"windows-close".equals(a)||"honk-flash".equals(a);}
 static Bundle request(String action){Bundle b=new Bundle();b.putString(CarLinkBridgeContract.COMMAND,action);b.putString(CarLinkBridgeContract.REQUEST,java.util.UUID.randomUUID().toString());return b;}
 static Bundle command(Context c,String action){
  Bundle in=request(action);
  in.putParcelable(CarLinkBridgeContract.RECEIVER,new ResultReceiver(new Handler(Looper.getMainLooper())){
   @Override protected void onReceiveResult(int code,Bundle out){
    if(out==null)return;android.app.PendingIntent approval=out.getParcelable("authorizationIntent");
    if(approval!=null)BridgeClient.sendPendingIntent(c,approval);
    android.util.Log.i("PsCarLink","phase="+out.getString("phase","")+" confirmed="+out.getBoolean("vehicleConfirmed"));
   }
  });
  return BridgeClient.call(c,CarLinkBridgeContract.DISPATCH,in);
 }
}
