package io.github.polestarcn.companion;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.*;
import io.github.polestarcn.bridge.CarLinkBridgeContract;
import java.util.*;

/** Uploaded dual-route protocol adapted to the real coordinator; acknowledgements are not success. */
final class OppoCarLinkProtocol {
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static final long TTL=120_000;
 private static final Map<String,Ticket> TICKETS=new LinkedHashMap<>();
 private static final class Ticket {
  final String action,vehicle,id;final long at=SystemClock.elapsedRealtime();final ResultReceiver receiver;
  volatile Bundle result;Ticket(String a,String v,String i,ResultReceiver r){action=a;vehicle=v;id=i;receiver=r;}
 }
 private OppoCarLinkProtocol(){}
 static boolean supports(Uri uri){return uri!=null&&CarLinkBridgeContract.AUTHORITY.equals(uri.getAuthority())
   &&(uri.getPath()==null||uri.getPath().isEmpty()||"/".equals(uri.getPath())||("/"+CarLinkBridgeContract.BIND_PATH).equals(uri.getPath()));}
 static Bundle read(Context c,String method){
  VehicleCoordinator model=VehicleCoordinator.get(c);Bundle b=VirtualVehicleSessionManager.status(c,model);
  b.putInt("bridgeProtocolVersion",CarLinkBridgeContract.VERSION);
  boolean bound=BuildConfig.OPPO_SDK_MOCK?b.getBoolean("present"):OppoOfficialVehicleBindState.status(c,model).getBoolean("enabled")&&b.getBoolean("present");
  b.putBoolean("hasCarBinding",bound);b.putBoolean("has_car_binding",bound);b.putBoolean("is_bound",bound);
  b.putString("bindingScope","local-bridge");b.putBoolean("officialSdkAuthorized",OppoCarLinkRuntime.get(c).authorized());
  b.putString("brand_name","Polestar");b.putString("vehicle_name",b.getString("displayName","Polestar"));
  b.putInt("connect_state",b.getBoolean("active")?2:0);b.putInt("capability_mask",bound?15:0);
  // Compatibility identifiers stay synthetic, rather than exposing the account's VIN.
  b.putString("vehicle_vin",b.getString("testVin",""));b.putBoolean("syntheticIdentifiers",true);
  VehicleSnapshot s=BuildConfig.OPPO_SDK_MOCK?OppoCarLinkMockFixture.snapshot():model.snapshot;
  if(s==null)s=VehicleSnapshotStore.loadChina(c);
  if(s!=null&&!s.isDemo){
   integer(b,"soc_percent",s.batteryPercent);integer(b,"range_km",s.rangeKm);
   flag(b,"is_locked",s.lock,new String[]{"已上锁","已锁","LOCKED"},new String[]{"未锁","已解锁","UNLOCKED"});
   flag(b,"is_climate_active",s.climate,new String[]{"开启","ON","ACTIVE"},new String[]{"关闭","OFF","INACTIVE"});
   flag(b,"is_window_closed",s.windows,new String[]{"全部关闭","已关闭","CLOSED"},new String[]{"未关严","OPEN"});
   b.putLong("updated_at",s.timestampMillis);
  }
  return b;
 }
 static String selfCheck(Context c){
  android.content.ContentResolver resolver=c.getContentResolver();Uri root=PsAssistantBridgeProvider.URI;
  Bundle session=resolver.call(root,CarLinkBridgeContract.SESSION,null,null);
  Uri uri=Uri.withAppendedPath(root,CarLinkBridgeContract.BIND_PATH);int rows=0;try(Cursor cursor=resolver.query(uri,new String[]{"is_bound","bindingScope"},null,null,null)){if(cursor!=null)rows=cursor.getCount();}
  Bundle direction=new Bundle();direction.putBoolean(CarLinkBridgeContract.TARGET,true);
  boolean mapping="lock".equals(OppoCarLinkCommandMap.bridgeAction(1001,direction))&&"climate-start".equals(OppoCarLinkCommandMap.bridgeAction(1002,direction))&&"windows-close".equals(OppoCarLinkCommandMap.bridgeAction(1003,direction))&&"honk-flash".equals(OppoCarLinkCommandMap.bridgeAction(1004,direction));
  return "本机协议v"+(session==null?0:session.getInt("bridgeProtocolVersion"))+" · 绑定查询行="+rows+" · 四类映射="+mapping+"\n仅自检主程序协议；未发送车控，LSPosed宿主仍需真机验收。";
 }
 private static void integer(Bundle b,String key,Integer value){b.putBoolean(key+"_known",value!=null);if(value!=null)b.putInt(key,value);}
 private static void flag(Bundle b,String key,String raw,String[] yes,String[] no){String v=raw==null?"":raw.trim().toUpperCase(Locale.ROOT);boolean t=Arrays.asList(yes).contains(v),f=Arrays.asList(no).contains(v);b.putBoolean(key+"_known",t||f);if(t||f)b.putBoolean(key,t);}
 static Cursor query(Context c,Uri uri,String[] projection){
  if(!supports(uri))throw new IllegalArgumentException("Unsupported bridge URI");
  Bundle b=read(c,CarLinkBridgeContract.BIND_PATH);
  boolean bind=("/"+CarLinkBridgeContract.BIND_PATH).equals(uri.getPath());
  if(!bind)return vehicleCursor(c,b,projection);
  String[] columns=projection==null?new String[]{"hasCarBinding","has_car_binding","is_bound","vin","brand","vehicle_name","code","bindingScope"}:projection.clone();
  Set<String> allowed=new HashSet<>(Arrays.asList("hasCarBinding","has_car_binding","is_bound","vin","brand","vehicle_name","code","bindingScope"));
  for(String column:columns)if(!allowed.contains(column))throw new IllegalArgumentException("Unknown bridge column");
  MatrixCursor out=new MatrixCursor(columns,1);Object[] values=new Object[columns.length];
  for(int i=0;i<columns.length;i++)switch(columns[i]){
   case "hasCarBinding":case "has_car_binding":case "is_bound":values[i]=b.getBoolean("hasCarBinding")?1:0;break;
   case "vin":values[i]=b.getString("vehicle_vin","");break;case "brand":values[i]="Polestar";break;
   case "vehicle_name":values[i]=b.getString("vehicle_name");break;case "code":values[i]=0;break;default:values[i]="local-bridge";
  }
  out.addRow(values);out.setNotificationUri(c.getContentResolver(),Uri.parse("content://"+CarLinkBridgeContract.AUTHORITY));return out;
 }
 private static Cursor vehicleCursor(Context c,Bundle b,String[] projection){
  String[] defaults={"package_name","brand_name","vehicle_name","vin","hasCarBinding","has_binding","connect_state","capability_mask","is_locked","is_climate_active","is_window_closed","soc","range_km","synthetic_identifiers"};
  String[] columns=projection==null?defaults:projection.clone();Set<String> allowed=new HashSet<>(Arrays.asList(defaults));
  for(String key:columns)if(!allowed.contains(key))throw new IllegalArgumentException("Unknown vehicle column");
  MatrixCursor out=new MatrixCursor(columns,1);Object[] row=new Object[columns.length];
  for(int i=0;i<columns.length;i++){String key=columns[i];switch(key){
   case "package_name":row[i]=c.getPackageName();break;case "brand_name":row[i]="Polestar";break;case "vehicle_name":row[i]=b.getString(key);break;
   case "vin":row[i]=b.getString("vehicle_vin","");break;case "hasCarBinding":case "has_binding":row[i]=b.getBoolean("hasCarBinding")?1:0;break;
   case "connect_state":case "capability_mask":row[i]=b.getInt(key);break;case "synthetic_identifiers":row[i]=1;break;
   case "soc":row[i]=b.getBoolean("soc_percent_known")?b.getInt("soc_percent"):null;break;
   case "range_km":row[i]=b.getBoolean("range_km_known")?b.getInt(key):null;break;
   default:row[i]=b.getBoolean(key+"_known")?(b.getBoolean(key)?1:0):null;
  }}out.addRow(row);out.setNotificationUri(c.getContentResolver(),PsAssistantBridgeProvider.URI);return out;
 }
 static Bundle dispatch(Context c,Bundle input){
  Bundle in=input==null?new Bundle():new Bundle(input);
  String action=OppoCarLinkCommandMap.bridgeAction(in.getInt(CarLinkBridgeContract.EVENT,-1),in);
  if(action==null)return reject("unsupported-or-incomplete-event");
  if(in.containsKey("target_temp"))return reject("request-temperature-not-supported");
  VehicleCoordinator model=VehicleCoordinator.get(c);Bundle gate=VehicleProtocolGateway.resolve(c,model,action);
  if(!gate.getBoolean("accepted",false)){
   // A stale cache keeps the vehicle card visible but MUST NOT dispatch a
   // command. Request one real refresh; the next user action is revalidated.
   if("fresh-vehicle-state-required".equals(gate.getString("reason",""))){
    MAIN.post(()->{if(OppoDeviceIntegration.enabled(c)&&model.hasSession())
     VehicleRefreshManager.get(c).requestBridge(null);});
    gate.putBoolean("refreshRequested",true);
   }
   return gate;
  }
  if(!FirstUseConsent.accepted(c)||!model.canCommand(action))return reject("vehicle-not-ready");
  String id=in.getString(CarLinkBridgeContract.REQUEST,"");if(id.isEmpty())id=UUID.randomUUID().toString();
  if(!id.matches("[A-Za-z0-9_.:-]{1,128}"))return reject("invalid-request-id");
  String vehicle=gate.getString("vehicleKey","");final Ticket ticket;
  synchronized(TICKETS){
   TICKETS.values().removeIf(t->SystemClock.elapsedRealtime()-t.at>=TTL);
   Ticket old=TICKETS.get(id);
   if(old!=null){if(!old.vehicle.equals(vehicle)||!old.action.equals(action))return reject("request-id-conflict");Bundle duplicate=new Bundle(old.result);duplicate.putBoolean("duplicate",true);return duplicate;}
   if(TICKETS.size()>=128)return reject("protocol-queue-full");
   ticket=new Ticket(action,vehicle,id,in.getParcelable(CarLinkBridgeContract.RECEIVER));
   ticket.result=ack(ticket,"queued",true,false);TICKETS.put(id,ticket);
  }
  MAIN.post(()->execute(c.getApplicationContext(),ticket));
  return new Bundle(ticket.result);
 }
 private static void execute(Context c,Ticket t){
  VehicleCoordinator m=VehicleCoordinator.get(c);
  Bundle gate=VehicleProtocolGateway.resolve(c,m,t.action);
  if(!gate.getBoolean("accepted")||!t.vehicle.equals(gate.getString("vehicleKey"))||!m.canCommand(t.action)||!FirstUseConsent.accepted(c)) {finish(t,OperationState.NOT_SENT);return;}
  if(BuildConfig.OPPO_SDK_MOCK){Bundle mock=OppoCarLinkMockHarness.submit(c,t.action);Bundle out=ack(t,mock.getBoolean("accepted")?"mock-dry-run":"mock-rejected",mock.getBoolean("accepted"),true);out.putBoolean("mock",true);out.putBoolean("exec_success",false);publish(t,out);return;}
  java.util.function.Consumer<OperationState> completion=s->finish(t,s);
  if(!"direct".equals(ControlAuthorization.mode(c,t.action))){
   Bundle approval=BridgeCommandRequests.request(c,t.action,m,completion);
   Bundle out=ack(t,approval.getString("reason","authorization-rejected"),approval.getBoolean("accepted"),!approval.getBoolean("accepted"));out.putAll(approval);publish(t,out);return;
  }
  m.command(t.action,completion);
 }
 private static Bundle ack(Ticket t,String phase,boolean accepted,boolean terminal){Bundle b=new Bundle();b.putInt("bridgeProtocolVersion",1);b.putString("request_id",t.id);b.putString("action",t.action);b.putBoolean("accepted",accepted);b.putString("reason",phase);b.putString("phase",phase);b.putBoolean("terminal",terminal);b.putBoolean("exec_success",false);b.putBoolean("vehicleConfirmed",false);b.putInt("exec_code",accepted?1:-1);return b;}
 private static void finish(Ticket t,OperationState s){Bundle b=ack(t,s.name(),s!=OperationState.NOT_SENT&&s!=OperationState.FAILED,true);b.putBoolean("exec_success",s.isVehicleSuccess());b.putBoolean("vehicleConfirmed",s.isVehicleSuccess());b.putInt("exec_code",s.isVehicleSuccess()?0:s==OperationState.SERVER_ACCEPTED?2:-1);publish(t,b);}
 private static void publish(Ticket t,Bundle value){synchronized(TICKETS){t.result=new Bundle(value);}if(t.receiver!=null)try{t.receiver.send(value.getInt("exec_code"),new Bundle(value));}catch(RuntimeException ignored){}}
 private static Bundle reject(String reason){Bundle b=new Bundle();b.putInt("bridgeProtocolVersion",1);b.putBoolean("accepted",false);b.putBoolean("exec_success",false);b.putInt("exec_code",-1);b.putBoolean("terminal",true);b.putString("reason",reason);return b;}
}
