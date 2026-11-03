package io.github.polestarcn.companion;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OppoVehicleAccessSettingsActivity extends Activity {
  private final LinkedHashMap<String,EditText> fields=new LinkedHashMap<>();private TextView status;
  @Override public void onCreate(Bundle state){
    super.onCreate(state);LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
    int p=(int)(16*getResources().getDisplayMetrics().density);column.setPadding(p,p,p,p);
    TextView title=new TextView(this);title.setTextSize(20);title.setText("Polestar 车型接入参数 · TestLab");column.addView(title);
    TextView fixed=new TextView(this);fixed.setText("固定系统契约（无需填写）\nProvider Code: POLESTAR\nDevice Type: CAR\n4×2: chesuo / kongtiao / shanguang / mingdi\n\n下方参数已预填，可按测试系统实际类名调整。修改后重启对应 OPPO 进程。\n");column.addView(fixed);
    add(column,OppoVehicleProfileSettings.DISPLAY_NAME,"显示名称");add(column,OppoVehicleProfileSettings.BRAND,"品牌");
    add(column,OppoVehicleProfileSettings.MANUFACTURER,"制造商");add(column,OppoVehicleProfileSettings.MODEL_ID,"Model ID");
    add(column,OppoVehicleProfileSettings.MODEL_NAME,"Model Name");add(column,OppoVehicleProfileSettings.CONNECT_STATE,"连接状态");
    add(column,OppoVehicleProfileSettings.VIN_PREFIX,"测试 VIN 前缀");add(column,OppoVehicleProfileSettings.CAPABILITIES,"Capabilities（逗号分隔）");
    add(column,OppoVehicleProfileSettings.CAR_CONTROL_CLASS,"CarControl 类名");add(column,OppoVehicleProfileSettings.SESSION_MANAGER_CLASS,"SessionManager 类名");
    add(column,OppoVehicleProfileSettings.SESSION_CLASS,"CarDeviceSession 类名");add(column,OppoVehicleProfileSettings.CATALOG_AUTHORITIES,"车型目录 Provider authorities（逗号分隔）");
    Button save=new Button(this);save.setText("保存车型接入参数");save.setOnClickListener(v->save());column.addView(save);
    Button restore=new Button(this);restore.setText("一键恢复 Polestar 默认配置");restore.setOnClickListener(v->{OppoVehicleProfileSettings.restoreDefaults(this);populate();status.setText("已恢复默认车型参数。返回上一页后可继续执行‘恢复全部默认并启用’。");});column.addView(restore);
    status=new TextView(this);status.setTextIsSelectable(true);column.addView(status);ScrollView scroll=new ScrollView(this);scroll.addView(column);setContentView(scroll);populate();
  }
  private void add(LinearLayout parent,String key,String label){TextView t=new TextView(this);t.setText(label);parent.addView(t);EditText e=new EditText(this);e.setSingleLine(!OppoVehicleProfileSettings.CAPABILITIES.equals(key)&&!OppoVehicleProfileSettings.CATALOG_AUTHORITIES.equals(key));fields.put(key,e);parent.addView(e);}
  private void populate(){Bundle current=OppoVehicleProfileSettings.snapshot(this);for(Map.Entry<String,EditText> entry:fields.entrySet())entry.getValue().setText(current.getString(entry.getKey(),""));}
  private void save(){Bundle values=new Bundle();for(Map.Entry<String,EditText> entry:fields.entrySet())values.putString(entry.getKey(),entry.getValue().getText().toString());
    try{OppoVehicleProfileSettings.save(this,values);populate();status.setText("已保存。车型对象、目录 Overlay 与 CarLink Session 将使用新参数；重启 MyDevices / AssistantScreen / CarLink 后生效。");}
    catch(RuntimeException failure){status.setText("保存失败："+failure.getClass().getSimpleName());}}
}
