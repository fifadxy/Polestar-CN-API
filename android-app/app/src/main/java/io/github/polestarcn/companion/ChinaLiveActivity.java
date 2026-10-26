package io.github.polestarcn.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import io.github.polestarcn.companion.api.ChinaPolestarApi;

public final class ChinaLiveActivity extends Activity {
    private final int INK = Color.rgb(25,27,30);
    private final int SUBTLE = Color.rgb(99,104,112);
    private final int PAPER = Color.rgb(247,247,245);
    private LinearLayout content;
    private ChinaPolestarApi api;
    private ChinaPolestarApi.VehicleInfo selected;
    private VehicleSnapshot snapshot;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        api = new ChinaPolestarApi(this);
        render();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView t(String s, int sp, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(INK);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        v.setPadding(0, dp(5), 0, dp(5)); return v;
    }
    private Button b(String s, View.OnClickListener l) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setOnClickListener(l); return b;
    }
    private void render() {
        ScrollView sc = new ScrollView(this); sc.setBackgroundColor(PAPER);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(20),dp(20),dp(30)); sc.addView(content);
        content.addView(t("中国区 · 实车连接实验版", 22, true));
        TextView sub=t("v0.2.0-alpha1 · 基于已验证模板协议独立重写。真实网络功能需在本人授权车辆上逐项验证。",13,false);
        sub.setTextColor(SUBTLE); content.addView(sub);
        content.addView(b(api.session()==null ? "中国区登录" : "重新登录", v->loginDialog()));
        content.addView(b("发现车辆并读取 SOC / 续航 / 里程", v->refreshLive()));
        content.addView(b("退出并清除会话", v->{api.logout(); selected=null; snapshot=null; toast("会话已清除"); render();}));
        if (snapshot != null) {
            content.addView(t("车辆状态",18,true));
            content.addView(t(snapshot.name + " · " + snapshot.id,14,true));
            content.addView(t("SOC: " + VehicleSnapshot.metric(snapshot.batteryPercent,"%"),16,false));
            content.addView(t("续航: " + VehicleSnapshot.metric(snapshot.rangeKm," km"),16,false));
            content.addView(t("累计里程: " + VehicleSnapshot.metric(snapshot.odometerKm," km"),16,false));
            content.addView(t("门锁: " + snapshot.lock,16,false));
            content.addView(t("数据源: " + snapshot.source,12,false));
        }
        if (selected != null) {
            content.addView(t("远程控制（本人授权车辆）",18,true));
            content.addView(b("启动空调 · 22°C", v->confirm("启动空调", ()->runCommand("空调启动",()->api.climateStart(selected.vin,22f)))));
            content.addView(b("停止空调", v->confirm("停止空调", ()->runCommand("空调停止",()->api.climateStop(selected.vin)))));
            content.addView(b("解锁车门", v->confirm("确认解锁车门？该操作会真实发送到车辆。", ()->runCommand("车门解锁",()->api.unlock(selected.vin)))));
        }
        content.addView(t("说明：服务器接受命令不等于车辆已执行。此 alpha 版会明确显示“服务器已接受/需回读确认”，不会自动重复解锁或其他写操作。兼容签名材料仅在运行时从 SHA-256 完全匹配的已安装模板 APK 中读取，不写入本仓库、日志或本应用持久存储。",12,false));
        setContentView(sc);
    }

    private void loginDialog() {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),0,dp(20),0);
        EditText phone=new EditText(this); phone.setHint("中国区手机号"); phone.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText pass=new EditText(this); pass.setHint("密码"); pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(phone); box.addView(pass);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Polestar China 登录").setView(box)
                .setNegativeButton("取消",null).setPositiveButton("登录",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String p=phone.getText().toString(); String pwd=pass.getText().toString();
            if(p.trim().isEmpty()||pwd.isEmpty()){toast("请输入手机号和密码");return;}
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new Thread(()->{
                try { api.login(p,pwd); runOnUiThread(()->{ pass.setText(""); dialog.dismiss(); toast("登录成功，会话已加密保存"); render(); }); }
                catch(Exception e){ runOnUiThread(()->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); error("登录失败",e);}); }
            }).start();
        }));
        dialog.show();
    }

    private void refreshLive() {
        toast("正在连接中国区车辆服务…");
        new Thread(()->{
            try {
                List<ChinaPolestarApi.VehicleInfo> cars=api.discoverVehicles();
                selected=cars.get(0);
                snapshot=api.readVehicle(selected);
                runOnUiThread(()->{toast("实车状态读取完成");render();});
            } catch(Exception e){runOnUiThread(()->error("读取失败",e));}
        }).start();
    }

    private interface Throwing { byte[] run() throws Exception; }
    private void runCommand(String label, Throwing call) {
        toast(label+"：正在发送…");
        new Thread(()->{
            try { call.run(); runOnUiThread(()->new AlertDialog.Builder(this).setTitle(label)
                    .setMessage("服务器已接受请求。此状态不等于车辆已完成动作，请刷新车辆状态或实车确认。")
                    .setPositiveButton("知道了",null).show()); }
            catch(Exception e){runOnUiThread(()->error(label+"失败",e));}
        }).start();
    }

    private void confirm(String message, Runnable yes) {
        new AlertDialog.Builder(this).setTitle("远程控制确认").setMessage(message)
                .setNegativeButton("取消",null).setPositiveButton("确认",(d,w)->yes.run()).show();
    }
    private void error(String title, Exception e) {
        String m=e.getMessage(); if(m==null||m.isEmpty())m=e.getClass().getSimpleName();
        new AlertDialog.Builder(this).setTitle(title).setMessage(m).setPositiveButton("知道了",null).show();
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
