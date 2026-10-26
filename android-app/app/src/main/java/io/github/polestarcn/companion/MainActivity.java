package io.github.polestarcn.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import io.github.polestarcn.companion.api.ChinaPolestarApi;
import io.github.polestarcn.companion.auth.SecureSessionStore;

/** Main dashboard. Remote writes are delegated to the confirmation-gated China live screen. */
public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(25, 27, 30);
    private static final int SUBTLE = Color.rgb(99, 104, 112);
    private static final int PAPER = Color.rgb(247, 247, 245);
    private static final int LINE = Color.rgb(229, 230, 229);
    private static final int ACCENT = Color.rgb(240, 208, 58);
    private static final int GREEN = Color.rgb(16, 125, 77);
    private int page = 0;
    private LinearLayout content;
    private Region region;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (savedInstanceState != null) page = savedInstanceState.getInt("page", 0);
        region = AppState.region(this);
        DiagnosticLog.add("App opened");
        if (region == null) {
            region = Region.CHINA;
            render();
            getWindow().getDecorView().post(this::showInitialRegionChooser);
        } else render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("page", page);
        super.onSaveInstanceState(state);
    }
    @Override protected void onResume() {
        super.onResume();
        if (content != null) render();
    }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private TextView text(String value, int sp, int color, boolean heavy) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextColor(color); v.setTextSize(sp);
        v.setFontFeatureSettings("kern");
        if (heavy) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }
    private LinearLayout horizontal() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private void gap(LinearLayout into, int height) {
        View v = new View(this);
        into.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout v = vertical();
        v.setPadding(dp(19), dp(18), dp(19), dp(18));
        v.setBackground(background(Color.WHITE, 17, LINE));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        parent.addView(v, p);
        return v;
    }
    private TextView action(String label, boolean dark, Runnable click) {
        TextView v = text(label, 14, dark ? Color.WHITE : INK, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(14), dp(12), dp(14), dp(12));
        v.setBackground(background(dark ? INK : PAPER, 12, dark ? 0 : LINE));
        v.setOnClickListener(w -> click.run());
        return v;
    }
    private void row(LinearLayout into, String key, String value) {
        LinearLayout r = horizontal();
        TextView k = text(key, 13, SUBTLE, false);
        TextView v = text(value, 13, INK, true);
        v.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        r.addView(k, new LinearLayout.LayoutParams(0, dp(37), 1));
        r.addView(v, new LinearLayout.LayoutParams(-2, dp(37)));
        into.addView(r);
    }
    private void sectionHeader(LinearLayout into, String title, String subtitle) {
        into.addView(text(title, 20, INK, true));
        if (subtitle != null) {
            TextView t = text(subtitle, 12, SUBTLE, false);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(5); p.bottomMargin = dp(13);
            into.addView(t,p);
        } else gap(into, 12);
    }
    private void render() {
        LinearLayout shell = vertical();
        shell.setBackgroundColor(PAPER);
        TextView top = text("P O L E S T A R", 16, INK, true);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(20), dp(14), dp(20), dp(12));
        shell.addView(top, new LinearLayout.LayoutParams(-1, dp(58)));
        View line = new View(this); line.setBackgroundColor(LINE);
        shell.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = vertical();
        content.setPadding(dp(18), dp(19), dp(18), dp(25));
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (page == 0) dashboard(); else if (page == 1) carDetails(); else settings();

        LinearLayout tabs = horizontal();
        tabs.setPadding(dp(10), dp(8), dp(10), dp(9));
        tabs.setBackgroundColor(Color.WHITE);
        addTab(tabs, "总览", 0);
        addTab(tabs, "车辆", 1);
        addTab(tabs, "设置 / 日志", 2);
        shell.addView(tabs, new LinearLayout.LayoutParams(-1, dp(67)));
        setContentView(shell);
    }
    private void addTab(LinearLayout tabs, String label, int which) {
        TextView v = text(label, 13, page == which ? INK : SUBTLE, page == which);
        v.setGravity(Gravity.CENTER); v.setPadding(dp(4), dp(10), dp(4), dp(10));
        if (page == which) v.setBackground(background(ACCENT, 12, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        p.setMargins(dp(3),0,dp(3),0);
        tabs.addView(v,p);
        v.setOnClickListener(view -> { page = which; render(); });
    }
    private void flag(LinearLayout parent, String title, String message, boolean warn) {
        LinearLayout v = card(parent);
        v.setBackground(background(warn ? Color.rgb(255,250,223) : Color.WHITE, 16, warn ? Color.rgb(246,226,139) : LINE));
        v.addView(text(title, 13, INK, true));
        gap(v, 6);
        v.addView(text(message, 12, SUBTLE, false));
    }
    private void dashboard() {
        VehicleSnapshot state = currentSnapshot();
        TelemetryStatus status = TelemetryStatus.classify(state.isDemo, state.source,
                state.timestampMillis, System.currentTimeMillis(), 15L * 60L * 1000L);
        sectionHeader(content, "车辆概览", "REGION  /  " + region.english + "  ·  Android v0.3.0-alpha2");
        if (state.isDemo) {
            flag(content, "尚未连接车辆", region == Region.CHINA
                    ? "请登录中国区账号并读取本人车辆。当前显示的数值是明确标记的界面演示数据。"
                    : "Global 授权仍未接入，当前显示的数值是明确标记的界面演示数据。", true);
        } else {
            flag(content, status == TelemetryStatus.STALE ? "车辆缓存已过期" : "中国区车辆缓存",
                    status.description() + "。点击刷新会重新连接车辆服务。", status == TelemetryStatus.STALE);
        }
        LinearLayout vehicle = card(content);
        vehicle.addView(text(state.name, 18, INK, true));
        gap(vehicle, 4);
        vehicle.addView(text(state.id + " · " + status.description(), 12, SUBTLE, false));
        gap(vehicle, 15);
        LinearLayout metrics = horizontal();
        LinearLayout soc = vertical(), range = vertical();
        soc.addView(text("电池 SOC",12,SUBTLE,false));
        soc.addView(text(VehicleSnapshot.metric(state.batteryPercent, "%"),34,INK,true));
        range.addView(text("剩余续航",12,SUBTLE,false));
        range.addView(text(VehicleSnapshot.metric(state.rangeKm, " km"),27,INK,true));
        metrics.addView(soc, new LinearLayout.LayoutParams(0,dp(76),1));
        metrics.addView(range, new LinearLayout.LayoutParams(0,dp(76),1));
        vehicle.addView(metrics);
        row(vehicle, "累计里程", VehicleSnapshot.metric(state.odometerKm, " km"));
        row(vehicle, "充电状态", state.charging);
        gap(vehicle, 12);
        vehicle.addView(action(region == Region.CHINA ? "刷新车辆状态" : "刷新演示数据", true,
                region == Region.CHINA ? this::refreshChina : () -> {
                    DiagnosticLog.add("Global demo refresh: no network request");
                    Toast.makeText(this, "Global 尚未接入 · 无车辆网络请求", Toast.LENGTH_SHORT).show();
                    render(); refreshWidgets();
                }));
        LinearLayout exterior = card(content);
        exterior.addView(text("车辆外观与开闭状态", 17, INK, true));
        exterior.addView(new VehicleDiagramView(this), new LinearLayout.LayoutParams(-1, dp(170)));
        row(exterior, "门锁",state.lock);
        row(exterior, "车门",state.doors);
        row(exterior, "车窗",state.windows);
        row(exterior, "尾门",state.tailgate);
        LinearLayout remote = card(content);
        boolean chinaConnected = region == Region.CHINA && SecureSessionStore.hasSession(this);
        remote.addView(text(chinaConnected ? "远程控制 · 手动确认" : "远程控制 · 暂不可用", 17, INK, true));
        gap(remote, 5);
        remote.addView(text(chinaConnected
                ? "空调启动/停止与解锁已接入设备验证通道。每次写操作都需要确认，网络不确定时不会自动重试。"
                : "请先连接中国区车辆；Global 控制接口仍未接入。",12,SUBTLE,false));
        gap(remote,11);
        remote.addView(action(chinaConnected ? "打开远程控制" : "查看连接设置", false,
                chinaConnected ? this::openChinaLive : () -> { page = 2; render(); }));
        TextView ts = text("数据源: " + state.source + "  |  " + TelemetryStatus.classify(
                state.isDemo, state.source, state.timestampMillis, System.currentTimeMillis(), 15L * 60L * 1000L).description(),11,SUBTLE,false);
        content.addView(ts);
    }
    private void carDetails() {
        VehicleSnapshot v = currentSnapshot();
        sectionHeader(content, "车辆与接口", "当前模式: " + region.chinese);
        LinearLayout r = card(content);
        r.addView(text("车辆列表",17,INK,true));
        gap(r,9);row(r,"车辆",v.name);row(r,"数据源",v.source);
        gap(r,11);
        r.addView(action(region == Region.CHINA ? "发现 / 选择车辆" : "Global 车辆发现未接入",false,
                region == Region.CHINA ? this::openChinaLive : () -> disabledCommand("Global 车辆发现")));
        LinearLayout m = card(content);
        m.addView(text("中国区 / 全球区 API 接入情况",17,INK,true));
        gap(m,8);
        row(m,"Account Login",region == Region.CHINA ? "DEVICE_VERIFICATION" : "MAPPING_REQUIRED");
        row(m,"Vehicle Discovery",CapabilityMatrix.vehicleDiscovery(region).name());
        row(m,"Battery / Range",CapabilityMatrix.battery(region).name());
        row(m,"Exterior / Odometer",CapabilityMatrix.exterior(region).name());
        row(m,"C3 Live Stream","MAPPING_REQUIRED");
        row(m,"PCCS","MAPPING_REQUIRED");
        row(m,"Remote Commands",CapabilityMatrix.remoteCommands(region).name());
        flag(content,"说明","真实 C3/PCCS、账户 Token 不能通过替换地区域名直接接通。新接口必须经过数据结构与权限验证后才能标注 VERIFIED_LIVE。",false);
    }
    private void settings() {
        sectionHeader(content, "设置与诊断", "所有操作可反馈，便于在设备上收集测试结果");
        LinearLayout r = card(content);
        r.addView(text("区域与会话",17,INK,true));gap(r,8);
        row(r,"当前地区",region.chinese);
        row(r,"本地会话",AppState.sessionState(this,region));
        gap(r,10);
        r.addView(action("切换 China / Global",true,()->showRegionChooser(false)));
        gap(r,8);
        if (region == Region.CHINA) {
            r.addView(action("中国区实车连接 / 登录",false,()->{
                openChinaLive();
            }));
        } else {
            r.addView(action("Global 登录（后续阶段）",false,()->info("Global 尚未接入", "当前 alpha 优先完成中国区真实链路，Global 保持独立会话并在后续阶段接入。")));
        }
        LinearLayout diag = card(content);
        diag.addView(text("运行诊断",17,INK,true));gap(diag,8);
        row(diag,"编译版本","0.3.0-alpha2");
        row(diag,"China 车辆 API","DEVICE_VERIFICATION");
        row(diag,"China 核心远控","MANUAL_CONFIRM / NO_RETRY");
        row(diag,"R8 混淆","DEFERRED");
        row(diag,"当前数据",currentSnapshot().isDemo ? "DEMO" : "CACHED / LIVE REFRESH");gap(diag,10);
        diag.addView(action("复制诊断日志",true,()->{
            DiagnosticLog.add("Diagnostic log copied");
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(cm!=null) cm.setPrimaryClip(ClipData.newPlainText("Polestar Companion diagnostics",DiagnosticLog.export()));
            Toast.makeText(this,"已复制诊断日志",Toast.LENGTH_SHORT).show();
        }));
        gap(diag,8);
        diag.addView(action("查看日志",false,()->info("诊断日志",DiagnosticLog.export())));
        flag(content,"软件身份","独立开发的非官方 Android 研究分支；原始第三方 APK 仅用于功能结构分析，未复制账户密钥或控制指令。",false);
    }
    private void info(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("知道了",null).show();
    }
    private void disabledCommand(String action) {
        DiagnosticLog.add("Blocked remote command: " + action);
        info("远控未开放", action + "：" + OperationState.DISABLED.label + "。本次点击没有发送车辆控制指令。");
    }
    private VehicleSnapshot currentSnapshot() {
        if (region == Region.CHINA) {
            VehicleSnapshot cached = VehicleSnapshotStore.loadChina(this);
            if (cached != null) return cached;
        }
        return new DemoRepository().snapshot(region);
    }
    private void openChinaLive() {
        DiagnosticLog.add("Open China vehicle screen");
        startActivity(new Intent(this, ChinaLiveActivity.class));
    }
    private void refreshChina() {
        if (!SecureSessionStore.hasSession(this)) {
            openChinaLive();
            return;
        }
        Toast.makeText(this, "正在刷新车辆状态…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                ChinaPolestarApi api = new ChinaPolestarApi(this);
                List<ChinaPolestarApi.VehicleInfo> vehicles = api.discoverVehicles();
                VehicleSnapshot snapshot = api.readVehicle(vehicles.get(0));
                VehicleSnapshotStore.saveChina(this, snapshot);
                runOnUiThread(() -> {
                    DiagnosticLog.add("Dashboard live refresh completed");
                    Toast.makeText(this, "车辆状态已更新", Toast.LENGTH_SHORT).show();
                    render();
                    refreshWidgets();
                });
            } catch (Exception e) {
                runOnUiThread(() -> info("刷新失败", safeMessage(e)));
            }
        }, "china-dashboard-refresh").start();
    }
    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? e.getClass().getSimpleName() : message;
    }
    private void showInitialRegionChooser() { showRegionChooser(true); }
    private void showRegionChooser(boolean firstLaunch) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Select Polestar Region")
            .setMessage("请选择车辆所在地区。当前提供离线模拟界面，China 实验链路已接入；Global 仍未接入。")
            .setCancelable(!firstLaunch)
            .setItems(new String[]{"China · 中国", "Global · 全球"}, (dialog, which)->{
                region = which==0 ? Region.CHINA : Region.GLOBAL;
                AppState.setRegion(this, region);
                DiagnosticLog.add("Region selected: " + region.english);
                refreshWidgets(); page=0; render();
            });
        if (!firstLaunch) builder.setNegativeButton("取消", null);
        builder.show();
    }
    private void refreshWidgets() {
        AppWidgetManager m=AppWidgetManager.getInstance(this);
        ComponentName big=new ComponentName(this,VehicleWidgetProvider.class);
        ComponentName small=new ComponentName(this,CompactWidgetProvider.class);
        for(int id:m.getAppWidgetIds(big)) WidgetRenderer.update(this,m,id,false);
        for(int id:m.getAppWidgetIds(small)) WidgetRenderer.update(this,m,id,true);
    }
}
