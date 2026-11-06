package io.github.polestarcn.companion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Single OPPO compatibility hub: LSPosed, CarLink, Watch Indicator and Fluid Cloud. */
public final class OppoSystemCompatActivity extends Activity {
  private static final int ORANGE=0xffff6b00;
  private LinearLayout root;
  private boolean dark;
  private int foreground,secondary,surface;
  private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
  private String tr(String zh,String en){return AppPreferences.tr(this,zh,en);}
  @Override public void onCreate(Bundle state){super.onCreate(state);draw();handleIntent(getIntent());}
  @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
  private void handleIntent(Intent intent){
    if(intent==null)return;String action=intent.getStringExtra(OppoFluidCloud.TEST_EXTRA);
    if(action!=null){intent.removeExtra(OppoFluidCloud.TEST_EXTRA);
      new AlertDialog.Builder(this).setTitle(tr("流体云二级按钮模拟","Fluid card action preview"))
        .setMessage(tr("测试动作：","Simulated action: ")+action+"\n"+
            tr("未发送任何车辆远控指令。这只验证按钮能够打开测试页面。",
                "No vehicle command was sent. This checks the expanded-card action only."))
        .setPositiveButton(tr("知道了","OK"),null).show();}
  }
  private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);
    if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
  private void gap(int h){View v=new View(this);root.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
  private void header(String title,String subtitle){
    gap(18);TextView t=text(title,18,ORANGE,true);root.addView(t);
    if(subtitle!=null){TextView d=text(subtitle,12,secondary,false);d.setPadding(0,dp(5),0,dp(8));root.addView(d);}
  }
  private void button(String label,Runnable click){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);
    b.setTextColor(foreground);b.setBackground(bg(surface,10));b.setOnClickListener(v->click.run());
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(45));p.topMargin=dp(5);root.addView(b,p);
  }
  private void toggle(String title,String subtitle,String key,boolean def,Runnable update){
    LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
    wrap.setPadding(dp(10),dp(5),dp(10),dp(5));wrap.setBackground(bg(surface,10));
    Switch s=new Switch(this);s.setText(title);s.setTextColor(foreground);s.setTextSize(14);s.setMinHeight(dp(44));s.setChecked(AppPreferences.visible(this,key,def));
    s.setOnCheckedChangeListener((view,checked)->{AppPreferences.set(this,key,checked?"on":"off");
      if(update!=null)update.run();});wrap.addView(s);
    if(subtitle!=null&&!subtitle.isEmpty()){TextView sub=text(subtitle,11,secondary,false);sub.setPadding(dp(4),0,dp(6),dp(8));wrap.addView(sub);}
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(5);root.addView(wrap,p);
  }
  private void choose(String name,String key,String[] values,String[] labels,String def){
    button(name+"  ·  "+labels[index(values,AppPreferences.get(this,key,def))],()->{
      String existing=AppPreferences.get(this,key,def);
      new AlertDialog.Builder(this).setTitle(name).setSingleChoiceItems(labels,index(values,existing),(d,position)->{
        AppPreferences.set(this,key,values[position]);d.dismiss();OppoFluidCloud.updateFromCache(this);draw();}).setNegativeButton(tr("取消","Cancel"),null).show();
    });
  }
  private int index(String[] values,String key){for(int i=0;i<values.length;i++)if(values[i].equals(key))return i;return 0;}
  private void info(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(tr("关闭","Close"),null).show();}
  private void runTest(String scene){
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},3345);
      Toast.makeText(this,tr("授权通知权限后请再次点击测试","Allow notifications, then retry"),Toast.LENGTH_LONG).show();return;}
    info(tr("测试结果","Test result"),OppoFluidCloud.test(this,scene)+"\n"+
        tr("仅模拟显示，不发送车控或后台查询。","Display simulation only; no commands or network polling."));
  }
  private String title(String scene){switch(scene){
    case "lock":return tr("上锁成功","Lock confirmed");case "unlock":return tr("解锁成功","Unlock confirmed");
    case "climate":return tr("空调运行与倒计时","Climate and countdown");
    case "air":return tr("空气净化与倒计时","Air purification and countdown");
    case "camp":return tr("露营托管与到期","Camping and timer");
    case "charging":return tr("充电状态与电量","Charging and SOC");
    case "fault":return tr("车辆异常详细提醒","Detailed vehicle warnings");
    case "trunk":return tr("尾箱二级操作","Trunk actions");
    case "windows":return tr("车窗二级操作","Window actions");
    case "find":return tr("寻车 / 鸣笛 / 闪灯","Find / horn / flash");
    default:return tr("电量、续航、车况","Vehicle status / battery / range");}
  }
  private void showScene(String scene){
    OppoFluidScene demo=OppoFluidCloud.demo(this,scene);
    LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(bg(surface,12));card.setPadding(dp(12),dp(10),dp(12),dp(10));
    TextView head=text(title(scene),14,foreground,true);card.addView(head);
    TextView chip=text(tr("一级胶囊：","Level 1 chip: ")+demo.chip,12,ORANGE,true);chip.setPadding(0,dp(5),0,dp(5));card.addView(chip);
    TextView detail=text(tr("二级信息：","Expanded card: ")+demo.detail,12,secondary,false);card.addView(detail);
    LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);
    Button test=new Button(this);test.setText(tr("测试此项","Test"));test.setAllCaps(false);test.setTextColor(ORANGE);test.setOnClickListener(v->runTest(scene));
    controls.addView(test,new LinearLayout.LayoutParams(0,dp(44),1));
    if(demo.firstAction!=null){TextView action=text(tr("操作按钮：","Actions: ")+demo.firstAction+(demo.secondAction==null?"":" / "+demo.secondAction),10,secondary,false);
      controls.addView(action,new LinearLayout.LayoutParams(0,-2,2));}
    card.addView(controls);
    LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);root.addView(card,params);
  }
  /** Runtime LSPosed selection is stored in the shared Bridge provider, never in UI-only prefs. */
  private Bundle bridgeModeSnapshot(){
    try{
      Bundle result=getContentResolver().call(android.net.Uri.parse("content://io.github.polestarcn.companion.bridge-settings"),
          "snapshot",null,null);
      return result==null?new Bundle():result;
    }catch(RuntimeException ignored){return new Bundle();}
  }
  private String selectedMode(){return io.github.polestarcn.oppobridge.BridgeSettings.validMode(
      bridgeModeSnapshot().getString(io.github.polestarcn.oppobridge.BridgeSettings.MODE,"stable"));}
  private void updateBridgeMode(String key,String value){
    try {
      Bundle b=new Bundle();b.putString("key",key);b.putString("value",value);
      Bundle done=getContentResolver().call(android.net.Uri.parse("content://io.github.polestarcn.companion.bridge-settings"),
          "update-mode",null,b);
      if(done==null||!done.getBoolean("accepted",false))throw new IllegalStateException("Mode not saved");
      Toast.makeText(this,tr("模式已保存；宿主进程重启后生效","Mode saved; restart OPPO hosts"),Toast.LENGTH_LONG).show();
      draw();
    }catch(RuntimeException failure){info(tr("模块模式修改失败","Module switch failed"),failure.getClass().getSimpleName());}
  }
  private void bridgeModeChoice(){
    String[] values={"stable","new","mock"};
    String[] titles={tr("稳定版 · 默认","Stable · default"),tr("NEW · 动态适配实验","NEW · adaptive experiment"),
        tr("Mock · 隔离诊断，不发送车控","Mock · isolated/no vehicle commands")};
    new AlertDialog.Builder(this).setTitle(tr("LSPosed / CarLink 模式","LSPosed / CarLink mode"))
        .setSingleChoiceItems(titles,index(values,selectedMode()),(dialog,which)->{
          dialog.dismiss();String next=values[which];
          new AlertDialog.Builder(this).setTitle(tr("确认切换模式","Confirm module switch"))
            .setMessage(tr("当前宿主加载的 Hook 不会立即切换。重新启动 MyDevices、负一屏、CarLink 对应进程后才生效。NEW 需要逐项验证；Mock 不实际控车。",
                "Already installed hooks stay active until the OPPO host restarts. NEW needs device validation; Mock never commands the car."))
            .setPositiveButton(tr("确认","Confirm"),(d,w)->updateBridgeMode("compatibilityMode",next))
            .setNegativeButton(tr("取消","Cancel"),null).show();
        }).setNegativeButton(tr("取消","Cancel"),null).show();
  }
  private void rpkModeChoice(){
    String[] values={"exact","feature"};
    String now=bridgeModeSnapshot().getString("rpkCompatibilityMode","exact");
    String[] titles={tr("精确 SHA / 固定版（推荐）","Exact SHA/version (default)"),
        tr("结构特征验证（NEW 实验）","Feature-verified / NEW experimental")};
    new AlertDialog.Builder(this).setTitle("MyDevices RPK")
        .setSingleChoiceItems(titles,index(values,now),(d,w)->{d.dismiss();updateBridgeMode("rpkCompatibilityMode",values[w]);})
        .setNegativeButton(tr("取消","Cancel"),null).show();
  }
  private void modeDiagnostics(){
    String mode=selectedMode();
    String report="已选择 / Selected: "+mode+"\n"
        +"当前生效 / Last observed hosts:\n"+OppoBridgeModeDiagnostics.description(this,mode)
        +"\n未收到模式回报不等于 Hook 成功。切换后需要重启对应宿主，再检查。";
    info(tr("LSPosed 运行模式诊断","LSPosed mode diagnostics"),report);
  }
  private void runModeTest(){
    StringBuilder result=new StringBuilder("Beta46 · 逐项只读检测\n");
    String mode=selectedMode();
    result.append("selectedMode=").append(mode).append('\n');
    result.append("RPK strategy=").append(bridgeModeSnapshot().getString("rpkCompatibilityMode","exact")).append('\n');
    result.append(OppoBridgeModeDiagnostics.description(this,mode)).append("\n\n");
    result.append(io.github.polestarcn.oppobridge.BridgeTestOrchestrator.runReport(this));
    result.append("\n【Mock】本地模拟不调用车辆API；任何‘受理’须与实际车端回读分开。\n");
    info(tr("兼容链路完整测试","Full compatibility test"),result.toString());
  }
  private void modePartTest(String method){
    String result;
    try{
      Bundle output=getContentResolver().call(
          android.net.Uri.parse("content://io.github.polestarcn.companion.bridge"),method,null,null);
      result=output==null?"No data returned":output.toString();
    }catch(RuntimeException ex){result="读取失败："+ex.getClass().getSimpleName();}
    info(tr("单项只读诊断","Read-only test")+" · "+method,
        result+"\n\n只读测试不执行锁车、空调、闪灯、鸣笛或真实权限变更。");
  }
  private void rollbackMode(){
    new AlertDialog.Builder(this).setTitle(tr("恢复稳定版","Restore stable mode"))
      .setMessage(tr("恢复原稳定 Hook 与精确 RPK 校验。需要重启 OPPO 宿主进程才能完全回退。",
          "Restores stable hooks and exact RPK gate; restart affected hosts to finish rollback."))
      .setPositiveButton(tr("恢复","Restore"),(d,w)->{
        updateBridgeMode("rpkCompatibilityMode","exact");
        updateBridgeMode("compatibilityMode","stable");
      }).setNegativeButton(tr("取消","Cancel"),null).show();
  }
  private void draw(){
    dark=AppPreferences.dark(this);foreground=dark?Color.WHITE:Color.rgb(30,30,30);
    secondary=dark?Color.rgb(170,170,170):Color.rgb(95,95,95);
    surface=dark?Color.rgb(32,34,37):Color.rgb(242,242,242);
    int canvas=dark?Color.rgb(12,13,14):Color.WHITE;
    ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(18),dp(12),dp(18),dp(30));root.setBackgroundColor(canvas);scroll.addView(root);
    TextView top=text(tr("OPPO 系统兼容","OPPO system compatibility"),23,foreground,true);root.addView(top);
    TextView subtitle=text(tr("LSPosed · CarLink · 流体云 · 手表 Indicator","LSPosed · CarLink · Fluid Cloud · Watch Indicator"),12,ORANGE,false);subtitle.setPadding(0,dp(7),0,dp(6));root.addView(subtitle);
    button(tr("返回主程序设置","Back to app settings"),this::finish);
    header(tr("负一屏与设备空间卡片","Shelf and MyDevices cards"),
      tr("默认只显示电量与续航；分隔符和内容可自定义，编辑时实时预览。","Default battery/range only. Editable text and live preview."));
    button(tr("自定义卡片文字、数据字段、分隔符","Customize card fields and separator"),
        ()->startActivity(new Intent(this,OppoCardDisplaySettingsActivity.class)));
    header(tr("LSPosed / CarLink 双方案","LSPosed / CarLink dual-mode"),
      tr("当前选择与宿主已生效模式分别展示，默认为稳定版。","Selected and actually installed host mode are shown separately."));
    button(tr("模块模式","Module mode")+" · "+selectedMode().toUpperCase(java.util.Locale.ROOT),this::bridgeModeChoice);
    button(tr("MyDevices RPK 校验策略","RPK validation")+" · "+bridgeModeSnapshot().getString("rpkCompatibilityMode","exact"),this::rpkModeChoice);
    button(tr("查看 Hook 实际生效与重启状态","Show effective modes / restart status"),this::modeDiagnostics);
    button(tr("一键检测 NEW / 稳定 / Mock 各项链路","Run complete read-only mode diagnostics"),this::runModeTest);
    button(tr("单测：设备卡片 / MyDevices","Test: MyDevices host"),()->modePartTest("host-status"));
    button(tr("单测：负一屏原生卡片","Test: AssistantScreen native"),()->modePartTest("assistant-host-status"));
    button(tr("单测：CarLink 宿主状态","Test: CarLink host"),()->modePartTest("carlink-host-status"));
    button(tr("单测：RPK / 4×2 / 2×2 状态","Test: RPK / 4×2 / 2×2"),()->modePartTest("quickapp-runtime-status"));
    button(tr("单测：车辆发现与虚拟会话","Test: vehicle/session cache"),()->modePartTest("virtual-session-status"));
    button(tr("单测：IPC 与回读授权约束","Test: IPC capability"),()->modePartTest("capabilities"));
    button(tr("单测：当前生效模式与重启需求","Test: active mode / restart"),this::modeDiagnostics);
    button(tr("一键恢复稳定版（需重启宿主）","Restore stable mode (host restart)"),this::rollbackMode);
    header(tr("系统能力与模块","System integration"),tr("模块诊断独立，不将 Mock 当作官方授权。","Host diagnostics never imply OEM approval."));
    button(tr("CarLink / 设备空间 / LSPosed 设置与诊断","CarLink / MyDevices / LSPosed diagnostics"),()->startActivity(new Intent(this,OppoWidgetSetupActivity.class)));
    button(tr("OPPO Watch 手机端 Indicator 诊断","Phone-side Watch Indicator diagnostics"),
        ()->info("OPPO Watch Indicator · Phone",OppoWatchIndicatorLab.diagnostics(this)
          +"\n\n提示：已按 Beta46 1B 方案仅保留主 APK 手机端通知实验，未在手表安装应用。"));
    header(tr("流体云实验总设置","Fluid Cloud master controls"),tr("默认关闭；打开后才发送兼容通知。颜色采用主程序 VI 橙色。","Off by default. Compatible notifications use the VI orange accent."));
    toggle(tr("启用 OPPO 流体云与车况通知","Enable Fluid Cloud/vehicle notifications"),
      tr("关闭将撤销所有本功能产生的通知","Off cancels all notices from this feature"),OppoFluidPolicy.MASTER,false,
      ()->{if(!OppoFluidPolicy.enabled(this))OppoFluidCloud.cancelAll(this);draw();});
    choose(tr("兼容通道","Compatibility route"),OppoFluidPolicy.PATH,
      new String[]{"auto","v1","v2"},new String[]{tr("自动选择","Auto"),tr("1.0 标准通知兼容","1.0 notification"),tr("2.0 Android 16 实时动态请求","2.0 Android 16 live update")},"auto");
    choose(tr("显示密度","Display density"),OppoFluidPolicy.STYLE,new String[]{"simple","full"},
      new String[]{tr("简洁 VI 模式","Simple VI"),tr("全功能信息","Full details")},"simple");
    choose(tr("卡片交互层级","Card interaction level"),OppoFluidPolicy.LEVEL,new String[]{"1","2"},
      new String[]{tr("一级 · 仅显示","Level 1 · display"),tr("二级 · 展开信息与按钮","Level 2 · actions")},"1");
    toggle(tr("允许二级控制按钮","Enable expanded-card controls"),
      tr("默认关闭；真实操作须打开 App 并再次确认/授权","Off by default; real actions require app confirmation"),
      OppoFluidPolicy.ACTIONS,false,()->{OppoFluidCloud.updateFromCache(this);draw();});
    choose(tr("锁车/解锁通知时长","Lock/unlock notice duration"),OppoFluidPolicy.LOCK_SECONDS,
      new String[]{"5","10","15","20","30"},new String[]{"5s","10s","15s","20s","30s"},"10");
    choose(tr("车辆异常重复提醒冷却","Repeated warning cooldown"),OppoFluidPolicy.WARNING_COOLDOWN,
      new String[]{"15","30","60","120","1440"},new String[]{"15m","30m","60m","120m","24h"},"60");
    header(tr("OPPO Watch 指示器","OPPO Watch Indicator"),tr("手表原生 Indicator 需要手表应用本机发布通知；手机仅做兼容实验。","A real watch indicator requires a watch-side app; phone tests only exercise notification fields."));
    toggle(tr("启用手表 Indicator 实验","Enable watch Indicator lab"),null,OppoWatchIndicatorLab.PREF_ENABLED,false,null);
    toggle(tr("普通车况通知附加手表标记","Attach indicator hint to resident service"),
      tr("保留服务类别，不伪装成导航或运动","Keeps true SERVICE category; no navigation/workout spoofing"),OppoWatchIndicatorLab.PREF_RESIDENT,false,
      ()->ResidentService.update(this));
    button(tr("发送一次手表 Indicator 模拟通知","Post a watch Indicator test"),()->info("OPPO Watch",OppoWatchIndicatorLab.startTest(this)));
    button(tr("清除手表 Indicator 测试","Cancel watch Indicator lab"),()->{OppoWatchIndicatorLab.cancel(this);info(tr("完成","Done"),tr("手表 Indicator 手机模拟已清除","Phone-side watch indicator cancelled"));});
    header(tr("通知、流体云与操作逐项管理","Per-feature notification, cloud, actions"),
      tr("每项三种权限独立；不开通知时，也不会申请对应的流体云。","Each switch is independent. Cloud needs its notification enabled."));
    for(String scene:OppoFluidPolicy.SCENES){
      TextView label=text("● "+title(scene),14,ORANGE,true);label.setPadding(0,dp(13),0,dp(3));root.addView(label);
      toggle(tr("通知","Notification"),null,"oppo_compat_notify_"+scene,OppoFluidPolicy.defaultEnabled(scene),
        ()->{OppoFluidCloud.cancelRealScene(this,scene);OppoFluidCloud.updateFromCache(this);});
      toggle(tr("申请流体云展示","Request cloud display"),null,"oppo_compat_cloud_"+scene,OppoFluidPolicy.defaultEnabled(scene),
        ()->{OppoFluidCloud.cancelRealScene(this,scene);OppoFluidCloud.updateFromCache(this);});
      if(OppoFluidCloud.demo(this,scene).firstAction!=null)
        toggle(tr("允许二级操作","Allow expanded actions"),null,"oppo_compat_action_"+scene,false,
          ()->{OppoFluidCloud.cancelRealScene(this,scene);OppoFluidCloud.updateFromCache(this);});
    }
    header(tr("完整功能测试中心","Full test laboratory"),tr("全部内容为假数据；测试不会操作车辆、唤醒车辆或启动后台轮询。","Synthetic data only: no car control, network reads or background polling."));
    button(tr("一键测试全部 11 项","Run all 11 demonstration cases"),()->runTestAll());
    button(tr("验证 1.0/2.0 × 简洁/全功能 × 一级/二级 × 全部场景","Validate all 88 combinations"),()->{
      try{int result=OppoFluidCloud.validateMatrix(this);info(tr("组合测试通过","Matrix test passed"),
        tr("已验证 ","Validated ")+result+tr(" 个构造组合；这不代表 ColorOS 实际显示成功。"," notification builds; this is not native ColorOS display proof."));}
      catch(Exception e){info(tr("组合测试失败","Matrix validation failed"),e.toString());}
    });
    for(String scene:OppoFluidPolicy.SCENES)showScene(scene);
    button(tr("取消所有模拟通知","Cancel all synthetic tests"),()->{OppoFluidCloud.cancelTests(this);info(tr("完成","Done"),tr("全部模拟通知已撤销","All synthetic tests cancelled"));});
    button(tr("查看完整系统兼容诊断","View full compatibility diagnostics"),()->info(tr("OPPO 流体云诊断","Fluid Cloud diagnostics"),OppoFluidCloud.diagnose(this)));
    button(tr("打开系统通知授权设置","Open system notification settings"),()->{
      Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);
    });
    button(tr("恢复 OPPO 流体云默认设置","Restore Fluid Cloud defaults"),()->new AlertDialog.Builder(this)
      .setTitle(tr("恢复默认","Reset defaults"))
      .setMessage(tr("这会关闭本功能并清除本功能的通知，不会删除账号、车辆或 LSPosed 设置。","Disables Fluid Cloud and clears its notifications without affecting account, vehicle or LSPosed settings."))
      .setPositiveButton(tr("恢复","Reset"),(d,w)->{OppoFluidPolicy.reset(this);OppoFluidCloud.cancelAll(this);draw();})
      .setNegativeButton(tr("取消","Cancel"),null).show());
    header(tr("平台边界","Platform limits"),tr("OPPO 官方潘塔纳尔流体云仍需要业务审核和 serviceId。2.0 使用 Android 16 实时动态请求，系统决定是否显示胶囊；1.0 为标准通知兼容，不冒充官方模板。",
      "Official Pantanal Fluid Cloud needs approved service IDs. 2.0 requests Android 16 live updates; 1.0 is a normal notification fallback. SystemUI decides whether to show a capsule."));
    setContentView(scroll);
  }
  private void runTestAll(){
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},3345);return;}
    info(tr("完整模拟测试","Full synthetic test"),OppoFluidCloud.testAll(this));
  }
}
