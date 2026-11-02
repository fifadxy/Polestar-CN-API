package io.github.polestarcn.companion;

import android.content.*;
import android.os.*;
import com.oplus.ocs.carlink.CarLink;
import com.oplus.ocs.carlink.ICallback;
import com.oplus.ocs.carlink.control.*;
import com.oplus.ocs.carlink.model.*;
import java.util.*;

/** Official SDK client. All coordinator access is serialized on main; callbacks only read cache. */
final class OppoCarLinkRuntime {
  private static OppoCarLinkRuntime instance;
  static synchronized OppoCarLinkRuntime get(Context c){if(instance==null)instance=new OppoCarLinkRuntime(c.getApplicationContext());return instance;}
  private final Context context;private final Handler main=new Handler(Looper.getMainLooper());
  private final Map<String,Task> tasks=new LinkedHashMap<>();
  private final OppoCarLinkCommandWindow commandWindow=new OppoCarLinkCommandWindow();
  private final OppoCarLinkActivationWindow activationWindow=new OppoCarLinkActivationWindow();
  private volatile boolean registered,requested;private boolean authRequested,authInFlight;private volatile Integer authResult,subscribeResult;private volatile String phase="未初始化";private volatile int errorCode;
  private long publishedAt=-1;private String publishedCar="";
  private OppoCarLinkRuntime(Context c){context=c;}
  private static final class Task {
    final String id,action,car;final long deadline;boolean sent;
    Task(String id,String action,String car){this.id=id;this.action=action;this.car=car;deadline=SystemClock.elapsedRealtime()+60_000;}
  }
  private VehicleCoordinator model(){return VehicleCoordinator.get(context);}
  private boolean ready(){VehicleCoordinator m=model();return OppoDeviceIntegration.enabled(context)&&FirstUseConsent.accepted(context)&&m.hasSession()&&m.selected!=null&&m.snapshot!=null&&!m.snapshot.isDemo;}
  void start(){if(Looper.myLooper()!=Looper.getMainLooper()){main.post(this::start);return;}
    if(!BuildConfig.OPPO_SDK_PACKAGED){phase="公开验证构建未包含SDK实现；请使用FULL包";return;}
    if(!ready()){phase="请启用桥接、登录并刷新真实车辆";return;}
    List<String> missing=OppoCarLinkConfig.load(context).missing();if(!missing.isEmpty()){phase="缺少："+android.text.TextUtils.join("、",missing);return;}
    if(requested)return;authResult=null;subscribeResult=null;authInFlight=false;errorCode=0;requested=true;
    try {
      phase="SDK鉴权／服务绑定中";
      CarLink.init(context,CarLink.FLAG_CAR_CONTROL,new ICallback(){
        public void onCarControlSdkInitComplete(){main.post(()->initialized());}
        @Override public void onServiceConnected(){main.post(()->{phase="服务已连接，等待车控初始化";if(CarLink.isCarControlInitComplete())initialized();});}
        @Override public void onServiceDisconnected(){main.post(()->connectionLost("服务断开"));}
        @Override public void onUnbindService(){onServiceDisconnected();}
        @Override public void onError(int code,String ignored){main.post(()->{errorCode=code;connectionLost("SDK鉴权／绑定失败（code="+code+"）");});}
      },false);
      if(!registered){CarControl.registerCallback(callback);registered=true;}
    }catch(RuntimeException|LinkageError failure){requested=false;phase="SDK不可用："+failure.getClass().getSimpleName();}
  }
  private void initialized(){if(!ready()||!CarLink.isCarControlInitComplete())return;phase="车控SDK已初始化；授权／订阅另行核对";publish(true);requestAuthIfReady();}
  void connectionLost(String reason){requested=false;authResult=null;subscribeResult=null;authInFlight=false;phase=reason;cancelAll(reason);}
  void authorize(){authRequested=true;start();requestAuthIfReady();}
  private void requestAuthIfReady(){if(authRequested&&!authInFlight&&ready()&&CarLink.isCarControlInitComplete()){try{authInFlight=true;CarControl.auth();phase="已请求系统授权";}catch(RuntimeException failure){authInFlight=false;phase="授权调用失败："+failure.getClass().getSimpleName();}}}
  boolean authorized(){return requested&&Integer.valueOf(1).equals(authResult)&&CarLink.isCarControlInitComplete()&&OppoDeviceIntegration.enabled(context);}
  void subscribe(){if(ready()&&CarLink.isCarControlInitComplete()){try{CarControl.subscribe();phase="已请求订阅，等待真实回调";}catch(RuntimeException failure){phase="订阅调用失败："+failure.getClass().getSimpleName();}}}
  void changed(){if(Looper.myLooper()!=Looper.getMainLooper()){main.post(this::changed);return;}if(requested&&CarLink.isCarControlInitComplete())publish(false);}
  private void publish(boolean force){publish(force,force);}
  private void publish(boolean force,boolean includeInfo){if(!ready()||!CarLink.isCarControlInitComplete())return;
    VehicleCoordinator m=model();String car=OppoCarLinkModels.carId(m.selected.vin);
    if(!force&&m.snapshot.timestampMillis==publishedAt&&car.equals(publishedCar))return;
    try{if(includeInfo||!car.equals(publishedCar))CarControl.sendCarInfo(OppoCarLinkModels.info(context,OppoCarLinkConfig.load(context),m.snapshot,m.selected.vin));
    if(OppoCarLinkModels.canReport(m.snapshot))CarControl.sendCarStatus(OppoCarLinkModels.status(m.snapshot,m.selected.vin));else phase="缓存必填车况缺失，未上报零值状态";publishedAt=m.snapshot.timestampMillis;publishedCar=car;}catch(RuntimeException failure){phase="SDK发布失败："+failure.getClass().getSimpleName();requested=false;}
  }
  void stop(){if(Looper.myLooper()!=Looper.getMainLooper()){main.post(this::stop);return;}requested=false;cancelAll("桥接关闭或账号退出");
    if(registered){try{if(CarLink.isCarControlInitComplete())CarControl.accountExpired();CarControl.unregisterCallback(callback);}catch(RuntimeException ignored){}}
    registered=false;requested=false;authRequested=false;authInFlight=false;authResult=null;subscribeResult=null;publishedAt=-1;publishedCar="";phase="已停止输出";
  }
  private void cancelAll(String reason){for(String id:new ArrayList<>(tasks.keySet()))finish(id,OperationState.UNCONFIRMED,reason);}
  private final ICarControlCallback callback=new ICarControlCallback(){
    @Override public void onComplete(){main.post(()->initialized());}
    @Override public void inquireCarInfo(){main.post(()->publish(true));}
    @Override public void inquireCarStatus(){main.post(()->publish(true,false));}
    @Override public void onShowCard(){main.post(()->publish(true,false));}
    @Override public void onAuthResult(int code){main.post(()->{if(!requested)return;authResult=code;authInFlight=false;authRequested=false;phase="授权回调 code="+code+(code==1?"（成功）":"（未成功）");});}
    @Override public void onSubscribeResult(int code){main.post(()->{if(!requested)return;subscribeResult=code;phase="订阅回调 code="+code+(code==1?"（成功）":"（未成功）");});}
    @Override public void onUnbindResult(int code){main.post(()->{authResult=null;subscribeResult=null;phase="解绑回调 code="+code;cancelAll("系统解绑");});}
    @Override public void onVerify(String ignored){main.post(()->phase="收到SDK验证回调；车辆操作仍按本应用验证策略执行");}
    @Override public void onSwitchCar(String car){main.post(()->{if(ready()&&OppoCarLinkModels.carId(model().selected.vin).equals(car))publish(true);else phase="请求车辆不匹配，未切换";});}
    @Override public ActiveTask activateCar(List<String> cars){
      List<String> ids=cars!=null&&cars.size()<=8?new ArrayList<>(cars):Collections.emptyList();
      main.post(()->activate(ids));return new ActiveTask(60);
    }
    @Override public ControlTask onControl(ControlInstruction in){
      String id=UUID.randomUUID().toString();String car=in==null?"":in.getCarId();
      String instruction=in==null?"":in.getInstruction();String value=in==null?"":in.getInstructionValue();
      // Never infer a requested command from the status field: command values must be explicit.
      main.post(()->receive(id,car,instruction,value));return new ControlTask(id,60);
    }
  };
  private void activate(List<String> cars){
    if(!ready()||!authorized())return;
    VehicleCoordinator m=model();String car=OppoCarLinkModels.carId(m.selected.vin);
    if(!cars.contains(car))return;
    if(EffectStateController.fresh(m.snapshot,System.currentTimeMillis())){publish(true,false);return;}
    boolean optedIn=AppPreferences.visible(context,"carlink_activation_reads",false);
    if(!activationWindow.admit(cars,car,optedIn,VehiclePowerPolicy.automaticReads(context),m.busy||m.commandPending,SystemClock.elapsedRealtime())){
      phase="连接请求未新增读取：请检查按需更新、长停省电或稍后手动刷新";return;
    }
    phase="CarLink按需更新一次车况；未安排轮询";
    m.refresh(()->{if(ready()&&car.equals(OppoCarLinkModels.carId(model().selected.vin)))publish(true,false);});
  }
  private void receive(String id,String car,String instruction,String value){
    String canonical=OppoCarLinkCommandMap.canonical(instruction);
    String action=actionForInstruction(canonical,value);
    Task task=new Task(id,action,car);tasks.put(id,task);
    if(!ready()||!authorized()||action==null||!OppoCarLinkModels.carId(model().selected.vin).equals(car)||!model().canCommand(action)){
      finish(id,OperationState.FAILED,"车辆、技能或会话不匹配，未发送");return;
    }
    for(Task other:tasks.values())if(other!=task&&CommandSlots.group(action).equals(CommandSlots.group(other.action))){finish(id,OperationState.FAILED,"同类指令正在处理");return;}
    main.postDelayed(()->finish(id,OperationState.UNCONFIRMED,"请求超时，未自动重试"),60_000);
    if("direct".equals(ControlAuthorization.mode(context,action))){dispatch(id);return;}
    phase="等待用户验证";
    try{context.startActivity(new Intent(context,OppoCarLinkAuthorizeActivity.class).putExtra("task",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    catch(RuntimeException denied){finish(id,OperationState.FAILED,"系统阻止验证页面，请在主程序操作");}
  }
  static String actionForInstruction(String instruction,String value){
    String key=OppoCarLinkCommandMap.canonical(instruction);
    if(!OppoCarLinkModels.SUPPORTED.contains(key)||!("OPEN".equals(value)||"CLOSE".equals(value)))return null;
    if((key.equals("CAR-HORN")||key.equals("DOUBLE-FLASH")||key.equals("CAR-HORN&&DOUBLE-FLASH"))&&!"OPEN".equals(value))return null;
    return OppoCarLinkCommandMap.toPsAssistantAction(key,value);
  }
  String action(String id){Task t=tasks.get(id);return t==null?null:t.action;}
  void dispatch(String id){Task t=tasks.get(id);if(t==null)return;
    if(t.sent||SystemClock.elapsedRealtime()>=t.deadline||!ready()||!authorized()||!t.car.equals(OppoCarLinkModels.carId(model().selected.vin))||!model().canCommand(t.action)){
      finish(id,OperationState.FAILED,"请求已失效或车辆已变化");return;}
    if(!commandWindow.admit(t.car+"|"+t.action,SystemClock.elapsedRealtime())){finish(id,OperationState.FAILED,"重复指令冷却中，未发送");return;}
    t.sent=true;message(id,ControlMessage.CONTROL_EXECUTING,"执行中");
    model().command(t.action,result->finish(id,result,result==OperationState.VEHICLE_CONFIRMED?"车辆状态已确认":"已发送但车辆结果未确认"));
  }
  void cancel(String id){finish(id,OperationState.UNCONFIRMED,"用户取消，未继续发送");}
  static int messageCode(OperationState result){return result==OperationState.VEHICLE_CONFIRMED?ControlMessage.CONTROL_SUCCESS:ControlMessage.CONTROL_FAILED;}
  private void finish(String id,OperationState result,String reason){if(tasks.remove(id)==null)return;message(id,messageCode(result),reason);changed();}
  private void message(String id,int code,String reason){try{if(CarLink.isCarControlInitComplete())CarControl.sendControlMessage(new ControlMessage(code,reason,new ControlMessage.ControlData(id,code==ControlMessage.CONTROL_SUCCESS||code==ControlMessage.CONTROL_EXECUTING?0:500,reason)));}catch(RuntimeException ignored){}}
  String describe(){return "CarLink SDK："+phase+"\nsdkPackaged="+BuildConfig.OPPO_SDK_PACKAGED+"\nSDK错误码="+errorCode+" · 授权回调="+authResult+" · 订阅回调="+subscribeResult+" · callbackRegistered="+registered+" · initialized="+CarLink.isCarControlInitComplete()+"\nSDK只初始化远程车控FLAG=2；未启用蓝牙钥匙。读卡只返回缓存；按需更新默认关闭，开启后仍服从长停省电，5分钟最多一次，无轮询。\n缺项预检："+android.text.TextUtils.join("、",OppoCarLinkConfig.load(context).missing());}
}
