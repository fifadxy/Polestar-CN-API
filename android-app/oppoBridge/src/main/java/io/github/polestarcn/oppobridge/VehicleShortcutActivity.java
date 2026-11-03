package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Executes only user-tapped native AssistantScreen vehicle controls through PsAssistant. */
public final class VehicleShortcutActivity extends Activity {
  private final ExecutorService worker=Executors.newSingleThreadExecutor();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String requested=requestedAction(getIntent());
    long issuedAt=getIntent()==null?0L:getIntent().getLongExtra("issuedAt",0L);
    String expectedSession=getIntent()==null?"":getIntent().getStringExtra("sessionId");
    String expectedVehicle=getIntent()==null?"":getIntent().getStringExtra("vehicleId");
    if(requested==null||requested.isEmpty()) { finish();return; }
    if(issuedAt>0L&&!freshTicket(issuedAt,System.currentTimeMillis())) {Toast.makeText(this,"车控请求已过期，请重新点击卡片",Toast.LENGTH_SHORT).show();finish();return;}

    worker.execute(()->{
      try {
        if(expectedSession!=null&&!expectedSession.isEmpty()) {
          Bundle session=BridgeClient.virtualSession(this);
          if(!session.getBoolean("active",false)||!expectedSession.equals(session.getString("sessionId",""))
              ||(expectedVehicle!=null&&!expectedVehicle.isEmpty()&&!expectedVehicle.equals(session.getString("virtualCarId","")))) {
            toast("虚拟车辆会话已变化，请重新点击卡片");finishSafely();return;
          }
        }
        Bundle state=BridgeClient.snapshot(this);
        if(state.getBoolean("available",false)
            && !fresh(state.getLong("capturedAt",0L),System.currentTimeMillis())) {
          try { BridgeClient.refresh(this); } catch(RuntimeException ignored) {}
          toast("车况已过期，已尝试刷新，请稍后重试");
          finishSafely();
          return;
        }
        String action=resolveToggle(requested,state);
        if(action==null) {
          toast("当前车况不足，先刷新车辆状态");
          finishSafely();
          return;
        }
        execute(action); // Verification belongs to the current main app, not this OEM facade.
      } catch(Throwable failure) {
        toast("车控桥接不可用");
        finishSafely();
      }
    });
  }

  private void execute(String action) {
    try {
      Bundle result=BridgeClient.command(this,action);
      if(result.getBoolean("accepted",false))
        toast("请求已受理，等待车辆状态回读");
      else
        toast("未受理："+result.getString("reason","unknown"));
    } catch(Throwable failure) {
      toast("车辆指令提交失败");
    } finally {
      finishSafely();
    }
  }

  static String requestedAction(android.content.Intent intent) {
    if(intent==null)return null;String direct=intent.getStringExtra("action");
    if(direct!=null&&!direct.trim().isEmpty())return direct.trim();
    return intent.getData()==null?null:intent.getData().getQueryParameter("action");
  }

  static boolean freshTicket(long issuedAt,long now) {return issuedAt>0L&&issuedAt<=now+30_000L&&now-issuedAt<=60_000L;}

  static String resolveToggle(String requested,Bundle state) {
    if(state==null||!state.getBoolean("available",false)||state.getBoolean("demo",false)
        ||!state.getBoolean("cardEnabled",true)
        ||!fresh(state.getLong("capturedAt",0L),System.currentTimeMillis()))return null;
    if("flash".equals(requested)||"honk".equals(requested)||"honk-flash".equals(requested)
        ||"lock".equals(requested)||"unlock".equals(requested)||"climate-start".equals(requested)||"climate-stop".equals(requested)
        ||"air-start".equals(requested)||"air-stop".equals(requested)||"trunk-open".equals(requested)||"trunk-close".equals(requested))return requested;
    if("lock-toggle".equals(requested)) {
      String value=upper(state.getString("lock"));
      if(any(value,"已上锁","已锁","LOCKED"))return "unlock";
      if(any(value,"未锁","已解锁","未上锁","UNLOCKED"))return "lock";
      return null;
    }
    if("climate-toggle".equals(requested)) {
      String value=upper(state.getString("climate"));
      if(any(value,"开启","运行中","ON","ACTIVE","RUNNING"))return "climate-stop";
      if(any(value,"关闭","OFF","INACTIVE","STOPPED"))return "climate-start";
      return null;
    }
    if("air-toggle".equals(requested)) {
      String value=upper(state.getString("air"));
      if(any(value,"开启","运行中","ON","ACTIVE","RUNNING"))return "air-stop";
      if(any(value,"关闭","OFF","INACTIVE","STOPPED"))return "air-start";
      return null;
    }
    if("trunk-toggle".equals(requested)) {
      String value=upper(state.getString("tailgate"));
      if(any(value,"已打开","未关严","OPEN"))return "trunk-close";
      if(any(value,"已关闭","关闭","CLOSED"))return "trunk-open";
      return null;
    }
    return null;
  }

  // Never guess a state-dependent command from an expired snapshot.
  // A 30-second clock tolerance avoids false-negative timestamp jitter.
  static boolean fresh(long capturedAt,long now) {
    return capturedAt>0L && capturedAt<=now+30_000L && now-capturedAt<=5*60_000L;
  }

  private static String upper(String value) {
    return value==null?"":value.trim().toUpperCase(Locale.ROOT);
  }

  private static boolean any(String value,String... expected) {
    for(String item:expected)
      if(value.equals(item.toUpperCase(Locale.ROOT)))return true;
    return false;
  }

  private void toast(String text) {
    runOnUiThread(()->Toast.makeText(this,text,Toast.LENGTH_SHORT).show());
  }

  private void finishSafely() {
    runOnUiThread(()->{if(!isFinishing())finish();});
  }

  @Override protected void onDestroy() {
    worker.shutdownNow();
    super.onDestroy();
  }
}
