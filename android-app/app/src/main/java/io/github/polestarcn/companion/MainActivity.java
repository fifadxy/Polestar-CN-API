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
  private final Runnable listener = this::onModelChanged;
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
          && ("开启".equals(model.snapshot.climate) || CampModeManager.active(MainActivity.this))) onModelChanged();
      displayHandler.postDelayed(this, 60_000L);
    }
  };
  private static final int PICK_APK = 32, NOTIFICATIONS = 33;
  private static final String HOME_DEFAULTS =
      "vehicle_status,odometer,ta_distance,tm_distance,ta_speed,tm_speed,tm_consumption,temperature,aqi";

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
    if ("refresh".equals(request)) { page = 0; render(); model.refreshUser(null); return; }
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
    super.onResume();
    displayHandler.removeCallbacks(displayTick);
    displayHandler.postDelayed(displayTick, 60_000L);
  }

  @Override protected void onPause() {
    displayHandler.removeCallbacks(displayTick);
    super.onPause();
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
    if (AppPreferences.dark(this)) return Color.parseColor("#3A3C3E");
    return Color.parseColor(UiSkin.NIOLIKE.equals(UiSkin.get(this)) ? "#D8CDC8" : "#D9DAD6");
  }

  private int viSoft() {
    if (AppPreferences.dark(this)) return Color.parseColor("#242628");
    return Color.parseColor(UiSkin.NIOLIKE.equals(UiSkin.get(this)) ? "#F4F5F5" : "#F1F0EC");
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
    car.setScaleX(1.08f);
    car.setScaleY(1.08f);
    if (!AppPreferences.dark(this)) {
      car.clearColorFilter();
      return;
    }
    ColorMatrix matrix =
        new ColorMatrix(
            new float[] {
               .32f, 0f, 0f, 0f, 10f,
              0f, .32f, 0f, 0f, 10f,
              0f, 0f, .32f, 0f, 10f,
              0f, 0f, 0f, 1f, 0f
            });
    car.setColorFilter(new ColorMatrixColorFilter(matrix));
  }

  private String vehicleDisplayName(VehicleSnapshot snapshot) {
    String name = null;
    if (model != null && model.selected != null) {
      if (model.selected.modelName != null && !model.selected.modelName.trim().isEmpty())
        name = model.selected.modelName.trim();
      else if (model.selected.model != null && !model.selected.model.trim().isEmpty())
        name = model.selected.model.trim();
    }
    if ((name == null || name.isEmpty()) && snapshot != null) name = snapshot.name;
    if (name == null || name.trim().isEmpty()) return tr("连接车辆", "Connect vehicle");
    name = name.replaceAll("\\b(?:19|20)\\d{2}\\b", "").replaceAll("\\s{2,}", " ").trim();
    return name.isEmpty() ? "Polestar 2" : name;
  }

  private boolean fixedHomeMetric(DisplayMetric metric) {
    return metric == DisplayMetric.BATTERY
        || metric == DisplayMetric.RANGE
        || metric == DisplayMetric.CHARGE_CONNECTION
        || metric == DisplayMetric.CHARGE_TYPE
        || metric == DisplayMetric.CHARGE_POWER
        || metric == DisplayMetric.CHARGE_CURRENT
        || metric == DisplayMetric.CHARGE_ETA
        || metric == DisplayMetric.POWER_STATUS;
  }

  private java.util.List<DisplayMetric> controlMetrics() {
    java.util.List<DisplayMetric> chosen =
        MetricSelection.read(this, "home_metrics", HOME_DEFAULTS);
    chosen.removeIf(this::fixedHomeMetric);
    if (!AppPreferences.visible(this, "experimental_tire", false))
      chosen.remove(DisplayMetric.TIRE_STATUS);
    if (chosen.size() > 12) chosen = new java.util.ArrayList<>(chosen.subList(0, 12));
    return chosen;
  }

  private boolean chargingVisible(VehicleSnapshot v) {
    if (!AppPreferences.visible(this, "show_charging", true) || v == null) return false;
    if ("充电中".equals(v.charging) || "智能充电".equals(v.charging)) return true;
    Integer state = v.chargingDetails == null ? null : v.chargingDetails.powerStatus;
    return state != null && (state == 2 || state == 4);
  }

  private String chargingTitle(VehicleSnapshot v) {
    if (v == null) return tr("充电", "Charging");
    Integer state = v.chargingDetails == null ? null : v.chargingDetails.powerStatus;
    if (state != null && state == 2 && !"充电中".equals(v.charging))
      return tr("开始充电", "Starting charge");
    return tr("正在充电", "Charging");
  }

  private String chargingDetail(VehicleSnapshot v) {
    if (v == null) return "--";
    java.util.List<String> parts = new java.util.ArrayList<>();
    boolean en = AppPreferences.english(this);
    String type = v.chargingDetails.type(en);
    String power = v.chargingDetails.power(true);
    String current = v.chargingDetails.current(true);
    String eta = v.chargingDetails.timeToFull(en);
    if (!"未知".equals(type) && !"Unknown".equals(type) && !"未充电".equals(type)
        && !"Not charging".equals(type)) parts.add(type);
    if (!"--".equals(power)) parts.add(power);
    if (!"--".equals(current)) parts.add(current);
    if (!"--".equals(eta)) parts.add(tr("充满 ", "Full in ") + eta);
    return parts.isEmpty() ? status(v.charging) : android.text.TextUtils.join(" · ", parts).toString();
  }

  private String healthDisplayText() {
    VehicleSnapshot v = model.snapshot;
    if (v == null) return tr("车辆状态 --", "STATUS --");
    if (VehicleHealthUi.allGood(this, v.health)) return "ALL GOOD";
    if (VehicleHealthUi.hasProblem(this, v.health))
      return AppPreferences.english(this)
          ? "CHECK · " + VehicleHealthUi.issueCount(this, v.health)
          : "需检查 · " + VehicleHealthUi.issueCount(this, v.health) + "项";
    return VehicleHealthUi.summary(this, v.health, AppPreferences.english(this));
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
    view.setTextSize(12); view.setMaxLines(1); view.setEllipsize(android.text.TextUtils.TruncateAt.END);
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

  private TextView vehicleStatusUtility(VehicleSnapshot v) {
    VehicleHealthSnapshot h = v == null ? VehicleHealthSnapshot.unknown() : v.health;
    boolean problem = h.hasProblem();
    boolean good = h.allGood();
    String label =
        problem
            ? tr("车辆状态 !", "Vehicle !")
            : good ? "ALL GOOD" : tr("车辆状态 --", "Vehicle --");
    TextView view = button(label, this::vehicleStatusDialog);
    view.setTextSize(11);
    view.setSingleLine(true);
    view.setPadding(dp(6), dp(4), dp(6), dp(4));
    GradientDrawable background = bg(problem ? orange : surface, 8);
    background.setStroke(dp(problem ? 2 : 1), problem || good ? orange : line);
    view.setBackground(background);
    view.setTextColor(problem ? Color.rgb(21, 22, 23) : good ? orange : muted);
    view.setTag("vehicle_status_button");
    return view;
  }

  private void palette() {
    boolean dark = AppPreferences.dark(this);
    String skin = UiSkin.get(this);
    boolean simple = UiSkin.SIMPLE.equals(skin);
    boolean nio = UiSkin.NIOLIKE.equals(skin);
    ink = Color.parseColor(dark ? "#F4F4F2" : nio ? "#121313" : simple ? "#111417" : "#18191A");
    muted = Color.parseColor(dark ? "#A5A7A5" : nio ? "#857E7A" : simple ? "#747876" : "#626560");
    paper = Color.parseColor(dark ? "#0F1011" : nio ? "#E9D6CE" : simple ? "#F7F6F2" : "#F3F3F1");
    surface = Color.parseColor(dark ? "#1D1F20" : nio ? "#F9F3EF" : "#FFFFFF");
    line = Color.parseColor(dark ? "#343638" : nio ? "#E7D9D3" : simple ? "#E5E3DC" : "#DCDDD7");
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
    dashboardReady = false;
    dashboardBindings.clear();
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
      TextView name = text(getString(R.string.app_name), 17, ink, true);
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
    nav.setPadding(dp(14), dp(2), dp(14), dp(2));
    String[] names = UiSkin.DETAILED.equals(UiSkin.get(this))
        ? new String[]{tr("车辆控制", "Control"), tr("车况详情", "Details"), tr("设置", "Settings")}
        : new String[]{tr("车辆", "Vehicle"), tr("车况", "Status"), tr("设置", "Settings")};
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
    shell.addView(nav, new LinearLayout.LayoutParams(-1, dp(getResources().getConfiguration().screenWidthDp > getResources().getConfiguration().screenHeightDp ? 48 : 60)));
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
    if ("preview".equals(BuildConfig.BUILD_TYPE)) {
      v.addView(text(tr("并行验证版：旧版应用和数据保留，请在此重新登录。",
          "Parallel preview: your original app and data remain. Sign in here separately."), 12, muted, false));
      gap(v, 10);
    }

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
      if (UiSkin.NIOLIKE.equals(skin)) controlsNioLike();
      else if (UiSkin.SIMPLE.equals(skin)) controlsSimple();
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
    model.refreshUser(
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

  private void controlsDetailed() { buildDashboard(false); }
  private void controlsSimple() { buildDashboard(true); }
  private void controlsNioLike() { buildNioLikeDashboard(); }

  private final java.util.List<Runnable> dashboardBindings = new java.util.ArrayList<>();
  private boolean dashboardReady;

  private void bindDashboard(Runnable binding) {
    dashboardBindings.add(binding);
    binding.run();
  }

  private void onModelChanged() {
    if (dashboardReady && page == 0 && model.hasSession()) {
      for (Runnable binding : new java.util.ArrayList<>(dashboardBindings)) binding.run();
      if (controlRefresh != null)
        controlRefresh.setRefreshing(model.busy && model.activeAction.isEmpty());
    } else render();
  }

  private TextView liveText(java.util.function.Supplier<String> value, int sp, int color, boolean heavy) {
    TextView view = text("", sp, color, heavy);
    view.setMaxLines(1);
    view.setEllipsize(android.text.TextUtils.TruncateAt.END);
    bindDashboard(() -> {
      String next = value.get();
      if (!android.text.TextUtils.equals(next, view.getText())) view.setText(next);
    });
    return view;
  }

  private int textLineHeight(int sp) {
    TextView probe = text("国Ag °µ", sp, ink, false);
    probe.setMaxLines(1);
    probe.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    return probe.getMeasuredHeight();
  }

  private String dashboardStamp() {
    VehicleSnapshot v = model.snapshot;
    String stamp = v == null || v.timestampMillis <= 0 ? tr("更新时间未知", "Update time unknown")
        : tr("更新 ", "Updated ") + android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis);
    return (model.message.isEmpty() ? "" : model.message + " · ")
        + (AppPreferences.visible(this, "show_update", true) ? stamp : "");
  }

  private void buildDashboard(boolean simple) {
    android.content.res.Configuration config = getResources().getConfiguration();
    boolean landscape = config.screenWidthDp > config.screenHeightDp;
    boolean compact = landscape || config.screenHeightDp < 680 || config.fontScale > 1.2f;
    body.setPadding(0, 0, 0, dp(2));

    LinearLayout head = row();
    TextView name = liveText(() -> vehicleDisplayName(model.snapshot), landscape ? 13 : 16, ink, true);
    name.setGravity(Gravity.CENTER_VERTICAL);
    click(name, this::vehicleInfoDialog);
    head.addView(name, new LinearLayout.LayoutParams(0, -1, 1));
    TextView refresh = button("↻", () -> model.refreshUser(null));
    refresh.setPadding(0, 0, 0, 0);
    refresh.setContentDescription(tr("刷新车辆状态", "Refresh vehicle status"));
    bindDashboard(() -> { refresh.setEnabled(!model.busy); refresh.setText(model.busy ? "…" : "↻"); });
    head.addView(refresh, new LinearLayout.LayoutParams(dp(40), -1));
    body.addView(
        head,
        new LinearLayout.LayoutParams(
            -1,
            Math.max(
                dp(landscape ? 32 : 36),
                textLineHeight(landscape ? 13 : 16) + dp(landscape ? 6 : 8))));

    body.addView(dashboardEnergy(compact), new LinearLayout.LayoutParams(-1, dp(landscape ? 48 : 62)));

    java.util.List<DisplayMetric> chosen = controlMetrics();
    if (landscape) {
      LinearLayout middle = row();
      LinearLayout visual = vehicleVisual();
      middle.addView(visual, new LinearLayout.LayoutParams(0, -1, .31f));
      LinearLayout metrics = column();
      metrics.setGravity(Gravity.CENTER_VERTICAL);
      addMetricGrid(metrics, model.snapshot, chosen, true);
      middle.addView(metrics, new LinearLayout.LayoutParams(0, -1, .69f));
      body.addView(middle, new LinearLayout.LayoutParams(-1, 0, 1));
    } else if (simple) {
      body.addView(vehicleVisual(), new LinearLayout.LayoutParams(-1, 0, 1));
      addMetricGrid(body, model.snapshot, chosen, true);
    } else {
      addMetricGrid(body, model.snapshot, chosen, true);
      body.addView(vehicleVisual(), new LinearLayout.LayoutParams(-1, 0, 1));
    }

    LinearLayout charging = dashboardChargingSection();
    body.addView(charging, new LinearLayout.LayoutParams(-1, dp(compact ? 46 : 50)));
    gap(body, 3);
    addDashboardUtilities(body, landscape);
    gap(body, 4);

    boolean fourAcross = simple || compact;
    String[] order = simple ? new String[]{"lock", "climate", "air", "trunk"}
        : new String[]{"lock", "climate", "trunk", "air"};
    int controlHeight = fourAcross ? dp(34) + 2 * textLineHeight(10)
        : dp(22) + textLineHeight(14) + textLineHeight(12) + textLineHeight(11);
    if (fourAcross) {
      LinearLayout actions = row();
      for (String control : order) addDashboardControl(actions, control, true, simple);
      body.addView(actions, new LinearLayout.LayoutParams(-1, Math.max(dp(68), controlHeight)));
    } else {
      for (int i = 0; i < 4; i += 2) {
        LinearLayout actions = row();
        addDashboardControl(actions, order[i], false, false);
        addDashboardControl(actions, order[i + 1], false, false);
        body.addView(actions, new LinearLayout.LayoutParams(-1, controlHeight));
        if (i == 0) gap(body, 4);
      }
    }

    TextView footer = liveText(this::dashboardStamp, 9, muted, false);
    footer.setGravity(Gravity.CENTER_VERTICAL);
    click(footer, () -> info(tr("车辆状态", "Vehicle status"), dashboardStamp()));
    body.addView(footer, new LinearLayout.LayoutParams(-1, textLineHeight(9) + dp(4)));
    dashboardReady = true;
  }

  private LinearLayout vehicleVisual() {
    LinearLayout box = column();
    ImageView car = new ImageView(this);
    car.setTag("polestar_vehicle");
    car.setImageResource(R.drawable.vehicle_top);
    car.setScaleType(ImageView.ScaleType.FIT_CENTER);
    stylePolestarVehicle(car);
    car.setContentDescription(tr("查看车门与车窗状态", "View doors and windows"));
    click(car, this::exteriorDialog);
    car.setVisibility(AppPreferences.visible(this, "show_car", true) ? View.VISIBLE : View.GONE);
    box.addView(car, new LinearLayout.LayoutParams(-1, 0, 1));
    box.addView(vehicleHealthBanner(), new LinearLayout.LayoutParams(-1, dp(28)));
    return box;
  }

  private void addDashboardUtilities(LinearLayout parent, boolean landscape) {
    LinearLayout utilities = row();
    String[] kinds = {"windows", "body", "camp", "signal"};
    for (String kind : kinds) {
      Runnable action = kind.equals("windows") ? () -> directControl("windows")
          : kind.equals("body") ? this::exteriorDialog
          : kind.equals("camp") ? this::campDialog : this::signalMenu;
      String initialLabel = kind.equals("windows") ? tr("车窗", "Windows")
          : kind.equals("body") ? tr("门窗", "Body")
          : kind.equals("camp") ? tr("露营", "Camp") : tr("鸣笛", "Signal");
      TextView view = utility(initialLabel, false, false, action);
      view.setTextSize(11);
      view.setPadding(dp(3), dp(2), dp(3), dp(2));
      view.setTag(kind.equals("windows") ? "windows_control_button" : kind.equals("body")
          ? "exterior_button" : kind.equals("camp") ? "camp_button" : "signal_control_button");
      bindDashboard(() -> {
        VehicleSnapshot v = model.snapshot;
        boolean warn = kind.equals("windows")
            ? v != null && v.windows != null && v.windows.contains("个开启")
            : kind.equals("body") && ExteriorView.hasOpening(v);
        boolean active = warn || kind.equals("camp") && (CampModeManager.active(this)
            || "STOPPING".equals(CampModeManager.phase(this)));
        String label = kind.equals("windows") ? tr("车窗", "Windows")
            : kind.equals("body") ? tr("门窗", "Body")
            : kind.equals("camp") ? tr("露营", "Camp") : tr("鸣笛", "Signal");
        view.setText(label + (warn ? " !" : ""));
        GradientDrawable background = (GradientDrawable) view.getBackground();
        background.setColor(active ? orange : surface);
        background.setStroke(dp(1), active ? orange : line);
        view.setTextColor(active ? Color.rgb(21, 22, 23) : ink);
        String details = kind.equals("windows")
            ? (v == null ? tr("未知", "Unknown") : status(v.windows))
            : kind.equals("body")
                ? (v == null ? tr("未知", "Unknown") : status(v.doors) + " · " + status(v.windows))
                : kind.equals("camp") ? CampModeManager.status(this) : tr("鸣笛与闪灯", "Honk and flash");
        view.setContentDescription(label + " · " + details);
      });
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(landscape ? 38 : 44), 1);
      if (utilities.getChildCount() > 0) lp.leftMargin = dp(4);
      utilities.addView(view, lp);
    }
    parent.addView(utilities);
  }

  private LinearLayout dashboardEnergy(boolean compact) {
    boolean landscape =
        getResources().getConfiguration().screenWidthDp
            > getResources().getConfiguration().screenHeightDp;
    LinearLayout root = row();
    root.setTag("energy_header");
    root.setPadding(0, dp(2), 0, dp(2));

    LinearLayout battery = column();
    TextView batteryLabel = text(tr("电量", "Battery"), landscape ? 8 : 9, muted, false);
    TextView batteryValue =
        liveText(
            () -> model.snapshot == null ? "--%" : VehicleSnapshot.metric(model.snapshot.batteryPercent, "%"),
            landscape ? 16 : compact ? 21 : 28,
            ink,
            true);
    batteryValue.setTag("battery_value");
    ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setTag("battery_progress");
    progress.setMax(100);
    styleViProgress(progress);
    bindDashboard(
        () ->
            progress.setProgress(
                model.snapshot == null || model.snapshot.batteryPercent == null
                    ? 0
                    : model.snapshot.batteryPercent));
    battery.addView(batteryLabel);
    battery.addView(batteryValue, new LinearLayout.LayoutParams(-1, 0, 1));
    battery.addView(progress, new LinearLayout.LayoutParams(-1, dp(5)));

    LinearLayout range = column();
    range.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    TextView rangeLabel = text(tr("预估续航", "Est. range"), landscape ? 8 : 9, muted, false);
    rangeLabel.setGravity(Gravity.END);
    TextView rangeValue =
        liveText(
            () -> model.snapshot == null ? "-- km" : VehicleSnapshot.metric(model.snapshot.rangeKm, " km"),
            landscape ? 15 : compact ? 18 : 25,
            ink,
            true);
    rangeValue.setTag("range_value");
    rangeValue.setGravity(Gravity.END);
    range.addView(rangeLabel);
    range.addView(rangeValue, new LinearLayout.LayoutParams(-1, 0, 1));

    root.addView(battery, new LinearLayout.LayoutParams(0, -1, .56f));
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -1, .44f);
    rp.leftMargin = dp(18);
    root.addView(range, rp);
    return root;
  }

  private LinearLayout dashboardChargingSection() {
    LinearLayout card = row();
    card.setTag("charging_section");
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(10), dp(5), dp(10), dp(5));
    GradientDrawable background = bg(surface, 10);
    background.setStroke(dp(1), line);
    card.setBackground(background);

    ImageView icon = new ImageView(this);
    icon.setImageResource(R.drawable.ic_charge);
    icon.setColorFilter(orange);
    card.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

    LinearLayout copy = column();
    TextView title = text("", 12, ink, true);
    TextView detail = text("", 10, muted, false);
    title.setSingleLine(true);
    detail.setSingleLine(true);
    detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
    copy.addView(title);
    copy.addView(detail);
    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1);
    cp.leftMargin = dp(8);
    card.addView(copy, cp);

    bindDashboard(() -> {
      VehicleSnapshot v = model.snapshot;
      card.setVisibility(chargingVisible(v) ? View.VISIBLE : View.GONE);
      title.setText(chargingTitle(v));
      detail.setText(chargingDetail(v));
      card.setContentDescription(chargingTitle(v) + " · " + chargingDetail(v));
    });
    return card;
  }

  private LinearLayout vehicleHealthBanner() {
    LinearLayout banner = row();
    banner.setTag("vehicle_health_banner");
    banner.setGravity(Gravity.CENTER);
    banner.setPadding(dp(8), 0, dp(8), 0);
    ImageView icon = new ImageView(this);
    TextView label = text("", 11, ink, true);
    label.setGravity(Gravity.CENTER_VERTICAL);
    banner.addView(icon, new LinearLayout.LayoutParams(dp(16), dp(16)));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
    lp.leftMargin = dp(5);
    banner.addView(label, lp);
    bindDashboard(() -> {
      VehicleSnapshot v = model.snapshot;
      boolean problem = v != null && VehicleHealthUi.hasProblem(this, v.health);
      boolean good = v != null && VehicleHealthUi.allGood(this, v.health);
      icon.setImageResource(problem ? R.drawable.ic_vi_warning : R.drawable.ic_vi_check);
      int iconColor = problem ? orange : good ? orange : muted;
      icon.setColorFilter(iconColor);
      label.setText(healthDisplayText());
      label.setTextColor(problem ? orange : good ? ink : muted);
      banner.setContentDescription(tr("车辆状态 · ", "Vehicle status · ") + healthDisplayText());
    });
    click(banner, this::vehicleStatusDialog);
    banner.setFocusable(true);
    return banner;
  }

  private LinearLayout nioCard(String tag) {
    LinearLayout card = column();
    card.setTag(tag);
    card.setPadding(dp(10), dp(9), dp(10), dp(9));
    GradientDrawable background = bg(surface, 22);
    background.setStroke(dp(1), line);
    card.setBackground(background);
    return card;
  }

  private void addNioCard(LinearLayout parent, LinearLayout card) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
    if (parent.getChildCount() > 0) lp.leftMargin = dp(5);
    parent.addView(card, lp);
  }

  private LinearLayout nioEnergyCard() {
    LinearLayout card = nioCard("nio_energy_card");
    LinearLayout values = row();

    LinearLayout battery = column();
    battery.addView(text(tr("电量", "Battery"), 8, muted, false));
    TextView batteryValue =
        liveText(
            () -> model.snapshot == null ? "--%" : VehicleSnapshot.metric(model.snapshot.batteryPercent, "%"),
            24,
            ink,
            true);
    batteryValue.setTag("battery_value");
    battery.addView(batteryValue);
    ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setTag("battery_progress");
    progress.setMax(100);
    styleViProgress(progress);
    bindDashboard(
        () ->
            progress.setProgress(
                model.snapshot == null || model.snapshot.batteryPercent == null
                    ? 0
                    : model.snapshot.batteryPercent));
    battery.addView(progress, new LinearLayout.LayoutParams(-1, dp(5)));

    LinearLayout range = column();
    range.setGravity(Gravity.END);
    TextView rangeLabel = text(tr("预估续航", "Est. range"), 8, muted, false);
    rangeLabel.setGravity(Gravity.END);
    TextView rangeValue =
        liveText(
            () -> model.snapshot == null ? "-- km" : VehicleSnapshot.metric(model.snapshot.rangeKm, " km"),
            21,
            ink,
            true);
    rangeValue.setTag("range_value");
    rangeValue.setGravity(Gravity.END);
    range.addView(rangeLabel);
    range.addView(rangeValue);

    values.addView(battery, new LinearLayout.LayoutParams(0, -2, .56f));
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, .44f);
    rp.leftMargin = dp(10);
    values.addView(range, rp);
    card.addView(values);

    ImageView car = new ImageView(this);
    car.setTag("polestar_vehicle");
    car.setImageResource(R.drawable.vehicle_top);
    car.setScaleType(ImageView.ScaleType.FIT_CENTER);
    stylePolestarVehicle(car);
    click(car, this::exteriorDialog);
    card.addView(car, new LinearLayout.LayoutParams(-1, 0, 1));
    card.addView(vehicleHealthBanner(), new LinearLayout.LayoutParams(-1, dp(25)));
    return card;
  }

  private void addNioRoundAction(
      LinearLayout parent,
      String tag,
      int iconRes,
      String labelText,
      Runnable action,
      java.util.function.BooleanSupplier active) {
    LinearLayout root = column();
    root.setTag(tag);
    root.setGravity(Gravity.CENTER);
    ImageView icon = new ImageView(this);
    icon.setImageResource(iconRes);
    icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    icon.setPadding(dp(10), dp(10), dp(10), dp(10));
    GradientDrawable circle = oval(viSoft());
    icon.setBackground(circle);
    TextView label = text(labelText, 8, muted, false);
    label.setGravity(Gravity.CENTER);
    label.setMaxLines(1);
    root.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
    root.addView(label);
    bindDashboard(() -> {
      boolean on = active != null && active.getAsBoolean();
      circle.setColor(on ? orange : viSoft());
      icon.setColorFilter(on ? Color.rgb(21, 22, 23) : ink);
      label.setTextColor(on ? orange : muted);
      root.setContentDescription(labelText + (on ? tr(" · 开启", " · active") : ""));
    });
    click(root, action);
    root.setFocusable(true);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
    if (parent.getChildCount() > 0) lp.leftMargin = dp(4);
    parent.addView(root, lp);
  }

  private LinearLayout nioControlsCard() {
    LinearLayout card = nioCard("nio_control_card");
    LinearLayout first = row();
    LinearLayout second = row();
    addNioRoundAction(
        first,
        "nio_lock",
        R.drawable.ic_lock,
        tr("门锁", "Lock"),
        () -> directControl("lock"),
        () -> model.snapshot != null && "未锁".equals(model.snapshot.lock));
    addNioRoundAction(
        first,
        "nio_trunk",
        R.drawable.ic_tailgate,
        tr("尾箱", "Trunk"),
        () -> directControl("trunk"),
        () -> model.snapshot != null && ("已打开".equals(model.snapshot.tailgate) || "未关严".equals(model.snapshot.tailgate)));
    addNioRoundAction(
        second,
        "nio_windows",
        R.drawable.ic_windows,
        tr("车窗", "Windows"),
        () -> directControl("windows"),
        () -> model.snapshot != null && model.snapshot.windows != null && model.snapshot.windows.contains("个开启"));
    addNioRoundAction(
        second,
        "nio_signal",
        R.drawable.ic_signal,
        tr("鸣笛 / 闪灯", "Signal"),
        this::signalMenu,
        () -> false);
    card.addView(first, new LinearLayout.LayoutParams(-1, 0, 1));
    card.addView(second, new LinearLayout.LayoutParams(-1, 0, 1));
    return card;
  }

  private LinearLayout nioClimateCard() {
    LinearLayout card = nioCard("nio_climate_card");
    LinearLayout heading = row();
    ImageView fan = new ImageView(this);
    fan.setImageResource(R.drawable.ic_climate);
    fan.setColorFilter(ink);
    heading.addView(fan, new LinearLayout.LayoutParams(dp(26), dp(26)));
    TextView title = text(tr("驻车空调", "Climate"), 11, ink, true);
    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1);
    titleLp.leftMargin = dp(7);
    heading.addView(title, titleLp);

    ImageView power = new ImageView(this);
    power.setTag("nio_climate_power");
    power.setImageResource(R.drawable.ic_power);
    power.setPadding(dp(10), dp(10), dp(10), dp(10));
    GradientDrawable powerBg = oval(viSoft());
    power.setBackground(powerBg);
    click(power, () -> directControl("climate"));
    heading.addView(power, new LinearLayout.LayoutParams(dp(48), dp(48)));
    card.addView(heading);

    TextView target =
        liveText(
            () ->
                model.snapshot == null
                    ? "--°"
                    : EnvironmentSnapshot.temperatureText(model.snapshot.environment.targetTemperatureC, false) + "°",
            28,
            ink,
            true);
    target.setGravity(Gravity.CENTER);
    card.addView(target, new LinearLayout.LayoutParams(-1, 0, 1));

    TextView cabin =
        liveText(
            () ->
                model.snapshot == null
                    ? tr("车内温度 --", "Cabin --")
                    : tr("车内温度 ", "Cabin ")
                        + EnvironmentSnapshot.temperatureText(model.snapshot.environment.temperatureC, true),
            10,
            muted,
            false);
    cabin.setGravity(Gravity.CENTER);
    card.addView(cabin);

    TextView air = button("", () -> directControl("air"));
    air.setTextSize(9);
    air.setMinHeight(dp(30));
    air.setPadding(dp(6), dp(2), dp(6), dp(2));
    bindDashboard(() -> {
      VehicleSnapshot v = model.snapshot;
      boolean climateOn = v != null && "开启".equals(v.climate);
      boolean airOn = v != null && "开启".equals(v.air);
      powerBg.setColor(climateOn ? orange : viSoft());
      power.setColorFilter(climateOn ? Color.rgb(21, 22, 23) : ink);
      air.setText(tr("空气净化 · ", "Purification · ") + (v == null ? tr("未知", "Unknown") : status(v.air)));
      GradientDrawable airBg = bg(airOn ? orange : viSoft(), 8);
      air.setBackground(airBg);
      air.setTextColor(airOn ? Color.rgb(21, 22, 23) : ink);
    });
    card.addView(air, new LinearLayout.LayoutParams(-1, dp(30)));
    return card;
  }

  private TextView nioMiniAction(String label, Runnable action) {
    TextView view = button(label, action);
    view.setTextSize(9);
    view.setMinHeight(dp(32));
    view.setPadding(dp(4), dp(2), dp(4), dp(2));
    view.setBackground(bg(viSoft(), 10));
    return view;
  }

  private LinearLayout nioInfoCard() {
    LinearLayout card = nioCard("nio_info_card");
    LinearLayout heading = row();
    ImageView icon = new ImageView(this);
    icon.setImageResource(R.drawable.ic_status);
    icon.setColorFilter(ink);
    heading.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
    TextView title = text(tr("车辆信息", "Vehicle"), 11, ink, true);
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
    tp.leftMargin = dp(7);
    heading.addView(title, tp);
    card.addView(heading);

    TextView odometer =
        liveText(
            () ->
                model.snapshot == null
                    ? "-- km"
                    : DisplayMetric.ODOMETER.value(this, model.snapshot, true, AppPreferences.english(this)),
            23,
            ink,
            true);
    odometer.setGravity(Gravity.CENTER_VERTICAL);
    card.addView(odometer, new LinearLayout.LayoutParams(-1, 0, 1));
    card.addView(text(tr("总里程", "Odometer"), 9, muted, false));

    LinearLayout first = row();
    LinearLayout second = row();
    first.addView(nioMiniAction(tr("门窗", "Body"), this::exteriorDialog), new LinearLayout.LayoutParams(0, dp(32), 1));
    LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, dp(32), 1);
    hp.leftMargin = dp(4);
    first.addView(nioMiniAction(tr("车况", "Health"), this::vehicleStatusDialog), hp);
    second.addView(nioMiniAction(tr("露营", "Camp"), this::campDialog), new LinearLayout.LayoutParams(0, dp(32), 1));
    LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(0, dp(32), 1);
    dpLp.leftMargin = dp(4);
    second.addView(
        nioMiniAction(
            tr("详情", "Details"),
            () -> {
              page = 1;
              render();
            }),
        dpLp);
    gap(card, 4);
    card.addView(first);
    gap(card, 4);
    card.addView(second);
    return card;
  }

  private void buildNioLikeDashboard() {
    android.content.res.Configuration config = getResources().getConfiguration();
    boolean landscape = config.screenWidthDp > config.screenHeightDp;
    body.setPadding(0, 0, 0, dp(2));

    LinearLayout head = row();
    TextView name = liveText(() -> vehicleDisplayName(model.snapshot), landscape ? 12 : 15, ink, true);
    name.setGravity(Gravity.CENTER_VERTICAL);
    click(name, this::vehicleInfoDialog);
    head.addView(name, new LinearLayout.LayoutParams(0, -1, 1));
    TextView refresh = button("↻", () -> model.refreshUser(null));
    refresh.setPadding(0, 0, 0, 0);
    bindDashboard(() -> { refresh.setEnabled(!model.busy); refresh.setText(model.busy ? "…" : "↻"); });
    head.addView(refresh, new LinearLayout.LayoutParams(dp(40), -1));
    body.addView(head, new LinearLayout.LayoutParams(-1, dp(36)));

    LinearLayout charge = dashboardChargingSection();
    body.addView(charge, new LinearLayout.LayoutParams(-1, dp(48)));
    gap(body, 4);

    if (landscape) {
      LinearLayout grid = row();
      addNioCard(grid, nioEnergyCard());
      addNioCard(grid, nioControlsCard());
      addNioCard(grid, nioClimateCard());
      addNioCard(grid, nioInfoCard());
      body.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
    } else {
      LinearLayout grid = column();
      LinearLayout first = row();
      addNioCard(first, nioEnergyCard());
      addNioCard(first, nioControlsCard());
      grid.addView(first, new LinearLayout.LayoutParams(-1, 0, 1));
      gap(grid, 5);
      LinearLayout second = row();
      addNioCard(second, nioClimateCard());
      addNioCard(second, nioInfoCard());
      grid.addView(second, new LinearLayout.LayoutParams(-1, 0, 1));
      body.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    TextView footer = liveText(this::dashboardStamp, 8, muted, false);
    footer.setGravity(Gravity.CENTER_VERTICAL);
    footer.setSingleLine(true);
    click(footer, () -> info(tr("车辆状态", "Vehicle status"), dashboardStamp()));
    body.addView(footer, new LinearLayout.LayoutParams(-1, textLineHeight(8) + dp(4)));
    dashboardReady = true;
  }

  private boolean controlSending(String control) {
    String pending = model.activeAction;
    return model.busy && (control.equals("lock") ? pending.equals("lock") || pending.equals("unlock")
        : control.equals("trunk") ? pending.startsWith("trunk") : pending.startsWith(control));
  }

  private String controlRaw(String control) {
    VehicleSnapshot v = model.snapshot;
    return v == null ? "未知" : control.equals("lock") ? v.lock : control.equals("trunk") ? v.tailgate
        : control.equals("climate") ? v.climate : v.air;
  }

  private String controlFeedback(String control, boolean compact) {
    if (controlSending(control)) return model.operation == OperationState.SENDING
        ? tr("发送中", "Sending") : tr("待确认", "Verifying");
    VehicleSnapshot v = model.snapshot;
    if (control.equals("climate") && v != null && "开启".equals(v.climate)) {
      if (compact) return ClimateRuntime.shortLabel(this, v, AppPreferences.get(this, "vehicle", ""),
          AppPreferences.english(this)).replace("分钟", "分");
      return tr("运行中", "Running");
    }
    return status(controlRaw(control));
  }

  private void addDashboardControl(LinearLayout parent, String control, boolean compact, boolean circles) {
    String label = control.equals("lock") ? tr("门锁", "Lock") : control.equals("climate")
        ? tr(compact ? "空调" : "驻车空调", "Climate") : control.equals("trunk") ? tr("尾箱", "Trunk")
        : tr(compact ? "净化" : "空气净化", "Air");
    LinearLayout card = column(); card.setGravity(Gravity.CENTER_VERTICAL);
    card.setTag("control_tile"); card.setPadding(dp(compact ? 3 : 10), dp(4), dp(compact ? 3 : 8), dp(4));
    GradientDrawable background = bg(surface, 10); card.setBackground(background);
    ImageView icon = new ImageView(this);
    icon.setImageResource(control.equals("lock") ? R.drawable.ic_lock : control.equals("climate")
        ? R.drawable.ic_climate : control.equals("trunk") ? R.drawable.ic_tailgate : R.drawable.ic_air);
    icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    TextView title = text(label, compact ? 10 : 14, ink, false);
    TextView feedback = text("", compact ? 10 : 12, ink, false);
    TextView support = text("", 11, muted, false);
    for (TextView v : new TextView[]{title, feedback, support}) {
      v.setMaxLines(1); v.setEllipsize(android.text.TextUtils.TruncateAt.END);
    }
    if (compact) {
      card.setGravity(Gravity.CENTER);
      card.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
      title.setGravity(Gravity.CENTER); feedback.setGravity(Gravity.CENTER);
      card.addView(title, new LinearLayout.LayoutParams(-1, -2));
    } else {
      LinearLayout heading = row();
      heading.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));
      LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1); tp.leftMargin = dp(5);
      heading.addView(title, tp); card.addView(heading); gap(card, 2);
    }
    card.addView(feedback, new LinearLayout.LayoutParams(-1, -2));
    if (!compact) card.addView(support, new LinearLayout.LayoutParams(-1, -2));
    bindDashboard(() -> {
      String raw = controlRaw(control);
      boolean active = "开启".equals(raw) || "未锁".equals(raw) || "已打开".equals(raw) || "未关严".equals(raw);
      boolean known = active || "关闭".equals(raw) || "已上锁".equals(raw) || "已锁".equals(raw) || "已关闭".equals(raw);
      background.setColor(known ? active ? orange : Color.WHITE : surface);
      background.setStroke(dp(1), controlSending(control) ? orange : known ? Color.TRANSPARENT : line);
      int color = known ? Color.rgb(21,22,23) : ink;
      icon.setColorFilter(color);
      if (circles) icon.setBackground(oval(active ? Color.argb(18,0,0,0) : viSoft()));
      title.setTextColor(color); feedback.setTextColor(color);
      feedback.setText(controlFeedback(control, compact));
      String detail = model.snapshot == null ? "" : control.equals("climate")
          ? ClimateRuntime.detail(this, model.snapshot, AppPreferences.get(this, "vehicle", ""))
          : control.equals("air") ? "PM2.5 " + MileageSnapshot.decimalText(model.snapshot.environment.pm25UgM3, "")
              + " · AQI " + (model.snapshot.environment.aqi == null ? "--" : model.snapshot.environment.aqi) : "";
      support.setText(detail); support.setTextColor(known ? color : muted);
      card.setEnabled(!model.busy && model.selected != null);
      card.setAlpha(card.isEnabled() ? 1f : .55f);
      card.setContentDescription(label + " · " + controlFeedback(control, compact) + " · " + detail);
    });
    click(card, () -> directControl(control)); card.setFocusable(true);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
    if (parent.getChildCount() > 0) lp.leftMargin = dp(5);
    parent.addView(card, lp);
  }

  private void addMetricGrid(LinearLayout parent, VehicleSnapshot unused,
      java.util.List<DisplayMetric> chosen, boolean compact) {
    int count = chosen.size();
    if (count == 0) return;
    boolean landscape = getResources().getConfiguration().screenWidthDp
        > getResources().getConfiguration().screenHeightDp;
    int columns = landscape && count > 6 ? 6 : count <= 4 ? 2 : count <= 6 ? 3 : 4;
    int labelSp = 9, valueSp = 12;
    for (int i = 0; i < count; i += columns) {
      boolean energyRow = false;
      for (int k = i; k < Math.min(i + columns, count); k++)
        energyRow |= chosen.get(k) == DisplayMetric.TA_CONSUMPTION || chosen.get(k) == DisplayMetric.TM_CONSUMPTION;
      int captionLines = energyRow ? 2 : 1;
      int rowHeight = textLineHeight(labelSp) * captionLines + textLineHeight(valueSp) + dp(3);
      LinearLayout metricRow = row();
      for (int j = i; j < i + columns; j++) {
        LinearLayout cell = column(); cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setPadding(dp(3), 0, dp(3), 0);
        if (j < count) {
          DisplayMetric metric = chosen.get(j);
          String caption = metric == DisplayMetric.TA_CONSUMPTION ? "TA\nkWh/100km"
              : metric == DisplayMetric.TM_CONSUMPTION ? "TM\nkWh/100km" : metric.shortLabel(AppPreferences.english(this));
          TextView label = text(caption, labelSp, muted, false);
          label.setMaxLines(captionLines); label.setGravity(Gravity.BOTTOM);
          label.setEllipsize(android.text.TextUtils.TruncateAt.END);
          TextView value = liveText(() -> metric == DisplayMetric.VEHICLE_STATUS && model.snapshot != null
              && model.snapshot.health.allGood() ? tr("正常", "OK")
              : metric.value(this, model.snapshot, false, AppPreferences.english(this)), valueSp, ink, false);
          cell.addView(label, new LinearLayout.LayoutParams(-1, textLineHeight(labelSp) * captionLines)); cell.addView(value);
          bindDashboard(() -> cell.setContentDescription(metric.label(AppPreferences.english(this))
              + " · " + metric.value(this, model.snapshot, true, AppPreferences.english(this))));
          click(cell, () -> info(metric.label(AppPreferences.english(this)),
              metric.value(this, model.snapshot, true, AppPreferences.english(this))
                  + (metric.name().startsWith("SERVICE_") ? tr("\n本地保养提醒计算", "\nLocal service calculation") : "")));
        }
        metricRow.addView(cell, new LinearLayout.LayoutParams(0, -1, 1));
      }
      parent.addView(metricRow, new LinearLayout.LayoutParams(-1, rowHeight));
    }
  }

  private void vehicleInfoDialog() {
    io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo info = model.selected;
    LinearLayout panel = column();
    panel.setPadding(dp(16), dp(6), dp(16), dp(4));
    if (info == null) {
      panel.addView(text(tr("暂无车辆信息", "Vehicle information unavailable"), 14, muted, false));
    } else {
      String modelName =
          info.modelName != null && !info.modelName.trim().isEmpty()
              ? info.modelName.trim()
              : info.model != null && !info.model.trim().isEmpty() ? info.model.trim() : "Polestar 2";
      detailRow(panel, tr("车型", "Model"), modelName);
      detailRow(
          panel,
          tr("年款", "Model year"),
          info.modelYear == null || info.modelYear.trim().isEmpty() ? "--" : info.modelYear.trim());
      detailRow(panel, "VIN", info.vin == null || info.vin.trim().isEmpty() ? "--" : info.vin.trim());
    }
    panel.addView(button(tr("切换车辆", "Select another vehicle"), () -> chooseVehicle()));
    new HapticDialogBuilder(this)
        .setTitle(tr("车辆详情", "Vehicle details"))
        .setView(panel)
        .setPositiveButton(tr("关闭", "Close"), null)
        .show();
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
    detailRow(p, tr("车辆", "Vehicle"), vehicleDisplayName(v));
    p.addView(button(tr("车辆信息 · 年款 / VIN", "Vehicle info · year / VIN"), this::vehicleInfoDialog));
    gap(p, 6);
    detailRow(p, tr("电量", "Battery"), VehicleSnapshot.metric(v.batteryPercent, "%"));
    detailRow(p, tr("续航", "Range"), VehicleSnapshot.metric(v.rangeKm, " km"));
    detailRow(p, tr("累计里程", "Odometer"), v.mileage.totalText());
    detailRow(p, tr("TM 手动复位里程", "TM · Manual trip"), MileageSnapshot.tripText(v.mileage.manualKm));
    detailRow(p, tr("TA 自动复位里程", "TA · Automatic trip"), MileageSnapshot.tripText(v.mileage.automaticKm));
    detailRow(p, "TA · AVG Speed", MileageSnapshot.decimalText(v.mileage.automaticAverageSpeed, " km/h"));
    detailRow(p, "TA · Average Consumption", MileageSnapshot.decimalText(v.mileage.automaticConsumption, " kWh/100 km"));
    detailRow(p, "TM · AVG Speed", MileageSnapshot.decimalText(v.mileage.manualAverageSpeed, " km/h"));
    detailRow(p, "TM · Average Consumption", MileageSnapshot.decimalText(v.mileage.manualConsumption, " kWh/100 km"));
    if (chargingVisible(v)) {
      gap(p, 10);
      LinearLayout chargingCard = column();
      chargingCard.setPadding(dp(12), dp(10), dp(12), dp(10));
      GradientDrawable chargingBg = bg(surface, 12);
      chargingBg.setStroke(dp(1), line);
      chargingCard.setBackground(chargingBg);
      chargingCard.addView(text(chargingTitle(v), 15, ink, true));
      gap(chargingCard, 5);
      for (DisplayMetric metric : new DisplayMetric[]{DisplayMetric.CHARGE_CONNECTION, DisplayMetric.CHARGE_TYPE,
          DisplayMetric.CHARGE_POWER, DisplayMetric.CHARGE_CURRENT, DisplayMetric.CHARGE_ETA, DisplayMetric.POWER_STATUS})
        detailRow(chargingCard, metric.label(AppPreferences.english(this)),
            metric.value(this, v, true, AppPreferences.english(this)));
      p.addView(chargingCard);
      gap(p, 8);
    }
    detailRow(p, tr("门锁", "Door lock"), status(v.lock));
    detailRow(p, tr("车门", "Doors"), status(v.doors));
    detailRow(p, tr("车窗", "Windows"), status(v.windows));
    String[] positions = {tr("左前", "Front left"), tr("右前", "Front right"), tr("左后", "Rear left"), tr("右后", "Rear right")};
    for (int i = 0; i < 4; i++)
      detailRow(p, positions[i], tr("门 ", "Door ") + status(ExteriorSnapshot.label(v.exterior.state(false, i)))
          + " · " + tr("窗 ", "Window ") + status(ExteriorSnapshot.label(v.exterior.state(true, i))));
    detailRow(p, tr("尾箱", "Tailgate"), status(v.tailgate));
    detailRow(
        p,
        tr("车辆状态", "Vehicle status"),
        VehicleHealthUi.summary(this, v.health, AppPreferences.english(this)));
    detailRow(p, tr("12V 电瓶", "12 V battery"),
        VehicleHealthSnapshot.label(v.health.lowVoltageBattery, AppPreferences.english(this)));
    detailRow(p, tr("玻璃水", "Washer fluid"),
        VehicleHealthSnapshot.label(v.health.washerFluid, AppPreferences.english(this)));
    if (VehicleHealthUi.includeTire(this))
      detailRow(p, tr("胎压告警", "Tyre warning"),
          VehicleHealthSnapshot.label(v.health.tirePressure, AppPreferences.english(this)));
    p.addView(button(tr("车辆状态详情", "Vehicle status details"), this::vehicleStatusDialog));
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
    final String[] values = {UiSkin.SIMPLE, UiSkin.DETAILED, UiSkin.NIOLIKE};
    String[] labels = {
        tr("1 · 简洁风格", "1 · Simple"),
        tr("2 · 详细风格", "2 · Detailed"),
        tr("3 · NIOlike 模块风格", "3 · NIOlike cards")
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
        tr("车辆控制页常规数据 · 最多 12 项（充电自动单列）",
            "Vehicle data · up to 12 (charging is automatic)"),
        () -> metricDialog("home_metrics", 12, HOME_DEFAULTS)));
    gap(p, 8);
    Switch experimentalTire = new Switch(this);
    experimentalTire.setText(
        tr("显示胎压 / iTPMS（实验性）", "Show tyre / iTPMS (experimental)"));
    experimentalTire.setTextColor(ink);
    experimentalTire.setMinHeight(dp(46));
    experimentalTire.setChecked(AppPreferences.visible(this, "experimental_tire", false));
    experimentalTire.setOnCheckedChangeListener(
        (control, on) -> {
          Haptics.tap(this);
          AppPreferences.set(this, "experimental_tire", on ? "on" : "off");
          if (!on) {
            java.util.List<DisplayMetric> selected =
                MetricSelection.read(this, "home_metrics", HOME_DEFAULTS);
            selected.remove(DisplayMetric.TIRE_STATUS);
            MetricSelection.save(this, "home_metrics", selected);
          }
          render();
        });
    p.addView(experimentalTire);
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
            tr("日夜模式 · 三套皮肤通用", "Day / night · all three skins"),
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
    p.addView(button(tr("Widget 文字颜色", "Widget text color"), () ->
        new HapticDialogBuilder(this).setTitle(tr("Widget 文字颜色", "Widget text color"))
          .setItems(new String[]{tr("跟随日夜模式", "Follow day/night"), tr("浅色文字", "Light text"), tr("深色文字", "Dark text")},
            (dialog, which) -> {
              AppPreferences.set(this, "widget_ink", new String[]{"system", "light", "dark"}[which]);
              WidgetRenderer.refreshAll(this);
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
    java.util.List<DisplayMetric> available = new java.util.ArrayList<>();
    boolean hideExperimentalTire =
        "home_metrics".equals(key) && !AppPreferences.visible(this, "experimental_tire", false);
    for (DisplayMetric metric : DisplayMetric.values()) {
      if ("home_metrics".equals(key) && fixedHomeMetric(metric)) continue;
      if (hideExperimentalTire && metric == DisplayMetric.TIRE_STATUS) continue;
      available.add(metric);
    }
    DisplayMetric[] values = available.toArray(new DisplayMetric[0]);
    java.util.List<DisplayMetric> selected = MetricSelection.read(this, key, defaults);
    if (hideExperimentalTire) selected.remove(DisplayMetric.TIRE_STATUS);
    String[] labels = new String[values.length];
    boolean[] checked = new boolean[values.length];
    for (int i = 0; i < values.length; i++) {
      labels[i] = values[i].label(AppPreferences.english(this));
      checked[i] = selected.contains(values[i]);
    }
    new HapticDialogBuilder(this)
        .setTitle(tr("选择数据显示 · 最多 ", "Choose fields · up to ") + capacity)
        .setMultiChoiceItems(
            labels,
            checked,
            (dialog, which, on) -> {
              if (on && !selected.contains(values[which])) {
                if (selected.size() >= capacity) {
                  ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                  toast(tr("此页面最多显示 ", "This page supports up to ") + capacity
                      + tr(" 项", " fields"));
                  return;
                }
                selected.add(values[which]);
              } else if (!on) selected.remove(values[which]);
            })
        .setPositiveButton(
            tr("保存", "Save"),
            (d, w) -> {
              MetricSelection.save(this, key, selected);
              WidgetRenderer.refreshAll(this);
              render();
            })
        .setNegativeButton(tr("取消", "Cancel"), null)
        .show();
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

  private void vehicleStatusDialog() {
    VehicleSnapshot v = model.snapshot;
    if (v == null) return;
    boolean english = AppPreferences.english(this);
    LinearLayout panel = column();
    panel.setPadding(dp(16), dp(8), dp(16), dp(8));
    detailRow(panel, tr("总览", "Summary"), VehicleHealthUi.summary(this, v.health, english));
    detailRow(
        panel,
        tr("12V 电瓶", "12 V battery"),
        VehicleHealthSnapshot.label(v.health.lowVoltageBattery, english));
    detailRow(
        panel,
        tr("玻璃水", "Washer fluid"),
        VehicleHealthSnapshot.label(v.health.washerFluid, english));
    if (VehicleHealthUi.includeTire(this))
      detailRow(
          panel,
          tr("胎压告警", "Tyre warning"),
          VehicleHealthSnapshot.label(v.health.tirePressure, english));
    if (v.health.reportedAt > 0)
      detailRow(
          panel,
          tr("状态更新时间", "Status updated"),
          android.text.format.DateFormat.format("MM-dd HH:mm", v.health.reportedAt).toString());
    gap(panel, 6);
    panel.addView(
        text(
            tr(
                "12V 与玻璃水仅显示车辆告警状态，不代表电压值或液位。中国区 Health 协议映射已接入，仍需实车语义验证。",
                "12 V and washer fluid are warning states only, not measured voltage or fluid"
                    + " level. China Health mapping is integrated but still needs vehicle verification."),
            11,
            muted,
            false));
    gap(panel, 8);
    if (VehicleHealthUi.includeTire(this)) {
      panel.addView(
          button(
              tr("查看四轮 iTPMS · 实验性 / 待修复", "Four-wheel iTPMS · experimental"),
              () -> tireDialog()));
    }
    new HapticDialogBuilder(this)
        .setTitle(tr("车辆状态", "Vehicle status"))
        .setView(panel)
        .setPositiveButton(tr("关闭", "Close"), null)
        .show();
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
    new HapticDialogBuilder(this).setTitle(tr("iTPMS 四轮状态 · 实验性", "Four-wheel iTPMS · experimental"))
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
        }
      }
      @Override public void onStartTrackingTouch(SeekBar seek) {}
      @Override public void onStopTrackingTouch(SeekBar seek) { Haptics.tap(MainActivity.this); WidgetRenderer.refreshAll(MainActivity.this); }
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
