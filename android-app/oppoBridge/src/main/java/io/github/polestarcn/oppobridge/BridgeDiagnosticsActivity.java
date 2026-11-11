package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** LSPosed settings entry with user-triggered, read-only diagnostics and safe host restart. */
public final class BridgeDiagnosticsActivity extends Activity {
  private TextView note;
  private volatile String lastReport="";
  @Override protected void onCreate(Bundle state){
    super.onCreate(state);
    Intent settings=new Intent().setClassName(BridgeIdentity.INTEGRATED_PACKAGE,
        "io.github.polestarcn.companion.OppoSystemAccessActivity");
    // Same-UID integrated settings are private; external module uses own panel.
    if(BridgeIdentity.INTEGRATED_PACKAGE.equals(getPackageName())){
      startActivity(settings);finish();return;
    }
    settings.setClassName(BridgeIdentity.INTEGRATED_PACKAGE,"io.github.polestarcn.companion.MainActivity");
    LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
    int pad=(int)(16*getResources().getDisplayMetrics().density);
    page.setPadding(pad,pad,pad,pad);
    note=new TextView(this);note.setTextIsSelectable(true);
    note.setText("OPPO Bridge "+BridgeBuild.VERSION_NAME+"\n"
        +"此页面仅诊断模块、Root 与本地桥接。不发送车控指令。\n\n"+BridgeSettings.describe(this));
    page.addView(note);
    add(page,"一键自动诊断 LSPosed / Root / 桥接",()->diagnose());
    add(page,"复制脱敏诊断报告",()->{
      ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(
          ClipData.newPlainText("PsAssistant LSPosed diagnosis",lastReport));
    });
    add(page,"分享脱敏诊断报告",()->{
      Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");
      share.putExtra(Intent.EXTRA_TEXT,lastReport.isEmpty()?"请先运行诊断":lastReport);
      startActivity(Intent.createChooser(share,"导出模块诊断"));
    });
    add(page,"一键重启相关 OPPO 宿主进程（Root）",()->new AlertDialog.Builder(this)
        .setTitle("确认重载 OPPO 宿主")
        .setMessage("仅对已安装且位于白名单的 MyDevices / AssistantScreen / CarLink / QuickApp 请求重载；不重启 SystemUI、桌面、Magisk 或 LSPosed。期间卡片可能短暂消失。")
        .setNegativeButton("取消",null)
        .setPositiveButton("执行", (d,w)->runBackground(()->BridgeHostRestarter.restart(this))).show());
    add(page,"打开 PsAssistant 主程序",()->{
      try{startActivity(settings);}catch(RuntimeException missing){note.setText("请安装可独立登录的 PsAssistant 主程序");}
    });
    ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
  }
  private void add(LinearLayout group,String label,Runnable run){
    Button button=new Button(this);button.setText(label);button.setOnClickListener(v->run.run());group.addView(button);
  }
  private void diagnose(){runBackground(()->{
    Bundle cap=BridgeClient.capabilities(this);
    Bundle session=BridgeClient.virtualSession(this);
    String root=BridgeHostRestarter.rootStatus();
    StringBuilder out=new StringBuilder("PsAssistant · LSPosed 模块诊断 v2.7.1\n");
    out.append("Root=").append(root).append('\n').append(BridgeSettings.describe(this));
    out.append("Main app bridge reachable=").append(cap.getBoolean("available",false)).append('\n');
    out.append("Protocol=").append(cap.getInt("bridgeProtocolVersion",0)).append('\n');
    out.append("Local session visible=").append(session.getBoolean("present",false)).append('\n');
    out.append("Command ready=").append(session.getBoolean("commandReady",false)).append('\n');
    for(String method:new String[]{"host-status","assistant-host-status","carlink-host-status","quickapp-runtime-status"}){
      Bundle b=BridgeClient.call(this,method,null);
      out.append(method).append(": observed=").append(b.getLong("seenAt",0)>0)
          .append(", ready=").append(b.getBoolean("ready",false)).append('\n');
    }
    out.append("注意：历史 Hook 回报、模块启用、本地绑定、真实车辆回读和 OPPO 官方授权均为不同状态。\n");
    return out.toString();
  });}
  private interface Work{String execute();}
  private void runBackground(Work work){
    note.setText("正在检查（只读或已确认的重载操作）…");
    new Thread(()->{
      String result;try{result=work.execute();}catch(RuntimeException e){result="诊断失败: "+e.getClass().getSimpleName();}
      final String value=result;
      runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){lastReport=value;note.setText(value);}});
    },"psa-bridge-diagnostics").start();
  }
}
