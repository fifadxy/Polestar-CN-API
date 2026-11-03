package io.github.polestarcn.companion;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Local widget discovery/setup only; no vehicle network requests or automatic polling. */
public final class OppoWidgetSetupActivity extends Activity {
  private TextView report;
  private Button generateButton,shareButton,saveButton;
  private boolean autoShare;
  private final OppoResearchExportJob.Listener exportListener=this::showExportState;
  private static final int EXPORT_HOSTS=7305;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    autoShare=state!=null?state.getBoolean("autoShare"):getIntent().getBooleanExtra("autoShare",false);
    LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
    int p=(int)(16*getResources().getDisplayMetrics().density);column.setPadding(p,p,p,p);
    TextView help=new TextView(this);help.setTextSize(16);help.setText("OPPO 互联\n\n设备卡、本地车辆入口与官方 CarLink 授权是不同层。此页统一管理接入开关；卡片可见或显示 Connected 不等于官方授权、手表同步或车辆指令已完成。\n\n设备空间需在 LSPosed 中启用当前使用的 PsAssistant 模块，作用域勾选‘我的设备’；本地车辆目录另需负一屏、CarLink。请只启用内置或独立模块中的一种，更新后重启手机。关闭总开关后停止输出和操作。");column.addView(help);
    button(column,android.view.View.generateViewId(),"CarLink接入与Root定向诊断",()->startActivity(new Intent(this,OppoCarLinkSetupActivity.class)));
    android.widget.Switch integration=new android.widget.Switch(this);integration.setText("启用 OPPO 车辆桥接");integration.setChecked(OppoDeviceIntegration.enabled(this));
    integration.setOnCheckedChangeListener((v,on)->{try{OppoDeviceIntegration.setEnabled(this,on);report.setText(diagnostics());}catch(RuntimeException failure){integration.setChecked(OppoDeviceIntegration.enabled(this));report.setText("切换失败："+failure.getClass().getSimpleName());}});column.addView(integration);
    android.widget.Switch oemCar=new android.widget.Switch(this);oemCar.setText("启用 MyDevices 17.4.15 · 4×2 车辆直控实验");
    oemCar.setChecked(OppoOemCarExperimentState.status(this).getBoolean("enabled",false));
    oemCar.setOnCheckedChangeListener((v,on)->{
      try {
        OppoOemCarExperimentState.set(this,on);
        report.setText(diagnostics()+"\n4×2车辆直控实验已"+(on?"启用":"关闭")+"；请Root重载或重启MyDevices后重新打开卡片。");
      } catch(RuntimeException failure) {
        oemCar.setEnabled(false);
        report.setText("4×2实验切换失败："+failure.getClass().getSimpleName());
      }
    });column.addView(oemCar);
    button(column,android.view.View.generateViewId(),"一键启用 beta31 TestLab 车辆闭环",()->{
      try {OppoDeviceIntegration.setEnabled(this,true);OppoOemCarExperimentState.set(this,true);
        Bundle session=VirtualVehicleSessionManager.setEnabled(this,VehicleCoordinator.get(this),true);
        integration.setChecked(true);oemCar.setChecked(true);
        if(session.getBoolean("accepted",false)){OppoDeviceIntegration.restoreCard(this);report.setText(diagnostics()+"\nTestLab Session 已启用；请重载 MyDevices/负一屏 后测试四个 CAR 控制。");}
        else report.setText(diagnostics()+"\nSession 未就绪：请先完成中国区登录并刷新真实车辆。");
      }catch(RuntimeException failure){report.setText("TestLab Session 启用失败："+failure.getClass().getSimpleName());}
    });
    android.widget.Switch local=new android.widget.Switch(this);local.setText("向系统提供 Polestar 本地车辆入口");
    local.setChecked(OppoOfficialVehicleBindState.status(this,VehicleCoordinator.get(this)).getBoolean("requested",false));
    local.setOnCheckedChangeListener((v,on)->{setSystemVehicleEntry(on);local.setChecked(OppoOfficialVehicleBindState.status(this,VehicleCoordinator.get(this)).getBoolean("requested",false));});column.addView(local);
    button(column,android.view.View.generateViewId(),"打开 Polestar 本地车辆面板",()->startActivity(new Intent().setClassName(this,"io.github.polestarcn.oppobridge.PolestarOfficialBindActivity")));
    button(column,android.view.View.generateViewId(),"一键诊断 + 模拟测试 + 导出",()->startActivity(new Intent().setClassName(this,"io.github.polestarcn.oppobridge.BridgeDiagnosticsActivity").putExtra("autoOneClick",true)));
    button(column,android.view.View.generateViewId(),"高级诊断 · 模块／卡片／官方接口",()->startActivity(new Intent().setClassName(this,"io.github.polestarcn.oppobridge.BridgeDiagnosticsActivity")));
    button(column,android.view.View.generateViewId(),"修复 Provider 注册并恢复设备卡",()->{try{OppoDeviceIntegration.restoreCard(this);report.setText(diagnostics()+(OppoDeviceIntegration.enabled(this)?"\nProvider注册及本卡可见性已恢复；没有刷新车辆，请重新进入设备空间。":"\n桥接总开关关闭，Provider保持停用；开启后再恢复。"));}catch(RuntimeException e){report.setText("Provider修复失败："+e.getClass().getSimpleName());}});
    button(column,android.R.id.button1,"打开 OPPO 标准组件列表",()->{
      try {startActivity(standardPickerIntent());}
      catch(RuntimeException failure){report.setText(diagnostics()+"\n标准入口不可用: "+failure.getClass().getSimpleName()+"\n请尝试下方系统桌面添加请求。");}
    });
    button(column,android.R.id.button2,"请求添加桌面车控组件",()->{
      AppWidgetManager manager=AppWidgetManager.getInstance(this);
      try {
        if(!manager.isRequestPinAppWidgetSupported()){report.setText(diagnostics()+"\n当前桌面不支持系统固定组件请求。");return;}
        boolean requested=manager.requestPinAppWidget(new ComponentName(this,VehicleWidgetProvider.class),null,null);
        report.setText(diagnostics()+"\n桌面请求: "+(requested?"已提交，等待系统和用户确认；不代表已添加到负一屏":"未被宿主接受"));
      } catch(RuntimeException failure){report.setText(diagnostics()+"\n请求失败: "+failure.getClass().getSimpleName());}
    });
    button(column,android.R.id.button3,"刷新并复制添加诊断",()->{
      String value=diagnostics();report.setText(value);
      ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("OPPO widget setup",value));
    });
    generateButton=button(column,android.view.View.generateViewId(),"一键导出轻量资料（≤2 MB）",()->{
      autoShare=true;
      if(!OppoResearchExportJob.start(this))autoShare=false;
    });
    shareButton=button(column,android.view.View.generateViewId(),"分享已生成的资料包",this::shareExport);
    saveButton=button(column,android.view.View.generateViewId(),"另存资料包 ZIP",()->{
      try{startActivityForResult(hostExportIntent(),EXPORT_HOSTS);}
      catch(RuntimeException unavailable){report.setText("系统保存入口不可用："+unavailable.getClass().getSimpleName());}
    });
    TextView exportHelp=new TextView(this);exportHelp.setText("默认导出小于 2 MB 的轻量资料：版本、组件、模块历史与少量静态配置，不打包完整 APK。增加健康、小布、车联版本定位。手机不能读取手表内部资料；不收集账号、Token、车辆缓存或系统日志。");column.addView(exportHelp);
    report=new TextView(this);report.setTextIsSelectable(true);column.addView(report);
    ScrollView scroll=new ScrollView(this);scroll.addView(column);setContentView(scroll);report.setText(diagnostics());
  }
  static Intent hostExportIntent() {
    return new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
        .setType("application/zip").putExtra(Intent.EXTRA_TITLE,"PsAssistant-OPPO-light.zip");
  }
  @Override protected void onStart() {super.onStart();OppoResearchExportJob.observe(exportListener);}
  @Override protected void onStop() {OppoResearchExportJob.remove(exportListener);super.onStop();}
  @Override protected void onSaveInstanceState(Bundle out) {out.putBoolean("autoShare",autoShare);super.onSaveInstanceState(out);}
  private void showExportState(OppoResearchExportJob.State state) {
    if(isFinishing()||isDestroyed())return;
    generateButton.setEnabled(!state.running);
    boolean ready=!state.running&&state.file!=null&&state.file.isFile();
    shareButton.setEnabled(ready);saveButton.setEnabled(ready);report.setText(diagnostics()+"\n"+state.message);
    if(!state.running&&autoShare){autoShare=false;if(ready)shareExport();}
  }
  static Intent shareIntent(android.net.Uri uri) {
    Intent intent=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setClipData(ClipData.newRawUri("OPPO research archive",uri));return intent;
  }
  private void shareExport() {
    OppoResearchExportJob.State state=OppoResearchExportJob.current();
    if(state.running||state.file==null||!state.file.isFile())return;
    try {
      android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".oppoexports",state.file);
      startActivity(Intent.createChooser(shareIntent(uri),"分享 OPPO 适配资料包"));
    } catch(RuntimeException failure) {report.setText("分享入口不可用，请点击另存资料包 ZIP。");}
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=EXPORT_HOSTS||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
    OppoResearchExportJob.State state=OppoResearchExportJob.current();
    if(state.running||state.file==null)return;
    java.io.File file=state.file;android.net.Uri destination=data.getData();
    android.content.Context app=getApplicationContext();report.setText("正在保存资料包…");
    new Thread(()->{
      String message;
      try(java.io.InputStream input=new java.io.FileInputStream(file);
          java.io.OutputStream output=app.getContentResolver().openOutputStream(destination,"wt")) {
        if(output==null)throw new java.io.IOException("No output stream");
        byte[] bytes=new byte[32768];int count;while((count=input.read(bytes))!=-1)output.write(bytes,0,count);
        message="资料包已保存，可作为附件发回分析。";
      } catch(Exception failure) {message="保存未完成："+failure.getClass().getSimpleName()+"。请删除不完整文件，可重试或直接分享。";}
      final String value=message;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())report.setText(value);});
    },"oppo-research-save").start();
  }
  static Intent standardPickerIntent() {
    return new Intent().setComponent(new ComponentName("com.android.launcher","com.android.launcher.Launcher")).putExtra("state","ToggleBar_widget");
  }
  private Button button(LinearLayout parent,int id,String title,Runnable action){Button b=new Button(this);b.setId(id);b.setText(title);b.setOnClickListener(v->action.run());parent.addView(b);return b;}
  private String diagnostics(){
    StringBuilder out=new StringBuilder("PsAssistant "+BuildConfig.VERSION_NAME+" / "+BuildConfig.VERSION_CODE+"\n");
    for(String pkg:new String[]{"com.heytap.opluscarlink","com.finshell.wallet","com.heytap.health.wallet","com.oplus.cardigitalkey","com.oplus.smartengine","com.oplus.cardservice","com.oplus.assistantscreen","com.android.launcher","com.coloros.assistantscreen","com.heytap.mydevices"}){
      try{PackageInfo i=getPackageManager().getPackageInfo(pkg,0);out.append(pkg).append(": ").append(i.versionName).append('\n');}
      catch(Exception missing){out.append(pkg).append(": 未检测到／当前不可见\n");}
    }
    AppWidgetManager manager=AppWidgetManager.getInstance(this);int providers=0,instances=0;
    for(AppWidgetProviderInfo i:manager.getInstalledProviders())if(getPackageName().equals(i.provider.getPackageName())){
      providers++;int count=manager.getAppWidgetIds(i.provider).length;instances+=count;
      out.append(i.provider.getClassName()).append(" · 已绑定实例 ").append(count).append('\n');
    }
    out.append("本应用已注册组件: ").append(providers).append("；已绑定实例: ").append(instances).append('\n');
    out.append("设备卡Provider自检（仅缓存）：\n").append(OppoProviderDiagnostics.inspect(this).toString()).append('\n');
    out.append(OppoOfficialCarLink.describe(this)).append('\n');
    Bundle system=OppoSystemVehicleState.status(this,VehicleCoordinator.get(this));
    out.append("Polestar 系统车辆入口: ").append(system.getBoolean("providerEnabled",false)?"已启用":"未启用")
        .append(" · 登录=").append(system.getBoolean("signedIn",false))
        .append(" · 车辆=").append(system.getBoolean("vehicleAvailable",false)).append('\n');
    out.append("Provider 目录已出现 Polestar: ").append(system.getBoolean("providerCatalogContainsPolestar",false))
        .append(" · AssistantScreen Hook=").append(system.getBoolean("assistantHookSeen",false))
        .append(" (").append(system.getString("assistantVersion","")).append(")\n");
    out.append("CarLink: ").append(system.getString("carLinkVersion",""))
        .append(" · service=").append(system.getBoolean("carLinkServiceAvailable",false))
        .append(" · Hook=").append(system.getBoolean("carLinkHostHookSeen",false)).append('\n');
    Bundle virtualSession=VirtualVehicleSessionManager.status(this,VehicleCoordinator.get(this));
    out.append("TestLab Session: ").append(virtualSession.getBoolean("active",false)?"ACTIVE":"未就绪")
        .append(" · ").append(virtualSession.getString("lifecycle",""))
        .append(" · ").append(virtualSession.getString("virtualCarId",""))
        .append(" · ").append(virtualSession.getString("testMac","")).append('\n');
    Bundle quick=OppoQuickAppRuntimeDiagnostics.status(this);
    out.append("MyDevices 4×2实测链路: RPK放行=").append(quick.getBoolean("rpkPatchApplied",false))
        .append(" · Converter=").append(quick.getBoolean("converterWhitelistApplied",false))
        .append(" · shortcuts=").append(quick.getInt("selectedShortcutCount",0))
        .append(" · click=").append(quick.getBoolean("oemClickSeen",false))
        .append(" · carEvent=").append(quick.getBoolean("carEventReceived",false))
        .append(" · typeGate=").append(quick.getBoolean("carTypeGatePassed",false))
        .append(" · dispatcher=").append(quick.getBoolean("carDispatchIntercepted",false))
        .append(" · controller=").append(quick.getBoolean("controllerMatched",false))
        .append(" · intent=").append(quick.getBoolean("commandIntentCreated",false)).append("/").append(quick.getBoolean("pendingIntentSent",false))
        .append(" · accepted=").append(quick.getBoolean("lastCommandAccepted",false))
        .append(" · sent=").append(quick.getBoolean("lastCommandSent",false)).append('\n');
    out.append("BLE 车钥匙框架: ").append(system.getBoolean("oppoBluetoothKeySupported",false)?"入口可见":"未发现")
        .append(" · PAK 配对状态未知，不会伪造为已绑定。\n");
    Bundle shelfCard=OppoShelfCardCapability.status(this);
    Bundle shelfPreview=OppoShelfCardSnapshotAdapter.preview(VehicleCoordinator.get(this).snapshot);
    out.append("负一屏插件卡: host=").append(shelfCard.getBoolean("cardHostVisible",false))
        .append(" · CardWidget=").append(shelfCard.getBoolean("cardWidgetRuntimeSupported",false))
        .append(" SDK=").append(shelfCard.getString("cardWidgetSdkVersionCode",""))
        .append(" · UIEngine=").append(shelfCard.getString("uiEngineVersion",""))
        .append(" · CardService=").append(shelfCard.getBoolean("cardServiceInstalled",false))
        .append(" · Shelf metadata=").append(shelfCard.getBoolean("shelfMetadataReady",false))
        .append(" · Pantanal Hook=").append(shelfCard.getBoolean("pantanalHookSeen",false))
        .append(" (").append(shelfCard.getString("pantanalVersion","")).append(")\n");
    if(shelfPreview.getBoolean("available",false)) {
      out.append("卡片数据预览: ");
      if(shelfPreview.getBoolean("socKnown",false))out.append("SOC=").append(shelfPreview.getInt("socPercent",0)).append("% ");
      if(shelfPreview.getBoolean("rangeKnown",false))out.append("续航=").append(shelfPreview.getInt("rangeKm",0)).append("km ");
      out.append("charging=").append(shelfPreview.getBoolean("charging",false)).append('\n');
    } else out.append("卡片数据预览: 暂无车辆快照\n");
    out.append("Widget 实例数量不证明位于负一屏。此页面不刷新车辆。\n");return out.toString();
  }
  private void setSystemVehicleEntry(boolean enabled) {
    try {
      Bundle state=OppoOfficialVehicleBindState.set(this,VehicleCoordinator.get(this),enabled);
      if(!state.getBoolean("accepted",false)) {
        report.setText(diagnostics()+"\n未启用：需要先登录并刷新真实车辆。");
        return;
      }
      report.setText(diagnostics()+"\n"+(enabled?"已启用。请重启负一屏/CarLink 后重新进入“我的车辆”。":"已关闭系统车辆入口。"));
    } catch(RuntimeException failure) {
      report.setText(diagnostics()+"\n设置失败: "+failure.getClass().getSimpleName());
    }
  }
}
