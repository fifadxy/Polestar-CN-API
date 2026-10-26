package io.github.polestarcn.companion;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.github.polestarcn.companion.auth.ReferenceSignatureProvider;
import java.util.*;

/** Control is the launch page; the region chooser and all sample telemetry have been removed. */
public final class MainActivity extends FragmentActivity {
  private VehicleCoordinator model;
  private final Runnable listener = this::render;
  private LinearLayout shell, body;
  private HorizontalSwipeRefreshLayout controlRefresh;
  private int page = 0, ink, muted, paper, surface, line;
  private final int orange = Color.rgb(255, 107, 0);
  private Typeface regular, bold;
  private boolean authPending;
  private int authGeneration;
  private boolean handlingWidget;
  private BiometricPrompt biometric;
  private EditText phone, password;
  private String inputPhone = "", inputPassword = "";
  private int campHours = 1;
  private boolean campKeepAir;
  private boolean resumeRender;
  private final Handler displayHandler = new Handler(Looper.getMainLooper());
  private final Runnable displayTick = new Runnable() {
    public void run() {
      if (model != null && page == 0 && model.snapshot != null
          && ("开启".equals(model.snapshot.climate) || CampModeManager.active(MainActivity.this))) render();
      displayHandler.postDelayed(this, 60_000L);
    }
  };
  private static final int PICK_APK = 32, NOTIFICATIONS = 33;
  private static final String HOME_DEFAULTS =
      "odometer,ta_distance,tm_distance,ta_speed,tm_speed,tm_consumption,temperature,aqi,charge_power";

  private String tr(String zh, String en) {
    return AppPreferences.tr(this, zh, en);
  }

  private String status(String s) {
    return AppPreferences.status(this, s);
  }

  private int dp(float f) {
    return (int) (f * getResources().getDisplayMetrics().density + .5f);
  }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    AppState.setRegion(this, Region.CHINA);
    regular = getResources().getFont(R.font.lato_regular);
    bold = getResources().getFont(R.font.lato_bold);
    model = VehicleCoordinator.get(this);
    model.listen(listener);
    render();
    handleWidgetIntent(getIntent());
    if (model.hasSession() && !model.busy) model.refresh(null);
    if (AppPreferences.enabled(this))
      try {
        ResidentService.apply(this);
      } catch (RuntimeException e) {
        DiagnosticLog.add("Resident start restricted");
      }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleWidgetIntent(intent);
  }
  private void handleWidgetIntent(Intent i) {
    if (i == null || !i.hasExtra("widget_action") || handlingWidget) return;
    String request = i.getStringExtra("widget_action");
    i.removeExtra("widget_action");
    if (!"lock-choice".equals(request) && !"climate-choice".equals(request)) return;
    page = 0;
    render();
    if (!model.hasSession() || model.selected == null || model.busy) {
      toast(tr("请先连接车辆，刷新后重试", "Connect and refresh the vehicle before controlling"));
      if (model.hasSession() && model.selected == null && !model.busy) model.refresh(null);
      return;
    }
    handlingWidget = true;
    Haptics.tap(this);
    directControl("lock-choice".equals(request) ? "lock" : "climate");
    handlingWidget = false;
  }

  @Override
  protected void onDestroy() {
    displayHandler.removeCallbacks(displayTick);
    model.remove(listener);
    authGeneration++; authPending = false;
    if (biometric != null) biometric.cancelAuthentication();
    super.onDestroy();
  }

  @Override protected void onStart() {
    super.onStart();
    if (model != null) { model.listen(listener); if (resumeRender) { resumeRender = false; render(); } }
  }
  @Override protected void onResume() {
    super.onResume(); displayHandler.removeCallbacks(displayTick); displayHandler.postDelayed(displayTick, 60_000L);
  }
  @Override protected void onPause() {
    displayHandler.removeCallbacks(displayTick); super.onPause();
  }

  @Override
  protected void onStop() {
    if (model != null) model.remove(listener);
    resumeRender = true;
    if (biometric != null && authPending) {
      authGeneration++; authPending = false; biometric.cancelAuthentication();
    }
    super.onStop();
  }

  @Override
  public void onBackPressed() {
    if (page != 0) {
      page = 0;
      render();
    } else super.onBackPressed();
  }

  private LinearLayout column() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  private LinearLayout row() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.HORIZONTAL);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  private GradientDrawable bg(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private GradientDrawable oval(int color) {
    GradientDrawable d = new GradientDrawable();
    d.setShape(GradientDrawable.OVAL);
    d.setColor(color);
    return d;
  }

  private int viTrack() {
    return Color.parseColor(AppPreferences.dark(this) ? "#3A3C3E" : "#D9DAD6");
  }

  private int viSoft() {
    return Color.parseColor(AppPreferences.dark(this) ? "#242628" : "#F1F0EC");
  }

  private void styleViProgress(ProgressBar bar) {
    bar.setProgressTintList(android.content.res.ColorStateList.valueOf(orange));
    bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(viTrack()));
  }

  private void styleViSeekBar(SeekBar bar) {
    bar.setProgressTintList(android.content.res.ColorStateList.valueOf(orange));
    bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(viTrack()));
    bar.setThumbTintList(android.content.res.ColorStateList.valueOf(orange));
    bar.setSplitTrack(false);
  }

  private void stylePolestarVehicle(ImageView car) {
    if (!AppPreferences.dark(this)) {
      car.clearColorFilter();
      return;
    }
    ColorMatrix matrix =
        new ColorMatrix(
            new float[] {
              .14f, 0f, 0f, 0f, 10f,
              0f, .14f, 0f, 0f, 10f,
              0f, 0f, .14f, 0f, 10f,
              0f, 0f, 0f, 1f, 0f
            });
    car.setColorFilter(new ColorMatrixColorFilter(matrix));
  }

  private String vehicleDisplayName(VehicleSnapshot snapshot) {
    if (model != null && model.selected != null) return model.selected.displayLabel();
    return snapshot == null ? tr("连接车辆", "Connect vehicle") : snapshot.name;
  }

  private java.util.List<DisplayMetric> controlMetrics() {
    java.util.List<DisplayMetric> chosen =
        MetricSelection.read(this, "home_metrics", HOME_DEFAULTS);
    if (chosen.size() > 6) chosen = new java.util.ArrayList<>(chosen.subList(0, 6));
    return chosen;
  }

  private void addAmbientGlow(LinearLayout parent) {
    ViAmbientGlowView glow = new ViAmbientGlowView(this);
    glow.setTag("vi_ambient_glow");
    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(3));
    gp.leftMargin = dp(18);
    gp.rightMargin = dp(18);
    parent.addView(glow, gp);
  }

  private TextView text(String s, int sp, int color, boolean heavy) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(sp);
    v.setTextColor(color);
    v.setTypeface(heavy ? bold : regular);
    v.setIncludeFontPadding(false);
    return v;
  }

  private void gap(LinearLayout p, int size) {
    p.addView(new View(this), new LinearLayout.LayoutParams(1, dp(size)));
  }

  private TextView button(String s, Runnable r) {
    TextView t = text(s, 14, ink, true);
    t.setGravity(Gravity.CENTER);
    t.setMinHeight(dp(48));
    t.setPadding(dp(12), dp(8), dp(12), dp(8));
    t.setBackground(bg(surface, 8));
    click(t, r);
    t.setFocusable(true);
    t.setClickable(true);
    return t;
  }

  private void click(View view, Runnable action) {
    view.setHapticFeedbackEnabled(false);
    view.setOnTouchListener(
        (v, event) -> {
          if (!v.isEnabled()) return false;
          if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            v.animate().cancel();
            v.animate().scaleX(.975f).scaleY(.975f).alpha(.86f).setDuration(70L).start();
          } else if (event.getActionMasked() == MotionEvent.ACTION_UP
              || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            v.animate().cancel();
            v.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(150L)
                .setInterpolator(new android.view.animation.OvershootInterpolator(.55f))
                .start();
          }
          return false;
        });
    view.setOnClickListener(v -> { Haptics.tap(this); action.run(); });
  }

  private TextView utility(String title, boolean active, boolean warning, Runnable action) {
    TextView view = button(title + (warning ? " !" : ""), action);
    view.setTextSize(12); view.setSingleLine(true);
    view.setPadding(dp(6), dp(4), dp(6), dp(4));
    GradientDrawable background = bg(active ? orange : surface, 8);
    background.setStroke(dp(1), active ? orange : line);
    view.setBackground(background);
    view.setTextColor(active ? Color.rgb(21, 22, 23) : ink);
    return view;
  }

  private TextView tireUtility(VehicleSnapshot v) {
    boolean problem = v != null && v.tires.hasProblem();
    boolean good = v != null && v.tires.allGood();
    String label = v == null ? tr("胎压 --", "Tires --")
        : good ? "ALL GOOD" : problem ? tr("胎压 !", "Tires !") : tr("胎压 --", "Tires --");
    TextView view = button(label, this::tireDialog);
    view.setTextSize(11); view.setSingleLine(true); view.setPadding(dp(6), dp(4), dp(6), dp(4));
    GradientDrawable background = bg(problem ? orange : surface, 8);
    background.setStroke(dp(problem ? 2 : 1), problem || good ? orange : line);
    view.setBackground(background);
    view.setTextColor(problem ? Color.rgb(21,22,23) : good ? orange : muted);
    view.setTag("tire_button");
    return view;
  }

  private void palette() {
    boolean dark = AppPreferences.dark(this);
    boolean simple = UiSkin.SIMPLE.equals(UiSkin.get(this));
    ink = Color.parseColor(dark ? "#F4F4F2" : simple ? "#111417" : "#18191A");
    muted = Color.parseColor(dark ? "#A5A7A5" : simple ? "#747876" : "#626560");
    paper = Color.parseColor(dark ? "#0F1011" : simple ? "#F7F6F2" : "#F3F3F1");
    surface = Color.parseColor(dark ? "#1E2021" : "#FFFFFF");
    line = Color.parseColor(dark ? "#343638" : simple ? "#E5E3DC" : "#DCDDD7");
    getWindow().setStatusBarColor(paper);
    getWindow().setNavigationBarColor(paper);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            dark
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }

  private void render() {
    if (isFinishing() || isDestroyed()) return;
    if (phone != null) {
      inputPhone = phone.getText().toString();
      inputPassword = password.getText().toString();
      phone = null;
      password = null;
    }
    palette();
    setTheme(AppPreferences.dark(this) ? R.style.AppThemeDark : R.style.AppThemeLight);
    shell = column();
    shell.setBackgroundColor(paper);
    boolean controlDashboard = page == 0 && model.hasSession();
    if (!controlDashboard) {
      LinearLayout header = row();
      header.setPadding(dp(20), 0, dp(18), 0);
      ImageView brand = new ImageView(this);
      brand.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
      brand.setScaleType(ImageView.ScaleType.FIT_CENTER);
      brand.setContentDescription("PsAssistant");
      header.addView(brand, new LinearLayout.LayoutParams(dp(30), dp(30)));
      TextView name = text("PsAssistant", 17, ink, true);
      LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1);
      np.leftMargin = dp(12);
      header.addView(name, np);
      shell.addView(header, new LinearLayout.LayoutParams(-1, dp(56)));
    } else {
      shell.addView(new View(this), new LinearLayout.LayoutParams(1, dp(6)));
    }
    body = column();
    body.setPadding(dp(18), 0, dp(18), 0);
    shell.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
    if (page == 2) settings();
    else if (!model.hasSession()) login();
    else if (page == 1) details();
    else controls();
    LinearLayout nav = row();
    nav.setPadding(dp(14), dp(6), dp(14), dp(6));
    String[] names = UiSkin.SIMPLE.equals(UiSkin.get(this))
        ? new String[]{tr("车辆", "Vehicle"), tr("车况", "Status"), tr("设置", "Settings")}
        : new String[]{tr("车辆控制", "Control"), tr("车况详情", "Details"), tr("设置", "Settings")};
    for (int i = 0; i < names.length; i++) {
      final int tab = i;
      TextView t =
          button(
              names[i],
              () -> {
                page = tab;
                render();
              });
      t.setBackground(bg(i == page ? surface : paper, 6));
      t.setTextColor(i == page ? orange : muted);
      nav.addView(t, new LinearLayout.LayoutParams(0, -1, 1));
    }
    shell.addView(nav, new LinearLayout.LayoutParams(-1, dp(60)));
    setContentView(shell);
  }

  private LinearLayout scrollBody() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    LinearLayout v = column();
    v.setPadding(0, dp(12), 0, dp(20));
    scroll.addView(v);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    return v;
  }

  private EditText field(String hint, boolean secret) {
    EditText e = new EditText(this);
    e.setSingleLine(true);
    e.setTextSize(17);
    e.setTextColor(ink);
    e.setHintTextColor(muted);
    e.setTypeface(regular);
    e.setHint(hint);
    e.setInputType(
        secret
            ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            : InputType.TYPE_CLASS_PHONE);
    e.setPadding(dp(14), 0, dp(14), 0);
    e.setBackground(bg(surface, 8));
    e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
    e.setAutofillHints(secret ? View.AUTOFILL_HINT_PASSWORD : View.AUTOFILL_HINT_USERNAME);
    return e;
  }

  private void login() {
    LinearLayout v = scrollBody();
    gap(v, 20);
    v.addView(text(tr("你的车，\n一目了然。", "Your car.\nAt a glance."), 34, ink, false));
    gap(v, 12);
    v.addView(text(tr("中国区账号 · 手机号与密码登录", "China account · Phone and password"), 14, muted, false));
    gap(v, 28);
    phone = field(tr("手机号", "Phone number"), false);
    phone.setText(inputPhone);
    password = field(tr("密码", "Password"), true);
    password.setText(inputPassword);
    v.addView(phone, new LinearLayout.LayoutParams(-1, dp(54)));
    gap(v, 12);
    v.addView(password, new LinearLayout.LayoutParams(-1, dp(54)));
    gap(v, 20);
    TextView sign =
        button(
            model.busy ? tr("正在连接…", "Connecting…") : tr("登录并连接车辆", "Sign in and connect"),
            () -> {
              String p = phone.getText().toString().trim(), s = password.getText().toString();
              if (p.isEmpty() || s.isEmpty()) {
                toast(tr("请输入手机号和密码", "Enter phone number and password"));
                return;
              }
              ReferenceSignatureProvider ref = new ReferenceSignatureProvider(this);
              if (!ref.isReady()) {
                info(
                    tr("导入模板 APK", "Import reference APK"),
                    tr(
                        "请选择你提供的原模板 APK，完成一次性兼容配置后即可使用手机号和密码登录。",
                        "Select your original reference APK once to configure account"
                            + " compatibility, then sign in with your phone and password."));
                return;
              }
              inputPassword = "";
              password.setText("");
              model.login(p, s);
            });
    sign.setBackground(bg(orange, 8));
    sign.setTextColor(Color.BLACK);
    sign.setEnabled(!model.busy);
    v.addView(sign, new LinearLayout.LayoutParams(-1, dp(54)));
    gap(v, 12);
    v.addView(text(model.message, 13, muted, false));
    gap(v, 20);
    v.addView(button(tr("导入模板 APK（仅首次）", "Import reference APK (once)"), this::importReference));
    gap(v, 8);
    v.addView(
        text(
            tr(
                "仅在本机加密保存兼容参数与登录会话，密码不保存。",
                "Compatibility data and session stay encrypted on this phone. Your password is not"
                    + " stored."),
            12,
            muted,
            false));
  }

  private void controls() {
    LinearLayout outerBody = body;
    HorizontalSwipeRefreshLayout refresh = new HorizontalSwipeRefreshLayout(this);
    controlRefresh = refresh;
    refresh.setTag("control_pull_refresh");
    refresh.setColorSchemeColors(orange);
    refresh.setProgressBackgroundColorSchemeColor(surface);
    refresh.setDistanceToTriggerSync(dp(72));
    refresh.setOnChildScrollUpCallback(
        (parent, child) -> controlCanScrollUp(child));
    refresh.setOnRefreshListener(this::requestPullRefresh);
    refresh.setRefreshing(model.busy && model.activeAction.isEmpty());

    LinearLayout content = column();
    content.setTag("control_pull_content");
    refresh.addView(
        content,
        new SwipeRefreshLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    outerBody.addView(refresh, new LinearLayout.LayoutParams(-1, -1));

    body = content;
    try {
      String skin = UiSkin.get(this);
      if (UiSkin.SIMPLE.equals(skin)) controlsSimple();
      else controlsDetailed();
    } finally {
      body = outerBody;
    }
  }

  private void requestPullRefresh() {
    HorizontalSwipeRefreshLayout refresh = controlRefresh;
    if (model.busy) {
      if (refresh != null) refresh.setRefreshing(model.activeAction.isEmpty());
      return;
    }
    Haptics.tap(this);
    DiagnosticLog.add("Vehicle pull-to-refresh");
    if (refresh != null) refresh.setRefreshing(true);
    model.refresh(
        () -> {
          if (controlRefresh != null) controlRefresh.setRefreshing(false);
        });
  }

  private boolean controlCanScrollUp(View view) {
    if (view.canScrollVertically(-1)) return true;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        if (controlCanScrollUp(group.getChildAt(i))) return true;
      }
    }
    return false;
  }

  private void controlsDetailed() {
    VehicleSnapshot v = model.snapshot;
    int screenH = getResources().getConfiguration().screenHeightDp;
    boolean tiny = screenH < 610;
    body.setPadding(0, dp(1), 0, dp(2));

    LinearLayout selector = row();
    TextView title = text(vehicleDisplayName(v), tiny ? 14 : 16, ink, true);
    title.setMaxLines(2);
    title.setGravity(Gravity.CENTER_VERTICAL);
    click(title, this::chooseVehicle);
    selector.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
    TextView refresh = button(model.busy ? "…" : "↻", () -> model.refresh(null));
    refresh.setEnabled(!model.busy);
    refresh.setContentDescription(tr("刷新车辆状态", "Refresh vehicle status"));
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(44), dp(44));
    rp.leftMargin = dp(6);
    selector.addView(refresh, rp);
    body.addView(selector, new LinearLayout.LayoutParams(-1, dp(tiny ? 48 : 52)));

    LinearLayout headline = row();
    TextView soc = text(v == null ? "--%" : VehicleSnapshot.metric(v.batteryPercent, "%"),
        tiny ? 28 : 32, orange, false);
    headline.addView(soc, new LinearLayout.LayoutParams(0, -1, 1));
    LinearLayout range = column();
    range.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    range.addView(text(v == null ? "-- km" : VehicleSnapshot.metric(v.rangeKm, " km"),
        tiny ? 18 : 21, ink, false));
    if (AppPreferences.visible(this, "show_charging", true))
      range.addView(text(v == null ? tr("未知", "Unknown") : status(v.charging), 10, muted, false));
    headline.addView(range);
    body.addView(headline, new LinearLayout.LayoutParams(-1, dp(tiny ? 42 : 48)));

    java.util.List<DisplayMetric> chosen = controlMetrics();
    if (!chosen.isEmpty()) addMetricGrid(body, v, chosen, true);

    LinearLayout carBox = column();
    ImageView car = new ImageView(this);
    car.setTag("polestar_vehicle");
    car.setImageResource(R.drawable.vehicle_top);
    car.setScaleType(ImageView.ScaleType.FIT_CENTER);
    stylePolestarVehicle(car);
    car.setContentDescription("Polestar 2");
    click(car, this::exteriorDialog);
    carBox.addView(car, new LinearLayout.LayoutParams(-1, 0, 1));
    addAmbientGlow(carBox);
    body.addView(carBox, new LinearLayout.LayoutParams(-1, 0, 1));

    boolean windowsOpen = v != null && v.windows != null && v.windows.contains("个开启");
    boolean exteriorOpen = ExteriorView.hasOpening(v);
    boolean camping = CampModeManager.active(this) || "STOPPING".equals(CampModeManager.phase(this));

    LinearLayout utilityOne = row();
    TextView windows = utility(
        tr("车窗 · ", "Windows · ") + (v == null ? tr("未知", "Unknown") : status(v.windows)),
        windowsOpen, windowsOpen, () -> directControl("windows"));
    windows.setTag("windows_control_button");
    utilityOne.addView(windows, new LinearLayout.LayoutParams(0, dp(40), 1));
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(40), 1); bp.leftMargin = dp(5);
    TextView exterior = utility(tr("门窗", "Body"), exteriorOpen, exteriorOpen, this::exteriorDialog);
    exterior.setTag("exterior_button");
    utilityOne.addView(exterior, bp);
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(40), 1); tp.leftMargin = dp(5);
    utilityOne.addView(tireUtility(v), tp);
    body.addView(utilityOne);

    gap(body, 4);
    LinearLayout utilityTwo = row();
    TextView camp = utility(tr("△ 露营", "△ Camp"), camping, false, this::campDialog);
    camp.setTag("camp_button");
    utilityTwo.addView(camp, new LinearLayout.LayoutParams(0, dp(40), 1));
    TextView signals = utility(tr("鸣笛 / 闪灯", "Honk / flash"), false, false, this::signalMenu);
    signals.setTag("signal_control_button");
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(40), 1); sp.leftMargin = dp(6);
    utilityTwo.addView(signals, sp);
    body.addView(utilityTwo);

    gap(body, tiny ? 3 : 5);
    LinearLayout first = row(), second = row();
    body.addView(first, new LinearLayout.LayoutParams(-1, dp(tiny ? 62 : 66)));
    gap(body, 4);
    body.addView(second, new LinearLayout.LayoutParams(-1, dp(tiny ? 62 : 66)));
    tile(first, tr("门锁", "Door lock"), v == null ? tr("未知", "Unknown") : status(v.lock),
        "lock", () -> directControl("lock"));
    tile(first, tr("驻车空调", "Climate"),
        ClimateRuntime.label(this, v, AppPreferences.get(this, "vehicle", "")),
        "climate", () -> directControl("climate"));
    tile(second, tr("尾箱", "Tailgate"), v == null ? tr("未知", "Unknown") : status(v.tailgate),
        "trunk", () -> directControl("trunk"));
    tile(second, tr("空气净化", "Air"), v == null ? tr("未知", "Unknown") : status(v.air),
        "air", () -> directControl("air"));

    String stamp = v == null || v.timestampMillis <= 0 ? tr("更新时间未知", "Update time unknown")
        : tr("更新 ", "Updated ") + android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis);
    TextView footer = text((model.message.isEmpty() ? "" : model.message + " · ")
        + (AppPreferences.visible(this, "show_update", true) ? stamp : "")
        + (camping ? tr(" · 露营托管中", " · Camping active") : ""), 9, muted, false);
    footer.setGravity(Gravity.CENTER_VERTICAL);
    footer.setSingleLine(true);
    footer.setEllipsize(android.text.TextUtils.TruncateAt.END);
    footer.setContentDescription(model.message);
    click(footer, () -> info(tr("车辆状态", "Vehicle status"), model.message + "\n" + stamp));
    body.addView(footer, new LinearLayout.LayoutParams(-1, dp(tiny ? 16 : 20)));
  }


  private void controlsSimple() {
    VehicleSnapshot v = model.snapshot;
    LinearLayout p = body;
    int screenH = getResources().getConfiguration().screenHeightDp;
    boolean compact = screenH < 680;
    boolean tiny = screenH < 590;
    p.setPadding(0, dp(1), 0, dp(2));

    LinearLayout head = row();
    LinearLayout titles = column();
    TextView vehicle = text(vehicleDisplayName(v), tiny ? 15 : 17, ink, true);
    vehicle.setMaxLines(2);
    vehicle.setGravity(Gravity.CENTER_VERTICAL);
    click(vehicle, this::chooseVehicle);
    titles.addView(vehicle, new LinearLayout.LayoutParams(-1, 0, 1));
    head.addView(titles, new LinearLayout.LayoutParams(0, -1, 1));
    TextView refresh = text(model.busy ? "…" : "↻", 22, ink, false);
    refresh.setGravity(Gravity.CENTER);
    refresh.setMinHeight(dp(42)); refresh.setMinWidth(dp(42));
    click(refresh, () -> model.refresh(null)); refresh.setEnabled(!model.busy);
    head.addView(refresh, new LinearLayout.LayoutParams(dp(42), dp(42)));
    p.addView(head, new LinearLayout.LayoutParams(-1, dp(tiny ? 42 : 46)));
    gap(p, tiny ? 2 : 4);

    LinearLayout hero = row();
    LinearLayout stats = column(); stats.setGravity(Gravity.CENTER_VERTICAL);
    String rangeNumber = v == null || v.rangeKm == null ? "--" : String.valueOf(v.rangeKm);
    LinearLayout rangeLine = row(); rangeLine.setGravity(Gravity.BOTTOM);
    rangeLine.addView(text(rangeNumber, tiny ? 32 : 38, ink, true));
    TextView km = text(" km", tiny ? 14 : 16, ink, false);
    LinearLayout.LayoutParams kmp = new LinearLayout.LayoutParams(-2, -2);
    kmp.bottomMargin = dp(4); rangeLine.addView(km, kmp);
    stats.addView(rangeLine);
    stats.addView(text(tr("预计续航", "Est. Range"), 10, muted, false));
    gap(stats, 4);
    ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    bar.setTag("vi_progress"); bar.setMax(100);
    bar.setProgress(v == null || v.batteryPercent == null ? 0 : v.batteryPercent);
    styleViProgress(bar);
    stats.addView(bar, new LinearLayout.LayoutParams(-1, dp(6)));
    gap(stats, 4);
    stats.addView(text(v == null ? "--%" : VehicleSnapshot.metric(v.batteryPercent, "%"),
        tiny ? 17 : 20, ink, true));
    hero.addView(stats, new LinearLayout.LayoutParams(0, -1, .44f));

    LinearLayout carColumn = column();
    ImageView car = new ImageView(this);
    car.setTag("polestar_vehicle");
    car.setImageResource(R.drawable.vehicle_top);
    car.setScaleType(ImageView.ScaleType.FIT_CENTER);
    stylePolestarVehicle(car);
    car.setContentDescription("Polestar 2");
    click(car, this::exteriorDialog);
    carColumn.addView(car, new LinearLayout.LayoutParams(-1, 0, 1));
    addAmbientGlow(carColumn);
    hero.addView(carColumn, new LinearLayout.LayoutParams(0, -1, .56f));
    p.addView(hero, new LinearLayout.LayoutParams(-1, dp(tiny ? 106 : compact ? 120 : 134)));
    gap(p, tiny ? 2 : 4);

    LinearLayout controlCard = column();
    controlCard.setPadding(dp(6), dp(5), dp(6), dp(4));
    controlCard.setBackground(bg(surface, 16)); controlCard.setElevation(dp(2));
    LinearLayout actions = row();
    evAction(actions, tr("门锁", "Lock"), v == null ? tr("未知", "Unknown") : status(v.lock),
        R.drawable.ic_lock, "lock");
    evAction(actions, tr("空调", "Climate"),
        ClimateRuntime.label(this, v, AppPreferences.get(this, "vehicle", "")),
        R.drawable.ic_climate, "climate");
    evAction(actions, tr("净化", "Air"), v == null ? tr("未知", "Unknown") : status(v.air),
        R.drawable.ic_air, "air");
    evAction(actions, tr("尾箱", "Trunk"), v == null ? tr("未知", "Unknown") : status(v.tailgate),
        R.drawable.ic_tailgate, "trunk");
    controlCard.addView(actions, new LinearLayout.LayoutParams(-1, -1));
    p.addView(controlCard, new LinearLayout.LayoutParams(-1, dp(tiny ? 78 : 88)));
    gap(p, tiny ? 2 : 4);

    LinearLayout statusCard = row();
    statusCard.setPadding(dp(6), dp(4), dp(6), dp(4));
    statusCard.setBackground(bg(surface, 14));
    boolean locked = v != null && ("已上锁".equals(v.lock) || "已锁".equals(v.lock));
    boolean windowsClosed = v != null && "全部关闭".equals(v.windows);
    int tireState = v == null || v.tires == null || !v.tires.hasAnyReport()
        ? 0 : v.tires.hasProblem() ? -1 : v.tires.allGood() ? 1 : 0;
    evStatus(statusCard, locked ? tr("已上锁", "Locked")
        : v == null ? tr("门锁未知", "Lock unknown") : status(v.lock),
        locked ? 1 : v == null ? 0 : -1, () -> directControl("lock"));
    evStatus(statusCard, windowsClosed ? tr("车窗关闭", "Windows Closed")
        : v == null ? tr("车窗未知", "Windows unknown") : status(v.windows),
        windowsClosed ? 1 : v == null ? 0 : -1, () -> directControl("windows"));
    evStatus(statusCard, v == null ? tr("iTPMS未知", "iTPMS unknown")
        : v.tires.summary(AppPreferences.english(this)), tireState, this::tireDialog);
    p.addView(statusCard, new LinearLayout.LayoutParams(-1, dp(tiny ? 40 : 44)));
    gap(p, tiny ? 2 : 4);

    java.util.List<DisplayMetric> chosen = controlMetrics();
    if (!chosen.isEmpty()) {
      LinearLayout metrics = column();
      metrics.setPadding(dp(6), dp(3), dp(6), dp(3));
      metrics.setBackground(bg(surface, 12));
      addMetricGrid(metrics, v, chosen, true);
      p.addView(metrics);
      gap(p, tiny ? 2 : 4);
    }

    LinearLayout infoRow = row();
    String chargeSub = v == null ? tr("等待数据", "Waiting") : status(v.charging);
    infoRow.addView(evCompactInfoCard("⚡", tr("充电", "Charging"), chargeSub, () -> {
      page = 1; render();
    }), new LinearLayout.LayoutParams(0, dp(tiny ? 56 : 62), 1));
    LinearLayout.LayoutParams campInfo = new LinearLayout.LayoutParams(0, dp(tiny ? 56 : 62), 1);
    campInfo.leftMargin = dp(6);
    infoRow.addView(evCompactInfoCard("△", tr("露营", "Camping"),
        CampModeManager.active(this) ? tr("托管中", "Active") : tr("未开启", "Off"),
        this::campDialog), campInfo);
    p.addView(infoRow);
    gap(p, tiny ? 2 : 4);

    LinearLayout utilities = row();
    boolean opening = ExteriorView.hasOpening(v);
    utilities.addView(utility(tr("门窗", "Body"), opening, opening, this::exteriorDialog),
        new LinearLayout.LayoutParams(0, dp(40), 1));
    LinearLayout.LayoutParams tyreLp = new LinearLayout.LayoutParams(0, dp(40), 1);
    tyreLp.leftMargin = dp(5);
    utilities.addView(tireUtility(v), tyreLp);
    LinearLayout.LayoutParams sig = new LinearLayout.LayoutParams(0, dp(40), 1);
    sig.leftMargin = dp(5);
    utilities.addView(utility(tr("鸣笛 / 闪灯", "Honk / flash"), false, false, this::signalMenu), sig);
    p.addView(utilities);

    gap(p, 3);
    String stamp = v == null || v.timestampMillis <= 0 ? tr("更新时间未知", "Update time unknown")
        : tr("更新 ", "Updated ") + android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis);
    TextView footer = text((model.message.isEmpty() ? "" : model.message + " · ") + stamp, 9, muted, false);
    footer.setSingleLine(true); footer.setEllipsize(android.text.TextUtils.TruncateAt.END);
    footer.setGravity(Gravity.CENTER_VERTICAL);
    p.addView(footer, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  private void evAction(LinearLayout parent, String label, String state, int iconRes, String control) {
    LinearLayout item = column(); item.setGravity(Gravity.CENTER); item.setPadding(dp(2), 0, dp(2), 0);
    FrameLayout circle = new FrameLayout(this); circle.setBackground(oval(viSoft()));
    ImageView icon = new ImageView(this); icon.setImageResource(iconRes); icon.setColorFilter(ink); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    circle.addView(icon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
    item.addView(circle, new LinearLayout.LayoutParams(dp(48), dp(48)));
    TextView title = text(label, 10, ink, true); title.setGravity(Gravity.CENTER); title.setMaxLines(1); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2); tp.topMargin = dp(3); item.addView(title, tp);
    TextView detail = text(state, 8, muted, false); detail.setGravity(Gravity.CENTER); detail.setMaxLines(1); detail.setEllipsize(android.text.TextUtils.TruncateAt.END); item.addView(detail);
    boolean enabled = !model.busy && model.selected != null; item.setEnabled(enabled); item.setAlpha(enabled ? 1f : .55f);
    click(item, () -> directControl(control)); item.setFocusable(true); item.setContentDescription(label + " · " + state);
    parent.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
  }

  private void evStatus(LinearLayout parent, String label, int state, Runnable action) {
    LinearLayout item = row(); item.setGravity(Gravity.CENTER);
    int circleColor = state < 0 ? orange : state > 0 ? ink : muted;
    int markColor = state < 0 ? Color.BLACK : paper;
    TextView dot = text(state > 0 ? "✓" : state < 0 ? "!" : "•", 10, markColor, true);
    dot.setGravity(Gravity.CENTER); dot.setBackground(oval(circleColor)); item.addView(dot, new LinearLayout.LayoutParams(dp(23), dp(23)));
    TextView value = text(label, 9, ink, false); value.setMaxLines(1); value.setEllipsize(android.text.TextUtils.TruncateAt.END); value.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0, -2, 1); vp.leftMargin = dp(5); item.addView(value, vp);
    click(item, action); parent.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
  }

  private LinearLayout evCompactInfoCard(String symbol, String title, String subtitle, Runnable action) {
    LinearLayout card = row(); card.setPadding(dp(9), dp(6), dp(8), dp(6)); card.setBackground(bg(surface, 14)); card.setElevation(dp(1));
    TextView icon = text(symbol, 18, orange, true); icon.setGravity(Gravity.CENTER); icon.setBackground(oval(viSoft()));
    card.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
    LinearLayout copy = column(); copy.setGravity(Gravity.CENTER_VERTICAL);
    TextView a = text(title, 12, ink, true); TextView b = text(subtitle, 9, muted, false); b.setSingleLine(true); b.setEllipsize(android.text.TextUtils.TruncateAt.END);
    copy.addView(a); copy.addView(b);
    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1); cp.leftMargin = dp(8); card.addView(copy, cp);
    TextView arrow = text("›", 22, muted, false); arrow.setGravity(Gravity.CENTER); card.addView(arrow, new LinearLayout.LayoutParams(dp(18), -1));
    click(card, action); card.setFocusable(true); return card;
  }

  private void addMetricGrid(LinearLayout parent, VehicleSnapshot snapshot,
      java.util.List<DisplayMetric> chosen, boolean compact) {
    boolean dense = chosen.size() > 9;
    int columns = dense ? 4 : chosen.size() <= 4 ? 2 : 3;
    for (int i = 0; i < chosen.size(); i += columns) {
      LinearLayout metricRow = row();
      for (int j = i; j < Math.min(chosen.size(), i + columns); j++) {
        DisplayMetric metric = chosen.get(j);
        LinearLayout cell = column(); cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setPadding(dp(dense ? 3 : 5), 0, dp(dense ? 2 : 4), 0);
        TextView label = text(metric.shortLabel(AppPreferences.english(this)), dense ? 8 : compact ? 9 : 10, muted, false);
        label.setMaxLines(1); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView value = text(metric.value(this, snapshot, false, AppPreferences.english(this)), dense ? 11 : compact ? 12 : 14, ink, false);
        value.setMaxLines(1);
        cell.addView(label); cell.addView(value);
        cell.setContentDescription(metric.label(AppPreferences.english(this)) + " · " + metric.value(this, snapshot, true, AppPreferences.english(this)));
        click(cell, () -> info(metric.label(AppPreferences.english(this)), metric.value(snapshot, true, AppPreferences.english(this))));
        metricRow.addView(cell, new LinearLayout.LayoutParams(0, -1, 1));
      }
      parent.addView(metricRow, new LinearLayout.LayoutParams(-1, dp(compact ? 31 : 36)));
    }
  }

  private void tile(LinearLayout row, String label, String state, String icon, Runnable action) {
    VehicleSnapshot snapshot = model.snapshot;
    String raw = snapshot == null ? "未知" : icon.equals("lock") ? snapshot.lock
        : icon.equals("trunk") ? snapshot.tailgate : icon.equals("climate") ? snapshot.climate : snapshot.air;
    boolean active = "开启".equals(raw) || "未锁".equals(raw) || "已打开".equals(raw) || "未关严".equals(raw);
    boolean known = active || "关闭".equals(raw) || "已上锁".equals(raw) || "已锁".equals(raw) || "已关闭".equals(raw);
    String pending = model.activeAction;
    boolean sending = model.busy && (icon.equals("lock") ? pending.equals("lock") || pending.equals("unlock")
        : icon.equals("trunk") ? pending.startsWith("trunk") : pending.startsWith(icon));
    int foreground = known ? Color.rgb(21, 22, 23) : ink;
    LinearLayout t = column();
    t.setTag("control_tile");
    t.setGravity(Gravity.CENTER_VERTICAL);
    t.setPadding(dp(12), dp(6), dp(8), dp(6));
    GradientDrawable card = bg(known ? active ? orange : Color.WHITE : surface, 8);
    if (sending) card.setStroke(dp(2), orange);
    t.setBackground(card);
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -1, 1);
    tp.setMargins(0, 0, row.getChildCount() == 0 ? dp(8) : 0, 0);
    row.addView(t, tp);
    ImageView image = new ImageView(this);
    int id =
        icon.equals("lock")
            ? R.drawable.ic_lock
            : icon.equals("trunk")
                ? R.drawable.ic_tailgate
                : icon.equals("climate") ? R.drawable.ic_climate : R.drawable.ic_air;
    image.setImageResource(id);
    image.setColorFilter(foreground);
    LinearLayout top = row();
    top.addView(image, new LinearLayout.LayoutParams(dp(20), dp(20)));
    TextView a = text(label, 14, foreground, false);
    a.setMaxLines(1);
    a.setEllipsize(android.text.TextUtils.TruncateAt.END);
    LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, -2, 1); ap.leftMargin = dp(6);
    top.addView(a, ap);
    t.addView(top);
    gap(t, 2);
    String detail = "";
    if (snapshot != null && icon.equals("climate")) {
      detail = ClimateRuntime.detail(this, snapshot, AppPreferences.get(this, "vehicle", ""));
    } else if (snapshot != null && icon.equals("air")) {
      detail = "PM2.5 " + MileageSnapshot.decimalText(snapshot.environment.pm25UgM3, "")
          + " · AQI " + (snapshot.environment.aqi == null ? "--" : snapshot.environment.aqi);
    }
    String feedback = sending ? model.operation == OperationState.SENDING
        ? tr("正在发送…", "Sending…") : tr("等待车辆确认", "Waiting for vehicle") : state;
    if (!sending && icon.equals("climate") && "开启".equals(raw)) feedback = tr("运行中", "Running");
    TextView b = text(feedback, 12, known ? foreground : muted, false);
    b.setMaxLines(detail.isEmpty() ? 2 : 1);
    t.addView(b);
    if (!detail.isEmpty() && !sending) {
      TextView support = text(detail, 11, known ? foreground : muted, false);
      support.setMaxLines(1); support.setEllipsize(android.text.TextUtils.TruncateAt.END);
      t.addView(support);
    }
    t.setEnabled(!model.busy && model.selected != null);
    t.setAlpha(t.isEnabled() ? 1f : .55f);
    click(t, action);
    t.setContentDescription(label + ", " + feedback + (detail.isEmpty() ? "" : ", " + detail));
    t.setFocusable(true);
  }

  private void chooseVehicle() {
    if (model.busy) return;
    if (model.cars.isEmpty()) {
      model.refresh(null);
      return;
    }
    String[] labels = new String[model.cars.size()];
    for (int i = 0; i < labels.length; i++) labels[i] = model.cars.get(i).displayLabel();
    new HapticDialogBuilder(this)
        .setTitle(tr("选择车辆", "Select vehicle"))
        .setItems(labels, (d, w) -> model.choose(model.cars.get(w)))
        .show();
  }

  private void directControl(String control) {
    if (model.busy || authPending || model.selected == null) return;
    String action = ControlAction.next(control, model.snapshot);
    if (action == null) {
      toast(tr("状态未知或未确认，请刷新车况后再试", "State unknown or unconfirmed. Refresh before controlling"));
      return;
    }
    String zh, en;
    switch (action) {
      case "unlock": zh = "解锁车门"; en = "Unlock doors"; break;
      case "lock": zh = "锁定车门"; en = "Lock doors"; break;
      case "trunk-open": zh = "打开尾箱"; en = "Open tailgate"; break;
      case "trunk-close": zh = "关闭尾箱"; en = "Close tailgate"; break;
      case "climate-start": zh = "启动空调 · 22°C"; en = "Start climate · 22°C"; break;
      case "climate-stop": zh = "关闭空调"; en = "Stop climate"; break;
      case "air-start": zh = "开启空气净化"; en = "Start purification"; break;
      case "air-stop": zh = "关闭空气净化"; en = "Stop purification"; break;
      case "windows-open": zh = "打开全部车窗"; en = "Open all windows"; break;
      case "windows-close": zh = "关闭全部车窗"; en = "Close all windows"; break;
      default: zh = action; en = action;
    }
    authenticate(tr(zh, en), action);
  }

  private void signalMenu() {
    if (model.busy || authPending || model.selected == null) return;
    new HapticDialogBuilder(this)
        .setTitle(tr("鸣笛 / 闪灯", "Honk / flash"))
        .setItems(
            new String[] {
              tr("鸣笛", "Honk"),
              tr("闪灯", "Flash"),
              tr("鸣笛并闪灯", "Honk + flash")
            },
            (dialog, which) -> {
              String[] actions = {"honk", "flash", "honk-flash"};
              String[] zh = {"鸣笛", "闪灯", "鸣笛并闪灯"};
              String[] en = {"Honk", "Flash", "Honk + flash"};
              authenticate(tr(zh[which], en[which]), actions[which]);
            })
        .setNegativeButton(tr("取消", "Cancel"), null)
        .show();
  }

  private void authenticate(String label, String action) {
    final CampSession campTarget = "camp-stop".equals(action) ? CampModeManager.session(this) : null;
    if (authPending || model.busy || (model.selected == null && campTarget == null)) return;
    int available =
        BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
    if (available != BiometricManager.BIOMETRIC_SUCCESS) {
      info(
          tr("指纹不可用", "Biometrics unavailable"),
          tr(
              "请先在手机设置中录入指纹或启用强生物识别。当前操作未发送。",
              "Enroll a fingerprint or strong biometric in your phone settings. No command was"
                  + " sent."));
      return;
    }
    final String vin = campTarget == null ? model.selected.vin : campTarget.vin;
    authPending = true;
    final int generation = ++authGeneration;
    biometric =
        new BiometricPrompt(
            this,
            command -> runOnUiThread(command),
            new BiometricPrompt.AuthenticationCallback() {
              @Override
              public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                runOnUiThread(
                    () -> {
                      if (generation != authGeneration) return;
                      authGeneration++;
                      authPending = false;
                      if (model.busy) return;
                      if (campTarget != null) {
                        if (CampModeManager.current(MainActivity.this, campTarget.id)) model.stopCamp();
                      } else if (model.selected != null && vin.equals(model.selected.vin)) {
                        if ("camp-start".equals(action)) model.startCamp(campHours, campKeepAir);
                        else model.command(action);
                      }
                    });
              }

              @Override
              public void onAuthenticationError(int code, CharSequence error) {
                runOnUiThread(
                    () -> {
                      if (generation != authGeneration) return;
                      authGeneration++;
                      authPending = false;
                      if (code != 5 && code != 10 && code != 13) toast(error.toString());
                    });
              }
            });
    biometric.authenticate(
        new BiometricPrompt.PromptInfo.Builder()
            .setTitle(tr("指纹验证", "Biometric verification"))
            .setSubtitle(label)
            .setDescription(tr("验证通过后发送车辆控制指令", "Verify to send this vehicle command"))
            .setNegativeButtonText(tr("取消", "Cancel"))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build());
  }

  private void detailRow(LinearLayout p, String key, String value) {
    LinearLayout r = row();
    r.setPadding(dp(12), dp(12), dp(12), dp(12));
    r.setBackground(bg(surface, 4));
    TextView a = text(key, 14, muted, false), b = text(value, 14, ink, false);
    b.setGravity(Gravity.RIGHT);
    r.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
    r.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    p.addView(r);
    gap(p, 3);
  }

  private void details() {
    LinearLayout p = scrollBody();
    p.addView(text(tr("车况详情", "Vehicle details"), 24, ink, false));
    gap(p, 20);
    VehicleSnapshot v = model.snapshot;
    if (v == null) {
      p.addView(text(tr("暂无车辆数据，请刷新", "No vehicle data. Refresh to connect."), 14, muted, false));
      return;
    }
    detailRow(p, tr("车辆", "Vehicle"), v.name);
    detailRow(p, "VIN", v.id);
    detailRow(p, tr("电量", "Battery"), VehicleSnapshot.metric(v.batteryPercent, "%"));
    detailRow(p, tr("续航", "Range"), VehicleSnapshot.metric(v.rangeKm, " km"));
    detailRow(p, tr("累计里程", "Odometer"), v.mileage.totalText());
    detailRow(p, tr("TM 手动复位里程", "TM · Manual trip"), MileageSnapshot.tripText(v.mileage.manualKm));
    detailRow(p, tr("TA 自动复位里程", "TA · Automatic trip"), MileageSnapshot.tripText(v.mileage.automaticKm));
    detailRow(p, "TA · AVG Speed", MileageSnapshot.decimalText(v.mileage.automaticAverageSpeed, " km/h"));
    detailRow(p, "TA · Average Consumption", MileageSnapshot.decimalText(v.mileage.automaticConsumption, " kWh/100 km"));
    detailRow(p, "TM · AVG Speed", MileageSnapshot.decimalText(v.mileage.manualAverageSpeed, " km/h"));
    detailRow(p, "TM · Average Consumption", MileageSnapshot.decimalText(v.mileage.manualConsumption, " kWh/100 km"));
    gap(p, 6);
    detailRow(p, tr("充电", "Charging"), status(v.charging));
    for (DisplayMetric metric : new DisplayMetric[]{DisplayMetric.CHARGE_CONNECTION, DisplayMetric.CHARGE_TYPE,
        DisplayMetric.CHARGE_POWER, DisplayMetric.CHARGE_CURRENT, DisplayMetric.CHARGE_ETA, DisplayMetric.POWER_STATUS})
      detailRow(p, metric.label(AppPreferences.english(this)), metric.value(v, true, AppPreferences.english(this)));
    detailRow(p, tr("门锁", "Door lock"), status(v.lock));
    detailRow(p, tr("车门", "Doors"), status(v.doors));
    detailRow(p, tr("车窗", "Windows"), status(v.windows));
    String[] positions = {tr("左前", "Front left"), tr("右前", "Front right"), tr("左后", "Rear left"), tr("右后", "Rear right")};
    for (int i = 0; i < 4; i++)
      detailRow(p, positions[i], tr("门 ", "Door ") + status(ExteriorSnapshot.label(v.exterior.state(false, i)))
          + " · " + tr("窗 ", "Window ") + status(ExteriorSnapshot.label(v.exterior.state(true, i))));
    detailRow(p, tr("尾箱", "Tailgate"), status(v.tailgate));
    detailRow(p, tr("胎压", "Tire status"), v.tires.summary(AppPreferences.english(this)));
    String[] tirePositions = {tr("左前胎", "Front-left tire"), tr("右前胎", "Front-right tire"),
        tr("左后胎", "Rear-left tire"), tr("右后胎", "Rear-right tire")};
    for (int i = 0; i < 4; i++) detailRow(p, tirePositions[i], v.tires.label(i, AppPreferences.english(this)));
    detailRow(p, tr("空调", "Climate"), status(v.climate));
    detailRow(p, tr("净化", "Purification"), status(v.air));
    detailRow(p, tr("车内温度", "Cabin temperature"), DisplayMetric.TEMPERATURE.value(v, true));
    detailRow(p, tr("空调目标温度", "Climate target"), DisplayMetric.TARGET_TEMPERATURE.value(v, true));
    detailRow(p, "AQI", DisplayMetric.AQI.value(v, true));
    detailRow(p, "PM2.5", DisplayMetric.PM25.value(v, true));
    detailRow(p, tr("空气数据更新", "Air data updated"), v.environment.airReportedAt <= 0 ? "--"
        : android.text.format.DateFormat.format("MM-dd HH:mm", v.environment.airReportedAt).toString());
    detailRow(
        p,
        tr("更新时间", "Last update"),
        v.timestampMillis <= 0 ? "--" : android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis).toString());
    gap(p, 8);
    MaintenanceSchedule.Status service = MaintenanceSchedule.status(
        this, AppPreferences.get(this, "vehicle", ""), v);
    detailRow(p, tr("保养", "Service"), service.summary(AppPreferences.english(this)));
    detailRow(p, tr("保养剩余时间", "Service time left"),
        service.configured ? service.remainingDays() + tr(" 天 · ", " d · ") + service.remainingTimePercent() + "%" : "--");
    detailRow(p, tr("保养剩余里程", "Service distance left"),
        service.configured && service.remainingKm() != null
            ? service.remainingKm() + " km · " + service.remainingDistancePercent() + "%" : "--");
    p.addView(button(tr("设置上次保养记录", "Set last service record"), this::maintenanceDialog));
    gap(p, 14);
    p.addView(button(tr("切换车辆", "Select vehicle"), this::chooseVehicle));
  }

  private void languageDialog() {
    new HapticDialogBuilder(this)
        .setTitle("语言 / Language")
        .setItems(
            new String[] {"跟随系统 / System", "简体中文", "English"},
            (d, w) -> {
              AppPreferences.set(this, "language", new String[] {"system", "zh", "en"}[w]);
              render();
              ResidentService.update(this);
            })
        .show();
  }


  private void skinDialog() {
    final String[] values = {UiSkin.SIMPLE, UiSkin.DETAILED};
    String[] labels = {
        tr("1 · 简洁风格", "1 · Simple"),
        tr("2 · 详细风格", "2 · Detailed")
    };
    new HapticDialogBuilder(this)
        .setTitle(tr("界面皮肤", "UI skin"))
        .setSingleChoiceItems(labels, UiSkin.index(this), (dialog, which) -> {
          UiSkin.set(this, values[which]);
          dialog.dismiss();
          render();
        })
        .show();
  }

  private void settings() {
    LinearLayout p = scrollBody();
    p.addView(text(tr("设置", "Settings"), 24, ink, false));
    gap(p, 20);
    p.addView(button(tr("程序语言 / Language", "App language / 语言"), this::languageDialog));
    gap(p, 8);
    p.addView(button(tr("界面皮肤 · ", "UI skin · ") + UiSkin.label(this, UiSkin.get(this)), this::skinDialog));
    gap(p, 8);
    p.addView(button(tr("露营模式", "Camping mode"), this::campDialog));
    gap(p, 8);
    p.addView(button(
        tr("保养设置 · 默认 365天 / 20,000km", "Service · default 365 d / 20,000 km"),
        this::maintenanceDialog));
    gap(p, 8);
    p.addView(button(
        tr("车辆控制页数据显示 · 最多 6 项", "Vehicle control data · up to 6"),
        () -> metricDialog("home_metrics", 6, HOME_DEFAULTS)));
    gap(p, 8);
    p.addView(button(tr("按键震动 · 开关与强度", "Button haptics · switch and strength"), this::hapticsDialog));
    gap(p, 8);
    p.addView(button(tr("Widget 数据显示", "Widget data fields"), () -> {
      String[] labels = {"4×2 · 最多 8", "2×2 · 最多 4", "4×1 · 最多 4", "2×1 · 最多 2", "Data 4×4 · 最多 8"};
      WidgetRenderer.Size[] sizes = {WidgetRenderer.Size.LARGE, WidgetRenderer.Size.SQUARE,
          WidgetRenderer.Size.WIDE, WidgetRenderer.Size.MINI, WidgetRenderer.Size.DATA};
      new HapticDialogBuilder(this).setTitle(tr("选择组件尺寸与数据容量", "Widget size and field capacity"))
          .setItems(labels, (d, w) -> metricDialog(WidgetRenderer.metricKey(sizes[w]),
              WidgetRenderer.capacity(sizes[w]), WidgetRenderer.defaults(sizes[w]))).show();
    }));
    gap(p, 8);
    p.addView(button(tr("手表界面预览 · 圆屏 / 方屏", "Watch UI preview · round / square"), this::watchPreview));
    gap(p, 8);
    p.addView(button(tr("Widget 语言 · 中文 / English", "Widget language · 中文 / English"),
        this::widgetLanguageDialog));
    gap(p, 8);
    p.addView(
        button(
            tr("日夜模式 · 两套皮肤通用", "Day / night · both skins"),
            () ->
                new HapticDialogBuilder(this)
                    .setTitle(tr("日夜模式", "Day / night mode"))
                    .setItems(
                        new String[] {tr("跟随系统", "System"), tr("浅色", "Light"), tr("深色", "Dark")},
                        (d, w) -> {
                          AppPreferences.set(
                              this, "theme", new String[] {"system", "light", "dark"}[w]);
                          render();
                        })
                    .show()));
    gap(p, 20);
    p.addView(text(tr("主页面显示内容", "HOME INFORMATION"), 17, ink, true));
    for (String[] field : new String[][] {
        {"show_car", "车辆图", "Vehicle image", "on"},
        {"show_charging", "充电状态", "Charging status", "on"},
        {"show_update", "数据更新时间", "Data update time", "on"}}) {
      Switch toggle = new Switch(this);
      toggle.setText(tr(field[1], field[2]));
      toggle.setTextColor(ink);
      toggle.setMinHeight(dp(46));
      toggle.setChecked(AppPreferences.visible(this, field[0], "on".equals(field[3])));
      toggle.setHapticFeedbackEnabled(false);
      toggle.setOnCheckedChangeListener((control, on) -> {
        Haptics.tap(this); AppPreferences.set(this, field[0], on ? "on" : "off");
      });
      p.addView(toggle);
    }
    gap(p, 18);
    p.addView(text(tr("桌面 Widget 外观", "WIDGET APPEARANCE"), 17, ink, true));
    p.addView(button(tr("背景样式", "Background style"), () ->
      new HapticDialogBuilder(this).setTitle(tr("背景样式", "Widget style"))
        .setSingleChoiceItems(new String[]{tr("不透明", "Opaque"), tr("透明", "Transparent")},
          "opaque".equals(AppPreferences.get(this, "widget_style", "transparent")) ? 0 : 1,
          (dialog, index) -> {
            AppPreferences.set(this, "widget_style", index == 0 ? "opaque" : "transparent");
            WidgetRenderer.refreshAll(this);
            dialog.dismiss();
          }).show()));
    widgetSlider(p, tr("透明度", "Opacity"), "widget_opacity", 58);
    widgetSlider(p, tr("磨砂玻璃程度（模拟）", "Frost intensity (simulated)"), "widget_frost", 28);
    p.addView(text(tr("磨砂为渐变视觉模拟，不会实时模糊桌面壁纸。",
        "Frost is a gradient simulation; Android widgets cannot live-blur wallpaper."),
        12, muted, false));
    gap(p, 20);
    Switch keep = new Switch(this);
    keep.setText(tr("常驻车辆状态通知", "Persistent vehicle notification"));
    keep.setTextColor(ink);
    keep.setTextSize(15);
    keep.setMinHeight(dp(52));
    keep.setChecked(AppPreferences.enabled(this));
    keep.setOnCheckedChangeListener(
        (b, on) -> {
          Haptics.tap(this);
          if (on
              && Build.VERSION.SDK_INT >= 33
              && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                  != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[] {Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS);
            return;
          }
          setResident(on);
        });
    p.addView(keep);
    p.addView(
        text(
            tr(
                "显示电量、充电、空调／净化和车辆数据更新时间。静默通知；系统约每 30 分钟刷新，省电限制可能延迟。锁屏隐藏详细内容。",
                "Shows battery, charging, climate / air and data update time. Silent; Android"
                    + " schedules updates about every 30 minutes. Battery restrictions may delay"
                    + " updates. Details are hidden on the lock screen."),
            12,
            muted,
            false));
    gap(p, 12);
    p.addView(
        button(
            tr("后台与电池设置", "Background and battery settings"),
            () ->
                startActivity(
                    new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + getPackageName())))));
    gap(p, 16);
    p.addView(button(tr("导入模板 APK", "Import reference APK"), this::importReference));
    gap(p, 8);
    p.addView(
        button(
            tr("复制诊断日志", "Copy diagnostic log"),
            () -> {
              ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
              cm.setPrimaryClip(ClipData.newPlainText("Diagnostics", DiagnosticLog.export()));
              toast(tr("已复制", "Copied"));
            }));
    gap(p, 8);
    p.addView(
        button(
            tr("退出登录", "Sign out"),
            () -> {
              if (model.busy) {
                toast(tr("请等待当前操作完成", "Wait for the current operation"));
                return;
              }
              new HapticDialogBuilder(this)
                  .setMessage(tr("退出并清除本机登录会话？", "Sign out and clear this phone's session?"))
                  .setPositiveButton(
                      tr("退出", "Sign out"),
                      (d, w) -> {
                        inputPhone = "";
                        inputPassword = "";
                        model.logout();
                        page = 0;
                        render();
                      })
                  .setNegativeButton(tr("取消", "Cancel"), null)
                  .show();
            }));
    gap(p, 20);
    p.addView(
        text(
            "v" + BuildConfig.VERSION_NAME + " · " + tr("独立开发的非官方客户端", "Independent unofficial client"),
            12,
            muted,
            false));
    p.addView(text("Lato · SIL Open Font License", 12, muted, false));
  }

  private void maintenanceDialog() {
    String vehicleKey = AppPreferences.get(this, "vehicle", "");
    VehicleSnapshot v = model.snapshot;
    java.time.LocalDate savedDate = MaintenanceSchedule.lastDate(this, vehicleKey);
    Integer savedKm = MaintenanceSchedule.lastOdometerKm(this, vehicleKey);
    MaintenanceSchedule.Status current = MaintenanceSchedule.status(this, vehicleKey, v);

    LinearLayout panel = column();
    panel.setPadding(dp(18), dp(8), dp(18), dp(4));
    panel.addView(text(
        tr("默认周期：365天 / 20,000km。到达任一条件即提示保养。该记录仅保存在本机。",
            "Default interval: 365 days / 20,000 km. Either limit triggers service. This record stays on this phone."),
        13, muted, false));
    gap(panel, 12);

    EditText date = field(tr("上次保养日期 YYYY-MM-DD", "Last service date YYYY-MM-DD"), false);
    date.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
    date.setText(savedDate == null ? "" : savedDate.toString());
    panel.addView(date, new LinearLayout.LayoutParams(-1, dp(52)));
    gap(panel, 8);

    EditText odo = field(tr("上次保养里程 km", "Odometer at last service km"), false);
    odo.setInputType(InputType.TYPE_CLASS_NUMBER);
    if (savedKm != null) odo.setText(String.valueOf(savedKm));
    else if (v != null && v.odometerKm != null) odo.setHint(tr("当前 ", "Current ") + v.odometerKm + " km");
    panel.addView(odo, new LinearLayout.LayoutParams(-1, dp(52)));
    gap(panel, 10);
    panel.addView(text(current.detail(AppPreferences.english(this)), 13, ink, false));

    AlertDialog dialog = new HapticDialogBuilder(this)
        .setTitle(tr("保养记录", "Service record"))
        .setView(panel)
        .setPositiveButton(tr("保存", "Save"), null)
        .setNegativeButton(tr("关闭", "Close"), null)
        .create();
    dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
      Haptics.tap(this);
      try {
        String dateRaw = date.getText().toString().trim();
        if (dateRaw.isEmpty()) {
          MaintenanceSchedule.clear(this, vehicleKey);
          toast(tr("已清除保养记录", "Service record cleared"));
        } else {
          java.time.LocalDate parsed = java.time.LocalDate.parse(dateRaw);
          String kmRaw = odo.getText().toString().trim();
          Integer km = kmRaw.isEmpty() ? (v == null ? null : v.odometerKm) : Integer.valueOf(kmRaw);
          MaintenanceSchedule.save(this, vehicleKey, parsed, km);
          toast(tr("保养记录已保存", "Service record saved"));
        }
        dialog.dismiss();
        render();
      } catch (Exception e) {
        toast(tr("日期或里程格式不正确", "Invalid date or odometer"));
      }
    }));
    dialog.show();
  }

  private void widgetLanguageDialog() {
    new HapticDialogBuilder(this).setTitle("Widget 语言 / Language")
      .setItems(new String[]{"跟随系统 / System", "简体中文", "English"},
          (d, index) -> {
            AppPreferences.set(this, "widget_language",
                new String[]{"system", "zh", "en"}[index]);
            WidgetRenderer.refreshAll(this);
            render();
          }).show();
  }

  private void metricDialog(String key, int capacity, String defaults) {
    DisplayMetric[] values = DisplayMetric.values();
    java.util.List<DisplayMetric> selected = MetricSelection.read(this, key, defaults);
    String[] labels = new String[values.length];
    boolean[] checked = new boolean[values.length];
    for (int i = 0; i < values.length; i++) {
      labels[i] = values[i].label(AppPreferences.english(this)); checked[i] = selected.contains(values[i]);
    }
    new HapticDialogBuilder(this).setTitle(tr("选择数据显示 · 最多 ", "Choose fields · up to ") + capacity)
        .setMultiChoiceItems(labels, checked, (dialog, which, on) -> {
          if (on && !selected.contains(values[which])) {
            if (selected.size() >= capacity) {
              ((AlertDialog) dialog).getListView().setItemChecked(which, false);
              toast(tr("此尺寸最多显示 ", "This size supports up to ") + capacity + tr(" 项", " fields"));
              return;
            }
            selected.add(values[which]);
          } else if (!on) selected.remove(values[which]);
        }).setPositiveButton(tr("保存", "Save"), (d, w) -> {
          MetricSelection.save(this, key, selected); WidgetRenderer.refreshAll(this); render();
        }).setNegativeButton(tr("取消", "Cancel"), null).show();
  }

  private void watchPreview() {
    LinearLayout panel = column(); panel.setPadding(dp(16), 0, dp(16), dp(8));
    WatchPreviewView preview = new WatchPreviewView(this, model.snapshot);
    LinearLayout shapes = row();
    shapes.addView(button(tr("圆屏", "Round"), () -> preview.setRound(true)), new LinearLayout.LayoutParams(0, dp(48), 1));
    shapes.addView(button(tr("方屏", "Square"), () -> preview.setRound(false)), new LinearLayout.LayoutParams(0, dp(48), 1));
    panel.addView(shapes); panel.addView(preview, new LinearLayout.LayoutParams(-1, dp(250)));
    panel.addView(text(tr("OPPO Watch X2 · 圆屏 466×466。此处为布局预览，手表安装包与实机兼容性仍需单独验证。",
        "OPPO Watch X2 · 466×466 round display. Layout preview; a watch APK and device compatibility need separate verification."), 12, muted, false));
    new HapticDialogBuilder(this).setTitle(tr("手表布局预览", "Watch layout preview")).setView(panel)
        .setPositiveButton(tr("关闭", "Close"), null).show();
  }

  private void tireDialog() {
    VehicleSnapshot v = model.snapshot;
    if (v == null) return;
    LinearLayout panel = column(); panel.setPadding(dp(16), dp(8), dp(16), dp(8));
    panel.addView(new TirePressureView(this, v.tires), new LinearLayout.LayoutParams(-1,
        dp(Math.min(380, Math.max(280, getResources().getConfiguration().screenHeightDp - 230)))));
    panel.addView(text(tr(
        "橙色和 ! 表示该轮胎需要检查。本页按 iTPMS 告警状态展示，不显示未验证的具体 kPa。",
        "Orange / ! means that tyre needs attention. This view follows iTPMS warning state and does not show unverified kPa."),
        12, muted, false));
    new HapticDialogBuilder(this).setTitle(tr("胎压", "Tire status"))
        .setView(panel).setPositiveButton(tr("关闭", "Close"), null).show();
  }

  private void exteriorDialog() {
    VehicleSnapshot v = model.snapshot;
    if (v == null) return;
    LinearLayout panel = column(); panel.setPadding(dp(16), dp(8), dp(16), dp(8));
    panel.addView(new ExteriorView(this, v), new LinearLayout.LayoutParams(-1,
        dp(Math.min(380, Math.max(280, getResources().getConfiguration().screenHeightDp - 230)))));
    panel.addView(text(tr("橙色 / ! 表示未关闭；-- 表示状态未知。", "Orange / ! means open or ajar; -- means unknown."), 12, muted, false));
    new HapticDialogBuilder(this).setTitle(tr("车门与车窗", "Doors and windows"))
        .setView(panel).setPositiveButton(tr("关闭", "Close"), null).show();
  }

  private SeekBar campSlider(LinearLayout parent, String label, int min, int max, int initial,
      String key, java.util.function.IntConsumer changed) {
    TextView caption = text(label + " · " + initial, 14, ink, false);
    parent.addView(caption);
    SeekBar bar = new SeekBar(this);
    bar.setTag("vi_slider");
    bar.setMax(max - min); bar.setProgress(initial - min);
    styleViSeekBar(bar);
    parent.addView(bar, new LinearLayout.LayoutParams(-1, dp(44)));
    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
        int value = min + progress; caption.setText(label + " · " + value);
        if (key != null) AppPreferences.set(MainActivity.this, key, String.valueOf(value));
        if (changed != null) changed.accept(value);
      }
      public void onStartTrackingTouch(SeekBar b) {}
      public void onStopTrackingTouch(SeekBar b) { Haptics.tap(MainActivity.this); }
    });
    return bar;
  }

  private void campDialog() {
    LinearLayout panel = column(); panel.setPadding(dp(20), dp(10), dp(20), dp(4));
    CampSession current = CampModeManager.session(this);
    boolean active = CampModeManager.active(this) || "STOPPING".equals(CampModeManager.phase(this));
    panel.addView(text(CampModeManager.status(this), 14, ink, false));
    if (active && current != null) {
      gap(panel, 12);
      panel.addView(text(current.vehicleName + "\n" + tr("结束 ", "Ends ")
          + android.text.format.DateFormat.format("MM-dd HH:mm", current.endsAt)
          + "\n" + tr("检查间隔 ", "Check interval ") + current.intervalMinutes + tr(" 分钟", " min")
          + " · " + tr("电量下限 ", "Battery limit ") + current.minimumBattery + "%"
          + (current.keepAir ? "\n" + tr("同时维持净化", "Maintains purification") : ""), 13, muted, false));
      long checked = CampModeManager.checkedAt(this);
      gap(panel, 8);
      panel.addView(text(tr("上次检查 ", "Last check ") + (checked == 0 ? "--"
          : android.text.format.DateFormat.format("MM-dd HH:mm", checked)), 12, muted, false));
      new HapticDialogBuilder(this).setTitle(tr("露营模式", "Camping mode")).setView(panel)
          .setPositiveButton(tr("结束托管", "End camping"), (d, w) ->
              authenticate(tr("结束露营，并停止托管的空调／净化", "End camping and stop managed climate / air"), "camp-stop"))
          .setNegativeButton(tr("关闭", "Close"), null).show();
      return;
    }
    gap(panel, 8);
    SeekBar hours = campSlider(panel, tr("时长（小时）", "Duration (hours)"), 1, 24, 1, null, null);
    campSlider(panel, tr("检查间隔（分钟）", "Check interval (min)"), 20, 60,
        CampModeManager.interval(this), "camp_interval", null);
    final AlertDialog[] dialog = {null};
    campSlider(panel, tr("电量下限（%）", "Battery limit (%)"), 5, 90,
        CampModeManager.minimum(this), "camp_minimum", value -> {
          if (dialog[0] != null) dialog[0].getButton(AlertDialog.BUTTON_POSITIVE)
              .setEnabled(model.selected != null && !model.busy && model.snapshot != null
                  && CampSession.batteryAllows(model.snapshot.batteryPercent, value));
        });
    CheckBox air = new CheckBox(this); air.setText(tr("同时维持空气净化", "Maintain air purification"));
    air.setTextColor(ink); air.setChecked(model.snapshot != null && "开启".equals(model.snapshot.air));
    click(air, () -> {});
    panel.addView(air);
    panel.addView(text(tr("低于电量下限时不可开启；检查发现低于下限则结束托管并尝试停止空调／净化。Android 省电策略可能延迟检查。",
        "Below-limit battery prevents starting. A below-limit check ends camping and attempts to stop managed climate / air. Android battery restrictions can delay checks."), 12, muted, false));
    dialog[0] = new HapticDialogBuilder(this).setTitle(tr("露营模式", "Camping mode")).setView(panel)
        .setPositiveButton(tr("验证并开启", "Verify and start"), (d, w) -> {
          campHours = hours.getProgress() + 1; campKeepAir = air.isChecked();
          authenticate(tr("开启露营空调托管", "Enable camping climate maintenance"), "camp-start");
        }).setNegativeButton(tr("取消", "Cancel"), null).create();
    dialog[0].show();
    dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(model.selected != null && !model.busy
        && model.snapshot != null && CampSession.batteryAllows(model.snapshot.batteryPercent, CampModeManager.minimum(this)));
  }

  private void widgetSlider(LinearLayout parent, String label, String key, int fallback) {
    TextView caption = text(label + " · " + AppPreferences.slider(this, key, fallback) + "%", 14, ink, false);
    parent.addView(caption);
    SeekBar bar = new SeekBar(this);
    bar.setTag("vi_slider");
    bar.setMax(100);
    bar.setProgress(AppPreferences.slider(this, key, fallback));
    styleViSeekBar(bar);
    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override public void onProgressChanged(SeekBar seek, int value, boolean user) {
        caption.setText(label + " · " + value + "%");
        if (user) {
          AppPreferences.set(MainActivity.this, key, Integer.toString(value));
          WidgetRenderer.refreshAll(MainActivity.this);
        }
      }
      @Override public void onStartTrackingTouch(SeekBar seek) {}
      @Override public void onStopTrackingTouch(SeekBar seek) { Haptics.tap(MainActivity.this); }
    });
    parent.addView(bar);
  }

  private void hapticsDialog() {
    LinearLayout panel = column(); panel.setPadding(dp(20), dp(12), dp(20), dp(8));
    Switch enabled = new Switch(this); enabled.setText(tr("按键震动", "Button haptics"));
    enabled.setTextColor(ink); enabled.setMinHeight(dp(48));
    enabled.setChecked(AppPreferences.visible(this, "haptics", true)); enabled.setHapticFeedbackEnabled(false);
    enabled.setOnCheckedChangeListener((view, on) -> {
      AppPreferences.set(this, "haptics", on ? "on" : "off"); Haptics.tap(this);
    });
    panel.addView(enabled);
    campSlider(panel, tr("震动强度（%）", "Strength (%)"), 1, 100,
        Math.max(1, AppPreferences.slider(this, "haptic_strength", 35)), "haptic_strength", null);
    panel.addView(button(tr("试一下", "Test vibration"), () -> {}));
    gap(panel, 12);
    panel.addView(text(tr("跟随系统触感开关。不支持振幅调节的手机将用短脉冲时长近似强弱。",
        "Respects system haptics. Devices without amplitude control use short pulse durations."), 12, muted, false));
    new HapticDialogBuilder(this).setTitle(tr("按键震动", "Button haptics")).setView(panel)
        .setPositiveButton(tr("完成", "Done"), null).show();
  }

  private void setResident(boolean enabled) {
    AppPreferences.set(this, "resident", enabled ? "on" : "off");
    try {
      ResidentService.apply(this);
    } catch (RuntimeException e) {
      AppPreferences.set(this, "resident", "off");
      toast(VehicleCoordinator.safe(e));
    }
    render();
  }

  @Override
  public void onRequestPermissionsResult(int r, String[] p, int[] g) {
    super.onRequestPermissionsResult(r, p, g);
    if (r == NOTIFICATIONS) {
      setResident(g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED);
    }
  }

  private void importReference() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.setType("*/*");
    i.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(i, PICK_APK);
  }

  @Override
  protected void onActivityResult(int r, int result, Intent data) {
    super.onActivityResult(r, result, data);
    if (r == PICK_APK && result == RESULT_OK && data != null && data.getData() != null) {
      final android.net.Uri uri = data.getData();
      toast(tr("正在验证模板…", "Verifying reference…"));
      new Thread(
              () -> {
                try {
                  new ReferenceSignatureProvider(this).importApk(uri);
                  runOnUiThread(() -> toast(tr("导入完成，可以登录", "Imported. Ready to sign in")));
                } catch (Exception e) {
                  runOnUiThread(
                      () -> info(tr("导入失败", "Import failed"), VehicleCoordinator.safe(e)));
                }
              },
              "reference-import")
          .start();
    }
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  private void info(String title, String s) {
    new HapticDialogBuilder(this)
        .setTitle(title)
        .setMessage(s)
        .setPositiveButton(tr("知道了", "OK"), null)
        .show();
  }
}
