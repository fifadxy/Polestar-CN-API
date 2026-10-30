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
  private boolean exporting;
  private static final int EXPORT_HOSTS=7305;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
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
    button(column,android.view.View.generateViewId(),"导出负一屏适配文件（系统 APK）",()->{
      if(exporting)return;
      try{startActivityForResult(hostExportIntent(),EXPORT_HOSTS);}
      catch(RuntimeException unavailable){report.setText("系统保存文件入口不可用："+unavailable.getClass().getSimpleName());}
    });
    TextView exportHelp=new TextView(this);exportHelp.setText("导出负一屏、设备空间的程序安装文件与版本信息，用于分析卡片模板；不包含账号、Token、车辆缓存或操作日志。请在系统文件选择器中选择保存位置。");column.addView(exportHelp);
    report=new TextView(this);report.setTextIsSelectable(true);column.addView(report);
    ScrollView scroll=new ScrollView(this);scroll.addView(column);setContentView(scroll);report.setText(diagnostics());
  }
  static Intent hostExportIntent() {
    return new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
        .setType("application/zip").putExtra(Intent.EXTRA_TITLE,"PsAssistant-OPPO-hosts.zip");
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=EXPORT_HOSTS||resultCode!=RESULT_OK||data==null||data.getData()==null||exporting)return;
    android.net.Uri destination=data.getData();exporting=true;report.setText("正在导出系统 APK，请稍候…");
    new Thread(()->{
      String message;
      try{OppoHostApkExport.write(getApplicationContext(),destination);message="系统适配包已保存。请将 PsAssistant-OPPO-hosts.zip 发回用于分析。";}
      catch(Exception failure){message="导出未完成："+failure.getClass().getSimpleName()+"。请删除不完整 ZIP；若系统禁止读取 APK，可用 APK 提取工具导出负一屏与设备空间。";}
      final String value=message;
      runOnUiThread(()->{exporting=false;if(!isFinishing()&&!isDestroyed())report.setText(value);});
    },"oppo-host-export").start();
  }
  static Intent standardPickerIntent() {
    return new Intent().setComponent(new ComponentName("com.android.launcher","com.android.launcher.Launcher")).putExtra("state","ToggleBar_widget");
  }
  private void button(LinearLayout parent,int id,String title,Runnable action){Button b=new Button(this);b.setId(id);b.setText(title);b.setOnClickListener(v->action.run());parent.addView(b);}
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
