package io.github.polestarcn.companion;

import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** One-use approvals shared by Provider controls. The PendingIntent contains no action/vehicle data. */
final class BridgeCommandRequests {
  private static final Map<String,Request> pending=new HashMap<>();
  private static final AtomicInteger sequence=new AtomicInteger();
  private static final Handler main=new Handler(Looper.getMainLooper());
  private static final class Request {
    final java.util.function.Consumer<OperationState> completion;final String action,vehicle;final long expires=SystemClock.elapsedRealtime()+60_000L;
    Request(String action,String vehicle,java.util.function.Consumer<OperationState> callback){this.action=action;this.vehicle=vehicle;completion=callback;}
  }
  static synchronized Bundle request(Context c,String action,VehicleCoordinator model) {
    return request(c,action,model,null);
  }
  static synchronized Bundle request(Context c,String action,VehicleCoordinator model,java.util.function.Consumer<OperationState> completion) {
    if(!FirstUseConsent.accepted(c)||!OppoDeviceIntegration.enabled(c)||!model.canCommand(action)||!PsAssistantBridgeProvider.commandStateReady(model.snapshot,System.currentTimeMillis()))return failure("vehicle-not-ready");
    java.util.Iterator<Request> stale=pending.values().iterator();while(stale.hasNext()){Request old=stale.next();if(old.expires<=SystemClock.elapsedRealtime()){stale.remove();finish(old,OperationState.NOT_SENT);}}
    for(Request r:pending.values())if(CommandSlots.group(action).equals(CommandSlots.group(r.action)))return failure("authorization-pending");
    if(pending.size()>=8)return failure("authorization-queue-full");
    String id=UUID.randomUUID().toString();pending.put(id,new Request(action,VehicleCoordinator.identity(model.selected.vin),completion));
    Intent intent=new Intent(c,OppoCarLinkAuthorizeActivity.class).putExtra("task",id).putExtra("providerRequest",true);
    ActivityOptions options=ActivityOptions.makeBasic();
    if(Build.VERSION.SDK_INT>=35)options.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
    PendingIntent approval=PendingIntent.getActivity(c,sequence.incrementAndGet(),intent,PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE,options.toBundle());
    main.postDelayed(()->cancel(id),60_000L);
    Bundle out=new Bundle();out.putBoolean("accepted",true);out.putBoolean("commandSent",false);out.putBoolean("vehicleConfirmed",false);
    out.putString("reason","awaiting-authorization");out.putParcelable("authorizationIntent",approval);return out;
  }
  static synchronized String action(String id) {
    Request r=pending.get(id);if(r==null)return null;
    if(r.expires<=SystemClock.elapsedRealtime()){pending.remove(id);finish(r,OperationState.NOT_SENT);return null;}return r.action;
  }
  static void approve(Context c,String id,String verifiedMode) {
    Request r;synchronized(BridgeCommandRequests.class){r=pending.remove(id);}
    if(r==null)return;
    if(r.expires<=SystemClock.elapsedRealtime()||!OppoDeviceIntegration.enabled(c)||!FirstUseConsent.accepted(c)){finish(r,OperationState.NOT_SENT);return;}
    VehicleCoordinator model=VehicleCoordinator.get(c);
    if(!model.canCommand(r.action)||!PsAssistantBridgeProvider.commandStateReady(model.snapshot,System.currentTimeMillis())||!r.vehicle.equals(VehicleCoordinator.identity(model.selected.vin))
        ||!ControlAuthorization.mode(c,r.action).equals(verifiedMode)){finish(r,OperationState.NOT_SENT);return;}
    if(r.completion==null)model.command(r.action);else model.command(r.action,r.completion);
  }
  static synchronized void cancel(String id){Request r=pending.remove(id);if(r!=null)finish(r,OperationState.NOT_SENT);}
  static synchronized void cancelAll(){java.util.List<Request> old=new java.util.ArrayList<>(pending.values());pending.clear();for(Request r:old)finish(r,OperationState.NOT_SENT);}
  private static void finish(Request r,OperationState result){if(r.completion!=null)main.post(()->r.completion.accept(result));}
  private static Bundle failure(String reason){Bundle out=new Bundle();out.putBoolean("accepted",false);out.putString("reason",reason);return out;}
}
