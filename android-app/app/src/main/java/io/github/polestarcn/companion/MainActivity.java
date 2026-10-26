package io.github.polestarcn.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Lightweight native Android preview. This screen never sends vehicle commands. */
public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(25, 27, 30);
    private static final int SUBTLE = Color.rgb(99, 104, 112);
    private static final int PAPER = Color.rgb(247, 247, 245);
    private static final int LINE = Color.rgb(229, 230, 229);
    private static final int ACCENT = Color.rgb(240, 208, 58);
    private static final int GREEN = Color.rgb(16, 125, 77);
    private final VehicleRepository repository = new DemoRepository();
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
        VehicleSnapshot state = repository.snapshot(region);
        sectionHeader(content, "车辆概览", "REGION  /  " + region.english + "  ·  Android Dev v0.1.0-dev2");
        flag(content, "演示模式 · 未连接真实车辆", "当前全部 SOC、续航、里程和车辆状态均为模拟值。未执行登录或网络请求。", true);
        LinearLayout vehicle = card(content);
        vehicle.addView(text(state.name, 18, INK, true));
        gap(vehicle, 4);
        vehicle.addView(text("示例车辆 · 仅作界面和小组件验证", 12, SUBTLE, false));
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
        vehicle.addView(action("刷新演示数据", true, () -> {
            DiagnosticLog.add("Demo refresh: success, no network request");
            Toast.makeText(this, "模拟刷新成功 · 无车辆网络请求", Toast.LENGTH_SHORT).show();
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
        remote.addView(text("远程控制 · 暂不可用", 17, INK, true));
        gap(remote, 5);
        remote.addView(text("China / Global 的真实控制权限与方法映射未经验证，所有指令均被禁用。",12,SUBTLE,false));
        gap(remote,11);
        String[] actions={"车门上锁", "空调启动", "闪灯鸣笛"};
        for (String name:actions) {
            TextView blocked=action(name+"  ·  禁用",false,()->disabledCommand(name));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(7);
            remote.addView(blocked,p);
        }
        TextView ts = text("数据源: " + state.source + "  |  " + TelemetryStatus.classify(
                state.isDemo, state.source, state.timestampMillis, System.currentTimeMillis(), 15L * 60L * 1000L).description(),11,SUBTLE,false);
        content.addView(ts);
    }
    private void carDetails() {
        VehicleSnapshot v = repository.snapshot(region);
        sectionHeader(content, "车辆与接口", "当前模式: " + region.chinese);
        LinearLayout r = card(content);
        r.addView(text("车辆列表",17,INK,true));
        gap(r,9);row(r,"车辆",v.name);row(r,"数据源",v.source);
        gap(r,11);
        r.addView(action("选择车辆（仅一辆示例车）",false,()->{
            DiagnosticLog.add("Vehicle selector opened: demo only");
            new AlertDialog.Builder(this).setTitle("车辆选择").setItems(new String[]{v.name},(dialog,which)->{
                DiagnosticLog.add("Demo vehicle selected");
                Toast.makeText(this,"已选择示例车辆",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("取消",null).show();
        }));
        LinearLayout m = card(content);
        m.addView(text("中国区 / 全球区 API 接入情况",17,INK,true));
        gap(m,8);
        row(m,"Account Login","MAPPING_REQUIRED");
        row(m,"Vehicle Discovery","MAPPING_REQUIRED");
        row(m,"Battery / Range","DEMO_ONLY");
        row(m,"Exterior / Odometer","DEMO_ONLY");
        row(m,"C3 Live Stream","MAPPING_REQUIRED");
        row(m,"PCCS","MAPPING_REQUIRED");
        row(m,"Remote Commands","DISABLED");
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
        r.addView(action("登录（功能尚未接入）",false,()->{
            DiagnosticLog.add(region.english + " login: not implemented");
            info("登录未接入", "当前为离线演示版本，未获取或保存任何 China / Global 登录凭证。");
        }));
        LinearLayout diag = card(content);
        diag.addView(text("运行诊断",17,INK,true));gap(diag,8);
        row(diag,"编译版本","0.1.0-dev2");
        row(diag,"网络车辆 API","OFF");
        row(diag,"远程控制","OFF");
        row(diag,"R8 混淆","DEFERRED");
        row(diag,"模拟数据","ON");gap(diag,10);
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
    private void showInitialRegionChooser() { showRegionChooser(true); }
    private void showRegionChooser(boolean firstLaunch) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Select Polestar Region")
            .setMessage("请选择车辆所在地区。当前提供离线模拟界面，真实 API 登录尚未接入。")
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
