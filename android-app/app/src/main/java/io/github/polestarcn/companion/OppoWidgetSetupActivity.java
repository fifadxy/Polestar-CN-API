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
    autoShare=state!=null&&state.getBoolean("autoShare");
    LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
    int p=(int)(16*getResources().getDisplayMetrics().density);column.setPadding(p,p,p,p);
    TextView help=new TextView(this);help.setTextSize(16);help.setText("OPPO 组件添加\n\n设备空间车辆卡片、负一屏原生“我的车辆”、Android Widget 是不同入口。先确认标准列表能找到 PsAssistant。桌面添加请求不会指定负一屏位置。\n\n1. 打开标准组件列表，找到 PsAssistant，选择所需尺寸。\n2. 若负一屏提供第三方组件入口，在负一屏＋／编辑中选择；入口是否存在由当前固件决定。\n3. 仅有“我的车辆”品牌名单时，不代表支持第三方 Widget；该私有链路尚未验证。\n\n标准入口采用已公开验证的 ColorOS 启动参数，固件更新可能改变行为。");column.addView(help);
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
    for(String pkg:new String[]{"com.android.launcher","com.coloros.assistantscreen","com.heytap.mydevices"}){
      try{PackageInfo i=getPackageManager().getPackageInfo(pkg,0);out.append(pkg).append(": ").append(i.versionName).append('\n');}
      catch(Exception missing){out.append(pkg).append(": 未检测到／当前不可见\n");}
    }
    AppWidgetManager manager=AppWidgetManager.getInstance(this);int providers=0,instances=0;
    for(AppWidgetProviderInfo i:manager.getInstalledProviders())if(getPackageName().equals(i.provider.getPackageName())){
      providers++;int count=manager.getAppWidgetIds(i.provider).length;instances+=count;
      out.append(i.provider.getClassName()).append(" · 已绑定实例 ").append(count).append('\n');
    }
    out.append("本应用已注册组件: ").append(providers).append("；已绑定实例: ").append(instances).append('\n');
    out.append("实例数量不证明位于负一屏。原生我的车辆适配未验证。此页面不刷新车辆。\n");return out.toString();
  }
}
