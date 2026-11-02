package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.app.AlertDialog;
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
    String requested=getIntent()!=null&&getIntent().getData()!=null
        ?getIntent().getData().getQueryParameter("action"):null;
    if(requested==null||requested.isEmpty()) { finish();return; }

    worker.execute(()->{
      try {
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
        if("unlock".equals(action)||"trunk-open".equals(action)||"trunk-close".equals(action)) {
          runOnUiThread(()->confirm(action));
        } else {
          execute(action);
        }
      } catch(Throwable failure) {
        toast("车控桥接不可用");
        finishSafely();
      }
    });
  }

  private void confirm(String action) {
    if(isFinishing()||isDestroyed())return;
    String title="unlock".equals(action)?"确认解锁车辆":"确认操作尾门";
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage("指令会发送到已登录的 Polestar 车辆，并由 PsAssistant 继续执行状态回读。")
        .setNegativeButton("取消",(d,w)->finish())
        .setPositiveButton("确认",(d,w)->worker.execute(()->execute(action)))
        .setOnCancelListener(d->finish())
        .show();
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

  static String resolveToggle(String requested,Bundle state) {
    if(state==null||!state.getBoolean("available",false)||state.getBoolean("demo",false)
        ||!state.getBoolean("cardEnabled",true)
        ||!fresh(state.getLong("capturedAt",0L),System.currentTimeMillis()))return null;
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
