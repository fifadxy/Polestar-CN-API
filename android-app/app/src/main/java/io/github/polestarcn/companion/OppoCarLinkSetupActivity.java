package io.github.polestarcn.companion;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
/** One CarLink setup surface; root is requested only by a user-pressed diagnostic action. */
public final class OppoCarLinkSetupActivity extends Activity {
  private TextView status;private final java.util.concurrent.ExecutorService work=java.util.concurrent.Executors.newSingleThreadExecutor();
  @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int p=(int)(16*getResources().getDisplayMetrics().density);page.setPadding(p,p,p,p);
    TextView intro=new TextView(this);intro.setText("CarLink优先接入\nSDK初始化、账号授权、车型技能、系统卡片是不同阶段。Root可测试受保护入口并采集本机宿主静态契约；不能生成OPPO服务器的应用授权或车型配置。\n\n本页仅申请远程车控，不启用BLE钥匙扫描。");page.addView(intro);
    EditText type=field(page,"OPPO后台typeCode",AppPreferences.get(this,"carlink_type_code",""));
    EditText name=field(page,"车型名称typeName",AppPreferences.get(this,"carlink_type_name","Polestar 2"));
    EditText image=field(page,"HTTPS车辆图片URL",AppPreferences.get(this,"carlink_image_url",""));
    button(page,"保存车型映射",()->{AppPreferences.set(this,"carlink_type_code",type.getText().toString().trim());AppPreferences.set(this,"carlink_type_name",name.getText().toString().trim());AppPreferences.set(this,"carlink_image_url",image.getText().toString().trim());refresh();});
    button(page,"初始化官方CarLink SDK",()->{OppoCarLinkRuntime.get(this).start();refresh();});
    button(page,"请求CarLink授权",()->{OppoCarLinkRuntime.get(this).authorize();refresh();});
    button(page,"订阅车辆卡片",()->{OppoCarLinkRuntime.get(this).subscribe();refresh();});
    Switch activation=new Switch(this);activation.setText("允许CarLink按需更新车况（默认关闭）");activation.setChecked(AppPreferences.visible(this,"carlink_activation_reads",false));
    activation.setOnCheckedChangeListener((view,checked)->{AppPreferences.set(this,"carlink_activation_reads",checked?"on":"off");refresh();});page.addView(activation);
    TextView power=new TextView(this);power.setText("仅收到当前车辆的系统连接请求时更新一次，5分钟最多一次；长停省电开启时不新增读取。普通读卡不唤醒车辆，无定时轮询。");page.addView(power);
    button(page,"Root测试官方绑车入口",()->{status.setText("正在请求root打开本机CarLink绑定Activity…");work.execute(()->{String r;try{r=OppoCarLinkRoot.open(this);}catch(Exception e){r="Root入口测试未完成："+e.getClass().getSimpleName();}final String result=r;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())status.setText(result);});});});
    button(page,"一键Root采集并导出（≤2MB）",()->{if(OppoResearchExportJob.start(this,true))startActivity(new Intent(this,OppoWidgetSetupActivity.class).putExtra("autoShare",true));});
    button(page,"刷新接入状态",this::refresh);status=new TextView(this);status.setTextIsSelectable(true);page.addView(status);ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);refresh();
  }
  private EditText field(LinearLayout page,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setSingleLine(true);page.addView(e);return e;}
  private void button(LinearLayout page,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->action.run());page.addView(b);}
  private void refresh(){status.setText(OppoCarLinkRuntime.get(this).describe()+"\n\n"+OppoOfficialCarLink.describe(this));}
  @Override protected void onDestroy(){work.shutdownNow();super.onDestroy();}
}
