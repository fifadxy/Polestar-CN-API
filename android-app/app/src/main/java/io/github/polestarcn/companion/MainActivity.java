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
import io.github.polestarcn.companion.auth.ClientSignatureProvider;
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
  private int statusReturnPage;
  private int statusScrollY;
  private int settingsScrollY,dataScrollY;
  private final Handler displayHandler = new Handler(Looper.getMainLooper());
  private final Runnable displayTick = new Runnable() {
    public void run() {
      if (model != null && page == 0 && model.snapshot != null
          ) onModelChanged();
      displayHandler.postDelayed(this, 60_000L);
    }
  };
  private final Runnable foregroundRefresh=new Runnable(){public void run(){
    if(FirstUseConsent.accepted(MainActivity.this)&&model.hasSession()&&!model.busy&&!model.commandPending&&page!=2)model.refresh(null);
    scheduleForeground();
  }};
  private void scheduleForeground(){displayHandler.removeCallbacks(foregroundRefresh);int minutes=RefreshPolicy.foreground(this);if(minutes>0)displayHandler.postDelayed(foregroundRefresh,minutes*60_000L);}
  private static final int NOTIFICATIONS = 33, MAP_LOCATION = 34;
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
    super.onCreate(b);initialCreation=b==null;if(b!=null){statusReturnPage=Math.max(0,Math.min(2,b.getInt("statusReturn",0)));statusScrollY=b.getInt("statusY",0);settingsScrollY=b.getInt("settingsY",0);dataScrollY=b.getInt("dataY",0);}
    if(b!=null)page=Math.max(0,Math.min(3,b.getInt("page",0)));
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
    if (initialCreation && AppPreferences.visible(this,"refresh_on_open",true) && model.hasSession() && !model.busy) model.refresh(null);
    if (AppPreferences.enabled(this))
      try {
        ResidentService.apply(this);
      } catch (RuntimeException e) {
        DiagnosticLog.add("Resident start restricted");
      }
    ResidentService.scheduleUpdates(this);
  }

  @Override protected void onSaveInstanceState(Bundle out) { captureScrolls();out.putInt("page",page);out.putInt("statusReturn",statusReturnPage);out.putInt("statusY",statusScrollY);out.putInt("settingsY",settingsScrollY);out.putInt("dataY",dataScrollY);super.onSaveInstanceState(out); }

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
    if("status".equals(request)){if(model.hasSession()){statusReturnPage=0;page=3;}else page=0;render();return;}
    if ("location".equals(request)) { page=0; render(); if(model.hasSession()&&model.selected!=null)
      locationDialog();else toast(tr("请先连接车辆","Connect vehicle first"));return; }
    if("charging".equals(request)){page=1;AppPreferences.set(this,"expand_data_charging","on");render();return;}
    if(request.startsWith("control-")){String control=request.substring(8);if(!Arrays.asList("air","windows","trunk","find").contains(control))return;page=0;render();Haptics.tap(this);if(control.equals("find"))signalMenu();else directControl(control);return;}
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
    displayHandler.removeCallbacks(displayTick);displayHandler.removeCallbacks(foregroundRefresh);
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
    super.onResume();if(model!=null&&dashboardReady&&page==0)onModelChanged();scheduleForeground();displayHandler.removeCallbacks(displayTick); displayHandler.postDelayed(displayTick, 60_000L);
  }
  @Override protected void onPause() {
    displayHandler.removeCallbacks(displayTick);displayHandler.removeCallbacks(foregroundRefresh);View pulse=findViewById(android.R.id.content).findViewWithTag("charging_pulse");if(pulse instanceof ChargingPulseView)((ChargingPulseView)pulse).setActive(false); super.onPause();
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
    if(page==3){page=statusReturnPage;render();return;}
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

  private Typeface buttonTypeface(){return AppPreferences.visible(this,"ui_button_bold",true)?bold:regular;}
  private void fitSingleLine(TextView view,int sp){view.setMaxLines(1);view.setEllipsize(android.text.TextUtils.TruncateAt.END);view.setAutoSizeTextTypeUniformWithConfiguration(6,sp,1,android.util.TypedValue.COMPLEX_UNIT_SP);}
  private TextView feedbackText(String control,boolean compact){
    TextView view=text("",ButtonFeedback.sp(this),muted,false);view.setTag("control_feedback_"+control);view.setGravity(Gravity.CENTER);view.setTypeface(ButtonFeedback.typeface(this));fitSingleLine(view,ButtonFeedback.sp(this));
    bindDashboard(()->{String value=ButtonFeedback.value(this,control,model.snapshot,controlFeedback(control,compact));view.setText(value);view.setVisibility(AppPreferences.visible(this,"show_button_feedback",true)&&!value.isEmpty()?View.VISIBLE:View.GONE);});return view;
  }
  private void gap(LinearLayout p, int size) {
    p.addView(new View(this), new LinearLayout.LayoutParams(1, dp(size)));
  }

  private TextView button(String s, Runnable r) {
    TextView t = text(s, 14, ink, true);
    t.setTypeface(buttonTypeface());fitSingleLine(t,14);t.setGravity(Gravity.CENTER);
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
    UtilityButton view=new UtilityButton(this);view.setText(title+(warning?" !":""));view.setTextColor(ink);view.setTypeface(buttonTypeface());view.setGravity(Gravity.CENTER);view.setMinHeight(dp(48));click(view,action);view.setFocusable(true);
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

  private void captureScrolls(){View root=findViewById(android.R.id.content);if(root==null)return;View v=root.findViewWithTag("status_scroll");if(v instanceof ScrollView)statusScrollY=((ScrollView)v).getScrollY();v=root.findViewWithTag("settings_scroll");if(v instanceof ScrollView)settingsScrollY=((ScrollView)v).getScrollY();v=root.findViewWithTag("data_scroll");if(v instanceof ScrollView)dataScrollY=((ScrollView)v).getScrollY();}

  private void render() {
    captureScrolls();
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
      if(page==3){TextView back=button("‹",()->{page=statusReturnPage;render();});back.setTag("status_back");back.setContentDescription(tr("返回","Back"));header.addView(back,new LinearLayout.LayoutParams(dp(36),dp(48)));}
      ImageView brand = new ImageView(this);
      brand.setImageResource(R.drawable.ic_notification);brand.setColorFilter(ink);
      brand.setScaleType(ImageView.ScaleType.FIT_CENTER);
      brand.setContentDescription("PsAssistant");
      header.addView(brand, new LinearLayout.LayoutParams(dp(30), dp(30)));
      TextView name = text(getString(R.string.app_name), 17, ink, true);
      name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
    else if (page == 3) vehicleStatusPage();
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
      t.setTag("bottom_nav_button");t.setPadding(dp(3),dp(1),dp(3),dp(1));t.setMinHeight(0);
      GradientDrawable navFrame = bg(i == (page==3?1:page) ? surface : paper, 12);
      navFrame.setStroke(dp(1), i == (page==3?1:page) ? line : Color.TRANSPARENT);
      t.setBackground(navFrame);
      t.setTextColor(i == (page==3?1:page) ? orange : muted);
      if (i < 2) {
        int resource = i == 0 ? R.drawable.ic_notification : R.drawable.ic_nav_vehicle_data_ref;
        android.graphics.drawable.Drawable navIcon = getDrawable(resource).mutate();
        navIcon.setTint(i == (page==3?1:page) ? orange : muted);
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
              ClientSignatureProvider ref = new ClientSignatureProvider(this);
              if (!ref.isReady()) {
                info(
                    tr("安装包配置不完整", "Incomplete build"),
                    tr("此验证构建未包含登录配置，请使用完整签名版。无需安装或导入其他APK。",
                        "This verification build has no account client configuration. Install the complete signed version; no other APK is needed."));
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
    v.addView(text(tr("独立版本 · 无需其他 APK", "Standalone version · no other APK required"),12,muted,false));
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
    if(voLike())refresh.setOnRightSwipe(this::toggleVoDetails);
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
      else buildVoLikeDashboard(UiSkin.detailedView(this));
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

  private void toggleVoDetails(){Haptics.tap(this);AppPreferences.set(this,"volike_detail",UiSkin.detailedView(this)?"off":"on");render();}
  private void controlsDetailed() { buildDashboard(false); }
  private void controlsSimple() { buildDashboard(true); }

  private final java.util.List<Runnable> dashboardBindings = new java.util.ArrayList<>();
  private boolean dashboardReady;

  private void bindDashboard(Runnable binding) {
    dashboardBindings.add(binding);
    binding.run();
  }

  private void onModelChanged() {
    if(model.progressOnly&&page==3){TextView label=findViewById(android.R.id.content).findViewWithTag("status_update");if(label!=null)label.setText(dashboardStamp());}
    if(model.progressOnly&&page!=0||page==2&&model.hasSession())return;
    if(parkingDialog!=null&&(!model.hasSession()||model.selected==null||!java.util.Objects.equals(parkingVin,model.selected.vin)))dismissParking();
    if (dashboardReady && page == 0 && model.hasSession()) {
      for (Runnable binding : new java.util.ArrayList<>(dashboardBindings)) binding.run();
      if (controlRefresh != null)
        controlRefresh.setRefreshing(model.busy && model.activeAction.isEmpty());
    } else {View old=findViewById(android.R.id.content).findViewWithTag("status_scroll");if(old instanceof ScrollView)statusScrollY=((ScrollView)old).getScrollY();render();}
  }

  private TextView liveText(java.util.function.Supplier<String> value, int sp, int color, boolean heavy) {
    TextView view = text("", sp, color, AppPreferences.visible(this,"ui_data_bold",false));
    fitSingleLine(view,sp);
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
    FrameLayout section=new FrameLayout(this);section.setTag("charging_section");
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
    row.addView(lines,new LinearLayout.LayoutParams(0,-2,1));

    ChargingPulseView pulse=new ChargingPulseView(this);pulse.setAccentColor(chargeAccent);
    section.addView(pulse,new FrameLayout.LayoutParams(-1,-1));
    section.addView(row,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
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

  private void chargingDialog(){openDataSection("data_charging");}

  private void buildVoLikeDashboard(boolean detailed) {
    android.content.res.Configuration config=getResources().getConfiguration();
    boolean landscape=config.screenWidthDp>config.screenHeightDp;
    boolean compact=landscape||config.screenHeightDp<680||config.fontScale>1.2f;
    body.setPadding(0,0,0,dp(landscape?0:2));

    LinearLayout head=row();head.setTag("volike_header");
    TextView name=liveText(()->vehicleDisplayName(model.snapshot),landscape?12:16,ink,true);
    click(name,this::vehicleInfoDialog);head.addView(name,new LinearLayout.LayoutParams(0,-1,.65f));
    ImageView location=new ImageView(this);location.setImageResource(R.drawable.ic_location);location.setColorFilter(ink);
    location.setScaleType(ImageView.ScaleType.CENTER_INSIDE);location.setPadding(dp(9),dp(9),dp(9),dp(9));location.setTag("vehicle_location");
    click(location,this::locationDialog);bindDashboard(()->{location.setEnabled(!model.locationBusy&&!model.busy&&!model.commandPending&&model.selected!=null);location.setAlpha(location.isEnabled()?1f:.45f);});
    ImageView refresh=new ImageView(this);refresh.setImageResource(R.drawable.ic_refresh);refresh.setColorFilter(ink);refresh.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    refresh.setPadding(dp(9),dp(9),dp(9),dp(9));refresh.setTag("refresh_vehicle");click(refresh,()->model.refreshUser(null));
    bindDashboard(()->{refresh.setEnabled(!model.busy);refresh.setAlpha(refresh.isEnabled()?1f:.45f);});
    head.addView(parkingCaption(),new LinearLayout.LayoutParams(0,-1,1.35f));
    head.addView(location,new LinearLayout.LayoutParams(dp(40),-1));head.addView(refresh,new LinearLayout.LayoutParams(dp(40),-1));
    body.addView(head,new LinearLayout.LayoutParams(-1,dp(landscape?36:42)));
    TextView switchView=button(detailed?tr("详细 · 切换简洁 ›","Details · switch to simple ›"):tr("简洁 · 切换详细 ›","Simple · switch to details ›"),this::toggleVoDetails);switchView.setTag("volike_detail_toggle");switchView.setMinHeight(0);switchView.setPadding(dp(6),0,dp(6),0);switchView.setTextSize(10);body.addView(switchView,new LinearLayout.LayoutParams(-1,dp(26)));

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
        if(!detailed)addVoMetricGrid(body,chosen,false,false);
        addVoUtilities(body,false);
      } else {
        // Small-height / large-text phones keep the v0.8.1 compact structure so
        // every remote-control tile remains at least 48–60dp and fully visible.
        if(detailed){
          body.addView(voEnergyStrip(false,true),new LinearLayout.LayoutParams(-1,Math.max(dp(48),textLineHeight(17)+dp(18))));
          LinearLayout stage=column();stage.setTag("volike_hero");LinearLayout content=row();VehicleHeroView car=new VehicleHeroView(this);car.setTag("polestar_vehicle");car.setContentDescription(tr("查看车门与车窗状态","View doors and windows"));click(car,this::exteriorDialog);car.setVisibility(AppPreferences.visible(this,"show_car",true)?View.VISIBLE:View.INVISIBLE);content.addView(car,new LinearLayout.LayoutParams(0,-1,1));content.addView(voDetailRail(true),new LinearLayout.LayoutParams(dp(142),-1));stage.addView(content,new LinearLayout.LayoutParams(-1,0,1));stage.addView(voStatusStrip(true),new LinearLayout.LayoutParams(-1,dp(26)));body.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        }else body.addView(voHero(false),new LinearLayout.LayoutParams(
            -1,dp(compact?150:170),1));
        addChargingSection(body,compact);
        if(!detailed)addVoMetricGrid(body,chosen,false,false);
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
        new LinearLayout.LayoutParams(dp(compact?108:126),-1));
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
    label.setTypeface(buttonTypeface());fitSingleLine(label,compact?7:8);
    item.addView(label,new LinearLayout.LayoutParams(-1,hideTitle?0:textLineHeight(compact?7:8)));
    item.addView(feedbackText(control,true),new LinearLayout.LayoutParams(-1,-2));

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
    LinearLayout rail=column();rail.setTag("volike_detail_rail");rail.setPadding(dp(4),dp(4),dp(4),dp(4));GradientDrawable frame=bg(viSoft(),16);frame.setStroke(dp(1),line);rail.setBackground(frame);
    TextView title=text(tr("详细数据","Details"),9,ink,false);title.setGravity(Gravity.CENTER);title.setTypeface(buttonTypeface());rail.addView(title,new LinearLayout.LayoutParams(-1,textLineHeight(9)+dp(2)));
    java.util.List<DisplayMetric> metrics=controlMetrics();metrics.removeIf(m->m==DisplayMetric.VEHICLE_STATUS||m==DisplayMetric.BATTERY||m==DisplayMetric.RANGE||ChargingVisibility.isChargingMetric(m));
    if(metrics.isEmpty()){rail.addView(text(tr("在设置选择数据","Choose data in Settings"),9,muted,false));return rail;}
    for(int i=0;i<metrics.size();i+=2){LinearLayout row=row();for(int k=i;k<Math.min(i+2,metrics.size());k++){
      DisplayMetric metric=metrics.get(k);LinearLayout cell=column();cell.setGravity(Gravity.CENTER);
      TextView label=text(metric.shortLabel(AppPreferences.english(this)),7,muted,false);label.setGravity(Gravity.CENTER);fitSingleLine(label,7);
      TextView value=liveText(()->(compact?metric.shortLabel(AppPreferences.english(this))+" ":"")+metric.value(this,model.snapshot,true,AppPreferences.english(this)),compact?8:10,ink,false);value.setGravity(Gravity.CENTER);value.setTag("home_value_"+metric.id);
      cell.addView(value,new LinearLayout.LayoutParams(-1,0,1));if(!compact)cell.addView(label,new LinearLayout.LayoutParams(-1,0,1));
      click(cell,()->info(metric.label(AppPreferences.english(this)),metric.value(this,model.snapshot,true,AppPreferences.english(this))));row.addView(cell,new LinearLayout.LayoutParams(0,-1,1));}
      rail.addView(row,new LinearLayout.LayoutParams(-1,0,1));}
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
      label.setText(good?"All Good！":tr("车况","Status")+(warn?" !":""));
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
      label.setText(good?"All Good！":tr("车况","Status")+(warn?" !":""));
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
    if(landscape&&getResources().getConfiguration().screenHeightDp<=360)max=Math.min(max,6);
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
          value.setTag("home_value_"+metric.id);value.setGravity(Gravity.CENTER);TextView label=text(metric.shortLabel(AppPreferences.english(this)),landscape?7:8,muted,false);
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
    int size=dp(landscape?16:28);circle.setBackground(oval(viSoft()));
    ImageView icon=new ImageView(this);icon.setImageResource(control.equals("lock")?R.drawable.ic_lock:control.equals("climate")?R.drawable.ic_climate
        :control.equals("air")?R.drawable.ic_air:R.drawable.ic_tailgate);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setTag("control_icon_"+control);
    circle.addView(icon,new FrameLayout.LayoutParams(dp(landscape?14:22),dp(landscape?14:22),Gravity.CENTER));
    item.addView(circle,new LinearLayout.LayoutParams(size,size));
    TextView label=text(title,landscape?8:9,ink,false);label.setGravity(Gravity.CENTER);label.setSingleLine(true);
    boolean hideTitle=UiSkin.SIMPLE.equals(UiSkin.get(this))
        && !AppPreferences.visible(this,"volike_simple_labels",true);
    label.setVisibility(hideTitle?View.GONE:View.VISIBLE);
    label.setTag("volike_control_label");
    label.setTypeface(buttonTypeface());fitSingleLine(label,landscape?8:9);
    item.addView(label,new LinearLayout.LayoutParams(-1,hideTitle?0:textLineHeight(landscape?8:9)+dp(1)));
    TextView feedbackView=feedbackText(control,true);feedbackView.setMaxLines(1);item.addView(feedbackView,new LinearLayout.LayoutParams(-1,textLineHeight(8)));
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
    TextView camp=utility(tr("露营","Camp"),false,false,this::campDialog);camp.setTag("camp_button");
    TextView signal=utility(tr("寻车","Find car"),false,false,this::signalMenu);signal.setTag("signal_control_button");
    bindDashboard(()->{
      boolean warn=VehicleCondition.problem(model.snapshot,showTires());
      condition.setText(tr("车况","Status"));condition.refreshService();
      ((GradientDrawable)condition.getBackground()).setStroke(dp(1),warn?orange:line);
      boolean active=CampModeManager.active(this)||"STOPPING".equals(CampModeManager.phase(this));((GradientDrawable)camp.getBackground()).setColor(active?voAccent():surface);
      camp.setTextColor(active?Color.WHITE:ink);tintUtilityIcon(camp);((UtilityButton)camp).feedback(ButtonFeedback.value(this,"camp",model.snapshot,CampModeManager.status(this)));
      ((UtilityButton)windows).feedback(ButtonFeedback.value(this,"windows",model.snapshot,model.snapshot==null?"--":status(model.snapshot.windows)));
      ((UtilityButton)signal).feedback(ButtonFeedback.value(this,"signal",model.snapshot,model.commandPending&&model.activeAction.startsWith("signal")?tr("待确认","Verifying"):""));
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
    head.addView(parkingCaption(),new LinearLayout.LayoutParams(0,-1,1.35f));
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
          :dp(26)+textLineHeight(14)+textLineHeight(Math.max(12,ButtonFeedback.sp(this)))+textLineHeight(11);
      if(fourAcross)height=landscape?Math.max(dp(48),Math.max(dp(nioSkin()?36:24),textLineHeight(10)+textLineHeight(ButtonFeedback.sp(this)))+dp(8)):Math.max(height,dp(nioSkin()?52:34)+textLineHeight(10)+textLineHeight(ButtonFeedback.sp(this)));
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
      label.setText(good?"All Good！":tr("车况","Status")+(warn?" !":""));label.setTextColor(good||warn?accentText():muted);
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
    button.setText(title);button.setTextSize(11);button.setTextColor(ink);button.setTypeface(buttonTypeface());
    if(UiSkin.PS_DEFAULT.equals(UiSkin.get(this)))button.setConditionIcon(R.drawable.ic_nav_vehicle_data_ref);button.setGravity(Gravity.CENTER);
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
    parent.addView(shortcuts,new LinearLayout.LayoutParams(-1,Math.max(dp(80),dp(52)+textLineHeight(10)+textLineHeight(ButtonFeedback.sp(this)))));gap(parent,6);
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
      ((GradientDrawable)camp.getBackground()).setColor(active?orange:surface);camp.setTextColor(active?Color.BLACK:ink);tintUtilityIcon(camp);((UtilityButton)camp).feedback(ButtonFeedback.value(this,"camp",model.snapshot,CampModeManager.status(this)));
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
    TextView feedback=text("",ButtonFeedback.sp(this),muted,false);feedback.setTag("shortcut_feedback");feedback.setGravity(Gravity.CENTER);feedback.setTypeface(ButtonFeedback.typeface(this));fitSingleLine(feedback,ButtonFeedback.sp(this));label.setTypeface(buttonTypeface());fitSingleLine(label,10);
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
      feedback.setVisibility(AppPreferences.visible(this,"show_button_feedback",true)&&!"hidden".equals(ButtonFeedback.mode(this,kind))?View.VISIBLE:View.GONE);
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
          : kind.equals("health") ? tr("车况", "Health") : kind.equals("camp") ? tr("露营", "Camp") : tr("寻车", "Find car");
      TextView view = kind.equals("body")?conditionButton(initialLabel):utility(initialLabel, false, false, action);
      view.setTextSize(11);
      if(UiSkin.PS_DEFAULT.equals(UiSkin.get(this))&&(kind.equals("windows")||kind.equals("signal")))utilityIcon(view,kind.equals("windows")?R.drawable.ic_window:R.drawable.ic_horn);

      view.setPadding(dp(3), dp(2), dp(3), dp(2));
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
            : kind.equals("camp") ? tr("露营", "Camp") : tr("寻车", "Find car");
        view.setMaxLines(good ? 2 : 1);
        view.setText(nioSkin()&&(kind.equals("windows")||kind.equals("signal"))?"":good ? "ALL\nGOOD" : label + (warn ? " !" : ""));
        GradientDrawable background = (GradientDrawable) view.getBackground();
        background.setColor(active ? orange : surface);
        background.setStroke(dp(1), active || good ? orange : line);
        view.setTextColor(active ? Color.rgb(21,22,23) : good ? orange : ink);if(kind.equals("camp"))tintUtilityIcon(view);
        String details = kind.equals("health") ? (v == null ? tr("未上报", "Unavailable") : v.health.summary(AppPreferences.english(this), showTires()))
            : kind.equals("windows") ? (v == null ? tr("未知", "Unknown") : status(v.windows))
            : kind.equals("body") ? (v == null ? tr("未知", "Unknown") : status(v.doors) + " · " + status(v.windows))
            : kind.equals("camp") ? CampModeManager.status(this) : tr("鸣笛与闪灯", "Honk and flash");
        view.setContentDescription(label + " · " + details);
        if(view instanceof UtilityButton)((UtilityButton)view).feedback(ButtonFeedback.value(this,kind,model.snapshot,kind.equals("signal")?"":details));
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
      return tr("运行中", "Running");
    }
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
    TextView support = text("", ButtonFeedback.sp(this), muted, false);
    title.setTypeface(buttonTypeface());feedback.setTypeface(ButtonFeedback.typeface(this));support.setTypeface(ButtonFeedback.typeface(this));
    title.setGravity(Gravity.CENTER);feedback.setGravity(Gravity.CENTER);support.setGravity(Gravity.CENTER);
    for (TextView v : new TextView[]{title, feedback, support}) {
      fitSingleLine(v,v==title?(compact?10:14):ButtonFeedback.sp(this));
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
      LinearLayout heading = row();heading.setGravity(Gravity.CENTER);
      heading.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));
      LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2); tp.leftMargin = dp(5);
      heading.addView(title, tp); card.addView(heading); gap(card, 2);
      if (nioSkin() && (control.equals("climate")||control.equals("air"))) {
        TextView temperature=liveText(() -> control.equals("air")?("AQI "+(model.snapshot==null||model.snapshot.environment.aqi==null?"--":model.snapshot.environment.aqi))
            :model.snapshot==null ? "--°C":DisplayMetric.TARGET_TEMPERATURE.value(this,model.snapshot,true,AppPreferences.english(this)),22,ink,true);
        temperature.setGravity(Gravity.CENTER);temperature.setVisibility(View.VISIBLE);card.addView(temperature);
      }
    }
    if(!horizontalCompact)card.addView(feedback,
        new LinearLayout.LayoutParams(-1,-2));
    if (!compact) card.addView(support, new LinearLayout.LayoutParams(-1, -2));
    bindDashboard(() -> {
      String raw = controlRaw(control);icon.setImageResource(controlIcon(control,raw));
      boolean active = "开启".equals(raw) || "未锁".equals(raw) || "已打开".equals(raw) || "未关严".equals(raw);
      boolean known = active || "关闭".equals(raw) || "已上锁".equals(raw) || "已锁".equals(raw) || "已关闭".equals(raw);
      int activeColor = nioColors() && (control.equals("climate") || control.equals("air")) ? Color.rgb(0,191,185) : orange;
      background.setColor(nioSkin() ? surface : known ? active ? orange : Color.WHITE : surface);
      background.setStroke(dp(1), controlSending(control) || nioSkin() && active ? activeColor : nioSkin() ? line : known ? Color.TRANSPARENT : line);
      int color = nioSkin() ? ink : known ? Color.rgb(21,22,23) : ink;
      icon.setColorFilter(nioSkin() && active ? activeColor : color);
      if (circles && !nioSkin()) icon.setBackground(oval(active ? Color.argb(18,0,0,0) : viSoft()));
      title.setTextColor(color); feedback.setTextColor(color);
      String feedbackValue=ButtonFeedback.value(this,control,model.snapshot,controlFeedback(control,compact));
      feedback.setText(feedbackValue);feedback.setVisibility(AppPreferences.visible(this,"show_button_feedback",true)&&!feedbackValue.isEmpty()?View.VISIBLE:View.GONE);
      String detail = model.snapshot == null ? "" : control.equals("climate")
          ? ClimateRuntime.detail(this, model.snapshot, AppPreferences.get(this, "vehicle", ""))
          : control.equals("air") ? "PM2.5 " + MileageSnapshot.decimalText(model.snapshot.environment.pm25UgM3, "")
              + " · AQI " + (model.snapshot.environment.aqi == null ? "--" : model.snapshot.environment.aqi) : "";
      support.setText("");support.setVisibility(View.GONE);
      card.setEnabled(!model.busy && !model.commandPending && model.selected != null);
      card.setAlpha(card.isEnabled() ? 1f : .55f);
      card.setContentDescription(label+" · "+feedbackValue);
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
          value.setTag("home_value_"+metric.id);
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
        String.format(java.util.Locale.US,"%.5f, %.5f",model.parkedLocation.latitude,model.parkedLocation.longitude),9,muted,false);
    caption.setTag("parking_caption");caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.setGravity(Gravity.CENTER_VERTICAL);click(caption,this::locationDialog);return caption;
  }

  private void locationDialog(){locationDialog(false);}
  private void locationDialog(boolean force) {
    toast(tr("正在读取最后停车位置…","Reading last parked location…"));
    model.readLastParkedLocation(force,(location,error)->{
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
    if(MapLocationPermission.enabled(this))setting(panel,tr("刷新手机位置 · 仅本次预览","Refresh phone position · this preview only"),()->map.requestPhoneLocation());
    panel.addView(text(tr("纬度 ","Lat ")+String.format(java.util.Locale.US,"%.6f",location.latitude)+" · "+tr("经度 ","Lon ")+String.format(java.util.Locale.US,"%.6f",location.longitude),12,muted,false));
    panel.addView(text(tr("这是最后停车位置，并非实时定位。地图网页由高德提供，本次停车点会用于加载地图。","Last parked position, not live tracking. The Amap page receives this parked point to display the map."),12,muted,false));
    AlertDialog dialog=new HapticDialogBuilder(this).setTitle(tr("最后停车位置","Last parked location")).setView(panel)
        .setPositiveButton(tr("打开地图","Open map"),(d,w)->openLocation(location))
        .setNeutralButton(tr("重新读取车辆位置","Refresh vehicle position"),(d,w)->locationDialog(true))
        .setNegativeButton(tr("关闭","Close"),null).create();
    parkingDialog=dialog;dialog.setOnDismissListener(d->{map.close();if(parkingDialog==dialog){parkingDialog=null;parkingVin=null;parkingPoint=null;parkingPreview=null;}});dialog.show();
  }

  private void mapLocationDialog(){
    LinearLayout panel=detailPanel();mapLocationSwitch=new Switch(this);mapLocationSwitch.setTag("map_phone_location");
    mapLocationSwitch.setText(tr("允许高德网页使用手机位置","Allow Amap web page to use phone location"));mapLocationSwitch.setTextColor(ink);mapLocationSwitch.setChecked(MapLocationPermission.enabled(this));
    mapLocationSwitch.setOnCheckedChangeListener((button,on)->{Haptics.tap(this);if(on&&!MapLocationPermission.granted(this)){
      requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},MAP_LOCATION);
    }else{AppPreferences.set(this,MapLocationPermission.KEY,on?"on":"off");if(parkingPreview!=null)parkingPreview.refreshPhoneLocation();}});panel.addView(mapLocationSwitch);
    setting(panel,tr("手机位置刷新时机","Phone location refresh timing"),this::phoneRefreshDialog);
    panel.addView(text(tr("默认关闭。车辆停车位置不需要手机定位。开启需系统授权，可选择仅在二级预览打开时请求一次，或手动点击刷新；不在后台跟踪。距离不是步行路线。",
        "Off by default. Vehicle parking reads do not need phone location. With system permission, request once when opening the secondary map preview, or manually. No background tracking; distance is not a walking route."),12,muted,false));
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
    TextView a = text(key, 14, muted, false), b = text(value==null||value.trim().isEmpty()?"--":value, 14, ink, false);
    b.setGravity(Gravity.END);a.setMaxLines(2);b.setMaxLines(2);a.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setTypeface(AppPreferences.visible(this,"ui_data_bold",false)?bold:regular);
    r.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
    r.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    p.addView(r);
    gap(p, 3);
  }

  private void disclosure(LinearLayout parent,String key,String title,boolean initiallyOpen,java.util.function.Consumer<LinearLayout> fill){
    LinearLayout frame=column();frame.setTag("disclosure_"+key);GradientDrawable outline=bg(surface,8);outline.setStroke(dp(1),line);frame.setBackground(outline);
    LinearLayout header=row();header.setTag("disclosure_header_"+key);header.setPadding(dp(12),dp(6),dp(10),dp(6));header.setMinimumHeight(dp(48));header.setFocusable(true);
    TextView label=text(title,14,ink,false);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(label,new LinearLayout.LayoutParams(0,-2,1));
    TextView arrow=text("",16,ink,false);arrow.setGravity(Gravity.CENTER);header.addView(arrow,new LinearLayout.LayoutParams(dp(26),Math.max(dp(32),textLineHeight(16)+dp(4))));frame.addView(header);
    LinearLayout content=column();content.setTag("disclosure_content_"+key);content.setPadding(dp(8),0,dp(8),dp(8));frame.addView(content);final boolean[] built={false};
    java.util.function.Consumer<Boolean> show=open->{if(open&&!built[0]){fill.accept(content);disableSystemHaptics(content);built[0]=true;}content.setVisibility(open?View.VISIBLE:View.GONE);arrow.setText(open?"⌄":"›");header.setContentDescription(title+tr(open?"，已展开，点击收起":"，已收起，点击展开",open?", expanded. Collapse":", collapsed. Expand"));};
    show.accept(AppPreferences.visible(this,"expand_"+key,initiallyOpen));click(header,()->{boolean open=content.getVisibility()!=View.VISIBLE;AppPreferences.set(this,"expand_"+key,open?"on":"off");show.accept(open);});
    parent.addView(frame,new LinearLayout.LayoutParams(-1,-2));gap(parent,8);
  }

  private LinearLayout detailPanel(){LinearLayout p=column();p.setPadding(dp(12),dp(8),dp(12),dp(8));return p;}
  private void openDataSection(String key){AppPreferences.set(this,"expand_"+key,"on");page=1;render();}
  private void tripDialog(){openDataSection("data_trips");}
  private void airDetailsDialog(){openDataSection("data_air");}
  private void tripContents(LinearLayout panel){metricRows(panel,DisplayMetric.TA_DISTANCE,DisplayMetric.TA_SPEED,DisplayMetric.TA_CONSUMPTION,DisplayMetric.TA_DURATION,DisplayMetric.TM_DISTANCE,DisplayMetric.TM_SPEED,DisplayMetric.TM_CONSUMPTION,DisplayMetric.TM_DURATION,DisplayMetric.SINCE_CHARGE_DISTANCE,DisplayMetric.SINCE_CHARGE_SPEED,DisplayMetric.SINCE_CHARGE_CONSUMPTION);panel.addView(text(tr("行驶时长由同次行程距离÷均速换算，以h/m显示。TA能耗仅在来源可核对时显示，缺失或冲突显示 --。","Driving time uses trip distance / average speed, in h/m. TA consumption requires corroborated sources; missing/conflicting values show --."),12,muted,false));}
  private void airContents(LinearLayout panel){VehicleSnapshot v=model.snapshot;if(v==null)return;metricRows(panel,DisplayMetric.TEMPERATURE,DisplayMetric.TARGET_TEMPERATURE,DisplayMetric.AQI,DisplayMetric.PM25);detailRow(panel,tr("空调 / 净化","Climate / purification"),status(v.climate)+" / "+status(v.air));detailRow(panel,tr("空调剩余时间","Climate time left"),ClimateRuntime.detail(this,v,AppPreferences.get(this,"vehicle","")));panel.addView(text(tr("空调剩余时间按最近上报值倒计时；缺失时采用本机启动记录，不代表执行已确认。","Climate time counts down from the last report; missing reports may use the local start record and do not confirm execution."),11,muted,false));detailRow(panel,tr("空气数据更新","Air updated"),v.environment.airReportedAt>0?android.text.format.DateFormat.format("MM-dd HH:mm",v.environment.airReportedAt).toString():"--");}
  private void metricRows(LinearLayout panel,DisplayMetric... metrics){for(DisplayMetric metric:metrics)detailRow(panel,metric.label(AppPreferences.english(this)),metric.value(this,model.snapshot,true,AppPreferences.english(this)));}
  private void details(){
    LinearLayout panel=scrollBody();((ScrollView)panel.getParent()).setTag("data_scroll");VehicleSnapshot v=model.snapshot;
    panel.addView(text(tr("车辆数据","Vehicle data"),24,ink,false));if(v==null){panel.addView(text(tr("暂无车况，请连接车辆后刷新。","No vehicle snapshot. Connect and refresh first."),14,muted,false));setting(panel,tr("刷新车辆","Refresh vehicle"),()->model.refreshUser(null));return;}
    detailSection(panel,tr("基础数据","Overview"));metricRows(panel,DisplayMetric.BATTERY,DisplayMetric.RANGE,DisplayMetric.ODOMETER,DisplayMetric.CHARGE_LIMIT,DisplayMetric.SERVICE_CYCLE);
    disclosure(panel,"data_health",tr("车辆状况 · 已上报项目优先","Vehicle condition · reported checks first"),true,this::healthOverview);
    disclosure(panel,"data_exterior",tr("车门 / 车窗 / 尾箱","Doors / windows / tailgate"),false,this::exteriorContents);
    if(showTires())disclosure(panel,"data_tyres",tr("四轮 iTPMS 状态","Four-wheel iTPMS status"),false,this::tyreContents);
    disclosure(panel,"data_trips",tr("行程 · TA / TM / 充电后","Trips · TA / TM / since charge"),false,this::tripContents);
    if(ChargingVisibility.shouldShow(v))disclosure(panel,"data_charging",tr("充电详情","Charging details"),false,p->{for(DisplayMetric metric:DisplayMetric.values())if(ChargingVisibility.isChargingMetric(metric)||metric==DisplayMetric.CHARGE_LIMIT)metricRows(p,metric);});
    disclosure(panel,"data_air",tr("空调与空气质量","Climate and air quality"),false,this::airContents);
    disclosure(panel,"data_service",tr("保养周期与原始读数","Service cycle and raw values"),false,this::serviceContents);
    final int desired=dataScrollY;((ScrollView)panel.getParent()).post(()->((ScrollView)panel.getParent()).scrollTo(0,desired));
    disclosure(panel,"data_identity",tr("车辆信息 · 型号 / 年款 / VIN","Vehicle info · model / year / VIN"),false,p->{io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo info=model.selected;detailRow(p,tr("车型","Model"),info==null?"--":info.modelName);detailRow(p,tr("年款","Year"),info==null?"--":info.modelYear);detailRow(p,"VIN",info==null?"--":info.vin);setting(p,tr("切换车辆","Select vehicle"),this::chooseVehicle);});
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


  private String currentSkinLabel(){String skin=UiSkin.get(this);return UiSkin.SIMPLE.equals(skin)||UiSkin.DETAILED.equals(skin)?"VoLike · "+(UiSkin.detailedView(this)?tr("详细","Detailed"):tr("简洁","Simple")):UiSkin.label(this,skin);}
  private boolean selectedSkin(String value){return value.equals(UiSkin.get(this))||value.equals(UiSkin.SIMPLE)&&UiSkin.DETAILED.equals(UiSkin.get(this));}
  private void skinDialog() {
    LinearLayout panel=column();panel.setPadding(dp(8),dp(8),dp(8),dp(8));
    ScrollView scroller=new ScrollView(this);scroller.setFillViewport(false);
    final AlertDialog[] dialog={null};
    for(String skin:UiSkin.pickerChoices()){
      LinearLayout option=column();option.setTag("skin_picker_option_"+skin);
      option.setPadding(dp(6),dp(5),dp(6),dp(5));
      GradientDrawable frame=bg(surface,16);
      frame.setStroke(dp(selectedSkin(skin)?2:1),
          selectedSkin(skin)?voAccent():line);
      option.setBackground(frame);
      TextView title=text((UiSkin.SIMPLE.equals(skin)?"VoLike":UiSkin.label(this,skin))
          +(selectedSkin(skin)?"  ✓":""),14,ink,true);
      title.setGravity(Gravity.CENTER_VERTICAL);
      option.addView(title,new LinearLayout.LayoutParams(-1,dp(28)));
      SkinPreviewView preview=new SkinPreviewView(this,skin);
      preview.setSelectedSkin(selectedSkin(skin));
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

  private void setting(LinearLayout parent,String title,Runnable action){TextView entry=button(title,action);entry.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);parent.addView(entry);gap(parent,6);}
  private void settingToggle(LinearLayout parent,String key,String title,boolean fallback,boolean widget){
    Switch toggle=new Switch(this);toggle.setTag(key);toggle.setText(title);toggle.setTypeface(regular);toggle.setTextColor(ink);toggle.setMinHeight(dp(48));toggle.setHapticFeedbackEnabled(false);toggle.setChecked(AppPreferences.visible(this,key,fallback));
    toggle.setOnCheckedChangeListener((view,on)->{Haptics.tap(this);AppPreferences.set(this,key,on?"on":"off");if(widget)WidgetRenderer.refreshAll(this);if(key.equals("app_background_enabled")||key.equals("refresh_wifi_only")||key.equals("refresh_only_charging"))ResidentService.scheduleUpdates(this);});parent.addView(toggle);
  }
  private void themeChoice(){new HapticDialogBuilder(this).setTitle(tr("日夜模式","Day / night mode")).setItems(new String[]{tr("跟随系统","System"),tr("浅色","Light"),tr("深色","Dark")},(d,w)->{AppPreferences.set(this,"theme",new String[]{"system","light","dark"}[w]);WidgetRenderer.refreshAll(this);render();}).show();}
  private void widgetFields(){String[] labels={"4×2 · 8","2×2 · 4","4×1 · 4","5×1 · 4","2×1 · 2","Data 4×4 · 8"};WidgetRenderer.Size[] sizes={WidgetRenderer.Size.LARGE,WidgetRenderer.Size.SQUARE,WidgetRenderer.Size.WIDE,WidgetRenderer.Size.EXTRA_WIDE,WidgetRenderer.Size.MINI,WidgetRenderer.Size.DATA};new HapticDialogBuilder(this).setTitle(tr("Widget 尺寸与数据","Widget size and data")).setItems(labels,(d,w)->metricDialog(WidgetRenderer.selectionKey(this,sizes[w]),WidgetRenderer.capacity(sizes[w]),WidgetRenderer.selectionDefaults(this,sizes[w]))).show();}
  private void pseuChoice(){String[] keys={"battery","range","car","controls"};new HapticDialogBuilder(this).setTitle("PSeu").setSingleChoiceItems(new String[]{tr("电量优先","Battery first"),tr("续航优先","Range first"),tr("车辆图片","Vehicle image"),tr("扩展控制","Extra controls")},java.util.Arrays.asList(keys).indexOf(AppPreferences.get(this,"pseu_type","battery")),(d,w)->{AppPreferences.set(this,"pseu_type",keys[w]);WidgetRenderer.refreshAll(this);d.dismiss();}).show();}
  private void residentSettings(LinearLayout p){
    Switch keep=new Switch(this);keep.setTag("resident_toggle");keep.setText(tr("常驻车辆状态通知","Persistent vehicle notification"));keep.setTextColor(ink);keep.setMinHeight(dp(48));keep.setChecked(AppPreferences.enabled(this));keep.setOnCheckedChangeListener((view,on)->{Haptics.tap(this);if(on&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATIONS);return;}setResident(on);});p.addView(keep);
    p.addView(text(tr("静默显示电量、充电、空调/净化与更新时间。刷新周期在下方独立设置，Android省电限制可能延迟；锁屏隐藏详情。","Silent battery, charging, climate/air and update time. Configure refresh below; Android battery restrictions can delay it. Lock-screen details stay private."),12,muted,false));
    setting(p,tr("后台与电池设置","Background and battery settings"),()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));
  }
  private void signOut(){if(model.busy||model.commandPending){toast(tr("正在处理车辆操作，请稍后退出","A vehicle operation is pending. Sign out after it finishes."));return;}new HapticDialogBuilder(this).setMessage(tr("退出并清除本机登录会话？","Sign out and clear this phone's session?")).setPositiveButton(tr("退出","Sign out"),(d,w)->{inputPhone="";inputPassword="";model.logout();page=0;render();}).setNegativeButton(tr("取消","Cancel"),null).show();}
  private void settings(){
    LinearLayout root=scrollBody();((ScrollView)root.getParent()).setTag("settings_scroll");root.addView(text(tr("设置","Settings"),24,ink,false));root.addView(text("v"+BuildConfig.VERSION_NAME+" · "+tr("独立版本","Standalone version"),12,muted,false));gap(root,16);
    disclosure(root,"settings_appearance",tr("外观与交互","Appearance and interaction"),true,p->{
      setting(p,tr("界面皮肤 · ","UI skin · ")+currentSkinLabel(),this::skinDialog);
      setting(p,tr("日夜模式 · 跟随系统 / 浅色 / 深色","Day/night · system / light / dark"),this::themeChoice);
      setting(p,tr("程序语言 / Language","App language / 语言"),this::languageDialog);
      setting(p,tr("字体大小 · 当前皮肤 / Widget","Font size · current skin / Widget"),this::fontDialog);
      settingToggle(p,"ui_button_bold",tr("按钮文字加粗","Bold button labels"),true,false);
      settingToggle(p,"ui_data_bold",tr("数据文字加粗","Bold data values"),false,false);
      setting(p,tr("按钮反馈 · 内容 / 字号 / 粗细","Button feedback · content / size / weight"),this::feedbackDialog);
      settingToggle(p,"charging_animation",tr("充电文字框缓慢流光","Gentle charging-card sweep"),true,false);
      setting(p,tr("按键震动 · 开关与强度","Button haptics · switch and strength"),this::hapticsDialog);
      settingToggle(p,"volike_simple_labels",tr("VoLike 显示按钮文字","VoLike control labels"),true,false);
    });
    disclosure(root,"settings_home",tr("首页与车况数据","Home and vehicle data"),false,p->{
      setting(p,tr("首页数据 · 最多12项","Home data · up to 12"),()->metricDialog("home_metrics",12,HOME_DEFAULTS));
      setting(p,tr("主页数据拖动排序","Drag to reorder home data"),this::metricOrderDialog);
      setting(p,tr("车况按钮 · 百分比 / 进度条","Condition button · percentage / bar"),this::conditionDisplayDialog);
      settingToggle(p,"show_car",tr("车辆图","Vehicle image"),true,false);settingToggle(p,"show_charging",tr("充电状态（开始/充电时显示）","Charging status (while starting/charging)"),true,false);settingToggle(p,"show_update",tr("数据更新时间","Data update time"),true,false);
      settingToggle(p,"show_tire_status",tr("显示胎压状态（含车况汇总）","Show tyre status (including summary)"),true,true);
      settingToggle(p,"experimental_tire",tr("四轮iTPMS示意图（无未验证kPa）","Four-wheel iTPMS diagram (no unverified kPa)"),false,false);
      setting(p,tr("车辆状况 · 已上报项目优先","Vehicle condition · reported checks first"),this::vehicleStatusDialog);
      setting(p,tr("保养周期 · 自动读取","Service cycle · automatic"),this::maintenanceDialog);
      p.addView(text(tr("保养周期参考365天/30,000km，余量取较小值。完整读数在车况页展开查看。","Service ratio uses the lower of the 365-day / 30,000-km references. Expand the condition page for raw readings."),12,muted,false));
    });
    disclosure(root,"settings_widget",tr("桌面 Widget","Home-screen widgets"),false,p->{
      setting(p,tr("Widget 风格 · ","Widget design · ")+VoLikeWidgetRenderer.label(this,VoLikeWidgetRenderer.style(this)),this::widgetDesignDialog);
      setting(p,tr("Widget 数据 · 按尺寸选择","Widget data · choose by size"),this::widgetFields);setting(p,tr("PSeu 显示类型","PSeu display type"),this::pseuChoice);
      setting(p,tr("Widget 按钮 · 按尺寸选择","Widget buttons · choose by size"),this::widgetButtonSettings);
      setting(p,tr("Widget 字号（独立设置）","Widget text size (independent)"),this::fontDialog);
      settingToggle(p,"widget_bold",tr("Widget 加粗字体（所有风格）","Bold widget text (all styles)"),false,true);
      setting(p,tr("Widget 语言 · 中文 / English","Widget language · 中文 / English"),this::widgetLanguageDialog);
      setting(p,tr("背景样式 · 不透明 / 透明","Background · opaque / transparent"),()->new HapticDialogBuilder(this).setTitle(tr("Widget 背景","Widget background")).setItems(new String[]{tr("不透明","Opaque"),tr("透明","Transparent")},(d,w)->{AppPreferences.set(this,"widget_style",w==0?"opaque":"transparent");WidgetRenderer.refreshAll(this);}).show());
      setting(p,tr("Widget 文字颜色","Widget text color"),()->new HapticDialogBuilder(this).setTitle(tr("Widget 文字颜色","Widget text color")).setItems(new String[]{tr("跟随日夜","Follow day/night"),tr("浅色文字","Light text"),tr("深色文字","Dark text")},(d,w)->{AppPreferences.set(this,"widget_ink",new String[]{"system","light","dark"}[w]);WidgetRenderer.refreshAll(this);}).show());
      widgetSlider(p,tr("透明度","Opacity"),"widget_opacity",58);widgetSlider(p,tr("模拟磨砂程度","Simulated frost intensity"),"widget_frost",28);p.addView(text(tr("磨砂是视觉模拟，不实时模糊桌面壁纸。","Frost is simulated; it does not live-blur wallpaper."),11,muted,false));
    });
    disclosure(root,"settings_vehicle",tr("车辆功能与位置权限","Vehicle features and map permission"),false,p->{setting(p,tr("露营模式","Camping mode"),this::campDialog);setting(p,tr("车辆停车位置 · 缓存与刷新规则","Vehicle parking position · cache and refresh"),this::mapRefreshDialog);setting(p,tr("手机定位 · 权限与刷新时机","Phone location · permission and refresh timing"),this::mapLocationDialog);p.addView(text(tr("地图定位默认关闭；拒绝手机定位仍可查看车辆停车点。","Phone location is off by default. Denying it still allows viewing the parked vehicle point."),12,muted,false));});
    disclosure(root,"settings_background",tr("通知与后台刷新","Notifications and background refresh"),false,p->{residentSettings(p);setting(p,tr("App / Widget 刷新规则","App / Widget refresh rules"),this::refreshRulesDialog);});
    disclosure(root,"settings_watch",tr("手表布局预览","Watch layout previews"),false,p->{setting(p,tr("手表皮肤 · ","Watch skin · ")+watchSkinLabel()+tr(" · 圆屏 / 方屏"," · round / square"),this::watchPreview);p.addView(text(tr("这是布局预览，独立Wear OS安装包仍需单独验证。","These are layout previews; a standalone Wear OS APK requires separate validation."),12,muted,false));});
    disclosure(root,"settings_account",tr("账户、诊断与关于","Account, diagnostics and about"),false,p->{
      p.addView(text(new ClientSignatureProvider(this).isReady()?tr("完整独立构建 · 无需其他APK","Complete standalone build · no other APK needed"):tr("开发验证构建缺少登录配置，请安装完整签名版。","This developer verification build needs client configuration; install the complete signed APK."),12,muted,false));
      setting(p,tr("免责声明","Disclaimer"),()->info(tr("免责声明","Disclaimer"),FirstUseConsent.text(this)));
      setting(p,tr("复制诊断日志","Copy diagnostic log"),()->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Diagnostics",DiagnosticLog.export()));toast(tr("已复制","Copied"));});
      setting(p,tr("退出登录","Sign out"),this::signOut);p.addView(text("v"+BuildConfig.VERSION_NAME+" · "+tr("独立开发的非官方客户端","Independent unofficial client"),12,muted,false));
    });
    final int desired=settingsScrollY;((ScrollView)root.getParent()).post(()->((ScrollView)root.getParent()).scrollTo(0,desired));
  }

  private void choice(String title,String key,String[] keys,String[] labels,String fallback,Runnable after){
    new HapticDialogBuilder(this).setTitle(title).setSingleChoiceItems(labels,Math.max(0,Arrays.asList(keys).indexOf(AppPreferences.get(this,key,fallback))),(d,w)->{Haptics.tap(this);AppPreferences.set(this,key,keys[w]);if(after!=null)after.run();d.dismiss();}).setNegativeButton(tr("关闭","Close"),null).show();
  }
  private void conditionDisplayDialog(){choice(tr("车况按钮显示","Condition button display"),"condition_display",new String[]{"bar","percent","text"},new String[]{tr("文字 + 百分比 + 进度条","Text + percentage + bar"),tr("仅文字 + 百分比（无图标 / 进度条）","Text + percentage only (no icon / bar)"),tr("仅文字（隐藏百分比）","Text only (hide percentage)")},"bar",()->WidgetRenderer.refreshAll(this));}
  private void metricOrderDialog(){
    MetricOrderView list=new MetricOrderView(this,MetricSelection.read(this,"home_metrics",HOME_DEFAULTS));ScrollView scroll=new ScrollView(this);scroll.addView(list);
    new HapticDialogBuilder(this).setTitle(tr("主页排序 · 拖动 ≡","Home order · drag ≡")).setView(scroll).setPositiveButton(tr("保存","Save"),(d,w)->{MetricSelection.save(this,"home_metrics",list.values());render();}).setNegativeButton(tr("取消","Cancel"),null).show();
  }
  private void feedbackDialog(){
    LinearLayout panel=detailPanel();settingToggle(panel,"show_button_feedback",tr("显示按钮下方反馈文字","Show feedback below buttons"),true,false);settingToggle(panel,"feedback_bold",tr("反馈文字加粗","Bold feedback text"),false,false);
    SeekBar size=campSlider(panel,tr("反馈字号 sp","Feedback size sp"),8,14,ButtonFeedback.sp(this),null,null);
    String[] controls={"lock","trunk","climate","air","windows","signal","camp"};String[] names={tr("门锁","Lock"),tr("尾箱","Trunk"),tr("空调","Climate"),tr("空气净化","Air purification"),tr("车窗","Windows"),tr("寻车","Find car"),tr("露营","Camp")};
    for(int i=0;i<controls.length;i++){String control=controls[i];String label=names[i];setting(panel,label+tr(" · 反馈内容"," · feedback content"),()->{
      String[] keys=control.equals("climate")?new String[]{"climate","state","remaining","temperature","hidden"}:control.equals("air")?new String[]{"state","air","temperature","hidden"}:new String[]{"state","hidden"};
      String[] labels=new String[keys.length];for(int j=0;j<keys.length;j++)labels[j]=feedbackLabel(keys[j]);choice(label,"feedback_"+control,keys,labels,control.equals("climate")?"climate":"state",null);
    });}
    panel.addView(text(tr("空调默认显示目标温度、运行状态和剩余时间，隐藏车内温度。数据、按钮名称与反馈的粗细分别设置；紧凑区域会缩小文字以适应边界。","Climate defaults to target temperature, running state and remaining time, with cabin temperature hidden. Data, button labels and feedback have separate weight settings; compact areas fit text to their bounds."),12,muted,false));
    ScrollView scroll=new ScrollView(this);scroll.addView(panel);new HapticDialogBuilder(this).setTitle(tr("按钮反馈","Button feedback")).setView(scroll).setPositiveButton(tr("应用","Apply"),(d,w)->AppPreferences.set(this,"feedback_sp",String.valueOf(size.getProgress()+8))).show();
  }
  private String feedbackLabel(String value){switch(value){case "climate":return tr("运行状态 + 剩余时间 + 目标温度","Running state + time left + target");case "temperature":return tr("车内温度","Cabin temperature");case "remaining":return tr("剩余运行时间","Remaining running time");case "air":return "AQI";case "hidden":return tr("隐藏","Hide");default:return tr("车辆实际状态","Reported vehicle state");}}
  private void intervalChoice(String title,String key,int fallback,int[] minutes,String zero,Runnable after){String[] keys=new String[minutes.length],labels=new String[minutes.length];for(int i=0;i<minutes.length;i++){keys[i]=String.valueOf(minutes[i]);labels[i]=minutes[i]==0?zero:minutes[i]+tr(" 分钟"," min");}choice(title,key,keys,labels,String.valueOf(fallback),after);}
  private void refreshRulesDialog(){
    LinearLayout p=detailPanel();settingToggle(p,"app_background_enabled",tr("App后台自动刷新（无需常驻通知）","App background refresh (no persistent notification required)"),false,false);settingToggle(p,"refresh_on_open",tr("打开 App 时刷新车辆","Refresh vehicle when opening the app"),true,false);
    setting(p,tr("App 前台刷新","App foreground refresh"),()->intervalChoice(tr("前台刷新周期","Foreground refresh interval"),"foreground_refresh_minutes",5,new int[]{0,1,2,5,10,15},tr("关闭自动刷新","Automatic refresh off"),this::scheduleForeground));
    setting(p,tr("App 后台刷新周期","App background refresh interval"),()->intervalChoice(tr("App后台刷新周期","App background refresh interval"),"app_background_minutes",30,new int[]{0,15,30,60,120},tr("关闭","Off"),()->ResidentService.scheduleUpdates(this)));
    setting(p,tr("Widget 后台刷新","Widget background refresh"),()->intervalChoice(tr("Widget 刷新周期","Widget refresh interval"),"widget_refresh_minutes",30,new int[]{0,15,30,60,120},tr("仅手动刷新","Manual only"),()->ResidentService.scheduleUpdates(this)));
    settingToggle(p,"refresh_wifi_only",tr("后台仅使用不限流量网络","Background: unmetered network only"),false,false);settingToggle(p,"refresh_only_charging",tr("后台仅在手机充电时刷新","Background: only while phone is charging"),false,false);
    p.addView(text(tr("建议前台5分钟、后台/Widget30分钟。App和Widget共享车辆快照，使用已启用来源中较快的周期；后台最短15分钟，由Android调度，省电模式可能延迟。车辆停车点和手机定位均独立，不在此后台任务中读取。远控后的5/10/15/20秒回读不受这些周期影响。","Recommended: foreground 5 min, background/widgets 30 min. App and widgets share one vehicle snapshot and use the fastest enabled source. Android schedules jobs at 15 min or longer and can defer them. These jobs do not request vehicle or phone location. Command readbacks at 5/10/15/20 s remain separate."),12,muted,false));
    new HapticDialogBuilder(this).setTitle(tr("刷新规则","Refresh rules")).setView(p).setPositiveButton(tr("完成","Done"),(d,w)->ResidentService.scheduleUpdates(this)).show();
  }
  private void mapRefreshDialog(){
    LinearLayout p=detailPanel();setting(p,tr("车辆位置读取 / 缓存周期","Vehicle position read / cache interval"),()->intervalChoice(tr("车辆停车位置缓存","Parked vehicle position cache"),"map_refresh_minutes",15,new int[]{0,5,15,30,60},tr("每次打开预览都读取","Read on every preview open"),null));
    p.addView(text(tr("建议15分钟：打开地图时优先复用周期内的c3Lbs停车点；点“重新读取车辆位置”立即绕过缓存。不会开启手机定位，不会后台追踪；上报为过期的位置不复用。手机定位在独立设置中控制。","Recommended: 15 min. Opening the map reuses a recent c3Lbs parked point; Refresh vehicle position bypasses the cache. No phone location or background tracking is started, and stale reports are not reused. Configure phone location separately."),12,muted,false));
    new HapticDialogBuilder(this).setTitle(tr("车辆位置刷新规则","Vehicle position refresh rules")).setView(p).setPositiveButton(tr("完成","Done"),null).show();
  }
  private void phoneRefreshDialog(){choice(tr("手机定位刷新时机","Phone location refresh timing"),"phone_location_refresh",new String[]{"manual","on_open"},new String[]{tr("仅在二级预览点按刷新（建议）","Manual refresh in secondary preview (recommended)"),tr("打开二级地图预览时刷新一次","Refresh once when secondary map preview opens")},"manual",null);}
  private void widgetButtonSettings(){
    String[] names={"2×1","4×1","5×1","2×2","4×2","4×4"};WidgetRenderer.Size[] sizes={WidgetRenderer.Size.MINI,WidgetRenderer.Size.WIDE,WidgetRenderer.Size.EXTRA_WIDE,WidgetRenderer.Size.SQUARE,WidgetRenderer.Size.LARGE,WidgetRenderer.Size.DATA};
    new HapticDialogBuilder(this).setTitle(tr("选择Widget按钮","Choose widget buttons")).setItems(names,(d,w)->widgetButtons(sizes[w])).show();
  }
  private void widgetButtons(WidgetRenderer.Size size){
    java.util.List<String> chosen=new ArrayList<>(WidgetButtons.read(this,size));String[] ids=WidgetButtons.CHOICES;String[] labels=new String[ids.length];boolean[] checked=new boolean[ids.length];for(int i=0;i<ids.length;i++){labels[i]=WidgetButtons.label(this,ids[i]);checked[i]=chosen.contains(ids[i]);}
    new HapticDialogBuilder(this).setTitle(tr("按钮 · 按勾选顺序排列","Buttons · selection order")).setMultiChoiceItems(labels,checked,(d,i,on)->{if(on){if(chosen.size()>=6){((AlertDialog)d).getListView().setItemChecked(i,false);toast(tr("最多选择6个；空间不足按顺序显示","Up to 6; smaller frames show the first buttons"));}else chosen.add(ids[i]);}else chosen.remove(ids[i]);})
      .setPositiveButton(tr("保存","Save"),(d,w)->{WidgetButtons.save(this,size,chosen);WidgetRenderer.refreshAll(this);}).setNeutralButton(tr("恢复默认","Reset"),(d,w)->{AppPreferences.set(this,WidgetButtons.key(size),"");WidgetRenderer.refreshAll(this);}).setNegativeButton(tr("取消","Cancel"),null).show();
  }

  private void maintenanceDialog(){openStatusSection("status_service");}

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

  private void vehicleStatusDialog(){if(page!=3){statusReturnPage=page;statusScrollY=0;}page=3;render();}
  private void openStatusSection(String key){AppPreferences.set(this,"expand_"+key,"on");vehicleStatusDialog();ScrollView scroll=findViewById(android.R.id.content).findViewWithTag("status_scroll");View section=findViewById(android.R.id.content).findViewWithTag("disclosure_"+key);if(scroll!=null&&section!=null)scroll.post(()->scroll.smoothScrollTo(0,section.getTop()));}
  private static final class HealthRow {
    final String label,value;final boolean reported,warning;HealthRow(String l,String v,boolean r,boolean w){label=l;value=v;reported=r;warning=w;}
  }
  private void healthState(java.util.List<HealthRow> rows,String label,int state,boolean en){rows.add(new HealthRow(label,VehicleHealthSnapshot.label(state,en),state!=VehicleHealthSnapshot.UNKNOWN,state==VehicleHealthSnapshot.WARNING));}
  private java.util.List<HealthRow> healthRows(VehicleSnapshot v){
    java.util.List<HealthRow> rows=new ArrayList<>();if(v==null)return rows;boolean en=AppPreferences.english(this);VehicleHealthSnapshot h=v.health;
    healthState(rows,tr("12V 电池","12 V battery"),h.lowVoltageBattery,en);healthState(rows,tr("制动液","Brake fluid"),h.brakeFluid,en);
    healthState(rows,tr("冷却液","Coolant"),h.coolant,en);healthState(rows,tr("玻璃水","Washer fluid"),h.washerFluid,en);
    healthState(rows,tr("外部灯光","Exterior lights"),h.exteriorLights,en);healthState(rows,tr("保养状态","Maintenance"),h.maintenance,en);
    if(showTires()){if(h.tirePressure==VehicleHealthSnapshot.UNKNOWN&&v.tires.hasAnyReport())rows.add(new HealthRow(tr("胎压告警","Tyre warning"),v.tires.hasProblem()?tr("需检查","Check"):tr("部分已上报","Partial report"),true,v.tires.hasProblem()));else healthState(rows,tr("胎压告警","Tyre warning"),h.tirePressure,en);}healthState(rows,tr("机油（适用时）","Oil (if applicable)"),h.oil,en);
    rows.sort((x,y)->Boolean.compare(y.warning,x.warning));return rows;
  }
  private void healthRow(LinearLayout panel,HealthRow row){detailRow(panel,row.label,row.reported?row.value:"--");View view=panel.getChildAt(panel.getChildCount()-2);view.setTag("health_row_"+row.label);view.setContentDescription(row.label+" · "+(row.reported?row.value:tr("未上报","Not reported")));if(row.warning&&view instanceof ViewGroup){TextView value=(TextView)((ViewGroup)view).getChildAt(1);value.setTextColor(accentText());}}
  private void healthOverview(LinearLayout panel){
    java.util.List<HealthRow> known=new ArrayList<>(),missing=new ArrayList<>();for(HealthRow row:healthRows(model.snapshot))(row.reported?known:missing).add(row);
    if(known.isEmpty())panel.addView(text(tr("暂无已上报的检查项目，请刷新车辆。","No reported checks yet. Refresh the vehicle."),13,muted,false));
    for(int i=0;i<Math.min(5,known.size());i++)healthRow(panel,known.get(i));
    if(known.size()>5)disclosure(panel,"status_more_reported",tr("其他已上报项目","More reported checks")+" · "+(known.size()-5),false,p->{for(int i=5;i<known.size();i++)healthRow(p,known.get(i));});
    if(!missing.isEmpty())disclosure(panel,"status_missing",tr("未上报 / 不适用项目","Not reported / not applicable")+" · "+missing.size(),false,p->{for(HealthRow row:missing)healthRow(p,row);});
    panel.addView(text(tr("12V、制动液等为告警状态，不是电压或液位测量。未上报不代表故障。","12 V, brake fluid and similar checks are warning states, not measured voltage or fluid levels. A missing report does not imply a fault."),11,muted,false));
  }
  private void serviceContents(LinearLayout panel){VehicleSnapshot v=model.snapshot;
    detailRow(panel,tr("保养周期剩余（参考）","Service cycle remaining (reference)"),ServiceReading.number(ServiceReading.cyclePercent(this,v),"%"));
    detailRow(panel,tr("剩余天数","Days left"),ServiceReading.number(ServiceReading.days(this,v)," d"));detailRow(panel,tr("剩余里程","Distance left"),ServiceReading.number(ServiceReading.km(this,v)," km"));
    detailRow(panel,tr("剩余工时","Engine hours left"),ServiceReading.number(v==null?null:v.health.serviceHoursLeft," h"));
    detailRow(panel,tr("车辆上报时间","Vehicle report time"),v==null||v.health.reportedAt<=0?"--":android.text.format.DateFormat.format("yyyy-MM-dd HH:mm",v.health.reportedAt).toString());
    serviceProgress(panel,tr("时间剩余（参考）","Time remaining (reference)"),ServiceReading.timePercent(this,v));serviceProgress(panel,tr("里程剩余（参考）","Distance remaining (reference)"),ServiceReading.distancePercent(this,v));
    panel.addView(text(tr("余量取时间与里程参考比例的较小值；参考365天 / 30,000km，不是后台原生寿命百分比。","Remaining ratio uses the lower of the time/distance references: 365 days / 30,000 km. It is not a native vehicle life percentage."),12,muted,false));
  }
  private void exteriorContents(LinearLayout panel){VehicleSnapshot v=model.snapshot;if(v==null){panel.addView(text(tr("暂无门窗数据","No exterior data"),13,muted,false));return;}
    panel.addView(new ExteriorView(this,v),new LinearLayout.LayoutParams(-1,dp(320)));panel.addView(text(tr("橙色 / ! 表示未关闭；-- 表示未上报。","Orange / ! means open or ajar; -- means not reported."),12,muted,false));
  }
  private void tyreContents(LinearLayout panel){VehicleSnapshot v=model.snapshot;if(v==null)return;boolean en=AppPreferences.english(this);String[] names=en?new String[]{"Front left","Front right","Rear left","Rear right"}:new String[]{"左前","右前","左后","右后"};for(int i=0;i<4;i++)detailRow(panel,names[i],v.tires.warning(i)==TireSnapshot.UNKNOWN?"--":v.tires.label(i,en));if(AppPreferences.visible(this,"experimental_tire",false))panel.addView(new TirePressureView(this,v.tires),new LinearLayout.LayoutParams(-1,dp(320)));panel.addView(text(tr("四轮以iTPMS上报状态显示；示意图可在设置开启，未验证的kPa数值不显示。","Each wheel uses reported iTPMS status. Enable the diagram in Settings; unverified kPa values are not shown."),12,muted,false));}
  private void vehicleStatusPage(){
    LinearLayout panel=scrollBody();ScrollView scroll=(ScrollView)panel.getParent();scroll.setTag("status_scroll");
    panel.addView(text(tr("车辆状况","Vehicle condition"),24,ink,true));panel.addView(text(VehicleCondition.summary(model.snapshot,AppPreferences.english(this),showTires()),16,VehicleCondition.problem(model.snapshot,showTires())?accentText():ink,false));
    TextView update=text(dashboardStamp(),10,muted,false);update.setTag("status_update");panel.addView(update);TextView refresh=button(tr("刷新车况","Refresh condition"),()->model.refreshUser(null));refresh.setTag("condition_refresh");refresh.setEnabled(!model.busy&&model.hasSession());panel.addView(refresh);gap(panel,12);
    healthOverview(panel);
    disclosure(panel,"status_service",tr("保养周期与原始上报值","Service cycle and reported values"),false,this::serviceContents);
    disclosure(panel,"status_exterior",tr("车门 / 车窗 / 尾箱位置状态","Doors / windows / tailgate positions")+(model.snapshot!=null&&ExteriorView.hasOpening(model.snapshot)?" !":""),false,this::exteriorContents);
    if(showTires())disclosure(panel,"status_tyres",tr("四轮 iTPMS 状态","Four-wheel iTPMS status"),false,this::tyreContents);
    final int desired=statusScrollY;scroll.post(()->scroll.scrollTo(0,desired));
  }
  private void healthMoreDialog(){vehicleStatusDialog();}
  private void serviceProgress(LinearLayout panel,String label,Integer value) {
    detailRow(panel,label,ServiceReading.number(value,"%"));ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    styleViProgress(bar);bar.setTag("service_progress_bar");bar.setMax(100);bar.setProgress(value==null?0:value);bar.setVisibility(value==null?View.GONE:View.VISIBLE);
    panel.addView(bar,new LinearLayout.LayoutParams(-1,dp(4)));gap(panel,7);
  }

  private void tireDialog(){openStatusSection("status_tyres");}
  private void exteriorDialog(){openStatusSection("status_exterior");}

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
