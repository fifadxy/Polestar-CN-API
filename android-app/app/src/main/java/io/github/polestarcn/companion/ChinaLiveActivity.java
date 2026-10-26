package io.github.polestarcn.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
    private OperationState operationState = OperationState.NOT_SENT;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        api = new ChinaPolestarApi(this);
        snapshot = VehicleSnapshotStore.loadChina(this);
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
        content.addView(t("中国区 · 车辆连接", 22, true));
        TextView sub=t("v0.3.0-alpha2 · 修正模板协议签名、Token、Discovery 与 C3 Proto 映射。请在本人授权车辆上逐项验证。",13,false);
        sub.setTextColor(SUBTLE); content.addView(sub);
        content.addView(b(api.session()==null ? "中国区登录" : "重新登录", v->loginDialog()));
        content.addView(b("发现车辆并读取 SOC / 续航 / 里程", v->refreshLive()));
        content.addView(b("退出并清除会话", v->{
            api.logout(); selected=null; snapshot=null; operationState=OperationState.NOT_SENT;
            VehicleSnapshotStore.clearChina(this); refreshWidgets(); toast("会话和车辆缓存已清除"); render();
        }));
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
            content.addView(b("只读回读车辆状态", v->readSelected(selected)));
            content.addView(t("最近一次操作: " + operationState.label, 13, true));
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
                runOnUiThread(()->chooseVehicle(cars));
            } catch(Exception e){runOnUiThread(()->error("读取失败",e));}
        }, "china-discovery").start();
    }

    private void chooseVehicle(List<ChinaPolestarApi.VehicleInfo> cars) {
        if (cars == null || cars.isEmpty()) { error("读取失败", new Exception("账号下没有发现车辆")); return; }
        if (cars.size() == 1) { readSelected(cars.get(0)); return; }
        String[] labels = new String[cars.size()];
        for (int i = 0; i < cars.size(); i++) labels[i] = cars.get(i).displayLabel();
        new AlertDialog.Builder(this).setTitle("选择车辆").setItems(labels, (dialog, which) ->
                readSelected(cars.get(which))).setNegativeButton("取消", null).show();
    }

    private void readSelected(ChinaPolestarApi.VehicleInfo vehicle) {
        selected = vehicle;
        toast("正在读取 " + vehicle.displayLabel());
        new Thread(() -> {
            try {
                VehicleSnapshot live = api.readVehicle(vehicle);
                VehicleSnapshotStore.saveChina(this, live);
                snapshot = live;
                runOnUiThread(() -> { toast("实车状态读取完成"); refreshWidgets(); render(); });
            } catch (Exception e) {
                runOnUiThread(() -> error("状态读取失败", e));
            }
        }, "china-state-read").start();
    }

    private interface Throwing { byte[] run() throws Exception; }
    private void runCommand(String label, Throwing call) {
        operationState = OperationState.SENDING;
        render();
        toast(label+"：正在发送…");
        new Thread(()->{
            try { call.run(); runOnUiThread(()->{
                operationState = OperationState.SERVER_ACCEPTED;
                render();
                new AlertDialog.Builder(this).setTitle(label)
                        .setMessage("服务器已接受请求。此状态不等于车辆已完成动作，请使用只读回读或实车确认。")
                        .setPositiveButton("知道了",null).show();
            }); }
            catch(Exception e){runOnUiThread(()->{
                operationState = uncertain(e) ? OperationState.UNCONFIRMED : OperationState.FAILED;
                render(); error(label+"失败",e);
            });}
        }, "china-remote-command").start();
    }

    private void confirm(String message, Runnable yes) {
        new AlertDialog.Builder(this).setTitle("远程控制确认").setMessage(message)
                .setNegativeButton("取消",null).setPositiveButton("确认",(d,w)->yes.run()).show();
    }
    private void error(String title, Exception e) {
        String m=e.getMessage(); if(m==null||m.isEmpty())m=e.getClass().getSimpleName();
        new AlertDialog.Builder(this).setTitle(title).setMessage(m).setPositiveButton("知道了",null).show();
    }
    private boolean uncertain(Exception e) {
        String value = String.valueOf(e.getMessage()).toUpperCase(java.util.Locale.ROOT);
        return value.contains("TIMEOUT") || value.contains("DEADLINE_EXCEEDED") ||
                value.contains("UNAVAILABLE") || value.contains("RESET") || value.contains("BROKEN PIPE");
    }
    private void refreshWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName standard = new ComponentName(this, VehicleWidgetProvider.class);
        ComponentName compact = new ComponentName(this, CompactWidgetProvider.class);
        for (int id : manager.getAppWidgetIds(standard)) WidgetRenderer.update(this, manager, id, false);
        for (int id : manager.getAppWidgetIds(compact)) WidgetRenderer.update(this, manager, id, true);
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
