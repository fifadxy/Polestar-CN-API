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
  private AlertDialog parkingDialog;
  private String parkingVin;
  private LocationSnapshot parkingPoint;
  private Switch mapLocationSwitch;
  private ParkingWebPreview parkingPreview;
  private boolean authPending;
  private int authGeneration;
  private boolean handlingWidget;
  private BiometricPrompt biometric;
  private EditText phone, password;
  private String inputPhone = "", inputPassword = "";
  private int campHours = 1;
  private boolean campKeepAir;
  private boolean resumeRender;
  private boolean initialCreation;
  private final Handler displayHandler = new Handler(Looper.getMainLooper());
  private final Runnable displayTick = new Runnable() {
    public void run() {
      if (model != null && page == 0 && model.snapshot != null
          ) onModelChanged();
      displayHandler.postDelayed(this, 60_000L);
    }
  };
  private static final int PICK_APK = 32, NOTIFICATIONS = 33, MAP_LOCATION = 34;
  private static final String HOME_DEFAULTS =
      "vehicle_status,odometer,ta_distance,tm_distance,ta_speed,tm_speed,ta_consumption,tm_consumption,temperature,aqi,charge_power";

  private String tr(String zh, String en) {
    return AppPreferences.tr(this, zh, en);
  }

  private String status(String s) {
    return AppPreferences.status(this, s);
  }

  private int dp(float f) {
    return (int) (f * getResources().getDisplayMetrics().density + .5f);
  }

  @Override protected void attachBaseContext(Context base) { super.attachBaseContext(UiTypography.wrap(base)); }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);initialCreation=b==null;
    if(b!=null)page=Math.max(0,Math.min(2,b.getInt("page",0)));
    AppState.setRegion(this, Region.CHINA);
    ParkingMapLinks.clearLegacySettings(this);
    regular = PsaDesign.regular();
    bold = PsaDesign.display();
    String previousHome="vehicle_status,odometer,ta_distance,tm_distance,ta_speed,tm_speed,tm_consumption,temperature,aqi,charge_power";
    if(previousHome.equals(AppPreferences.get(this,"home_metrics","")))AppPreferences.set(this,"home_metrics",HOME_DEFAULTS);
    model = VehicleCoordinator.get(this);
    model.listen(listener);
    render();
    if(FirstUseConsent.accepted(this))startConsented();else FirstUseConsent.show(this,this::startConsented);
  }
  private void startConsented(){
    handleWidgetIntent(getIntent());
    if (initialCreation && model.hasSession() && !model.busy) model.refresh(null);
    if (AppPreferences.enabled(this))
      try {
        ResidentService.apply(this);
      } catch (RuntimeException e) {
        DiagnosticLog.add("Resident start restricted");
      }
  }

  @Override protected void onSaveInstanceState(Bundle out) { out.putInt("page",page);super.onSaveInstanceState(out); }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleWidgetIntent(intent);
  }
  private void handleWidgetIntent(Intent i) {
    if(i!=null && "io.github.polestarcn.companion.WIDGET".equals(i.getAction()) && !i.hasExtra("widget_action"))Haptics.tap(this);
    if (!FirstUseConsent.accepted(this) || i == null || !i.hasExtra("widget_action") || handlingWidget) return;
    String request = i.getStringExtra("widget_action");
    i.removeExtra("widget_action");
    if ("refresh".equals(request)) { Haptics.tap(this); page = 0; render(); model.refreshUser(null); return; }
    if ("location".equals(request)) { page=0; render(); if(model.hasSession()&&model.selected!=null)
      locationDialog();else toast(tr("请先连接车辆","Connect vehicle first"));return; }
    boolean fixed=java.util.Arrays.asList("widget-lock","widget-unlock","widget-climate-start","widget-climate-stop").contains(request);
    if (!fixed&&!"lock-choice".equals(request) && !"climate-choice".equals(request)) return;
    page = 0;
    render();
    if (!model.hasSession() || model.selected == null || model.busy) {
      toast(tr("请先连接车辆，刷新后重试", "Connect and refresh the vehicle before controlling"));
      if (model.hasSession() && model.selected == null && !model.busy) model.refresh(null);
      return;
    }
    handlingWidget = true;
    Haptics.tap(this);
    if(fixed)authenticate(tr("Widget 车辆控制","Widget vehicle control"),request.substring(7));
    else directControl("lock-choice".equals(request) ? "lock" : "climate");
    handlingWidget = false;
  }

  @Override
  protected void onDestroy() {
    displayHandler.removeCallbacks(displayTick);
    dismissParking();
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
    dismissParking();
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

  private java.util.List<DisplayMetric> controlMetrics() {
    java.util.List<DisplayMetric> chosen =
        MetricSelection.read(this, "home_metrics", HOME_DEFAULTS);
    if (!showTires() || !AppPreferences.visible(this, "experimental_tire", false))
      chosen.remove(DisplayMetric.TIRE_STATUS);
    if (chosen.size() > 12) chosen = new java.util.ArrayList<>(chosen.subList(0, 12));
    return chosen;
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
            v.animate().alpha(.86f).setDuration(70L).start();
          } else if (event.getActionMasked() == MotionEvent.ACTION_UP
              || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            v.animate().cancel();
            v.animate()
                .alpha(1f)
                .setDuration(150L)
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

  private void palette() {
    boolean dark = AppPreferences.dark(this);
    ink = PsaDesign.ink(dark); muted = PsaDesign.muted(dark);
    paper = PsaDesign.canvas(dark); surface = PsaDesign.surface(dark); line = PsaDesign.line(dark);
    if (nioColors()) {
      paper = Color.parseColor(dark ? "#101722" : "#F4F9FC");
      surface = Color.parseColor(dark ? "#1C2532" : "#FFFFFF");
      line = Color.parseColor(dark ? "#384252" : "#DFE8EE");
    } else if (voLike() && !voPolestar()) {
      paper = Color.parseColor(dark ? "#111514" : "#F3EEE8");
      surface = Color.parseColor(dark ? "#1B211F" : "#FCF9F5");
      line = Color.parseColor(dark ? "#35403C" : "#E3DBD3");
    }
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
    dashboardBindings.clear();lineHeightCache.clear();
    palette();
    setTheme(AppPreferences.dark(this) ? R.style.AppThemeDark : R.style.AppThemeLight);
    shell = column();
    shell.setBackgroundColor(paper);
    if (nioColors()) shell.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        AppPreferences.dark(this) ? new int[]{Color.parseColor("#162D43"), paper, paper}
            : new int[]{Color.parseColor("#D6EDF9"), Color.WHITE, Color.parseColor("#F8F9FB")}));
    boolean controlDashboard = page == 0 && model.hasSession();
    if (!controlDashboard) {
      LinearLayout header = row();
      header.setPadding(dp(20), 0, dp(18), 0);
      ImageView brand = new ImageView(this);
      brand.setImageResource(R.drawable.ic_notification);brand.setColorFilter(ink);
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
    boolean compactNav = getResources().getConfiguration().screenWidthDp > getResources().getConfiguration().screenHeightDp
        || getResources().getConfiguration().screenHeightDp < 680 || getResources().getConfiguration().fontScale > 1.2f;
    LinearLayout nav = row();
    nav.setTag("bottom_nav");
    nav.setPadding(dp(compactNav ? 18 : 22), dp(3), dp(compactNav ? 18 : 22), dp(5));
    String[] names = voLike()
        ? new String[]{tr("车辆", "Vehicle"), tr("车况", "Status"), tr("设置", "Settings")}
        : new String[]{tr("车辆控制", "Control"), tr("车况", "Status"), tr("设置", "Settings")};
    for (int i = 0; i < names.length; i++) {
      final int tab = i;
      TextView t =
          button(
              names[i],
              () -> {
                page = tab;
                render();
              });
      t.setTag("bottom_nav_button");
      GradientDrawable navFrame = bg(i == page ? surface : paper, 12);
      navFrame.setStroke(dp(1), i == page ? line : Color.TRANSPARENT);
      t.setBackground(navFrame);
      t.setTextColor(i == page ? orange : muted);
      if (i < 3) {
        int resource = i == 0 ? R.drawable.ic_notification : i == 1 ? R.drawable.ic_nav_vehicle_data_ref : R.drawable.ic_settings_list_vi;
        android.graphics.drawable.Drawable navIcon = getDrawable(resource).mutate();
        navIcon.setTint(i == page ? orange : muted);
        navIcon.setBounds(0, 0, dp(compactNav ? 19 : 21), dp(compactNav ? 19 : 21));
        t.setCompoundDrawables(null, navIcon, null, null);
        t.setCompoundDrawablePadding(dp(2));
      }
      LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, -1, 1);
      nlp.leftMargin = dp(3); nlp.rightMargin = dp(3);
      nav.addView(t, nlp);
    }
    shell.addView(nav, new LinearLayout.LayoutParams(-1, dp(compactNav ? 52 : 64)));
    disableSystemHaptics(shell);setContentView(shell);
  }

  private void disableSystemHaptics(View view) {
    view.setHapticFeedbackEnabled(false);
    if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)disableSystemHaptics(group.getChildAt(i));}
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
    refresh.setColorSchemeColors(voLike()?voAccent():orange);
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
      if (UiSkin.PS_DEFAULT.equals(skin)) buildDashboard(false);
      else if (UiSkin.nioLayout(this)) buildDashboard(true);
      else buildVoLikeDashboard(UiSkin.DETAILED.equals(skin)||UiSkin.VOLIKE_POLESTAR.equals(skin));
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

  private final java.util.List<Runnable> dashboardBindings = new java.util.ArrayList<>();
  private boolean dashboardReady;

  private void bindDashboard(Runnable binding) {
    dashboardBindings.add(binding);
    binding.run();
  }

  private void onModelChanged() {
    if(model.progressOnly&&page!=0||page==2&&model.hasSession())return;
    if(parkingDialog!=null&&(!model.hasSession()||model.selected==null||!java.util.Objects.equals(parkingVin,model.selected.vin)))dismissParking();
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

  private final Map<Integer,Integer> lineHeightCache=new HashMap<>();
  private int textLineHeight(int sp) {
    Integer known=lineHeightCache.get(sp);if(known!=null)return known;
    int height=0;
    for(String sample:new String[]{"Ag °µ~", "总里程小时分"}) {
      TextView probe=text(sample,sp,ink,false);probe.setMaxLines(1);
      probe.measure(View.MeasureSpec.makeMeasureSpec(dp(1000),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
      height=Math.max(height,Math.max(probe.getMeasuredHeight(),probe.getLayout()==null?0:probe.getLayout().getHeight()));
    }
    height+=dp(1);lineHeightCache.put(sp,height);return height;
  }

  private String dashboardStamp() {
    VehicleSnapshot v = model.snapshot;
    String stamp = v == null || v.timestampMillis <= 0 ? tr("更新时间未知", "Update time unknown")
        : tr("更新 ", "Updated ") + android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis);
    if(model.commandPending)return stamp+tr(" · 正在刷新 "," · Refreshing ")+model.readbackPercent+"%";
    return (model.message.isEmpty() ? "" : model.message + " · ")
        + (AppPreferences.visible(this, "show_update", true) ? stamp : "");
  }

  private boolean nioSkin() { return UiSkin.nioLayout(this); }
  private boolean nioColors() { return UiSkin.nioColors(this); }
  private boolean voLike() { String skin=UiSkin.get(this); return UiSkin.SIMPLE.equals(skin)||UiSkin.DETAILED.equals(skin)||UiSkin.VOLIKE_POLESTAR.equals(skin); }
  private boolean voPolestar() { return UiSkin.polestarVo(this); }
  private int voGreen() { return Color.parseColor(AppPreferences.dark(this) ? "#79B79F" : "#5E8D7A"); }
  private int voAccent() { return voPolestar() ? orange : voGreen(); }
  private boolean showTires() { return AppPreferences.visible(this, "show_tire_status", true); }
  private int accentText() { return PsaDesign.accentText(AppPreferences.dark(this)); }

  private void addBatterySummary(LinearLayout parent, boolean compact) {
    LinearLayout headline = row(); headline.setTag("battery_summary");
    LinearLayout soc = column();
    TextView percent = liveText(() -> model.snapshot == null ? "--%"
        : VehicleSnapshot.metric(model.snapshot.batteryPercent, "%"), compact ? 18 : 30, ink, true);
    percent.setTag("battery_soc"); soc.addView(percent);
    ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    bar.setTag("vi_progress"); bar.setMax(100); styleViProgress(bar);
    if (nioColors()) bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(32,196,121)));
    else if (voLike()) bar.setProgressTintList(android.content.res.ColorStateList.valueOf(voAccent()));
    bindDashboard(() -> bar.setProgress(model.snapshot == null || model.snapshot.batteryPercent == null ? 0 : model.snapshot.batteryPercent));
    gap(soc, 3); soc.addView(bar, new LinearLayout.LayoutParams(-1, dp(3)));
    headline.addView(soc, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout range = column(); range.setGravity(Gravity.END);
    TextView km = liveText(() -> model.snapshot == null ? "-- km"
        : VehicleSnapshot.metric(model.snapshot.rangeKm, " km"), compact ? 18 : 26, ink, true);
    km.setTag("battery_range"); range.addView(km);
    if (!compact) range.addView(text(tr("预计续航", "Estimated range"), 10, muted, false));
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1); rp.leftMargin = dp(20);
    headline.addView(range, rp); parent.addView(headline);
  }

  private void addChargingSection(LinearLayout parent, boolean compact) {
    LinearLayout section=column();section.setTag("charging_section");
    section.setPadding(dp(10),dp(5),dp(10),dp(5));
    GradientDrawable frame=bg(surface,voLike()?18:nioSkin()?12:8);frame.setStroke(dp(1),line);section.setBackground(frame);

    LinearLayout row=row();
    ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_charge);
    int chargeAccent=voLike()?voAccent():nioColors()?Color.rgb(32,196,121):accentText();
    icon.setColorFilter(chargeAccent);row.addView(icon,new LinearLayout.LayoutParams(dp(20),dp(20)));

    LinearLayout lines=column();lines.setPadding(dp(7),0,0,0);
    float fontScale=getResources().getConfiguration().fontScale;
    int chargeSp=fontScale>1.4f?8:compact?9:11;
    int detailSp=fontScale>1.3f?8:9;
    TextView state=liveText(()->{
      VehicleSnapshot v=model.snapshot;boolean en=AppPreferences.english(this);
      return tr("正在充电","Charging")+" · "+DisplayMetric.CHARGE_POWER.value(this,v,true,en)
          +" · "+DisplayMetric.CHARGE_ETA.value(this,v,true,en);
    },chargeSp,ink,true);
    TextView values=liveText(()->{
      VehicleSnapshot v=model.snapshot;boolean en=AppPreferences.english(this);
      return DisplayMetric.CHARGE_TYPE.value(this,v,true,en)+" · "
          +DisplayMetric.CHARGE_CURRENT.value(this,v,true,en)+" · "
          +DisplayMetric.CHARGE_CONNECTION.value(this,v,true,en);
    },detailSp,muted,false);
    lines.addView(state);lines.addView(values);values.setVisibility(compact?View.GONE:View.VISIBLE);
    row.addView(lines,new LinearLayout.LayoutParams(0,-2,1));section.addView(row);

    ChargingPulseView pulse=new ChargingPulseView(this);pulse.setAccentColor(chargeAccent);
    section.addView(pulse,new LinearLayout.LayoutParams(-1,dp(7)));
    bindDashboard(()->{
      boolean active=AppPreferences.visible(this,"show_charging",true)&&ChargingVisibility.shouldShow(model.snapshot);
      section.setVisibility(active?View.VISIBLE:View.GONE);pulse.setActive(active);
      section.setContentDescription(active?tr("正在充电","Charging")+" · "
          +DisplayMetric.CHARGE_POWER.value(this,model.snapshot,true,AppPreferences.english(this)):"");
    });
    click(section,this::chargingDialog);
    int h=Math.max(dp(compact?40:48),textLineHeight(chargeSp)+(compact?0:textLineHeight(detailSp))+dp(20));
    parent.addView(section,new LinearLayout.LayoutParams(-1,h));
  }

  private void chargingDialog() {
    if (!ChargingVisibility.shouldShow(model.snapshot)) return;
    LinearLayout panel = column(); panel.setPadding(dp(12),dp(4),dp(12),dp(4));
    for (DisplayMetric metric : DisplayMetric.values()) if (ChargingVisibility.isChargingMetric(metric))
      detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,model.snapshot,true,AppPreferences.english(this)));
    new HapticDialogBuilder(this).setTitle(tr("充电详情","Charging details")).setView(panel).setPositiveButton(tr("关闭","Close"),null).show();
  }

  private void buildVoLikeDashboard(boolean detailed) {
    android.content.res.Configuration config=getResources().getConfiguration();
    boolean landscape=config.screenWidthDp>config.screenHeightDp;
    boolean compact=landscape||config.screenHeightDp<680||config.fontScale>1.2f;
    body.setPadding(0,0,0,dp(landscape?0:2));

    LinearLayout head=row();head.setTag("volike_header");
    TextView name=liveText(()->vehicleDisplayName(model.snapshot),landscape?12:16,ink,true);
    click(name,this::vehicleInfoDialog);head.addView(name,new LinearLayout.LayoutParams(0,-1,1));
    ImageView location=new ImageView(this);location.setImageResource(R.drawable.ic_location);location.setColorFilter(ink);
    location.setScaleType(ImageView.ScaleType.CENTER_INSIDE);location.setPadding(dp(9),dp(9),dp(9),dp(9));location.setTag("vehicle_location");
    click(location,this::locationDialog);bindDashboard(()->{location.setEnabled(!model.locationBusy&&!model.busy&&!model.commandPending&&model.selected!=null);location.setAlpha(location.isEnabled()?1f:.45f);});
    ImageView refresh=new ImageView(this);refresh.setImageResource(R.drawable.ic_refresh);refresh.setColorFilter(ink);refresh.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    refresh.setPadding(dp(9),dp(9),dp(9),dp(9));refresh.setTag("refresh_vehicle");click(refresh,()->model.refreshUser(null));
    bindDashboard(()->{refresh.setEnabled(!model.busy);refresh.setAlpha(refresh.isEnabled()?1f:.45f);});
    head.addView(parkingCaption(),new LinearLayout.LayoutParams(dp(68),-1));
    head.addView(location,new LinearLayout.LayoutParams(dp(40),-1));head.addView(refresh,new LinearLayout.LayoutParams(dp(40),-1));
    body.addView(head,new LinearLayout.LayoutParams(-1,dp(landscape?36:42)));

    java.util.List<DisplayMetric> chosen=controlMetrics();
    chosen.removeIf(metric->ChargingVisibility.isChargingMetric(metric)||metric==DisplayMetric.VEHICLE_STATUS
        ||metric==DisplayMetric.BATTERY||metric==DisplayMetric.RANGE);

    if(landscape){
      LinearLayout middle=row();middle.setTag("volike_landscape");
      LinearLayout hero=voHero(true);
      middle.addView(hero,new LinearLayout.LayoutParams(0,-1,.43f));
      LinearLayout right=column();right.setPadding(dp(7),0,0,0);
      addVoMetricGrid(right,chosen,detailed,true);
      addVoQuickActions(right,true);
      addVoUtilities(right,true);
      middle.addView(right,new LinearLayout.LayoutParams(0,-1,.57f));
      body.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
      addChargingSection(body,true);
    } else {
      boolean referencePortrait=config.screenHeightDp>=680&&config.fontScale<=1.2f;
      if(referencePortrait){
        body.addView(voEnergyStrip(detailed,false),new LinearLayout.LayoutParams(-1,dp(62)));
        gap(body,4);
        body.addView(voPortraitStage(detailed,false),new LinearLayout.LayoutParams(-1,0,1));
        gap(body,4);
        addChargingSection(body,false);
        addVoMetricGrid(body,chosen,detailed,false);
        addVoUtilities(body,false);
      } else {
        // Small-height / large-text phones keep the v0.8.1 compact structure so
        // every remote-control tile remains at least 48–60dp and fully visible.
        body.addView(voHero(false),new LinearLayout.LayoutParams(
            -1,dp(compact?150:detailed?178:170),1));
        addChargingSection(body,compact);
        addVoMetricGrid(body,chosen,detailed,false);
        addVoQuickActions(body,false);
        addVoUtilities(body,false);
      }
      TextView footer=liveText(this::dashboardStamp,8,muted,false);footer.setGravity(Gravity.CENTER_VERTICAL);
      click(footer,()->info(tr("车况","Vehicle status"),dashboardStamp()));
      body.addView(footer,new LinearLayout.LayoutParams(-1,textLineHeight(8)+dp(2)));
    }
    dashboardReady=true;
  }

  private LinearLayout voEnergyStrip(boolean detailed,boolean compact) {
    LinearLayout card=column();card.setTag("volike_energy_strip");
    card.setPadding(dp(11),dp(5),dp(11),dp(5));
    GradientDrawable frame=bg(surface,18);frame.setStroke(dp(1),line);card.setBackground(frame);

    LinearLayout line1=row();line1.setGravity(Gravity.CENTER_VERTICAL);
    TextView soc=liveText(()->model.snapshot==null?"--%":VehicleSnapshot.metric(model.snapshot.batteryPercent,"%"),
        compact?17:21,ink,true);soc.setTag("battery_soc");line1.addView(soc,new LinearLayout.LayoutParams(0,-2,.72f));
    TextView range=liveText(()->model.snapshot==null?"-- km":VehicleSnapshot.metric(model.snapshot.rangeKm," km"),
        compact?16:20,ink,true);range.setTag("battery_range");range.setGravity(Gravity.END);
    line1.addView(range,new LinearLayout.LayoutParams(0,-2,.72f));
    TextView state=liveText(()->ChargingVisibility.shouldShow(model.snapshot)
        ?tr("充电中","Charging"):tr("车辆已连接","Vehicle ready"),compact?8:9,
        ChargingVisibility.shouldShow(model.snapshot)?voAccent():muted,true);
    state.setGravity(Gravity.END);line1.addView(state,new LinearLayout.LayoutParams(0,-2,.74f));
    card.addView(line1);

    ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    bar.setTag("vi_progress");bar.setMax(100);styleViProgress(bar);
    bar.setProgressTintList(android.content.res.ColorStateList.valueOf(voAccent()));
    bindDashboard(()->bar.setProgress(model.snapshot==null||model.snapshot.batteryPercent==null?0:model.snapshot.batteryPercent));
    card.addView(bar,new LinearLayout.LayoutParams(-1,dp(4)));

    if(!compact&&detailed){
      TextView detail=liveText(()->{
        VehicleSnapshot v=model.snapshot;
        if(v==null)return tr("预计续航 --","Estimated range --");
        String eta=ChargingVisibility.shouldShow(v)
            ?DisplayMetric.CHARGE_ETA.value(this,v,true,AppPreferences.english(this)):"";
        return tr("电量 ","Battery ")+VehicleSnapshot.metric(v.batteryPercent,"%")
            +" · "+tr("预计续航 ","Estimated range ")+VehicleSnapshot.metric(v.rangeKm," km")
            +(eta.isEmpty()?"":" · "+tr("充满 ","Full in ")+eta);
      },8,muted,false);
      detail.setSingleLine(true);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
      card.addView(detail);
    }
    return card;
  }

  private LinearLayout voPortraitStage(boolean detailed,boolean compact) {
    LinearLayout stage=column();stage.setTag("volike_hero");
    stage.setPadding(dp(7),dp(6),dp(7),dp(5));
    GradientDrawable frame=bg(surface,24);frame.setStroke(dp(1),line);stage.setBackground(frame);

    LinearLayout middle=row();middle.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout controls=column();controls.setTag("volike_side_controls");controls.setGravity(Gravity.CENTER);
    for(String control:new String[]{"lock","climate","air","trunk"})
      addVoSideControl(controls,control,compact);
    middle.addView(controls,new LinearLayout.LayoutParams(dp(compact?56:64),-1));

    VehicleHeroView car=new VehicleHeroView(this);car.setTag("polestar_vehicle");
    car.setContentDescription(tr("查看车门与车窗状态","View doors and windows"));
    click(car,this::exteriorDialog);
    car.setVisibility(AppPreferences.visible(this,"show_car",true)?View.VISIBLE:View.INVISIBLE);
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);
    cp.leftMargin=dp(4);cp.rightMargin=dp(detailed?4:0);middle.addView(car,cp);

    if(detailed) middle.addView(voDetailRail(compact),
        new LinearLayout.LayoutParams(dp(compact?86:96),-1));
    stage.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
    stage.addView(voStatusStrip(compact),new LinearLayout.LayoutParams(-1,dp(compact?26:30)));
    return stage;
  }

  private void addVoSideControl(LinearLayout parent,String control,boolean compact) {
    String title=control.equals("lock")?tr("门锁","Lock"):control.equals("climate")?tr("空调","Climate")
        :control.equals("air")?tr("净化","Air"):tr("尾箱","Trunk");
    LinearLayout item=column();item.setTag("control_tile");item.setGravity(Gravity.CENTER);
    FrameLayout circle=new FrameLayout(this);circle.setTag("volike_control_circle");
    int size=dp(compact?34:40);circle.setBackground(oval(viSoft()));
    ImageView icon=new ImageView(this);icon.setImageResource(control.equals("lock")?R.drawable.ic_lock
        :control.equals("climate")?R.drawable.ic_climate:control.equals("air")?R.drawable.ic_air:R.drawable.ic_tailgate);
    icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setTag("control_icon_"+control);
    circle.addView(icon,new FrameLayout.LayoutParams(dp(compact?19:22),dp(compact?19:22),Gravity.CENTER));
    item.addView(circle,new LinearLayout.LayoutParams(size,size));

    boolean hideTitle=UiSkin.SIMPLE.equals(UiSkin.get(this))
        && !AppPreferences.visible(this,"volike_simple_labels",true);
    TextView label=text(title,compact?7:8,ink,false);label.setTag("volike_control_label");
    label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setVisibility(hideTitle?View.GONE:View.VISIBLE);
    item.addView(label,new LinearLayout.LayoutParams(-1,hideTitle?0:textLineHeight(compact?7:8)));

    bindDashboard(()->{
      String raw=controlRaw(control);icon.setImageResource(controlIcon(control,raw));
      boolean active="开启".equals(raw)||"未锁".equals(raw)||"已打开".equals(raw)||"未关严".equals(raw);
      boolean warning=control.equals("lock")&&"未锁".equals(raw)||control.equals("trunk")&&active;
      int fill=warning?orange:(active&&(control.equals("climate")||control.equals("air"))?voAccent():viSoft());
      circle.setBackground(oval(fill));
      icon.setColorFilter((warning||active&&(control.equals("climate")||control.equals("air")))?Color.WHITE:ink);
      item.setEnabled(!model.busy&&!model.commandPending&&model.selected!=null);item.setAlpha(item.isEnabled()?1f:.5f);
      item.setContentDescription(title+" · "+controlFeedback(control,true));
    });
    click(item,()->directControl(control));
    parent.addView(item,new LinearLayout.LayoutParams(-1,0,1));
  }

  private LinearLayout voDetailRail(boolean compact) {
    LinearLayout rail=column();rail.setTag("volike_detail_rail");
    rail.setPadding(dp(5),dp(5),dp(5),dp(5));
    GradientDrawable bg=bg(viSoft(),16);bg.setStroke(dp(1),line);rail.setBackground(bg);

    TextView title=text(tr("车况","Status"),compact?9:10,ink,true);
    title.setGravity(Gravity.CENTER);click(title,this::vehicleStatusDialog);rail.addView(title);

    addVoRailProgress(rail,tr("时间","Time"),()->ServiceReading.timePercent(this,model.snapshot),compact);
    addVoRailProgress(rail,tr("里程","Distance"),()->ServiceReading.distancePercent(this,model.snapshot),compact);

    TextView temp=liveText(()->model.snapshot==null?"--°C":
        DisplayMetric.TEMPERATURE.value(this,model.snapshot,true,AppPreferences.english(this)),
        compact?9:11,ink,true);temp.setGravity(Gravity.CENTER);rail.addView(temp);
    TextView location=text(tr("停车位置","Parked"),compact?7:8,muted,false);
    location.setGravity(Gravity.CENTER);location.setSingleLine(true);click(location,this::locationDialog);
    rail.addView(location,new LinearLayout.LayoutParams(-1,0,1));
    return rail;
  }

  private void addVoRailProgress(LinearLayout rail,String label,java.util.function.Supplier<Integer> value,boolean compact) {
    TextView text=liveText(()->label+" "+ServiceReading.number(value.get(),"%"),compact?7:8,muted,false);
    text.setGravity(Gravity.CENTER);rail.addView(text);
    ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    bar.setMax(100);bar.setTag("volike_service_bar");styleViProgress(bar);
    bar.setProgressTintList(android.content.res.ColorStateList.valueOf(voAccent()));
    bindDashboard(()->{Integer v=value.get();bar.setProgress(v==null?0:v);bar.setVisibility(v==null?View.INVISIBLE:View.VISIBLE);});
    rail.addView(bar,new LinearLayout.LayoutParams(-1,dp(3)));
    gap(rail,compact?2:3);
  }

  private LinearLayout voStatusStrip(boolean compact) {
    LinearLayout statusRow=row();statusRow.setTag("vehicle_status_button");statusRow.setGravity(Gravity.CENTER_VERTICAL);
    ImageView symbol=new ImageView(this);symbol.setTag("health_symbol");statusRow.addView(symbol,new LinearLayout.LayoutParams(dp(16),dp(16)));
    TextView label=text(tr("车况","Status"),compact?9:10,ink,true);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-1);lp.leftMargin=dp(5);statusRow.addView(label,lp);
    View spacer=new View(this);statusRow.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
    TextView summary=text("",compact?7:8,muted,false);summary.setSingleLine(true);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
    statusRow.addView(summary,new LinearLayout.LayoutParams(0,-1,1.7f));
    click(statusRow,this::vehicleStatusDialog);
    bindDashboard(()->{
      boolean warn=VehicleCondition.problem(model.snapshot,showTires());
      boolean good=VehicleCondition.reported(model.snapshot,showTires())&&!warn;
      label.setText(warn?tr("请注意车辆状况！","Check vehicle!"):good?"All Good！":tr("车况","Status"));
      symbol.setImageResource(good?R.drawable.ic_check:R.drawable.ic_warning);
      int color=warn?orange:good?voAccent():muted;symbol.setColorFilter(color);
      symbol.setVisibility(good||warn?View.VISIBLE:View.INVISIBLE);
      summary.setText((model.snapshot==null?"--":status(model.snapshot.lock))+" · "
          +(model.snapshot==null?"--":status(model.snapshot.windows)));
      statusRow.setContentDescription(tr("车况","Status")+" · "
          +VehicleCondition.summary(model.snapshot,AppPreferences.english(this),showTires()));
    });
    return statusRow;
  }

  private LinearLayout voHero(boolean landscape) {
    LinearLayout hero=column();hero.setTag("volike_hero");hero.setPadding(dp(10),dp(7),dp(10),dp(5));
    GradientDrawable frame=bg(surface,landscape?18:24);frame.setStroke(dp(1),line);hero.setBackground(frame);

    LinearLayout top=row();
    LinearLayout data=column();data.setGravity(Gravity.CENTER_VERTICAL);
    TextView range=liveText(()->model.snapshot==null?"-- km":VehicleSnapshot.metric(model.snapshot.rangeKm," km"),landscape?23:32,ink,true);
    range.setTag("battery_range");data.addView(range);
    LinearLayout socRow=row();
    TextView soc=liveText(()->model.snapshot==null?"--%":VehicleSnapshot.metric(model.snapshot.batteryPercent,"%"),landscape?13:16,ink,true);
    soc.setTag("battery_soc");socRow.addView(soc);
    TextView caption=text(tr(" 电量"," Battery"),landscape?9:10,muted,false);socRow.addView(caption);data.addView(socRow);
    ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setTag("vi_progress");bar.setMax(100);
    styleViProgress(bar);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(voAccent()));
    bindDashboard(()->bar.setProgress(model.snapshot==null||model.snapshot.batteryPercent==null?0:model.snapshot.batteryPercent));
    data.addView(bar,new LinearLayout.LayoutParams(-1,dp(3)));
    top.addView(data,new LinearLayout.LayoutParams(0,-1,.78f));

    VehicleHeroView car=new VehicleHeroView(this);car.setTag("polestar_vehicle");
    car.setContentDescription(tr("查看车门与车窗状态","View doors and windows"));click(car,this::exteriorDialog);
    car.setVisibility(AppPreferences.visible(this,"show_car",true)?View.VISIBLE:View.INVISIBLE);
    top.addView(car,new LinearLayout.LayoutParams(0,-1,1.22f));
    hero.addView(top,new LinearLayout.LayoutParams(-1,0,1));

    LinearLayout statusRow=row();statusRow.setTag("vehicle_status_button");statusRow.setGravity(Gravity.CENTER_VERTICAL);
    ImageView symbol=new ImageView(this);symbol.setTag("health_symbol");statusRow.addView(symbol,new LinearLayout.LayoutParams(dp(17),dp(17)));
    TextView label=text(tr("车况","Status"),landscape?10:12,ink,true);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-1);lp.leftMargin=dp(5);statusRow.addView(label,lp);
    View spacer=new View(this);statusRow.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
    TextView summary=text("",landscape?8:9,muted,false);summary.setSingleLine(true);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
    statusRow.addView(summary,new LinearLayout.LayoutParams(0,-1,1.6f));
    click(statusRow,this::vehicleStatusDialog);
    bindDashboard(()->{
      boolean warn=VehicleCondition.problem(model.snapshot,showTires());
      boolean good=VehicleCondition.reported(model.snapshot,showTires())&&!warn;
      label.setText(warn?tr("请注意车辆状况！","Check vehicle!"):good?"All Good！":tr("车况","Status"));
      symbol.setImageResource(good?R.drawable.ic_check:R.drawable.ic_warning);
      int color=warn?orange:good?voAccent():muted;symbol.setColorFilter(color);
      symbol.setVisibility(good||warn?View.VISIBLE:View.INVISIBLE);
      summary.setText((model.snapshot==null?"--":status(model.snapshot.lock))+" · "
          +(model.snapshot==null?"--":status(model.snapshot.windows)));
      statusRow.setContentDescription(tr("车况","Status")+" · "+VehicleCondition.summary(model.snapshot,AppPreferences.english(this),showTires()));
    });
    hero.addView(statusRow,new LinearLayout.LayoutParams(-1,dp(landscape?26:32)));
    return hero;
  }

  private void addVoMetricGrid(LinearLayout parent,java.util.List<DisplayMetric> chosen,boolean detailed,boolean landscape) {
    int max=detailed?12:4;
    // At 1.5x+ type on a 320dp-high landscape phone, prioritize one
    // readable metric row. Saved selection remains untouched.
    if(landscape && getResources().getConfiguration().fontScale>1.4f)
      max=Math.min(max,6);
    int count=Math.min(max,chosen.size());if(count==0)return;
    int columns=detailed?Math.min(6,count):Math.min(4,count);
    int rows=(count+columns-1)/columns;
    int rowHeight=Math.max(dp(landscape?32:36),
        textLineHeight(landscape?9:10)+textLineHeight(landscape?7:8)+dp(12));
    LinearLayout group=column();group.setTag("volike_metrics");
    group.setPadding(dp(2),0,dp(2),0);
    for(int start=0;start<count;start+=columns){
      LinearLayout row=row();
      for(int j=0;j<columns;j++){
        LinearLayout cell=column();cell.setGravity(Gravity.CENTER);
        int index=start+j;
        if(index<count){
          DisplayMetric metric=chosen.get(index);
          TextView value=liveText(()->metric.value(this,model.snapshot,false,AppPreferences.english(this)),landscape?9:10,ink,true);
          value.setGravity(Gravity.CENTER);TextView label=text(metric.shortLabel(AppPreferences.english(this)),landscape?7:8,muted,false);
          label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
          cell.addView(value,new LinearLayout.LayoutParams(-1,0,1));cell.addView(label,new LinearLayout.LayoutParams(-1,textLineHeight(landscape?7:8)));
          click(cell,()->info(metric.label(AppPreferences.english(this)),metric.value(this,model.snapshot,true,AppPreferences.english(this))));
        }
        row.addView(cell,new LinearLayout.LayoutParams(0,-1,1));
      }
      group.addView(row,new LinearLayout.LayoutParams(-1,rowHeight));
    }
    parent.addView(group,new LinearLayout.LayoutParams(-1,rowHeight*rows));
  }

  private void addVoQuickActions(LinearLayout parent,boolean landscape) {
    LinearLayout card=row();card.setTag("volike_quick_actions");card.setPadding(dp(5),dp(4),dp(5),dp(4));
    GradientDrawable frame=bg(surface,landscape?16:22);frame.setStroke(dp(1),line);card.setBackground(frame);
    for(String control:new String[]{"lock","climate","air","trunk"})addVoControl(card,control,landscape);
    parent.addView(card,new LinearLayout.LayoutParams(-1,dp(landscape?58:72)));
  }

  private void addVoControl(LinearLayout parent,String control,boolean landscape) {
    String title=control.equals("lock")?tr("门锁","Lock"):control.equals("climate")?tr("空调","Climate")
        :control.equals("air")?tr("净化","Air"):tr("尾箱","Trunk");
    LinearLayout item=column();item.setTag("control_tile");item.setGravity(Gravity.CENTER);
    FrameLayout circle=new FrameLayout(this);circle.setTag("volike_control_circle");
    int size=dp(landscape?28:40);circle.setBackground(oval(viSoft()));
    ImageView icon=new ImageView(this);icon.setImageResource(control.equals("lock")?R.drawable.ic_lock:control.equals("climate")?R.drawable.ic_climate
        :control.equals("air")?R.drawable.ic_air:R.drawable.ic_tailgate);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setTag("control_icon_"+control);
    circle.addView(icon,new FrameLayout.LayoutParams(dp(landscape?17:22),dp(landscape?17:22),Gravity.CENTER));
    item.addView(circle,new LinearLayout.LayoutParams(size,size));
    TextView label=text(title,landscape?8:9,ink,false);label.setGravity(Gravity.CENTER);label.setSingleLine(true);
    boolean hideTitle=UiSkin.SIMPLE.equals(UiSkin.get(this))
        && !AppPreferences.visible(this,"volike_simple_labels",true);
    label.setVisibility(hideTitle?View.GONE:View.VISIBLE);
    label.setTag("volike_control_label");
    item.addView(label,new LinearLayout.LayoutParams(-1,hideTitle?0:textLineHeight(landscape?8:9)+dp(1)));
    bindDashboard(()->{
      String raw=controlRaw(control);icon.setImageResource(controlIcon(control,raw));
      boolean active="开启".equals(raw)||"未锁".equals(raw)||"已打开".equals(raw)||"未关严".equals(raw);
      boolean warning=control.equals("lock")&&"未锁".equals(raw)||control.equals("trunk")&&active;
      int fill=warning?orange:(active&&(control.equals("climate")||control.equals("air"))?voAccent():viSoft());
      circle.setBackground(oval(fill));
      icon.setColorFilter((warning||active&&(control.equals("climate")||control.equals("air")))?Color.WHITE:ink);
      item.setEnabled(!model.busy&&!model.commandPending&&model.selected!=null);item.setAlpha(item.isEnabled()?1f:.5f);
      item.setContentDescription(title+" · "+controlFeedback(control,true));
    });
    click(item,()->directControl(control));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(parent.getChildCount()>0)lp.leftMargin=dp(2);parent.addView(item,lp);
  }

  private void addVoUtilities(LinearLayout parent,boolean landscape) {
    LinearLayout row=row();row.setTag("volike_utilities");
    VehicleConditionButton condition=conditionButton(tr("车况","Status"));
    TextView windows=utility(tr("车窗","Windows"),false,false,()->directControl("windows"));windows.setTag("windows_control_button");
    TextView camp=utility(tr("露营模式","Camping mode"),false,false,this::campDialog);camp.setTag("camp_button");utilityIcon(camp,R.drawable.ic_camp_clock_vi);camp.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    TextView signal=utility(tr("寻车","Find car"),false,false,this::signalMenu);signal.setTag("signal_control_button");
    bindDashboard(()->{
      boolean warn=VehicleCondition.problem(model.snapshot,showTires());
      condition.setText(tr("车况","Status"));condition.refreshService();
      ((GradientDrawable)condition.getBackground()).setStroke(dp(1),warn?orange:line);
      boolean active=CampModeManager.active(this)||"STOPPING".equals(CampModeManager.phase(this));((GradientDrawable)camp.getBackground()).setColor(active?voAccent():surface);
      camp.setTextColor(active?Color.WHITE:ink);tintUtilityIcon(camp);
    });
    int h=dp(landscape?46:56);
    row.addView(condition,new LinearLayout.LayoutParams(0,h,1f));
    for(TextView view:new TextView[]{windows,camp,signal}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,h,1);lp.leftMargin=dp(4);row.addView(view,lp);}
    parent.addView(row,new LinearLayout.LayoutParams(-1,h));
  }

  private void buildDashboard(boolean simple) {
    android.content.res.Configuration config = getResources().getConfiguration();
    boolean landscape = config.screenWidthDp > config.screenHeightDp;
    boolean compact = landscape || config.screenHeightDp < 680 || config.fontScale > 1.2f;
    body.setPadding(0,0,0,dp(landscape?0:2));
    LinearLayout head = row();
    TextView name = liveText(() -> vehicleDisplayName(model.snapshot),landscape?13:17,ink,true);
    click(name,this::vehicleInfoDialog);
    if(landscape){LinearLayout identity=column();identity.addView(name);
      TextView updated=liveText(this::dashboardStamp,9,muted,false);click(updated,()->info(tr("更新时间","Update time"),dashboardStamp()));identity.addView(updated);
      head.addView(identity,new LinearLayout.LayoutParams(0,-2,1));}
    else head.addView(name,new LinearLayout.LayoutParams(0,-2,1));
    ImageView refresh = new ImageView(this); refresh.setImageResource(R.drawable.ic_refresh);
    refresh.setColorFilter(ink); refresh.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    refresh.setPadding(dp(12),dp(12),dp(12),dp(12)); refresh.setTag("refresh_vehicle");
    GradientDrawable refreshFrame=bg(Color.TRANSPARENT,nioSkin()?24:8);refreshFrame.setStroke(dp(1),line);refresh.setBackground(refreshFrame);
    click(refresh,()->model.refreshUser(null));
    refresh.setContentDescription(tr("刷新车辆状态","Refresh vehicle status"));
    bindDashboard(()->{refresh.setEnabled(!model.busy);refresh.setAlpha(model.busy?.4f:1f);});
    if(landscape) {LinearLayout energy=column();addBatterySummary(energy,true);
      LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(dp(230),-2);ep.leftMargin=dp(8);ep.rightMargin=dp(8);head.addView(energy,ep);}
    ImageView location=new ImageView(this);location.setTag("vehicle_location");location.setImageResource(R.drawable.ic_location);
    location.setColorFilter(ink);location.setScaleType(ImageView.ScaleType.CENTER_INSIDE);location.setPadding(dp(10),dp(10),dp(10),dp(10));
    location.setContentDescription(tr("最后停车位置","Last parked location"));click(location,this::locationDialog);
    bindDashboard(()->{location.setEnabled(!model.locationBusy&&!model.busy&&!model.commandPending&&model.selected!=null);location.setAlpha(model.locationBusy?.4f:1f);});
    head.addView(parkingCaption(),new LinearLayout.LayoutParams(dp(68),-1));
    head.addView(location,new LinearLayout.LayoutParams(dp(44),dp(48)));
    head.addView(refresh,new LinearLayout.LayoutParams(dp(48),dp(48)));
    body.addView(head,new LinearLayout.LayoutParams(-1,dp(48)));
    java.util.List<DisplayMetric> chosen=controlMetrics();
    chosen.removeIf(m->ChargingVisibility.isChargingMetric(m)||m==DisplayMetric.VEHICLE_STATUS||m==DisplayMetric.BATTERY||m==DisplayMetric.RANGE);
    // A 320dp-tall landscape viewport cannot display two dense rows at enlarged
    // system font sizes without clipping. Keep the 12-item user selection intact;
    // show the six highest-priority entries on this viewport and retain the rest
    // in the vehicle data/details page.
    if (landscape && config.screenHeightDp<=360 && config.fontScale>1.15f && chosen.size()>6)
      chosen=new java.util.ArrayList<>(chosen.subList(0,6));
    if (landscape) {
      LinearLayout middle=row();
      LinearLayout visual=vehicleVisual();
      middle.addView(visual,new LinearLayout.LayoutParams(0,-1,.34f));
      LinearLayout metrics=column(); metrics.setGravity(Gravity.CENTER_VERTICAL); addMetricGrid(metrics,model.snapshot,chosen,true);
      middle.addView(metrics,new LinearLayout.LayoutParams(0,-1,.66f));
      body.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
    } else {
      addBatterySummary(body,compact);
      if (nioSkin() || simple) {
        body.addView(vehicleVisual(),new LinearLayout.LayoutParams(-1,0,1));
        addMetricGrid(body,model.snapshot,chosen,true);
      } else {
        addMetricGrid(body,model.snapshot,chosen,true);
        body.addView(vehicleVisual(),new LinearLayout.LayoutParams(-1,0,1));
      }
    }
    addChargingSection(body,compact);
    if(!landscape)gap(body,3);
    if(nioSkin()&&!compact) addNioActions(body);
    else {
      addDashboardUtilities(body,landscape); if(!landscape)gap(body,4);
      boolean fourAcross=simple||compact;
      String[] order=simple?new String[]{"lock","climate","air","trunk"}:new String[]{"lock","climate","trunk","air"};
      int height=fourAcross?(landscape?Math.max(dp(48),dp(8)+2*textLineHeight(10)):Math.max(dp(68),dp(nioSkin()?46:34)+2*textLineHeight(10)))
          :dp(22)+textLineHeight(14)+textLineHeight(12)+textLineHeight(11);
      for(int i=0;i<4;i+=fourAcross?4:2) {
        LinearLayout actions=row(); for(int j=i;j<Math.min(4,i+(fourAcross?4:2));j++) addDashboardControl(actions,order[j],fourAcross,simple);
        body.addView(actions,new LinearLayout.LayoutParams(-1,height)); if(i==0&&!fourAcross)gap(body,4);
      }
    }
    TextView footer=liveText(this::dashboardStamp,9,muted,false); footer.setGravity(Gravity.CENTER_VERTICAL);
    click(footer,()->info(tr("车况","Vehicle status"),dashboardStamp()));
    if(!landscape)body.addView(footer,new LinearLayout.LayoutParams(-1,textLineHeight(9)+dp(4))); dashboardReady=true;
  }

  private LinearLayout vehicleVisual() {
    LinearLayout box=column();
    ImageView car=nioSkin()?new VehicleHeroView(this):new ImageView(this);
    car.setTag("polestar_vehicle");
    if(!nioSkin()){car.setImageResource(R.drawable.vehicle_top);car.setScaleType(ImageView.ScaleType.FIT_CENTER);stylePolestarVehicle(car);}
    car.setVisibility(AppPreferences.visible(this,"show_car",true)?View.VISIBLE:View.INVISIBLE);
    car.setContentDescription(tr("查看车门与车窗状态","View doors and windows")); click(car,this::exteriorDialog);
    box.addView(car,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout health=row();health.setTag("vehicle_status_button");health.setGravity(Gravity.CENTER);
    ImageView symbol=new ImageView(this); symbol.setTag("health_symbol");
    boolean compact=getResources().getConfiguration().screenWidthDp>getResources().getConfiguration().screenHeightDp
        ||getResources().getConfiguration().screenHeightDp<680||getResources().getConfiguration().fontScale>1.2f;
    int summarySp=compact?15:24;
    TextView label=text("",summarySp,accentText(),true);label.setMaxLines(1);label.setSingleLine(true);
    health.addView(symbol,new LinearLayout.LayoutParams(dp(18),dp(18)));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(5);health.addView(label,lp);
    click(health,this::vehicleStatusDialog);health.setContentDescription(tr("查看车况","View vehicle status"));
    bindDashboard(()->{
      VehicleHealthSnapshot h=model.snapshot==null?VehicleHealthSnapshot.unknown():model.snapshot.health;
      boolean warn=VehicleCondition.problem(model.snapshot,showTires()),good=VehicleCondition.reported(model.snapshot,showTires())&&!warn;
      label.setText(warn?tr("请注意车辆状况！","Check vehicle!"):good?"All Good！":tr("车况","Status"));label.setTextColor(good||warn?accentText():muted);
      symbol.setImageResource(good?R.drawable.ic_check:R.drawable.ic_warning);symbol.setColorFilter(good||warn?accentText():muted);
      symbol.setVisibility(good||warn?View.VISIBLE:View.GONE);
      health.setContentDescription(tr("车况","Status")+" · "+VehicleCondition.summary(model.snapshot,AppPreferences.english(this),showTires()));
    });
    box.addView(health,new LinearLayout.LayoutParams(-1,Math.max(dp(32),textLineHeight(summarySp)+dp(8))));
    return box;
  }

  private void utilityIcon(TextView view,int resource) {
    android.graphics.drawable.Drawable icon=getDrawable(resource).mutate();icon.setTintList(new android.content.res.ColorStateList(new int[][]{new int[0]},new int[]{ink}));
    icon.setBounds(0,0,dp(16),dp(16));view.setCompoundDrawablesRelative(icon,null,null,null);view.setCompoundDrawablePadding(dp(4));view.setGravity(Gravity.CENTER);
  }

  private void tintUtilityIcon(TextView view){for(android.graphics.drawable.Drawable icon:view.getCompoundDrawablesRelative())if(icon!=null)icon.setTint(view.getCurrentTextColor());}

  private VehicleConditionButton conditionButton(String title) {
    VehicleConditionButton button=new VehicleConditionButton(this,()->model.snapshot);
    button.setText(title);button.setTextSize(11);button.setTextColor(ink);button.setTypeface(regular);button.setGravity(Gravity.CENTER);
    button.setSingleLine(true);button.setEllipsize(android.text.TextUtils.TruncateAt.END);
    GradientDrawable frame=bg(surface,nioSkin()?12:8);frame.setStroke(dp(1),line);button.setBackground(frame);
    button.setTag("exterior_button");click(button,this::vehicleStatusDialog);button.refreshService();return button;
  }

  private void addNioActions(LinearLayout parent) {
    LinearLayout shortcuts=row();
    addDashboardControl(shortcuts,"lock",true,true);
    addNioShortcut(shortcuts,"windows",R.drawable.ic_window,tr("车窗","Windows"),()->directControl("windows"));
    addDashboardControl(shortcuts,"trunk",true,true);
    addNioShortcut(shortcuts,"signal",R.drawable.ic_horn,tr("寻车","Find car"),this::signalMenu);
    parent.addView(shortcuts,new LinearLayout.LayoutParams(-1,dp(80)));gap(parent,6);
    LinearLayout features=row();
    addDashboardControl(features,"climate",false,false);
    addDashboardControl(features,"air",false,false);
    parent.addView(features,new LinearLayout.LayoutParams(-1,Math.max(dp(84),textLineHeight(14)+textLineHeight(22)+textLineHeight(12)+textLineHeight(11)+dp(8))));gap(parent,5);
    LinearLayout utilities=row();
    VehicleConditionButton bodyButton=conditionButton(tr("车况","Status"));
    TextView camp=utility(tr("露营","Camp"),false,false,this::campDialog);camp.setTag("camp_button");
    bindDashboard(()->{
      boolean opened=VehicleCondition.problem(model.snapshot,showTires()),active=CampModeManager.active(this);
      bodyButton.setText(tr("车况","Status")+(opened?" !":""));
      ((GradientDrawable)bodyButton.getBackground()).setStroke(dp(1),opened?orange:line);bodyButton.setTextColor(opened?accentText():ink);bodyButton.refreshService();
      ((GradientDrawable)camp.getBackground()).setColor(active?orange:surface);camp.setTextColor(active?Color.BLACK:ink);camp.setSingleLine(false);camp.setMaxLines(2);camp.setText(tr("露营模式","Camping mode")+(active?"\n"+RuntimeLabels.camp(this):""));tintUtilityIcon(camp);
    });
    utilities.addView(bodyButton,new LinearLayout.LayoutParams(0,dp(58),1));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(58),1);cp.leftMargin=dp(6);utilities.addView(camp,cp);parent.addView(utilities);
  }

  private void addNioOrb(LinearLayout parent,ImageView icon) {
    boolean landscape=getResources().getConfiguration().screenWidthDp>getResources().getConfiguration().screenHeightDp;
    int size=landscape?32:38, slotHeight=landscape?36:42;
    FrameLayout slot=new FrameLayout(this);slot.setTag("shortcut_orb_slot");
    FrameLayout circle=new FrameLayout(this);circle.setTag("shortcut_circle");
    GradientDrawable outline=oval(Color.TRANSPARENT);outline.setStroke(dp(1),line);circle.setBackground(outline);
    FrameLayout.LayoutParams image=new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER);circle.addView(icon,image);
    slot.addView(circle,new FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER));
    if(parent.getOrientation()==LinearLayout.HORIZONTAL)
      parent.addView(slot,new LinearLayout.LayoutParams(dp(size+8),dp(slotHeight)));
    else parent.addView(slot,new LinearLayout.LayoutParams(-1,dp(slotHeight)));
  }

  private void addNioShortcut(LinearLayout parent,String kind,int drawable,String title,Runnable action) {
    LinearLayout card=column();card.setGravity(Gravity.CENTER);card.setBackground(bg(surface,16));
    card.setTag(kind.equals("windows")?"windows_control_button":"signal_control_button");
    ImageView image=new ImageView(this);image.setImageResource(drawable);image.setColorFilter(ink);addNioOrb(card,image);
    TextView label=text(title,10,ink,false);label.setTag("shortcut_label");label.setGravity(Gravity.CENTER);
    TextView feedback=text("",9,muted,false);feedback.setTag("shortcut_feedback");feedback.setGravity(Gravity.CENTER);
    label.setSingleLine(true);feedback.setSingleLine(true);
    card.addView(label,new LinearLayout.LayoutParams(-1,textLineHeight(10)+dp(1)));
    card.addView(feedback,new LinearLayout.LayoutParams(-1,textLineHeight(9)+dp(1)));
    click(card,action);
    bindDashboard(()->{
      boolean warning=kind.equals("windows")&&model.snapshot!=null&&model.snapshot.windows!=null&&model.snapshot.windows.contains("个开启");
      label.setText(title+(warning?" !":""));
      feedback.setText(kind.equals("windows")
          ? (model.snapshot==null?tr("未知","Unknown"):status(model.snapshot.windows))
          : tr("操作","Action"));
      feedback.setTextColor(warning?accentText():muted);image.setColorFilter(warning?accentText():ink);
    });
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(parent.getChildCount()>0)lp.leftMargin=dp(5);parent.addView(card,lp);
  }

  private void addDashboardUtilities(LinearLayout parent, boolean landscape) {
    LinearLayout utilities = row();
    String[] kinds = {"windows", "body", "camp", "signal"};
    for (String kind : kinds) {
      Runnable action = kind.equals("windows") ? () -> directControl("windows")
          : kind.equals("body") ? this::vehicleStatusDialog : kind.equals("health") ? this::vehicleStatusDialog
          : kind.equals("camp") ? this::campDialog : this::signalMenu;
      String initialLabel = kind.equals("windows") ? tr("车窗", "Windows") : kind.equals("body") ? tr("车况", "Status")
          : kind.equals("health") ? tr("车况", "Health") : kind.equals("camp") ? tr("露营模式", "Camping") : tr("寻车", "Find car");
      TextView view = kind.equals("body")?conditionButton(initialLabel):utility(initialLabel, false, false, action);
      view.setTextSize(11);

      view.setPadding(dp(3), dp(2), dp(3), dp(2));view.setGravity(Gravity.CENTER);
      if(kind.equals("camp")){utilityIcon(view,R.drawable.ic_camp_clock_vi);view.setGravity(Gravity.CENTER);}
      if(nioSkin()&&(kind.equals("windows")||kind.equals("signal"))) {
        android.graphics.drawable.Drawable glyph=getDrawable(kind.equals("windows")?R.drawable.ic_window:R.drawable.ic_horn).mutate();glyph.setTint(ink);
        GradientDrawable circle=oval(Color.TRANSPARENT);circle.setStroke(dp(1),line);
        android.graphics.drawable.LayerDrawable badge=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{circle,glyph});
        badge.setLayerInset(1,dp(5),dp(5),dp(5),dp(5));badge.setBounds(0,0,dp(32),dp(32));view.setCompoundDrawables(badge,null,null,null);
      }
      view.setTag(kind.equals("windows") ? "windows_control_button" : kind.equals("body")
          ? "exterior_button" : kind.equals("health") ? "vehicle_status_button"
          : kind.equals("camp") ? "camp_button" : "signal_control_button");
      bindDashboard(() -> {
        VehicleSnapshot v = model.snapshot;
        boolean warn = kind.equals("windows") ? v != null && v.windows != null && v.windows.contains("个开启")
            : kind.equals("body") ? VehicleCondition.problem(v,showTires()) : kind.equals("health") && v != null && v.health.hasProblem();
        boolean active = warn || kind.equals("camp") && (CampModeManager.active(this)
            || "STOPPING".equals(CampModeManager.phase(this)));
        boolean good = kind.equals("health") && v != null && v.health.allGood();
        String label = kind.equals("windows") ? tr("车窗", "Windows") : kind.equals("body") ? tr("车况", "Status")
            : kind.equals("health") ? (good ? "ALL GOOD" : tr("车况", "Health"))
            : kind.equals("camp") ? tr("露营模式", "Camping") : tr("寻车", "Find car");
        view.setMaxLines(good || kind.equals("camp") && active ? 2 : 1);view.setAutoSizeTextTypeUniformWithConfiguration(8,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        view.setText(nioSkin()&&(kind.equals("windows")||kind.equals("signal"))?"":good ? "ALL\nGOOD" : label + (warn ? " !" : "") + (kind.equals("camp")&&active?"\n"+RuntimeLabels.camp(this):""));
        GradientDrawable background = (GradientDrawable) view.getBackground();
        background.setColor(active ? orange : surface);
        background.setStroke(dp(1), active || good ? orange : line);
        view.setTextColor(active ? Color.rgb(21,22,23) : good ? orange : ink);if(kind.equals("camp"))tintUtilityIcon(view);
        String details = kind.equals("health") ? (v == null ? tr("未上报", "Unavailable") : v.health.summary(AppPreferences.english(this), showTires()))
            : kind.equals("windows") ? (v == null ? tr("未知", "Unknown") : status(v.windows))
            : kind.equals("body") ? (v == null ? tr("未知", "Unknown") : status(v.doors) + " · " + status(v.windows))
            : kind.equals("camp") ? CampModeManager.status(this) : tr("鸣笛与闪灯", "Honk and flash");
        view.setContentDescription(label + " · " + details);
        if(view instanceof VehicleConditionButton)((VehicleConditionButton)view).refreshService();
      });
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
          dp(landscape ? 48 : 58), 1);
      if (utilities.getChildCount() > 0) lp.leftMargin = dp(4);
      utilities.addView(view, lp);
    }
    parent.addView(utilities);
  }

  private boolean controlSending(String control) {
    String pending = model.activeAction;
    return (model.busy||model.commandPending) && (control.equals("lock") ? pending.equals("lock") || pending.equals("unlock")
        : control.equals("trunk") ? pending.startsWith("trunk") : pending.startsWith(control));
  }

  private int controlIcon(String control,String raw){
    if(control.equals("lock")||control.equals("trunk"))return "未锁".equals(raw)||"已打开".equals(raw)||"未关严".equals(raw)?R.drawable.ic_unlock:R.drawable.ic_lock;
    return control.equals("climate")?R.drawable.ic_climate:R.drawable.ic_air;
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
      return RuntimeLabels.climate(this,v,AppPreferences.get(this,"vehicle",""));
    }
    if(control.equals("air") && v != null && "开启".equals(v.air))return RuntimeLabels.air(this,v);
    return status(controlRaw(control));
  }

  private void addDashboardControl(LinearLayout parent, String control, boolean compact, boolean circles) {
    String label = control.equals("lock") ? tr("门锁", "Lock") : control.equals("climate")
        ? tr(compact ? "空调" : "驻车空调", "Climate") : control.equals("trunk") ? tr("尾箱", "Trunk")
        : tr(compact ? "净化" : "空气净化", "Air");
    LinearLayout card = column(); card.setGravity(Gravity.CENTER);
    card.setTag("control_tile"); card.setPadding(dp(compact ? 3 : 10), dp(4), dp(compact ? 3 : 8), dp(4));
    GradientDrawable background = bg(surface, nioSkin() ? 16 : 8); card.setBackground(background);
    ImageView icon = new ImageView(this);
    icon.setImageResource(control.equals("lock") ? R.drawable.ic_lock : control.equals("climate")
        ? R.drawable.ic_climate : control.equals("trunk") ? R.drawable.ic_tailgate : R.drawable.ic_air);
    icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setTag("control_icon_"+control);
    TextView title = text(label, compact ? 10 : 14, ink, false);
    title.setTag("control_title_"+control);
    TextView feedback = text("", compact ? 10 : 12, ink, false);feedback.setTag("control_feedback_"+control);
    TextView support = text("", 11, muted, false);
    for (TextView v : new TextView[]{title, feedback, support}) {
      v.setMaxLines(1); v.setEllipsize(android.text.TextUtils.TruncateAt.END);
    }
    boolean horizontalCompact=compact && getResources().getConfiguration().screenWidthDp>getResources().getConfiguration().screenHeightDp;
    if (compact) {
      card.setGravity(Gravity.CENTER);
      boolean horizontal=getResources().getConfiguration().screenWidthDp>getResources().getConfiguration().screenHeightDp;
      if(horizontal){LinearLayout heading=row();heading.setGravity(Gravity.CENTER);
        if(nioSkin())addNioOrb(heading,icon);else heading.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));
        LinearLayout words=column();words.addView(title);words.addView(feedback);heading.addView(words);card.addView(heading);}
      else {if(nioSkin())addNioOrb(card,icon);else card.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));
        title.setGravity(Gravity.CENTER);
        card.addView(title,new LinearLayout.LayoutParams(-1,nioSkin()?textLineHeight(10)+dp(1):-2));}
      feedback.setGravity(Gravity.CENTER);
    } else {
      LinearLayout heading = row();
      heading.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));
      LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1); tp.leftMargin = dp(5);
      heading.addView(title, tp); card.addView(heading); gap(card, 2);
      if (nioSkin() && (control.equals("climate")||control.equals("air"))) {
        TextView temperature=liveText(() -> control.equals("air")?("AQI "+(model.snapshot==null||model.snapshot.environment.aqi==null?"--":model.snapshot.environment.aqi))
            :model.snapshot==null ? "--°C":DisplayMetric.TEMPERATURE.value(this,model.snapshot,true,AppPreferences.english(this)),14,ink,true);
        card.addView(temperature);
      }
    }
    if(!horizontalCompact)card.addView(feedback,
        new LinearLayout.LayoutParams(-1,nioSkin()&&compact?textLineHeight(10)+dp(1):-2));
    if (!compact) card.addView(support, new LinearLayout.LayoutParams(-1, -2));
    bindDashboard(() -> {
      String raw = controlRaw(control);icon.setImageResource(controlIcon(control,raw));
      boolean active = "开启".equals(raw) || "未锁".equals(raw) || "已打开".equals(raw) || "未关严".equals(raw);
      boolean known = active || "关闭".equals(raw) || "已上锁".equals(raw) || "已锁".equals(raw) || "已关闭".equals(raw);
      int activeColor = nioColors() && (control.equals("climate") || control.equals("air")) ? Color.rgb(0,191,185) : orange;
      background.setColor(nioSkin() ? (active ? activeColor : surface) : known ? active ? orange : Color.WHITE : surface);
      background.setStroke(dp(1), controlSending(control) || nioSkin() && active ? activeColor : nioSkin() ? line : known ? Color.TRANSPARENT : line);
      int color = nioSkin() ? (active?Color.rgb(21,22,23):ink) : known ? Color.rgb(21,22,23) : ink;
      icon.setColorFilter(nioSkin() && active ? activeColor : color);
      if (circles && !nioSkin()) icon.setBackground(oval(active ? Color.argb(18,0,0,0) : viSoft()));
      title.setTextColor(color); feedback.setTextColor(color);
      feedback.setText(nioSkin() && !compact && control.equals("climate") && "开启".equals(raw)
          ? tr("运行中 · ","Running · ") + controlFeedback(control,true) : controlFeedback(control, compact));
      String detail = model.snapshot == null ? "" : control.equals("climate")
          ? ClimateRuntime.detail(this, model.snapshot, AppPreferences.get(this, "vehicle", ""))
          : control.equals("air") ? RuntimeLabels.air(this,model.snapshot)+" · PM2.5 " + MileageSnapshot.decimalText(model.snapshot.environment.pm25UgM3, "")
              + " · AQI " + (model.snapshot.environment.aqi == null ? "--" : model.snapshot.environment.aqi) : "";
      support.setText(nioSkin() && !compact && control.equals("climate")
          ? tr("目标 ","Target ") + DisplayMetric.TARGET_TEMPERATURE.value(this,model.snapshot,true,AppPreferences.english(this)) : detail); support.setTextColor(known ? color : muted);
      card.setEnabled(!model.busy && !model.commandPending && model.selected != null);
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
    boolean dense=getResources().getConfiguration().screenHeightDp<680||getResources().getConfiguration().fontScale>1.2f;
    // In short landscape the main dashboard selects at most six metrics.
    // Always use a single row for those fields, even when the count is exactly six.
    int columns = landscape ? Math.min(6,count)
        : nioSkin() ? (count<=4?2:dense&&count>6?4:3)
            : count<=4?2:count<=6?3:4;
    float fontScale=getResources().getConfiguration().fontScale;
    boolean tightLandscape=landscape && count>6
        && getResources().getConfiguration().screenHeightDp<=360 && fontScale>1.15f;
    int labelSp=tightLandscape?(fontScale>1.45f?6:7):9;
    int valueSp=tightLandscape?(fontScale>1.45f?8:10):12;
    for (int i = 0; i < count; i += columns) {
      boolean energyRow = false;
      for (int k = i; k < Math.min(i + columns, count); k++)
        energyRow |= chosen.get(k) == DisplayMetric.TA_CONSUMPTION || chosen.get(k) == DisplayMetric.TM_CONSUMPTION;
      int captionLines = 1;
      int valueHeight=textLineHeight(valueSp)+dp(tightLandscape?1:4);
      int captionHeight=textLineHeight(labelSp)+dp(tightLandscape?1:4);
      int rowHeight=captionHeight*captionLines+valueHeight+dp(tightLandscape?0:3);
      LinearLayout metricRow = row();
      for (int j = i; j < i + columns; j++) {
        LinearLayout cell = column(); cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setPadding(dp(3), 0, dp(3), 0);
        if (j < count) {
          DisplayMetric metric = chosen.get(j);
          String caption = metric == DisplayMetric.TA_CONSUMPTION ? "TA\nkWh/100km"
              : metric == DisplayMetric.TM_CONSUMPTION ? "TM\nkWh/100km" : metric.shortLabel(AppPreferences.english(this));
          caption=metric==DisplayMetric.TA_CONSUMPTION?tr("TA 能耗","TA Energy")
              :metric==DisplayMetric.TM_CONSUMPTION?tr("TM 能耗","TM Energy"):metric.shortLabel(AppPreferences.english(this));
          TextView label = text(caption, labelSp, muted, false);
          label.setMaxLines(captionLines); label.setGravity(Gravity.BOTTOM);
          label.setEllipsize(android.text.TextUtils.TruncateAt.END);
          TextView value = liveText(() -> metric == DisplayMetric.VEHICLE_STATUS && model.snapshot != null
              && model.snapshot.health.allGood() ? tr("正常", "OK")
              : metric.value(this, model.snapshot, false, AppPreferences.english(this)), valueSp, ink, false);
          cell.addView(label, new LinearLayout.LayoutParams(-1, captionHeight * captionLines)); cell.addView(value,new LinearLayout.LayoutParams(-1,valueHeight));
          bindDashboard(() -> cell.setContentDescription(metric.label(AppPreferences.english(this))
              + " · " + metric.value(this, model.snapshot, true, AppPreferences.english(this))));
          click(cell, () -> info(metric.label(AppPreferences.english(this)),
              metric.value(this, model.snapshot, true, AppPreferences.english(this))
                  + (metric.name().startsWith("SERVICE_") ? tr("\n车辆上报的剩余值；百分比仅为参考周期比例", "\nVehicle-reported remaining value; percentages use a reference cycle") : "")));
        }
        metricRow.addView(cell, new LinearLayout.LayoutParams(0, -1, 1));
      }
      parent.addView(metricRow, new LinearLayout.LayoutParams(-1, rowHeight));
    }
  }

  private TextView parkingCaption(){
    TextView caption=liveText(()->model.parkedLocation==null?tr("最后停车位置","Last parked"):
        String.format(java.util.Locale.US,"%.3f, %.3f",model.parkedLocation.latitude,model.parkedLocation.longitude),8,muted,false);
    caption.setTag("parking_caption");caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.setGravity(Gravity.CENTER_VERTICAL);click(caption,this::locationDialog);return caption;
  }

  private void locationDialog() {
    toast(tr("正在读取最后停车位置…","Reading last parked location…"));
    model.readLastParkedLocation((location,error)->{
      if(isFinishing()||isDestroyed()||!getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))return;
      if(location==null){info(tr("最后停车位置","Last parked location"),error);return;}
      showParkingLocation(location);
    });
  }

  private void dismissParking(){if(parkingDialog!=null){AlertDialog old=parkingDialog;parkingDialog=null;parkingVin=null;parkingPoint=null;parkingPreview=null;old.dismiss();}}

  private void showParkingLocation(LocationSnapshot location) {
    if(location==null||!location.valid())return;
    dismissParking();
    parkingVin=model.selected==null?null:model.selected.vin;parkingPoint=location;
    String time=location.reportedAt>0?android.text.format.DateFormat.format("yyyy-MM-dd HH:mm",location.reportedAt).toString():"--";
    LinearLayout panel=detailPanel();panel.setTag("parking_location_panel");
    TextView report=text(tr("车辆上报时间：","Vehicle report: ")+time,13,muted,false);report.setTag("parking_report_time");panel.addView(report);
    if(location.stale){TextView stale=text(tr("位置可能不是最新，请以车辆上报时间为准。","This position may be out of date. Check the vehicle report time."),13,PsaDesign.accentText(AppPreferences.dark(this)),false);stale.setTag("parking_stale");panel.addView(stale);}
    gap(panel,6);
    TextView mapOptions=text(tr("手机定位：","Phone location: ")+(MapLocationPermission.enabled(this)?tr("允许","allowed"):tr("关闭","off"))+tr(" · 点按设置"," · tap to change"),11,muted,false);click(mapOptions,this::mapLocationDialog);panel.addView(mapOptions);
    ParkingWebPreview map=new ParkingWebPreview(this,location,AppPreferences.dark(this),()->openLocation(location));parkingPreview=map;
    int mapHeight=Math.max(220,Math.min(360,(int)(getResources().getConfiguration().screenHeightDp*.55f)));
    panel.addView(map.view,new LinearLayout.LayoutParams(-1,dp(mapHeight)));gap(panel,8);
    panel.addView(text(tr("纬度 ","Lat ")+String.format(java.util.Locale.US,"%.6f",location.latitude)+" · "+tr("经度 ","Lon ")+String.format(java.util.Locale.US,"%.6f",location.longitude),12,muted,false));
    panel.addView(text(tr("这是最后停车位置，并非实时定位。地图网页由高德提供，本次停车点会用于加载地图。","Last parked position, not live tracking. The Amap page receives this parked point to display the map."),12,muted,false));
    AlertDialog dialog=new HapticDialogBuilder(this).setTitle(tr("最后停车位置","Last parked location")).setView(panel)
        .setPositiveButton(tr("打开地图","Open map"),(d,w)->openLocation(location))
        .setNeutralButton(tr("重新读取","Refresh"),(d,w)->locationDialog())
        .setNegativeButton(tr("关闭","Close"),null).create();
    parkingDialog=dialog;dialog.setOnDismissListener(d->{map.close();if(parkingDialog==dialog){parkingDialog=null;parkingVin=null;parkingPoint=null;parkingPreview=null;}});dialog.show();
  }

  private void mapLocationDialog(){
    LinearLayout panel=detailPanel();mapLocationSwitch=new Switch(this);mapLocationSwitch.setTag("map_phone_location");
    mapLocationSwitch.setText(tr("允许高德网页使用手机位置","Allow Amap web page to use phone location"));mapLocationSwitch.setTextColor(ink);mapLocationSwitch.setChecked(MapLocationPermission.enabled(this));
    mapLocationSwitch.setOnCheckedChangeListener((button,on)->{Haptics.tap(this);if(on&&!MapLocationPermission.granted(this)){
      requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},MAP_LOCATION);
    }else{AppPreferences.set(this,MapLocationPermission.KEY,on?"on":"off");if(parkingPreview!=null)parkingPreview.refreshPhoneLocation();}});panel.addView(mapLocationSwitch);
    panel.addView(text(tr("默认关闭。开启需系统授权，近似位置也可使用；点地图定位按钮可判断距车距离，不在后台跟踪。网页显示的是地理距离，不代表步行路线。",
        "Off by default. Requires system permission; approximate location also works. Tap the map locate button to estimate distance to the car, with no background tracking. Geographic distance is not a walking route."),12,muted,false));
    new HapticDialogBuilder(this).setTitle(tr("地图手机定位","Map phone location")).setView(panel).setPositiveButton(tr("完成","Done"),(d,w)->{}).show();
  }

  private void openLocation(LocationSnapshot location) {
    if(!ParkingMapLinks.open(this,location,tr("最后停车位置","Last parked location")))
      toast(tr("没有可用的地图或浏览器应用","No map or browser application available"));
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
    detailRow(panel,tr("充电上限","Charge limit"),DisplayMetric.CHARGE_LIMIT.value(this,model.snapshot,true,AppPreferences.english(this)));
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
    if (model.busy || model.commandPending || authPending || model.selected == null) return;
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
    if (model.busy || model.commandPending || authPending || model.selected == null) return;
    new HapticDialogBuilder(this)
        .setTitle(tr("寻车", "Find car"))
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
    if(!FirstUseConsent.accepted(this)){FirstUseConsent.show(this,()->{});return;}
    final CampSession campTarget = "camp-stop".equals(action) ? CampModeManager.session(this) : null;
    if (authPending || model.busy || model.commandPending || (model.selected == null && campTarget == null)) return;
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

  private void detailSection(LinearLayout parent, String title) {
    gap(parent,16); TextView label=text(title,13,ink,false);label.setTag("detail_section");label.setPadding(dp(2),0,0,dp(7));parent.addView(label);
    View rule=new View(this);rule.setBackgroundColor(line);parent.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));gap(parent,6);
  }

  private void detailRow(LinearLayout p, String key, String value) {
    LinearLayout r = row();r.setTag("detail_row");
    r.setPadding(dp(12), dp(12), dp(12), dp(12));
    r.setBackground(bg(surface, 4));
    TextView a = text(key, 14, muted, false), b = text(value, 14, ink, false);
    b.setGravity(Gravity.RIGHT);
    r.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
    r.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    p.addView(r);
    gap(p, 3);
  }

  private void detailMore(LinearLayout parent,String title,Runnable action) {
    TextView more=button(title+"  ›",action);more.setTag("detail_more");more.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
    GradientDrawable frame=bg(surface,nioSkin()?12:4);frame.setStroke(dp(1),line);more.setBackground(frame);parent.addView(more);gap(parent,8);
  }
  private void detailDialog(String title,LinearLayout panel) {
    ScrollView scroll=new ScrollView(this);scroll.addView(panel);
    new HapticDialogBuilder(this).setTitle(title).setView(scroll).setPositiveButton(tr("关闭","Close"),null).show();
  }
  private LinearLayout detailPanel() {LinearLayout panel=column();panel.setPadding(dp(12),dp(8),dp(12),dp(8));return panel;}
  private void tripDialog() {
    LinearLayout panel=detailPanel();VehicleSnapshot v=model.snapshot;if(v==null)return;
    for(DisplayMetric metric:new DisplayMetric[]{DisplayMetric.TA_DISTANCE,DisplayMetric.TA_SPEED,DisplayMetric.TA_CONSUMPTION,DisplayMetric.TA_DURATION,
        DisplayMetric.TM_DISTANCE,DisplayMetric.TM_SPEED,DisplayMetric.TM_CONSUMPTION,DisplayMetric.TM_DURATION,
        DisplayMetric.SINCE_CHARGE_DISTANCE,DisplayMetric.SINCE_CHARGE_SPEED,DisplayMetric.SINCE_CHARGE_CONSUMPTION})
      detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,v,true,AppPreferences.english(this)));
    panel.addView(text(tr("~ 表示估算。行驶时长由同次行程距离÷均速换算；TA 能耗仅在能量与距离来源可核对时显示。未上报或来源冲突为 --。",
        "~ marks estimates. Trip time uses distance / mean speed from one report. TA energy requires corroborating energy and distance. Missing or conflicting data stays --."),12,muted,false));
    detailDialog(tr("完整行程数据","All trip data"),panel);
  }
  private void airDetailsDialog() {
    LinearLayout panel=detailPanel();VehicleSnapshot v=model.snapshot;if(v==null)return;
    for(DisplayMetric metric:new DisplayMetric[]{DisplayMetric.TEMPERATURE,DisplayMetric.TARGET_TEMPERATURE,DisplayMetric.AQI,DisplayMetric.PM25})
      detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,v,true,AppPreferences.english(this)));
    detailRow(panel,tr("空调剩余时间","Climate time left"),ClimateRuntime.detail(this,v,AppPreferences.get(this,"vehicle","")));
    detailRow(panel,tr("空气数据更新","Air updated"),v.environment.airReportedAt>0?android.text.format.DateFormat.format("MM-dd HH:mm",v.environment.airReportedAt).toString():"--");
    detailDialog(tr("空调与空气详情","Climate & air details"),panel);
  }
  private void details() {
    LinearLayout panel=scrollBody();panel.addView(text(tr("车况详情","Vehicle details"),24,ink,false));
    VehicleSnapshot v=model.snapshot;if(v==null){panel.addView(text(tr("暂无车辆数据，请刷新","No vehicle data. Refresh to connect."),14,muted,false));return;}
    detailSection(panel,tr("电池与续航","Battery & range"));addBatterySummary(panel,false);
    detailRow(panel,tr("车辆","Vehicle"),vehicleDisplayName(v));
    detailRow(panel,tr("更新时间","Last update"),v.timestampMillis>0?android.text.format.DateFormat.format("MM-dd HH:mm",v.timestampMillis).toString():"--");
    detailMore(panel,tr("车辆信息 · 年款 / VIN","Vehicle info · year / VIN"),this::vehicleInfoDialog);
    detailSection(panel,tr("里程与行程","Distance & trips"));
    for(DisplayMetric metric:new DisplayMetric[]{DisplayMetric.ODOMETER,DisplayMetric.TA_DISTANCE,DisplayMetric.TA_CONSUMPTION,DisplayMetric.TM_DISTANCE,DisplayMetric.TM_CONSUMPTION})
      detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,v,true,AppPreferences.english(this)));
    detailMore(panel,tr("更多行程 · 均速 / 行驶时间 / 充电后","More trips · speed / time / since charge"),this::tripDialog);
    if(ChargingVisibility.shouldShow(v)) {
      detailSection(panel,tr("充电","Charging"));
      for(DisplayMetric metric:new DisplayMetric[]{DisplayMetric.CHARGE_LIMIT,DisplayMetric.CHARGE_CONNECTION,DisplayMetric.CHARGE_POWER,DisplayMetric.CHARGE_CURRENT,DisplayMetric.CHARGE_ETA})
        detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,v,true,AppPreferences.english(this)));
      detailMore(panel,tr("更多充电 · 电源状态","More charging · power status"),this::chargingDialog);
    }
    detailSection(panel,tr("车况","Vehicle status"));
    detailRow(panel,tr("车门 / 车窗","Doors / windows"),status(v.doors)+" · "+status(v.windows));
    detailRow(panel,tr("尾箱","Tailgate"),status(v.tailgate));
    detailRow(panel,tr("12V 电瓶","12 V battery"),VehicleHealthSnapshot.label(v.health.lowVoltageBattery,AppPreferences.english(this)));
    detailRow(panel,tr("玻璃水","Washer fluid"),VehicleHealthSnapshot.label(v.health.washerFluid,AppPreferences.english(this)));
    if(showTires())detailRow(panel,tr("胎压告警","Tyre warning"),VehicleHealthSnapshot.label(v.health.tirePressure,AppPreferences.english(this)));
    detailMore(panel,tr("车况详情","Vehicle status details"),this::vehicleStatusDialog);
    detailSection(panel,tr("空调与空气","Climate & air"));
    detailRow(panel,tr("空调","Climate"),status(v.climate));detailRow(panel,tr("空气净化","Air purification"),status(v.air));
    for(DisplayMetric metric:new DisplayMetric[]{DisplayMetric.TEMPERATURE,DisplayMetric.AQI,DisplayMetric.PM25})
      detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,v,true,AppPreferences.english(this)));
    detailMore(panel,tr("更多空气数据 · 目标 / 剩余时间","More climate · target / remaining time"),this::airDetailsDialog);
    detailSection(panel,tr("保养","Service"));
    for(DisplayMetric metric:new DisplayMetric[]{DisplayMetric.SERVICE_DAYS_LEFT,DisplayMetric.SERVICE_KM_LEFT,DisplayMetric.SERVICE_TIME_PERCENT,DisplayMetric.SERVICE_KM_PERCENT})
      detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,v,true,AppPreferences.english(this)));
    detailMore(panel,tr("车辆保养","Vehicle service"),this::maintenanceDialog);
    panel.addView(button(tr("切换车辆","Select vehicle"),this::chooseVehicle));
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


  /** One persisted text color shared by all Widget sizes, independent of background style. */
  private void widgetTextColorDialog() {
    final String[] values={"system","#FFFFFFFF","#FF18191B","#FFFF7900","#FF94979B"};
    final String[] labels={tr("恢复默认 · VI 自动配色","Restore default · VI automatic"),
        tr("纯白色","White"),tr("深黑色","Black"),tr("VI 橘色","VI orange"),
        tr("中性灰色","Neutral gray")};
    new HapticDialogBuilder(this).setTitle(tr("Widget 字体颜色","Widget text color"))
        .setItems(new String[]{labels[0],labels[1],labels[2],labels[3],labels[4],tr("自定义 HEX 颜色","Custom HEX color")},(dialog,index)->{
          if(index<values.length){
            AppPreferences.set(this,"widget_ink",values[index]);
            WidgetRenderer.refreshAll(this);
          }else{
            android.widget.EditText input=new android.widget.EditText(this);
            input.setSingleLine(true);input.setHint("#FF7900 或 #FFFF7900");
            String current=AppPreferences.get(this,"widget_ink","system");
            if(current.startsWith("#"))input.setText(current);
            LinearLayout wrapper=column();wrapper.setPadding(dp(20),dp(8),dp(20),dp(2));wrapper.addView(input);
            new HapticDialogBuilder(this).setTitle(tr("输入 Widget 文字颜色","Enter Widget text color"))
                .setView(wrapper).setPositiveButton(tr("保存","Save"),(d,w)->{
                  String hex=input.getText().toString().trim();
                  try{
                    android.graphics.Color.parseColor(hex);
                    AppPreferences.set(this,"widget_ink",hex);
                    WidgetRenderer.refreshAll(this);
                  }catch(IllegalArgumentException invalid){
                    toast(tr("颜色无效，请输入 #RRGGBB 或 #AARRGGBB","Invalid color. Use #RRGGBB or #AARRGGBB"));
                  }
                }).setNegativeButton(tr("取消","Cancel"),null).show();
          }
        }).show();
  }

  private void skinDialog() {
    LinearLayout panel=column();panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    ScrollView scroller=new ScrollView(this);scroller.setFillViewport(false);
    final AlertDialog[] dialog={null};
    for(String skin:UiSkin.CHOICES){
      LinearLayout option=column();option.setTag("skin_picker_option_"+skin);
      option.setPadding(dp(6),dp(5),dp(6),dp(5));
      GradientDrawable frame=bg(surface,16);
      frame.setStroke(dp(UiSkin.get(this).equals(skin)?2:1),
          UiSkin.get(this).equals(skin)?voAccent():line);
      option.setBackground(frame);
      TextView title=text(UiSkin.label(this,skin)
          +(UiSkin.get(this).equals(skin)?"  ✓":""),14,ink,true);
      title.setGravity(Gravity.CENTER_VERTICAL);
      option.addView(title,new LinearLayout.LayoutParams(-1,dp(28)));
      SkinPreviewView preview=new SkinPreviewView(this,skin);
      preview.setSelectedSkin(UiSkin.get(this).equals(skin));
      option.addView(preview,new LinearLayout.LayoutParams(-1,dp(138)));
      click(option,()->{
        UiSkin.set(this,skin);
        WidgetRenderer.refreshAll(this);
        if(dialog[0]!=null)dialog[0].dismiss();
        recreate();
      });
      panel.addView(option);
      gap(panel,7);
    }
    scroller.addView(panel);
    dialog[0]=new HapticDialogBuilder(this).setTitle(tr("皮肤选择 · 预览","Choose skin · preview"))
        .setView(scroller).setNegativeButton(tr("取消","Cancel"),null).create();
    dialog[0].show();
  }

  private void fontDialog() {
    LinearLayout panel=column();panel.setPadding(dp(16),dp(8),dp(16),dp(8));
    String key=UiTypography.key(this);
    TextView sample=text(tr("字体预览 · 电量 63%","Type preview · Battery 63%"),18,ink,false);panel.addView(sample);
    SeekBar app=campSlider(panel,UiSkin.label(this,UiSkin.get(this))+tr(" 字体 %"," type %"),80,120,UiTypography.percent(this,key),null,
        value->sample.setTextSize(18*value/(float)UiTypography.percent(this,key)));
    SeekBar widget=campSlider(panel,tr("Widget 字体 %","Widget type %"),80,120,UiTypography.percent(this,"widget_font_percent"),null,null);
    panel.addView(text(tr("各皮肤独立保存，叠加系统字号。空间不足时优先适配边界，完整数据可点开查看。",
        "Saved per skin, in addition to system text size. Compact areas fit their bounds; tap data for full values."),12,muted,false));
    new HapticDialogBuilder(this).setTitle(tr("字体大小","Font size")).setView(panel)
        .setPositiveButton(tr("应用","Apply"),(d,w)->{AppPreferences.set(this,key,String.valueOf(app.getProgress()+80));
          AppPreferences.set(this,"widget_font_percent",String.valueOf(widget.getProgress()+80));WidgetRenderer.refreshAll(this);recreate();})
        .setNeutralButton(tr("恢复默认","Reset"),(d,w)->{AppPreferences.set(this,key,"100");AppPreferences.set(this,"widget_font_percent","100");WidgetRenderer.refreshAll(this);recreate();})
        .setNegativeButton(tr("取消","Cancel"),null).show();
  }

  private void settings() {
    LinearLayout p = scrollBody();
    p.addView(text(tr("设置", "Settings"), 24, ink, false));
    gap(p, 20);
    p.addView(button(tr("免责声明","Disclaimer"),()->info(tr("免责声明","Disclaimer"),FirstUseConsent.text(this))));gap(p,8);
    p.addView(button(tr("程序语言 / Language", "App language / 语言"), this::languageDialog));
    gap(p, 8);
    p.addView(button(tr("界面皮肤 · ", "UI skin · ") + UiSkin.label(this, UiSkin.get(this)), this::skinDialog));
    gap(p, 8);    Switch simpleLabels=new Switch(this);
    simpleLabels.setTag("volike_simple_labels");
    simpleLabels.setText(tr("VoLike 简洁版：显示按钮文字","VoLike Simple: show control labels"));
    simpleLabels.setTextColor(ink);
    simpleLabels.setMinHeight(dp(48));
    simpleLabels.setChecked(AppPreferences.visible(this,"volike_simple_labels",true));
    simpleLabels.setOnCheckedChangeListener((v,on)->{
      Haptics.tap(this);AppPreferences.set(this,"volike_simple_labels",on?"on":"off");render();
    });
    p.addView(simpleLabels);
    gap(p,8);
    p.addView(button(tr("停车地图 · 手机定位权限","Parking map · phone location permission"),this::mapLocationDialog));gap(p,8);
    p.addView(button(tr("字体大小 · 当前皮肤 / Widget", "Font size · current skin / Widget"),this::fontDialog));
    gap(p,8);
    p.addView(button(tr("露营模式", "Camping mode"), this::campDialog));
    gap(p, 8);
    p.addView(button(
        tr("车辆保养 · 自动读取", "Vehicle service · automatic"),
        this::maintenanceDialog));
    gap(p, 8);
    p.addView(button(
        tr("车辆控制页数据显示 · 最多 12 项", "Vehicle control data · up to 12"),
        () -> metricDialog("home_metrics", 12, HOME_DEFAULTS)));
    gap(p, 8);
    p.addView(text(tr("车况按钮会在有保养数据时自动显示时间 / 里程剩余百分比与进度条。",
        "The Status button automatically shows service time / distance percentages and bars when data is available."),12,muted,false));
    gap(p,8);
    Switch tireVisibility = new Switch(this);
    tireVisibility.setTag("show_tire_status");
    tireVisibility.setText(tr("显示胎压状态（含车况汇总）", "Show tyre status (including health summary)"));
    tireVisibility.setTypeface(regular); tireVisibility.setTextColor(ink); tireVisibility.setMinHeight(dp(48));
    tireVisibility.setChecked(showTires());
    tireVisibility.setOnCheckedChangeListener((control, on) -> {
      Haptics.tap(this); AppPreferences.set(this, "show_tire_status", on ? "on" : "off");
      WidgetRenderer.refreshAll(this); render();
    });
    p.addView(tireVisibility);
    Switch experimentalTire = new Switch(this);
    experimentalTire.setText(
        tr("实验性 iTPMS 四轮状态（待修复）", "Experimental four-wheel iTPMS (pending fix)"));
    experimentalTire.setTextColor(ink);
    experimentalTire.setMinHeight(dp(46));
    experimentalTire.setChecked(AppPreferences.visible(this, "experimental_tire", false));
    experimentalTire.setOnCheckedChangeListener(
        (control, on) -> {
          Haptics.tap(this);
          AppPreferences.set(this, "experimental_tire", on ? "on" : "off");
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
          .setItems(labels, (d, w) -> metricDialog(WidgetRenderer.selectionKey(this,sizes[w]),
              WidgetRenderer.capacity(sizes[w]), WidgetRenderer.selectionDefaults(this,sizes[w]))).show();
    }));
    gap(p, 8);
    p.addView(button(tr("手表皮肤 · ", "Watch skin · ") + watchSkinLabel()
        + tr(" · 圆屏 / 方屏", " · round / square"), this::watchPreview));
    gap(p, 8);    p.addView(button(tr("Widget 风格 · ","Widget design · ")+
        VoLikeWidgetRenderer.label(this,VoLikeWidgetRenderer.style(this)),this::widgetDesignDialog));
    gap(p,8);
    p.addView(button(tr("PSeu 显示类型 · 电量 / 续航 / 车辆 / 控制","PSeu layout · battery / range / car / controls"),()->new HapticDialogBuilder(this).setTitle("PSeu").setSingleChoiceItems(new String[]{tr("电量优先","Battery first"),tr("续航优先","Range first"),tr("车辆图片","Vehicle image"),tr("扩展控制（小尺寸保留门锁/空调）","Extra controls (compact sizes keep lock/climate)")},java.util.Arrays.asList("battery","range","car","controls").indexOf(AppPreferences.get(this,"pseu_type","battery")),(d,w)->{AppPreferences.set(this,"pseu_type",new String[]{"battery","range","car","controls"}[w]);WidgetRenderer.refreshAll(this);d.dismiss();}).show()));gap(p,8);
    Switch widgetBold=new Switch(this);widgetBold.setTag("widget_bold");widgetBold.setText(tr("Widget 加粗字体（所有风格）","Bold widget text (all styles)"));widgetBold.setTextColor(ink);widgetBold.setMinHeight(dp(48));widgetBold.setChecked(AppPreferences.visible(this,"widget_bold",false));
    widgetBold.setOnCheckedChangeListener((button,on)->{Haptics.tap(this);AppPreferences.set(this,"widget_bold",on?"on":"off");WidgetRenderer.refreshAll(this);});p.addView(widgetBold);gap(p,8);
    p.addView(button(tr("Widget 语言 · 中文 / English", "Widget language · 中文 / English"),
        this::widgetLanguageDialog));
    gap(p, 8);
    p.addView(
        button(
            tr("日夜模式 · 全部皮肤通用", "Day / night · all skins"),
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
    p.addView(button(tr("Widget 字体颜色 · 所有尺寸与风格", "Widget text color · all sizes/styles"), this::widgetTextColorDialog));
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

  }

  private void maintenanceDialog() {
    VehicleSnapshot v=model.snapshot;LinearLayout panel=detailPanel();
    detailRow(panel,tr("剩余保养里程","Service distance remaining"),ServiceReading.number(ServiceReading.km(this,v)," km"));
    detailRow(panel,tr("剩余保养天数","Service days remaining"),ServiceReading.number(ServiceReading.days(this,v),tr(" 天"," d")));
    detailRow(panel,tr("剩余保养工时","Service engine hours remaining"),ServiceReading.number(v==null?null:v.health.serviceHoursLeft," h"));
    String stamp=v==null||v.health.reportedAt<=0?"--":android.text.format.DateFormat.format("yyyy-MM-dd HH:mm",v.health.reportedAt).toString();
    detailRow(panel,tr("车辆上报时间","Vehicle report time"),stamp);
    panel.addView(text(tr("剩余值直接来自车辆，无需填写上次保养记录；未上报显示 --。百分比仅按365天/30,000 km参考周期估算，不是车辆上报的寿命百分比。",
        "Remaining values come directly from the vehicle; no manual service record is needed. Missing reports show --. Percentages use a 365-day / 30,000-km reference, not a vehicle-reported life percentage."),12,muted,false));
    new HapticDialogBuilder(this).setTitle(tr("车辆保养","Vehicle service")).setView(panel)
        .setPositiveButton(tr("刷新车况","Refresh vehicle"),(d,w)->model.refreshUser(null)).setNegativeButton(tr("关闭","Close"),null).show();
  }

  private void widgetDesignDialog() {
    String[] options={VoLikeWidgetRenderer.CLASSIC,VoLikeWidgetRenderer.SIMPLE,
        VoLikeWidgetRenderer.DETAILED,VoLikeWidgetRenderer.POLESTAR,VoLikeWidgetRenderer.PSEU};
    LinearLayout panel=column();panel.setPadding(dp(8),dp(6),dp(8),dp(6));
    ScrollView scroll=new ScrollView(this);
    final AlertDialog[] dialog={null};
    for(String style:options) {
      LinearLayout row=column();row.setTag("widget_style_option_"+style);
      row.setPadding(dp(7),dp(5),dp(7),dp(5));
      GradientDrawable frame=bg(surface,14);
      frame.setStroke(dp(1),VoLikeWidgetRenderer.style(this).equals(style)?orange:line);
      row.setBackground(frame);
      TextView label=text(VoLikeWidgetRenderer.label(this,style),14,ink,true);
      row.addView(label);
      ImageView preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
      preview.setTag("widget_style_preview");
      preview.setImageBitmap(VoLikeWidgetRenderer.preview(this,model.snapshot,style,300,165));
      row.addView(preview,new LinearLayout.LayoutParams(-1,dp(108)));
      click(row,()->{
        AppPreferences.set(this,"widget_design",style);
        WidgetRenderer.refreshAll(this);
        if(dialog[0]!=null)dialog[0].dismiss();
        render();
      });
      panel.addView(row);gap(panel,7);
    }
    scroll.addView(panel);
    dialog[0]=new HapticDialogBuilder(this)
        .setTitle(tr("Widget 风格 · 预览","Widget style · preview")).setView(scroll)
        .setNegativeButton(tr("取消","Cancel"),null).create();
    dialog[0].show();
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
        !showTires() || "home_metrics".equals(key) && !AppPreferences.visible(this, "experimental_tire", false);
    for (DisplayMetric metric : DisplayMetric.values())
      if (!(hideExperimentalTire && metric == DisplayMetric.TIRE_STATUS)) available.add(metric);
    DisplayMetric[] values = available.toArray(new DisplayMetric[0]);
    java.util.List<DisplayMetric> selected = MetricSelection.read(this, key, defaults);
    boolean retainedTyre = hideExperimentalTire && selected.remove(DisplayMetric.TIRE_STATUS);
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
              if (retainedTyre && !selected.contains(DisplayMetric.TIRE_STATUS)) selected.add(DisplayMetric.TIRE_STATUS);
              MetricSelection.save(this, key, selected);
              WidgetRenderer.refreshAll(this);
              render();
            })
        .setNegativeButton(tr("取消", "Cancel"), null)
        .show();
  }

  private String watchSkinLabel() {
    return "psa".equals(AppPreferences.get(this,"watch_skin","volvo"))?"Psa":"Volvo";
  }

  private void watchPreview() {
    LinearLayout panel=column();panel.setPadding(dp(16),0,dp(16),dp(8));
    WatchPreviewView preview=new WatchPreviewView(this,model.snapshot);
    boolean psa="psa".equals(AppPreferences.get(this,"watch_skin","volvo"));
    preview.setStyle(psa?WatchPreviewView.STYLE_PSA:WatchPreviewView.STYLE_VOLVO);

    LinearLayout skins=row();
    skins.addView(button("Psa",()->{AppPreferences.set(this,"watch_skin","psa");preview.setStyle(WatchPreviewView.STYLE_PSA);}),
        new LinearLayout.LayoutParams(0,dp(44),1));
    LinearLayout.LayoutParams sv=new LinearLayout.LayoutParams(0,dp(44),1);sv.leftMargin=dp(6);
    skins.addView(button(tr("Volvo 风格","Volvo style"),()->{AppPreferences.set(this,"watch_skin","volvo");preview.setStyle(WatchPreviewView.STYLE_VOLVO);}),sv);
    panel.addView(skins);gap(panel,6);

    LinearLayout shapes=row();
    shapes.addView(button(tr("圆屏","Round"),()->preview.setRound(true)),new LinearLayout.LayoutParams(0,dp(42),1));
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(42),1);sp.leftMargin=dp(6);
    shapes.addView(button(tr("方屏","Square"),()->preview.setRound(false)),sp);
    panel.addView(shapes);panel.addView(preview,new LinearLayout.LayoutParams(-1,dp(250)));
    panel.addView(text(tr("新增 Volvo 风格手表皮肤，可与 Psa 版本切换；车辆、电量、续航与充电状态使用当前车辆数据预览。当前仍为手表布局预览，独立 Wear OS 安装包需单独构建验证。",
        "A Volvo-inspired watch skin can now switch with the Psa skin. Vehicle, battery, range and charging use the current snapshot. This remains a watch layout preview; a standalone Wear OS APK still requires separate build and device validation."),11,muted,false));
    new HapticDialogBuilder(this).setTitle(tr("手表皮肤预览 · ","Watch skin preview · ")+watchSkinLabel()).setView(panel)
        .setPositiveButton(tr("关闭","Close"),null).show();
  }

  private void vehicleStatusDialog() {
    VehicleSnapshot v=model.snapshot;if(v==null)return;
    boolean english=AppPreferences.english(this);LinearLayout panel=detailPanel();
    detailRow(panel,tr("车况","Status"),VehicleCondition.summary(v,english,showTires()));
    detailSection(panel,tr("保养进度","Service progress"));
    detailRow(panel,tr("保养状态","Maintenance"),VehicleHealthSnapshot.label(v.health.maintenance,english));
    detailRow(panel,tr("剩余天数","Days left"),ServiceReading.number(ServiceReading.days(this,v)," d"));
    detailRow(panel,tr("剩余里程","Distance left"),ServiceReading.number(ServiceReading.km(this,v)," km"));
    serviceProgress(panel,tr("时间剩余","Time remaining"),ServiceReading.timePercent(this,v));
    serviceProgress(panel,tr("里程剩余","Distance remaining"),ServiceReading.distancePercent(this,v));
    detailSection(panel,tr("车辆检查","Vehicle checks"));
    detailRow(panel,tr("12V 电瓶","12 V battery"),VehicleHealthSnapshot.label(v.health.lowVoltageBattery,english));
    detailMore(panel,tr("雨刮液 · ","Washer fluid · ")+VehicleHealthSnapshot.label(v.health.washerFluid,english),this::healthMoreDialog);
    if(showTires())detailRow(panel,tr("胎压告警","Tyre warning"),VehicleHealthSnapshot.label(v.health.tirePressure,english));
    detailMore(panel,tr("液位 / 灯光 / 更多保养数据","Fluids / lights / more service data"),this::healthMoreDialog);
    detailRow(panel,tr("门 / 窗 / 尾箱","Doors / windows / tailgate"),status(v.doors)+" · "+status(v.windows)+" · "+status(v.tailgate));
    detailMore(panel,tr("四门 / 四窗位置状态","Door & window positions"),this::exteriorDialog);
    if(showTires()&&AppPreferences.visible(this,"experimental_tire",false))detailMore(panel,tr("四轮 iTPMS · 实验性","Four-wheel iTPMS · experimental"),this::tireDialog);
    else if(showTires())panel.addView(text(tr("四轮胎压详情默认隐藏，可在设置中开启实验性视图。","Four-wheel tyre details are hidden by default; enable the experimental view in Settings."),11,muted,false));
    panel.addView(text(tr("车况汇总只代表已上报项目；12V 与玻璃水是告警状态，不是电压或液位读数。保养百分比按参考周期估算。",
        "Status covers reported checks only. 12 V and washer fluid are warning states, not measured levels. Service percentages use the reference cycle."),12,muted,false));
    detailDialog(tr("车况","Vehicle status"),panel);
  }

  private void healthMoreDialog() {
    VehicleSnapshot v=model.snapshot;if(v==null)return;boolean en=AppPreferences.english(this);LinearLayout panel=detailPanel();
    for(Object[] row:new Object[][]{{tr("制动液","Brake fluid"),v.health.brakeFluid},{tr("冷却液","Coolant"),v.health.coolant},
        {tr("机油","Oil"),v.health.oil},{tr("雨刮液","Washer fluid"),v.health.washerFluid},{tr("外部灯光","Exterior lights"),v.health.exteriorLights}})
      detailRow(panel,(String)row[0],VehicleHealthSnapshot.label((Integer)row[1],en));
    detailSection(panel,tr("保养剩余（车辆上报）","Service remaining (vehicle report)"));
    detailRow(panel,tr("剩余工时","Engine hours left"),ServiceReading.number(v.health.serviceHoursLeft," h"));
    detailRow(panel,tr("剩余天数","Days left"),ServiceReading.number(v.health.serviceDaysLeft," d"));
    detailRow(panel,tr("剩余里程","Distance left"),ServiceReading.number(v.health.serviceKmLeft," km"));
    serviceProgress(panel,tr("剩余时间比例（估算）","Time remaining (estimated)"),ServiceReading.timePercent(this,v));
    serviceProgress(panel,tr("剩余里程比例（估算）","Distance remaining (estimated)"),ServiceReading.distancePercent(this,v));
    panel.addView(text(tr("百分比按参考周期 ","Percentages use reference ")+ServiceReading.REFERENCE_DAYS+tr(" 天 / "," d / ")
        +ServiceReading.REFERENCE_KM+" km"+tr(" 周期估算，不是车辆直接上报的百分比。缺失或不适用的项目显示 --。机油未上报不代表故障，纯电车型可能不适用。",
        " cycles, not a vehicle-reported percentage. Unavailable items show --. Missing oil data does not imply a fault; it may not apply to an EV."),12,muted,false));
    detailMore(panel,tr("车辆保养详情","Vehicle service details"),this::maintenanceDialog);
    detailDialog(tr("完整车况","Full vehicle status"),panel);
  }
  private void serviceProgress(LinearLayout panel,String label,Integer value) {
    detailRow(panel,label,ServiceReading.number(value,"%"));ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    styleViProgress(bar);bar.setTag("service_progress_bar");bar.setMax(100);bar.setProgress(value==null?0:value);bar.setVisibility(value==null?View.GONE:View.VISIBLE);
    panel.addView(bar,new LinearLayout.LayoutParams(-1,dp(4)));gap(panel,7);
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
    CheckBox air = new CheckBox(this);
    air.setButtonDrawable(R.drawable.vi_checkbox); air.setText(tr("同时维持空气净化", "Maintain air purification"));
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
    if(r==MAP_LOCATION){
      boolean allowed=false;for(int result:g)if(result==PackageManager.PERMISSION_GRANTED)allowed=true;
      AppPreferences.set(this,MapLocationPermission.KEY,allowed?"on":"off");
      if(mapLocationSwitch!=null)mapLocationSwitch.setChecked(allowed);
      if(parkingPreview!=null)parkingPreview.refreshPhoneLocation();
      toast(allowed?tr("已允许地图使用手机位置","Map phone location allowed"):tr("未授权，仍可查看停车点","Not allowed; parked point is still available"));
    }
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
