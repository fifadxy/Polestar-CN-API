package io.github.polestarcn.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Single live-device settings surface. Automatic checks never issue a vehicle command. */
public final class OppoSystemAccessActivity extends Activity {
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private TextView status;private Switch master;
  private final OppoResearchExportJob.Listener exportListener=s->{if(!isFinishing()&&!isDestroyed())show(s.message);};
  @Override public void onCreate(Bundle b){super.onCreate(b);setTitle("系统车辆接入 · 2.7.1");
    LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int p=(int)(18*getResources().getDisplayMetrics().density);page.setPadding(p,p,p,p);
    TextView title=new TextView(this);title.setText("OPPO 系统车辆接入");title.setTextSize(24);page.addView(title);
    TextView intro=new TextView(this);intro.setText("Beta41 将当前真实 Polestar 会话投影为 CarLink 本地已绑定车辆，并让 PsAssistant 进入支持程序校验。先登录、选车并刷新一次，再执行“一键准备 CarLink 测试”。本地已绑定显示不等于 OPPO 云端车企认证。");page.addView(intro);
    master=new Switch(this);master.setText("启用系统车辆接入");master.setChecked(OppoDeviceIntegration.enabled(this));page.addView(master);
    master.setOnCheckedChangeListener((v,on)->{try{OppoSystemAccess.setEnabled(this,on);show("已保存；改变模块启用状态后请重载宿主。");}catch(RuntimeException e){show("设置失败："+e.getClass().getSimpleName());}});
    button(page,"检查并配置接入",()->{try{OppoSystemAccess.setEnabled(this,true);master.setChecked(true);show("已配置设备卡、车辆目录和快捷控制。\n"+OppoCarLinkProtocol.selfCheck(this));}catch(RuntimeException e){show("配置失败："+e.getClass().getSimpleName());}});
    button(page,"手动刷新真实车况",()->{VehicleCoordinator m=VehicleCoordinator.get(this);if(!m.hasSession()||m.selected==null){show("请先在主程序登录并选择车辆");return;}if(m.busy||m.commandPending){show("车辆操作中，请稍后重试");return;}m.refresh(()->show("刷新完成，请核对更新时间。"));});
    button(page,"一键准备 CarLink 已绑定测试",()->prepareCarLinkTest());
    button(page,"真车控制测试",()->startActivity(new Intent().setClassName(getPackageName(),"io.github.polestarcn.oppobridge.VehicleCardDetailActivity")));
    button(page,"CarLink 本机面板（Root）",()->{show("正在请求 Root 启动 CarLink…");worker.execute(()->{try{String result=OppoCarLinkRoot.openPanel(this);runOnUiThread(()->show(result));}catch(Exception e){runOnUiThread(()->show("Root 入口失败："+e.getClass().getSimpleName()));}});});
    button(page,"一键自动诊断 LSPosed / Root / 桥接",this::diagnoseBridge);
    button(page,"导出脱敏诊断 ZIP（≤2 MB）",()->OppoResearchExportJob.start(this,false));
    button(page,"确认后一键重启相关宿主进程（Root）",this::reloadHosts);
    button(page,"诊断与导出",()->new AlertDialog.Builder(this).setItems(new String[]{"复制文字摘要","生成轻量 ZIP（≤2MB）","分享已生成 ZIP"},(d,n)->{
      if(n==0){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PsAssistant 真机诊断",OppoSystemAccess.summary(this)));show("诊断已复制");}
      else if(n==1)OppoResearchExportJob.start(this,true);else shareExport();
    }).show());
    button(page,"高级：官方 SDK 与车型参数",()->new AlertDialog.Builder(this).setItems(new String[]{"官方 SDK 接入状态","车型参数"},(d,n)->startActivity(new Intent(this,n==0?OppoCarLinkSetupActivity.class:OppoVehicleAccessSettingsActivity.class))).show());
    status=new TextView(this);status.setTextIsSelectable(true);page.addView(status);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);show("");
  }
  private void button(LinearLayout p,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setOnClickListener(v->action.run());p.addView(b);}
  private void prepareCarLinkTest(){
    VehicleCoordinator m=VehicleCoordinator.get(this);
    if(!m.hasSession()||m.selected==null){show("准备失败：请先登录、选择车辆并刷新真实车况。");return;}
    try{
      OppoSystemAccess.setEnabled(this,true);master.setChecked(true);
      io.github.polestarcn.oppobridge.BridgeSettings.setUnified(this,true);
      Bundle session=VirtualVehicleSessionManager.setEnabled(this,m,true);
      Bundle bind=OppoOfficialVehicleBindState.set(this,m,true);
      boolean ready=session.getBoolean("active",false)&&bind.getBoolean("enabled",false);
      String note=ready
          ?"CarLink 本地测试态已准备：Polestar=已绑定，PsAssistant=支持程序。请点击“重载系统组件（Root）”后重新进入 CarLink。"
          :"准备未完成：session="+session.getString("lifecycle","")+"，bind="+bind.getString("reason","");
      show(note);
    }catch(RuntimeException e){show("准备失败："+e.getClass().getSimpleName());}
  }
  private void show(String note){if(status!=null&&!isFinishing()&&!isDestroyed())status.setText(note+"\n\n"+OppoSystemAccess.summary(this));}
  private void diagnoseBridge(){
    show("正在只读检查 Root、LSPosed 宿主回报及车辆桥接…");
    worker.execute(()->{
      String result=OppoBridgeAutoDiagnostic.run(getApplicationContext());
      runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())status.setText(result);});
    });
  }
  private void reloadHosts(){
    new AlertDialog.Builder(this).setTitle("确认重启 OPPO 相关进程")
      .setMessage("仅重载设备空间、负一屏、CarLink 及 QuickApp 白名单宿主。"
          +"不重启 SystemUI、桌面、Magisk、LSPosed 或 PsAssistant 本身；"
          +"需要 Root。没有公开启动入口的宿主只发送温和进程重载请求。")
      .setNegativeButton("取消",null)
      .setPositiveButton("重启",(d,w)->{
        show("正在按白名单逐项重载相关进程…");
        worker.execute(()->{
          String result=io.github.polestarcn.oppobridge.BridgeHostRestarter.restart(this);
          runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())status.setText(result);});
        });
      }).show();
  }
  private void shareExport(){OppoResearchExportJob.State s=OppoResearchExportJob.current();if(s.running||s.file==null||!s.file.isFile()){show("请先生成资料包");return;}
    android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".oppoexports",s.file);
    startActivity(Intent.createChooser(OppoWidgetSetupActivity.shareIntent(uri),"分享真机诊断"));}
  @Override protected void onResume(){super.onResume();show("");}
  @Override protected void onStart(){super.onStart();OppoResearchExportJob.observe(exportListener);}
  @Override protected void onStop(){OppoResearchExportJob.remove(exportListener);super.onStop();}
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
