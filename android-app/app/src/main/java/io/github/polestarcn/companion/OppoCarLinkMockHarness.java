package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/** Runs the optional TestLab transport and the native-card bridge against one isolated fixture. */
final class OppoCarLinkMockHarness {
  private static final Handler MAIN=new Handler(Looper.getMainLooper());
  private static volatile String report="尚未运行本次测试";
  private static int generation;
  private static volatile boolean running;
  private OppoCarLinkMockHarness(){}
  static boolean available(){return BuildConfig.OPPO_SDK_MOCK;}
  static boolean running(){return running;}
  static String report(){return report;}

  static Bundle status(){
    if(!available()){Bundle out=new Bundle();out.putBoolean("mock",false);return out;}
    Object value=invoke("status",new Class<?>[0],new Object[0]);
    if(value instanceof Bundle)return (Bundle)value;
    Bundle out=new Bundle();out.putBoolean("mock",true);out.putString("error","mock-server-unavailable");return out;
  }

  static void reset(Context context){
    if(Looper.myLooper()!=Looper.getMainLooper()){MAIN.post(()->reset(context.getApplicationContext()));return;}
    if(!available())return;
    generation++;running=false;
    OppoCarLinkRuntime.get(context).stop();
    OppoCarLinkMockFixture.reset();invoke("reset",new Class<?>[0],new Object[0]);
    report="Mock 已重置；旧测试和未完成指令已取消";
    context.getContentResolver().notifyChange(PsAssistantBridgeProvider.URI,null);
  }

  static void runFull(Context context,Runnable done){
    Context app=context.getApplicationContext();
    if(Looper.myLooper()!=Looper.getMainLooper()){MAIN.post(()->runFull(app,done));return;}
    if(running)return;
    if(!available()||!FirstUseConsent.accepted(app)){
      report="FAIL：请使用 Mock 测试包并完成首次使用确认";if(done!=null)done.run();return;
    }
    reset(app);running=true;int run=generation;
    report="Mock 本机测试 #"+System.currentTimeMillis()+"\n（不代表 OPPO 官方授权或宿主卡片已显示）";
    AppPreferences.set(app,"carlink_type_code","POLESTAR_2_TESTLAB");
    AppPreferences.set(app,"carlink_type_name","Polestar 2 TestLab");
    AppPreferences.set(app,"carlink_image_url","https://test.invalid/polestar2-testlab.png");
    OppoDeviceIntegration.setEnabled(app,true);
    VirtualVehicleSessionManager.setEnabled(app,VehicleCoordinator.get(app),true);
    OppoOemCarExperimentState.set(app,true);
    OppoCarLinkRuntime runtime=OppoCarLinkRuntime.get(app);
    runtime.start();
    await(run,"初始化",()->status().getBoolean("initialized",false),()->{
      runtime.authorize();
      await(run,"授权和车辆绑定",()->runtime.authorized()&&status().getBoolean("bound",false),()->{
        runtime.subscribe();
        await(run,"订阅和车况",()->status().getBoolean("subscribed",false)&&status().getInt("statusCount",0)>0,()->{
          contextChanged(app);controlStep(app,run,0,done);
        },done);
      },done);
    },done);
  }

  private static final String[] ACTIONS={"unlock","lock","climate-start","climate-stop","trunk-open","trunk-close","flash","honk"};
  private static void controlStep(Context app,int run,int index,Runnable done){
    if(run!=generation)return;
    if(index==ACTIONS.length){
      report+="\nPASS：8 项模拟命令回执。请回到负一屏点击按钮，随后复制诊断。\n宿主点击、真实车辆执行：仍需单独实测。";
      running=false;contextChanged(app);if(done!=null)done.run();return;
    }
    String action=ACTIONS[index];Bundle result=submit(app,action);String task=result.getString("taskId","");
    if(!result.getBoolean("accepted",false)||task.isEmpty()){
      fail("命令 "+action+"："+result.getString("reason","missing-task"),done);return;
    }
    await(run,action,()->{
      Bundle s=status();return task.equals(s.getString("lastTaskId",""))&&s.getInt("lastControlCode",-1)==1&&fixtureMatches(action);
    },()->controlStep(app,run,index+1,done),done);
  }

  private static boolean fixtureMatches(String action){
    VehicleSnapshot s=OppoCarLinkMockFixture.snapshot();
    switch(action){
      case "lock":return "已锁".equals(s.lock);case "unlock":return "未锁".equals(s.lock);
      case "climate-start":return "开启".equals(s.climate);case "climate-stop":return "关闭".equals(s.climate);
      case "trunk-open":return "已打开".equals(s.tailgate);case "trunk-close":return "已关闭".equals(s.tailgate);
      default:return true; // Momentary commands have a correlated receipt, not persistent telemetry.
    }
  }

  private static void await(int run,String stage,BooleanSupplier ready,Runnable next,Runnable done){
    long deadline=SystemClock.elapsedRealtime()+8000L;
    MAIN.post(new Runnable(){public void run(){
      if(run!=generation)return;
      if(ready.getAsBoolean()){report+="\nPASS："+stage;next.run();return;}
      if(SystemClock.elapsedRealtime()>=deadline){fail(stage+"超时；lastEvent="+status().getString("lastEvent",""),done);return;}
      MAIN.postDelayed(this,50L); // Polls local memory only; never wakes the car.
    }});
  }
  private static void fail(String reason,Runnable done){running=false;report+="\nFAIL："+reason;if(done!=null)done.run();}
  private static void contextChanged(Context app){app.getContentResolver().notifyChange(PsAssistantBridgeProvider.URI,null);}

  static Bundle submit(Context context,String action){
    Bundle out=new Bundle();out.putBoolean("mock",true);out.putBoolean("commandSent",false);
    String[] instruction=instruction(action);
    if(!available()||instruction==null){out.putString("reason","mock-command-unsupported");return out;}
    if(!FirstUseConsent.accepted(context)||!OppoDeviceIntegration.enabled(context)||!OppoCarLinkRuntime.get(context).authorized()){
      out.putString("reason","mock-not-ready-run-initialization");return out;
    }
    Object task=issue(instruction[0],instruction[1]);
    String id="";try{if(task!=null)id=String.valueOf(task.getClass().getMethod("getTaskId").invoke(task));}catch(Exception ignored){}
    out.putBoolean("accepted",!id.isEmpty());out.putString("taskId",id);
    out.putString("reason",id.isEmpty()?"mock-task-rejected":"mock-queued");return out;
  }

  static String[] instruction(String action){
    if(action==null)return null;
    switch(action){
      case "lock":return new String[]{"LOCK-CAR","OPEN"};case "unlock":return new String[]{"CAR-DOOR","OPEN"};
      case "climate-start":return new String[]{"AC-TEMP","OPEN"};case "climate-stop":return new String[]{"AC-TEMP","CLOSE"};
      case "trunk-open":return new String[]{"CAR-BOOT","OPEN"};case "trunk-close":return new String[]{"CAR-BOOT","CLOSE"};
      case "windows-open":return new String[]{"ALL-WINDOW","OPEN"};case "windows-close":return new String[]{"ALL-WINDOW","CLOSE"};
      case "flash":return new String[]{"DOUBLE-FLASH","OPEN"};case "honk":return new String[]{"CAR-HORN","OPEN"};
      case "honk-flash":return new String[]{"CAR-HORN&&DOUBLE-FLASH","OPEN"};default:return null;
    }
  }
  static Object issue(String instruction,String value){
    if(!available())return null;
    String car=status().getString("currentCar","");if(car.isEmpty())return null;
    return invoke("issueControl",new Class<?>[]{String.class,String.class,String.class},new Object[]{car,instruction,value});
  }
  private static Object invoke(String name,Class<?>[] types,Object[] args){
    try{Class<?> type=Class.forName("com.oplus.ocs.carlink.mock.MockCarLinkServer");Method m=type.getMethod(name,types);return m.invoke(null,args);}
    catch(ReflectiveOperationException|LinkageError unavailable){return null;}
  }
}
