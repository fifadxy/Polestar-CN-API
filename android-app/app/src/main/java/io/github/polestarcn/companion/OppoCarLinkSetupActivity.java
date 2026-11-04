package io.github.polestarcn.companion;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
/** One CarLink setup surface; root is requested only by a user-pressed diagnostic action. */
public final class OppoCarLinkSetupActivity extends Activity {
  private TextView status;private final java.util.concurrent.ExecutorService work=java.util.concurrent.Executors.newSingleThreadExecutor();
  @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int p=(int)(16*getResources().getDisplayMetrics().density);page.setPadding(p,p,p,p);
    TextView intro=new TextView(this);intro.setText(BuildConfig.OPPO_SDK_MOCK
        ?"CarLink Mock TestLab\n当前包使用本地 OCS 兼容 AAR + 虚拟 OPPO 服务。companyId/AUTH_CODE 仅在本 APK 内有效，不连接 OPPO 正式服务器。所有系统车控均 dry-run，不发送真实车辆写指令。"
        :"CarLink优先接入\nSDK初始化、账号授权、车型技能、系统卡片是不同阶段。Root可测试受保护入口并采集本机宿主静态契约；不能生成OPPO服务器的应用授权或车型配置。\n\n本页仅申请远程车控，不启用BLE钥匙扫描。");page.addView(intro);
    EditText type=field(page,"OPPO后台typeCode",AppPreferences.get(this,"carlink_type_code",""));
    EditText name=field(page,"车型名称typeName",AppPreferences.get(this,"carlink_type_name","Polestar 2"));
    EditText image=field(page,"HTTPS车辆图片URL",AppPreferences.get(this,"carlink_image_url",""));
    button(page,"保存车型映射",()->{AppPreferences.set(this,"carlink_type_code",type.getText().toString().trim());AppPreferences.set(this,"carlink_type_name",name.getText().toString().trim());AppPreferences.set(this,"carlink_image_url",image.getText().toString().trim());refresh();});
    button(page,BuildConfig.OPPO_SDK_MOCK?"初始化本机 Mock SDK":"初始化官方CarLink SDK",()->{OppoCarLinkRuntime.get(this).start();refresh();});
    button(page,"请求CarLink授权",()->{OppoCarLinkRuntime.get(this).authorize();refresh();});
    button(page,"订阅车辆卡片",()->{OppoCarLinkRuntime.get(this).subscribe();refresh();});
    if(BuildConfig.OPPO_SDK_MOCK){
      button(page,"一键完整 Mock OCS 测试",()->{
        status.setText("正在执行 Mock OCS：init → auth → subscribe → inquire → lock → climate → trunk…");
        OppoCarLinkMockHarness.runFull(this,()->runOnUiThread(this::refresh));
      });
      button(page,"Mock 查询车辆/车况",()->{
        try{
          Class<?> server=Class.forName("com.oplus.ocs.carlink.mock.MockCarLinkServer");
          server.getMethod("requestCarInfo").invoke(null);server.getMethod("requestCarStatus").invoke(null);
        }catch(Throwable ignored){}
        refresh();
      });
      button(page,"Mock 系统锁车",()->{OppoCarLinkMockHarness.issue("LOCK-CAR","OPEN");new android.os.Handler().postDelayed(this::refresh,350L);});
      button(page,"Mock 系统解锁",()->{OppoCarLinkMockHarness.issue("CAR-DOOR","OPEN");new android.os.Handler().postDelayed(this::refresh,350L);});
      button(page,"Mock 空调开启",()->{OppoCarLinkMockHarness.issue("AC-TEMP","OPEN");new android.os.Handler().postDelayed(this::refresh,350L);});
      button(page,"Mock 空调关闭",()->{OppoCarLinkMockHarness.issue("AC-TEMP","CLOSE");new android.os.Handler().postDelayed(this::refresh,350L);});
      button(page,"Mock 尾门开启",()->{OppoCarLinkMockHarness.issue("CAR-BOOT","OPEN");new android.os.Handler().postDelayed(this::refresh,350L);});
      button(page,"Mock 尾门关闭",()->{OppoCarLinkMockHarness.issue("CAR-BOOT","CLOSE");new android.os.Handler().postDelayed(this::refresh,350L);});
      button(page,"复制本次测试及负一屏诊断",()->{
        refresh();android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard!=null)clipboard.setPrimaryClip(android.content.ClipData.newPlainText("PsAssistant Mock 诊断",status.getText()));
        Toast.makeText(this,"已复制文字诊断",Toast.LENGTH_SHORT).show();
      });
      button(page,"重置 Mock OCS",()->{OppoCarLinkMockHarness.reset(this);refresh();});
    }
    Switch activation=new Switch(this);activation.setText("允许CarLink按需更新车况（默认关闭）");activation.setChecked(AppPreferences.visible(this,"carlink_activation_reads",false));
    activation.setOnCheckedChangeListener((view,checked)->{AppPreferences.set(this,"carlink_activation_reads",checked?"on":"off");refresh();});page.addView(activation);
    TextView power=new TextView(this);power.setText("仅收到当前车辆的系统连接请求时更新一次，5分钟最多一次；长停省电开启时不新增读取。普通读卡不唤醒车辆，无定时轮询。");page.addView(power);
    button(page,"刷新接入状态",this::refresh);status=new TextView(this);status.setTextIsSelectable(true);page.addView(status);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);refresh();
  }
  private EditText field(LinearLayout page,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setSingleLine(true);page.addView(e);return e;}
  private void button(LinearLayout page,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->action.run());page.addView(b);}
  private void refresh(){
    String extra="";
    if(BuildConfig.OPPO_SDK_MOCK){
      Bundle m=OppoCarLinkMockHarness.status();
      extra="\n\n"+OppoCarLinkMockHarness.report()+"\n\nMock OCS\ncompanyId="+m.getInt("companyId",0)
          +" · AUTH_CODE=TESTLAB_MOCK_ONLY"
          +"\ninitialized="+m.getBoolean("initialized",false)
          +" · authorized="+m.getBoolean("authorized",false)
          +" · subscribed="+m.getBoolean("subscribed",false)
          +" · bound="+m.getBoolean("bound",false)
          +"\ncarCount="+m.getInt("carCount",0)+" · statusCount="+m.getInt("statusCount",0)
          +" · currentCar="+m.getString("currentCar","")
          +"\nlastEvent="+m.getString("lastEvent","")
          +" · lastControlCode="+m.getInt("lastControlCode",-1)
          +" · "+m.getString("lastControlMessage","");
    }
    Bundle host=OppoQuickAppRuntimeDiagnostics.status(this);
    extra+="\n\n负一屏独立观测（请核对点击时间）\n事件="+host.getBoolean("carEventReceived",false)
      +" · 事件时间="+host.getLong("carEventReceivedAt",0)+" · 已拦截="+host.getBoolean("carDispatchIntercepted",false)
      +"\nPendingIntent="+host.getBoolean("pendingIntentSent",false)+" · 命令受理="+host.getBoolean("lastCommandAccepted",false)
      +" · 原因="+host.getString("lastCommandReason","")+" · 命令时间="+host.getLong("lastCommandAt",0);
    status.setText(OppoCarLinkRuntime.get(this).describe()+extra+"\n\n"+OppoOfficialCarLink.describe(this));
  }
  @Override protected void onDestroy(){work.shutdownNow();super.onDestroy();}
}
