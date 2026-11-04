package io.github.polestarcn.companion;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import io.github.polestarcn.bridge.CarLinkBridgeContract;
/** Explicit Messenger protocol, not a guessed implementation of an OEM AIDL interface. */
public final class OppoCarLinkBridgeService extends Service {
 private final Messenger messenger=new Messenger(new Handler(Looper.getMainLooper()){
  @Override public void handleMessage(Message message){
   if(!OppoCarLinkIpcCaller.allowed(OppoCarLinkBridgeService.this,message.sendingUid))return;
   Bundle data=new Bundle(message.getData());String method=data.getString("method","");
   Bundle result;
   try{if(CarLinkBridgeContract.readMethod(method))result=OppoCarLinkProtocol.read(OppoCarLinkBridgeService.this,method);
    else if(CarLinkBridgeContract.DISPATCH.equals(method))result=OppoCarLinkProtocol.dispatch(OppoCarLinkBridgeService.this,data);
    else {result=new Bundle();result.putBoolean("accepted",false);result.putString("reason","unsupported-method");}
   }catch(RuntimeException malformed){result=new Bundle();result.putBoolean("accepted",false);result.putString("reason","malformed-request");}
   if(message.replyTo!=null)try{Message reply=Message.obtain();reply.what=message.what;reply.setData(result);message.replyTo.send(reply);}catch(RemoteException ignored){}
  }
 });
 @Override public IBinder onBind(Intent intent){return messenger.getBinder();}
 @Override public int onStartCommand(Intent intent,int flags,int id){stopSelf(id);return START_NOT_STICKY;}
}
