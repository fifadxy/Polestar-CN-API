package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** External VIEW fallback: always user-confirmed; never trusted just because URI came from OEM. */
public final class OppoShortcutConfirmActivity extends Activity {
  private final ExecutorService executor=Executors.newSingleThreadExecutor();
  @Override protected void onCreate(Bundle state){
    super.onCreate(state);
    String request=AdaptiveShelfShortcutInterceptor.requestedAction(getIntent());
    if(request==null){finish();return;}
    new AlertDialog.Builder(this).setTitle("PsAssistant · 快捷车控确认")
        .setMessage("确认执行操作："+request+"？\n将优先核对最新车辆状态；成功与否以车端回读为准。")
        .setPositiveButton("确认",(d,w)->run(request)).setNegativeButton("取消",(d,w)->finish())
        .setOnCancelListener(d->finish()).show();
  }
  private void run(String request){
    executor.execute(()->{
      try {
        Bundle snapshot=BridgeClient.snapshot(this);
        if(!AdaptiveShelfShortcutInterceptor.freshAndReal(snapshot)){
          BridgeClient.refresh(this);
          long deadline=android.os.SystemClock.elapsedRealtime()+12_000L;
          while(android.os.SystemClock.elapsedRealtime()<deadline
              &&!AdaptiveShelfShortcutInterceptor.freshAndReal(snapshot)){
            try{Thread.sleep(1100L);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();return;}
            snapshot=BridgeClient.snapshot(this);
          }
        }
        if(!AdaptiveShelfShortcutInterceptor.freshAndReal(snapshot)){
          feedback("真实车况仍未更新，操作已取消");return;
        }
        Bundle current=BridgeClient.snapshot(this);
        if(!AdaptiveShelfShortcutInterceptor.freshAndReal(current)
            ||!snapshot.getString("vehicleKey","").equals(current.getString("vehicleKey",""))){
          feedback("车辆状态已改变，操作取消");return;
        }
        String action=VehicleShortcutActivity.resolveToggle(request,current);
        if(action==null){feedback("无法确定目标状态，操作已取消");return;}
        Bundle result=BridgeClient.command(this,action);
        if(result.getBoolean("mock",false))feedback("Mock：无实际车辆操作");
        else if(result.getBoolean("accepted",false))feedback("已提交请求，等待车辆回读确认");
        else feedback("操作未受理："+result.getString("reason","unknown"));
      }catch(Throwable failure){feedback("控车桥接不可用，操作已取消");}
    });
  }
  private void feedback(String msg){runOnUiThread(()->{
    Toast.makeText(this,msg,Toast.LENGTH_LONG).show();finish();
  });}
  @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
