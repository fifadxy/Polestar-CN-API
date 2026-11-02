package io.github.polestarcn.companion;
import androidx.appcompat.widget.SwitchCompat;

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
  private final Runnable listener = this::onModelChanged;private Runnable statusDockBinding;
  private LinearLayout shell, body;private FrameLayout mapHost;private ParkingBackdrop backdrop;private LocationSnapshot backdropPoint;
  private HorizontalSwipeRefreshLayout controlRefresh;
  private int page = 0, ink, muted, paper, surface, line;
  private final int orange = Color.rgb(255, 107, 0);
  private Typeface regular, bold;
  private SettingsSearch settingsSearch;private boolean indexSettings;private String settingsGroup="";
  private AlertDialog parkingDialog,carDialog;
  private String parkingVin;
  private LocationSnapshot parkingPoint;
  private SwitchCompat mapLocationSwitch;
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
  private boolean transitionRequested;private int transitionDirection=1;
  private final Handler displayHandler = new Handler(Looper.getMainLooper());
  private android.net.ConnectivityManager campConnectivity;
  private android.net.ConnectivityManager.NetworkCallback campNetworkCallback;
  private long lastCampRecoveryElapsed;
  private void recoverCampingOnNetwork(){
    if(!FirstUseConsent.accepted(this)||model==null||!CampModeManager.needsRecovery(this))return;
    if(!model.hasSession())return;
    long now=android.os.SystemClock.elapsedRealtime();
    if(now-lastCampRecoveryElapsed<60_000L&&lastCampRecoveryElapsed>0)return;
    // Refresh must complete successfully before any remote-recovery check.
    // Network validation only proves internet is available, not a valid vehicle session.
    if(model.busy)return;
    lastCampRecoveryElapsed=now;
    final CampSession previous=CampModeManager.session(this);
    if(previous==null)return;
    CampModeManager.report(this,previous.id,"RECOVERING_REFRESH");
    model.refresh(()->{
      if(isFinishing()||!model.hasSession()||!CampModeManager.needsRecovery(this))return;
      CampSession still=CampModeManager.session(this);
      if(still==null||!previous.id.equals(still.id))return;
      boolean safe=CampRecoveryGate.permits(previous,model.selected,model.snapshot,
          model.hasSession(),model.lastVehicleReadOk,System.currentTimeMillis());
      if(!safe){
        if(!model.lastVehicleReadOk){
          String fail=model.message==null?"":model.message;
          String classification=CampRecovery.classify(new Exception(fail));
          CampModeManager.report(this,previous.id,classification);
        }else{
          CampModeManager.report(this,previous.id,"LOGIN_REQUIRED");
        }
        DiagnosticLog.add("Camping reconnect: live same-VIN state not verified; no remote commands");
        return;
      }
      CampModeManager.report(this,previous.id,"RECOVERING_VERIFY");
      CampModeManager.retryNetworkRecovery(this);
      model.checkCamp(new java.util.concurrent.atomic.AtomicBoolean(false),()->{});
    });
  }
  private void monitorCampingNetwork(){
    if(!CampModeManager.needsRecovery(this)||campNetworkCallback!=null)return;
    try{
      campConnectivity=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
      if(campConnectivity==null)return;
      campNetworkCallback=new android.net.ConnectivityManager.NetworkCallback(){
        @Override public void onCapabilitiesChanged(android.net.Network network,android.net.NetworkCapabilities caps){
          if(caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
              &&caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            runOnUiThread(()->recoverCampingOnNetwork());
        }
      };
      campConnectivity.registerDefaultNetworkCallback(campNetworkCallback);
      android.net.Network active=campConnectivity.getActiveNetwork();
      android.net.NetworkCapabilities caps=active==null?null:campConnectivity.getNetworkCapabilities(active);
      if(caps!=null&&caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED))recoverCampingOnNetwork();
    }catch(RuntimeException unavailable){DiagnosticLog.add("Camping network monitor unavailable; JobScheduler remains active");}
  }
  private void stopCampingNetworkMonitor(){
    if(campConnectivity!=null&&campNetworkCallback!=null)
      try{campConnectivity.unregisterNetworkCallback(campNetworkCallback);}catch(RuntimeException ignored){}
    campNetworkCallback=null;campConnectivity=null;
  }
  private final Runnable displayTick = new Runnable() {
    public void run() {
      if(statusDockBinding!=null)statusDockBinding.run();
      if (model != null && page == 0 && model.snapshot != null
          ) onModelChanged();
      displayHandler.postDelayed(this, 60_000L);
    }
  };
  private static boolean openingRefreshClaimed,openingRefreshFinished;private static long openingRefreshDue;
  private final Runnable openingRefresh=()->{openingRefreshFinished=true;if(getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)&&model.hasSession()&&!model.busy&&!model.commandPending)model.refreshQuiet();};
  private final Runnable foregroundRefresh=new Runnable(){public void run(){
    if(FirstUseConsent.accepted(MainActivity.this)&&model.hasSession()&&!model.busy&&!model.commandPending )model.refreshQuiet();
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

  private boolean minimalMode(){return UiDisplayMode.minimal(this);}
  private void toggleMinimalMode(){
    boolean on=UiDisplayMode.toggleMinimal(this);
    toast(tr(on?"极简模式已开启":"极简模式已关闭",on?"Minimal mode on":"Minimal mode off"));
    render();
  }

  private int dp(float f) {
    return (int) (f * getResources().getDisplayMetrics().density + .5f);
  }

  @Override protected void attachBaseContext(Context base) { super.attachBaseContext(UiTypography.wrap(base)); }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);AppIcons.sync(this);initialCreation=b==null;if(b!=null){statusReturnPage=Math.max(0,Math.min(2,b.getInt("statusReturn",0)));statusScrollY=b.getInt("statusY",0);settingsScrollY=b.getInt("settingsY",0);dataScrollY=b.getInt("dataY",0);}
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
    if(AppPreferences.visible(this,"refresh_on_open",true)){if(!openingRefreshClaimed){openingRefreshClaimed=true;openingRefreshDue=android.os.SystemClock.elapsedRealtime()+3000L;}if(!openingRefreshFinished)displayHandler.postDelayed(openingRefresh,Math.max(0,openingRefreshDue-android.os.SystemClock.elapsedRealtime()));}
    if (AppPreferences.enabled(this))
      try {
        ResidentService.apply(this);
      } catch (RuntimeException e) {
        DiagnosticLog.add("Resident start restricted");
      }
    ResidentService.scheduleUpdates(this);
    scheduleOpeningLocation();
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
    if (!model.hasSession() || model.selected == null) {
      toast(tr("请先连接车辆，刷新后重试", "Connect and refresh the vehicle before controlling"));
      if (model.hasSession() && model.selected == null && !model.busy) model.refresh(null);
      return;
    }
    handlingWidget = true;
    Haptics.tap(this);
    if(fixed){if(!repeatPending(request.substring(7)))authenticate(tr("Widget 车辆控制","Widget vehicle control"),request.substring(7));}
    else directControl("lock-choice".equals(request) ? "lock" : "climate");
    handlingWidget = false;
  }

  @Override
  protected void onDestroy() {
    lockHoldHandler.removeCallbacksAndMessages(null);displayHandler.removeCallbacks(locationProbe3);displayHandler.removeCallbacks(locationProbe5);
    displayHandler.removeCallbacks(openingRefresh);displayHandler.removeCallbacks(displayTick);displayHandler.removeCallbacks(foregroundRefresh);
    displayHandler.removeCallbacks(mapRefreshTick);dismissParking();if(carDialog!=null)carDialog.dismiss();if(backdrop!=null)backdrop.release();
    model.remove(listener);
    authGeneration++; authPending = false;
    if (biometric != null) biometric.cancelAuthentication();
    stopCampingNetworkMonitor();super.onDestroy();
  }

  @Override protected void onStart() {
    super.onStart();
    if (model != null) { model.listen(listener); if (resumeRender) { resumeRender = false; render(); } }
  }
  @Override protected void onResume() {
    super.onResume();mapRefreshHostActive=true;scheduleMapRefresh();if(backdrop!=null)backdrop.resume();monitorCampingNetwork();AllGoodTextView.host(findViewById(android.R.id.content),true);if(model!=null&&dashboardReady&&page==0)onModelChanged();scheduleForeground();displayHandler.removeCallbacks(displayTick); displayHandler.postDelayed(displayTick, 60_000L);
  }
  @Override protected void onPause() {
    lockHoldHandler.removeCallbacksAndMessages(null);mapRefreshHostActive=false;displayHandler.removeCallbacks(mapRefreshTick);if(backdrop!=null)backdrop.pause();stopCampingNetworkMonitor();AllGoodTextView.host(findViewById(android.R.id.content),false);displayHandler.removeCallbacks(displayTick);displayHandler.removeCallbacks(foregroundRefresh);View pulse=findViewById(android.R.id.content).findViewWithTag("charging_pulse");if(pulse instanceof ChargingPulseView)((ChargingPulseView)pulse).setActive(false); super.onPause();
  }

  @Override
  protected void onStop() {
    displayHandler.removeCallbacks(mapRefreshTick);dismissParking();if(carDialog!=null)carDialog.dismiss();
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
    if (!AppPreferences.dark(this)||UiSkin.PS_DEFAULT.equals(UiSkin.get(this))) {
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

  private TextView goodText(String value,int sp,int color,boolean heavy){AllGoodTextView view=new AllGoodTextView(this);view.setText(value);view.setTextSize(sp);view.setTextColor(color);view.setTypeface(heavy?bold:regular);view.setIncludeFontPadding(false);return view;}
  private Typeface buttonTypeface(){return AppPreferences.visible(this,"ui_button_bold",true)?bold:regular;}
  private void fitSingleLine(TextView view,int sp){view.setMaxLines(1);view.setEllipsize(android.text.TextUtils.TruncateAt.END);view.setAutoSizeTextTypeUniformWithConfiguration(6,sp,1,android.util.TypedValue.COMPLEX_UNIT_SP);}
  private int feedbackSp(boolean compact){return Math.min(ButtonFeedback.sp(this),voLike()?8:compact?10:11);}
  private TextView feedbackText(String control,boolean compact){
    TextView view=text("",feedbackSp(compact),muted,false);view.setTag("control_feedback_"+control);view.setGravity(Gravity.CENTER);view.setTypeface(ButtonFeedback.typeface(this));fitSingleLine(view,feedbackSp(compact));
    bindDashboard(()->{String value=ButtonFeedback.value(this,control,model.snapshot,controlFeedback(control,compact));view.setText(value);view.setVisibility(AppPreferences.visible(this,"show_button_feedback",true)&&!value.isEmpty()?View.VISIBLE:View.GONE);});return view;
  }
  private void gap(LinearLayout p, int size) {
    p.addView(new View(this), new LinearLayout.LayoutParams(1, dp(size)));
  }

  private TextView button(String s, Runnable r) {
    TextView t = text(s, 14, ink, true);
    t.setTag(R.id.psa_button_label,Boolean.TRUE);t.setTypeface(buttonTypeface());fitSingleLine(t,14);t.setGravity(Gravity.CENTER);
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
    if(view instanceof VehicleHeroView)((VehicleHeroView)view).setVehicleHold(this::toggleMinimalMode);
    android.animation.StateListAnimator states=new android.animation.StateListAnimator();
    android.animation.ObjectAnimator press=android.animation.ObjectAnimator.ofFloat(view,"alpha",.86f);press.setDuration(70);states.addState(new int[]{android.R.attr.state_pressed,android.R.attr.state_enabled},press);
    android.animation.ObjectAnimator release=android.animation.ObjectAnimator.ofFloat(view,"alpha",1f);release.setDuration(150);states.addState(new int[]{android.R.attr.state_enabled},release);view.setStateListAnimator(states);
    view.setOnClickListener(v -> { Haptics.tap(this); action.run(); });
  }

  private TextView utility(String title, boolean active, boolean warning, Runnable action) {
    UtilityButton view=new UtilityButton(this);view.setTag(R.id.psa_button_label,Boolean.TRUE);view.setText(getString(R.string.ui_text_parts_2, String.valueOf(title), String.valueOf((warning?" !":""))));view.setTextColor(ink);view.setTypeface(buttonTypeface());view.setGravity(Gravity.CENTER);view.setMinHeight(dp(48));click(view,action);view.setFocusable(true);
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
      brand.setImageResource(AppIcons.small(this));brand.setColorFilter(ink);
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
    shell.addView(connectionDock(),new LinearLayout.LayoutParams(-1,dp(22)));
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
                if(page!=tab){transitionDirection=tab>page?1:-1;transitionRequested=true;page=tab;render();}
              });
      t.setTag("bottom_nav_button");t.setPadding(dp(3),dp(1),dp(3),dp(1));t.setMinHeight(0);
      GradientDrawable navFrame = bg(i == (page==3?1:page) ? surface : paper, 12);
      navFrame.setStroke(dp(1), i == (page==3?1:page) ? line : Color.TRANSPARENT);
      t.setBackground(navFrame);
      t.setTextColor(i == (page==3?1:page) ? orange : muted);
      {
        int resource = i == 0 ? AppIcons.small(this) : i==1?R.drawable.ic_nav_vehicle_data_ref:R.drawable.ic_settings;
        android.graphics.drawable.Drawable navIcon = androidx.core.content.ContextCompat.getDrawable(this,resource).mutate();
        navIcon.setTint(i == (page==3?1:page) ? orange : muted);
        navIcon.setBounds(0, 0, dp(compactNav ? 23 : 25), dp(compactNav ? 23 : 25));
        t.setCompoundDrawables(null, navIcon, null, null);
        t.setCompoundDrawablePadding(dp(2));
      }
      LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, -1, 1);
      nlp.leftMargin = dp(3); nlp.rightMargin = dp(3);
      nav.addView(t, nlp);
    }
    shell.addView(nav, new LinearLayout.LayoutParams(-1, dp(compactNav ? 52 : 64)));
    applyButtonHierarchy(shell);disableSystemHaptics(shell);
    if(AppPreferences.visible(this,"map_backdrop",false)&&model.parkedLocation!=null){
      if(mapHost==null){mapHost=new FrameLayout(this);setContentView(mapHost);}
      for(int i=mapHost.getChildCount()-1;i>=0;i--)if(mapHost.getChildAt(i)!=backdrop)mapHost.removeViewAt(i);
      shell.setBackgroundColor(Color.TRANSPARENT);softenMapCards(shell);applyButtonOpacity(shell);mapHost.addView(shell,new FrameLayout.LayoutParams(-1,-1));updateBackdrop();
    }else{if(backdrop!=null)backdrop.release();backdrop=null;mapHost=null;backdropPoint=null;if(AppPreferences.visible(this,"background_image_enabled",false)&&BackgroundImages.file(this).isFile())softenMapCards(shell);applyButtonOpacity(shell);setContentView(BackgroundImages.wrap(this,shell));}

    AppWindow.apply(this,shell);
    if(transitionRequested)PageTransition.slide(body,transitionDirection);transitionRequested=false;
  }

  private void applyButtonHierarchy(View v){ConditionStyle.symbol(v);if(v instanceof TextView)ConditionStyle.apply((TextView)v);String tag=v.getTag() instanceof String?(String)v.getTag():"";boolean role=Boolean.TRUE.equals(v.getTag(R.id.psa_button_label));boolean tile=tag.equals("control_tile")||tag.equals("nio_shortcut")||tag.equals("shortcut_card");
    if(v instanceof TextView&&(role||v instanceof SwitchCompat||tag.startsWith("control_title_")||tag.equals("volike_control_label")||tag.equals("shortcut_label")))((TextView)v).setTextColor(AppPreferences.dark(this)?ink:Color.BLACK);
    if(v instanceof TextView&&(tag.startsWith("control_feedback_")||tag.equals("shortcut_feedback")))((TextView)v).setTextColor(AppPreferences.dark(this)?muted:Color.rgb(115,115,115));
    if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)applyButtonHierarchy(((ViewGroup)v).getChildAt(i));
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
    sign.setEnabled(!(model.busy&&!model.quietRefresh));
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

  private void softenMapCards(View v){if(v!=shell&&v.getBackground()!=null)v.getBackground().setAlpha(222);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)softenMapCards(((ViewGroup)v).getChildAt(i));}
  private String backdropStyleKey="";
  private void updateBackdrop(){if(mapHost==null)return;LocationSnapshot point=model.parkedLocation;if(point==null){if(backdrop!=null){backdrop.release();mapHost.removeView(backdrop);backdrop=null;}backdropPoint=null;return;}
    String key=MapAppearance.dark(this)+":"+AppPreferences.get(this,"map_backdrop_blur","12")+":"+AppPreferences.get(this,"map_backdrop_spread","60")+":"+AppPreferences.get(this,"map_backdrop_zoom","16")+":"+new MapMarkerSettings(this).signature();
    boolean same=backdropPoint!=null&&Math.abs(point.latitude-backdropPoint.latitude)<.00001&&Math.abs(point.longitude-backdropPoint.longitude)<.00001;
    if(backdrop!=null&&same){if(!key.equals(backdropStyleKey)){backdrop.restyle();backdropStyleKey=key;}return;}
    if(backdrop!=null){backdrop.release();mapHost.removeView(backdrop);}backdropPoint=point;backdropStyleKey=key;backdrop=new ParkingBackdrop(this,point);mapHost.addView(backdrop,0,new FrameLayout.LayoutParams(-1,-1));
  }
  private boolean mapRefreshHostActive;
  private final Runnable mapRefreshTick=()->{if(VehiclePowerPolicy.automaticReads(this)&&model.hasSession()&&model.selected!=null&&AppPreferences.visible(this,"map_backdrop",false))model.readLastParkedLocation(false,(point,error)->scheduleMapRefresh());else scheduleMapRefresh();};
  private void scheduleMapRefresh(){displayHandler.removeCallbacks(mapRefreshTick);long delay=model==null?-1:model.parkingRefreshDelay();if(mapRefreshHostActive&&delay>=0&&AppPreferences.visible(this,"map_backdrop",false))displayHandler.postDelayed(mapRefreshTick,Math.max(1000,delay));}

  private void backgroundChoice(){new HapticDialogBuilder(this).setTitle(tr("界面背景","UI background")).setItems(new String[]{tr("纯色","Solid"),tr("高德停车地图","Amap parking map"),tr("选择本机图片并裁切","Choose and crop local image")},(d,w)->{if(w!=2){AppPreferences.set(this,"background_image_enabled","off");AppPreferences.set(this,"map_backdrop",w==1?"on":"off");}if(w==2){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,801);}else render();}).show();}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==811||request==812){if(result==RESULT_OK&&data!=null&&data.getData()!=null)finishSettingsFile(request,data.getData());return;}if(request==801&&result==RESULT_OK&&data!=null&&data.getData()!=null)startActivityForResult(new Intent(this,BackgroundCropActivity.class).setData(data.getData()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),802);else if(request==802&&result==RESULT_OK)render();}
  private void allGoodDialog(){LinearLayout p=detailPanel();setting(p,tr("显示位置 · 下方 / 车辆后方","Placement · below / behind"),()->choice(tr("All Good显示位置","All Good placement"),"all_good_position",new String[]{"below","behind"},new String[]{tr("车辆下方（默认）","Below vehicle (default)"),tr("车辆后方","Behind vehicle")},"below",this::render));setting(p,tr("后方排版 · 单行 / 两行 / C方案","Backdrop · one / two lines / layout C"),()->choice(tr("All Good后方排版","All Good backdrop layout"),"all_good_lines",new String[]{"one","two","offset_c"},new String[]{tr("单行","One line"),tr("两行","Two lines"),tr("C方案","Layout C")},"one",()->{if("offset_c".equals(AppPreferences.get(this,"all_good_lines","one")))AppPreferences.set(this,"all_good_position","behind");render();}));
    p.addView(text(tr("正常时的文字（最多80字，可换行）","Healthy text (up to 80 characters, multiline)"),14,ink,false));EditText good=new EditText(this);good.setTextColor(ink);good.setText(ConditionStyle.message(this,false));good.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});p.addView(good);
    p.addView(text(tr("有问题时的文字","Problem text"),14,ink,false));EditText issue=new EditText(this);issue.setTextColor(ink);issue.setText(ConditionStyle.message(this,true));issue.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});p.addView(issue);
    p.addView(text(tr("文字颜色 #RRGGBB（默认VI橙色）","Text color #RRGGBB (default VI orange)"),14,ink,false));EditText color=new EditText(this);color.setSingleLine(true);color.setTextColor(ink);color.setText(AppPreferences.get(this,"condition_color","#FF6B00"));p.addView(color);
    String[] keys={"all_good_weight","condition_line_spacing","condition_focus_x","condition_focus_y","condition_stretch","condition_size","condition_check_size"};String[] labels={tr("文字粗细","Weight"),tr("行间距 %","Line spacing %"),tr("横向焦点 %","Horizontal focus %"),tr("纵向焦点 %","Vertical focus %"),tr("横向拉伸 %","Horizontal stretch %"),tr("文字大小 sp（0跟随皮肤）","Text size sp (0 follows skin)"),tr("打钩大小 %","Checkmark size %")};int[] lows={300,50,0,0,50,0,50},highs={900,200,100,100,200,100,250},defaults={500,100,50,50,100,0,100};SeekBar[] sliders=new SeekBar[keys.length];for(int i=0;i<keys.length;i++)sliders[i]=campSlider(p,labels[i],lows[i],highs[i],EffectSettings.number(this,keys[i],defaults[i],lows[i],highs[i]),null,null);
    note(p,tr("说明","Info"),tr("仅改变显示，正常/异常仍按车辆数据判断；焦点在可用范围内移动，文字会按控件边界缩放。","Presentation only. Vehicle data still decides healthy/problem. Focus stays in available space; text fits control bounds."));new HapticDialogBuilder(this).setTitle(tr("车况文字与All Good","Condition text and All Good")).setView(p).setPositiveButton(tr("保存","Save"),(d,w)->{String hex=color.getText().toString().trim();if(!WidgetColors.validHex(hex)){toast(tr("请输入#RRGGBB颜色","Enter #RRGGBB"));return;}AppPreferences.set(this,"condition_good_text",good.getText().toString());AppPreferences.set(this,"condition_problem_text",issue.getText().toString());AppPreferences.set(this,"condition_color",hex);for(int i=0;i<keys.length;i++)AppPreferences.set(this,keys[i],String.valueOf(lows[i]+sliders[i].getProgress()));WidgetRenderer.refreshAll(this);render();}).setNeutralButton(tr("恢复本项默认","Reset this section"),(d,w)->{android.content.SharedPreferences prefs=getSharedPreferences("preferences",0);android.content.SharedPreferences.Editor e=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith("condition_")&&!key.equals("condition_display"))e.remove(key);e.remove("all_good_weight");e.apply();render();}).setNegativeButton(tr("取消","Cancel"),null).show();}
  private String settingsAliases(String title){if(title.contains("备份")||title.contains("Back up"))return "备份 backup export 文件 file";if(title.contains("恢复")||title.contains("Restore")||title.contains("Reset"))return "恢复 默认 导入 restore reset import defaults";if(title.contains("车况按钮")||title.contains("Condition button"))return "保养 百分比 进度条 service percentage progress";if(title.contains("All Good"))return "车况 正常 异常 行间距 焦点 拉伸 文字 颜色 粗细 字号 打钩 healthy problem spacing focus stretch color weight checkmark";if(title.contains(tr("动态效果","effects")))return "粒子 吸入 密度 分层 焦点 车窗 光效 充电 净化 通风 周期 强度 颜色 流光 充电周期 流光周期 车窗周期 吸入焦点 吸入范围 粒子密度 前后分层 particle absorption density depth window charge purification ventilation period intensity color";return "";}
  private void settingsFile(boolean restore){Intent i=new Intent(restore?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);if(!restore)i.putExtra(Intent.EXTRA_TITLE,"Psa-settings-"+BuildConfig.VERSION_NAME+".json");startActivityForResult(i,restore?812:811);}
  private void finishSettingsFile(int request,android.net.Uri uri){SettingsBackup.WORK.execute(()->{try{if(request==811){try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new java.io.IOException();out.write(SettingsBackup.export(this).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}runOnUiThread(()->toast(tr("设置已备份","Settings backed up")));}else{org.json.JSONObject values;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){values=SettingsBackup.read(in);}runOnUiThread(()->{if(isFinishing()||isDestroyed())return;new HapticDialogBuilder(this).setTitle(tr("恢复设置","Restore settings")).setMessage(tr("将导入 ","Import ")+values.length()+tr(" 项设置，账号与历史数据不变。"," settings. Account and history are preserved.")).setPositiveButton(tr("恢复","Restore"),(d,w)->{try{SettingsBackup.restore(this,values);applyRestoredSettings();}catch(Exception e){toast(tr("恢复失败","Restore failed"));}}).setNegativeButton(tr("取消","Cancel"),null).show();});}}catch(Exception e){runOnUiThread(()->toast(tr("文件操作失败，请检查设置备份文件","File operation failed; check the settings backup")));}});}
  private void resetSettings(){new HapticDialogBuilder(this).setTitle(tr("全部恢复默认设置","Reset all settings")).setMessage(tr("重置显示与功能偏好，保留账号、车辆与能耗记录；不改变车端运行状态。","Reset display and feature preferences; keep account, vehicle and energy history. Vehicle operation is unchanged.")).setPositiveButton(tr("恢复默认","Reset"),(d,w)->{SettingsBackup.reset(this);applyRestoredSettings();}).setNegativeButton(tr("取消","Cancel"),null).show();}
  private void applyRestoredSettings(){settingsScrollY=0;AppIcons.sync(this);WidgetRenderer.refreshAll(this);ResidentService.apply(this);scheduleForeground();scheduleMapRefresh();recreate();}
  private void mapThemeChoice(){String[] modes={"system","light","dark"};int selected=Arrays.asList(modes).indexOf(MapAppearance.mode(this));new HapticDialogBuilder(this).setTitle(tr("地图显示模式","Map appearance")).setSingleChoiceItems(new String[]{tr("跟随系统","Follow system"),tr("白天地图","Day map"),tr("黑夜地图","Night map")},selected,(d,w)->{AppPreferences.set(this,"map_theme",modes[w]);d.dismiss();render();}).setNegativeButton(tr("取消","Cancel"),null).show();}
  private void mapBackdropDialog(){LinearLayout p=detailPanel();setting(p,tr("地图模式 · 白天 / 黑夜 / 跟随系统","Map theme · day / night / system"),this::mapThemeChoice);setting(p,tr("背景来源 · 纯色 / 高德 / 本机图片","Background · solid / Amap / local image"),this::backgroundChoice);SeekBar zoom=campSlider(p,tr("高德地图缩放","Amap zoom"),3,20,EffectSettings.number(this,"map_backdrop_zoom",16,3,20),null,null);settingToggle(p,"map_backdrop",tr("试验性高德地图背景","Experimental Amap backdrop"),false,false);SeekBar blur=campSlider(p,tr("模糊强度","Blur strength"),0,80,EffectSettings.number(this,"map_backdrop_blur",12,0,80),null,null);SeekBar spread=campSlider(p,tr("渐变模糊外扩比例 %","Blur spread %"),0,300,EffectSettings.number(this,"map_backdrop_spread",60,0,300),null,null);MapMarkerSettings pin=new MapMarkerSettings(this);SeekBar cropX=campSlider(p,tr("地图裁切焦点 · 横向 %","Map crop focus · horizontal %"),0,100,pin.cropX,null,null);SeekBar cropY=campSlider(p,tr("地图裁切焦点 · 纵向 %","Map crop focus · vertical %"),0,100,pin.cropY,null,null);SeekBar size=campSlider(p,tr("定位标记大小 %","Location marker size %"),25,300,pin.size,null,null);p.addView(text(tr("定位标记颜色 #RRGGBB（默认VI橙色）","Marker color #RRGGBB (default VI orange)"),14,ink,false));EditText color=new EditText(this);color.setSingleLine(true);color.setTextColor(ink);color.setText(pin.color);p.addView(color);SeekBar x=campSlider(p,tr("定位焦点 · 横向 %","Location focus · horizontal %"),0,100,pin.focusX,null,null);SeekBar y=campSlider(p,tr("定位焦点 · 纵向 %","Location focus · vertical %"),0,100,pin.focusY,null,null);note(p,tr("说明","Info"),tr("裁切焦点默认50%/50%，用于移动地图取景。定位标记随道路一起移动；边缘会限制取景，避免露底。不修改车辆经纬度，不调用手机定位；调整时复用地图。","Crop focus defaults to 50%/50% and moves the map framing. Marker and roads move together; edge limits prevent blank margins. Vehicle coordinates and phone location are unchanged. Updates reuse the map."));AlertDialog dialog=new HapticDialogBuilder(this).setTitle(tr("地图背景与定位标记","Map backdrop and location marker")).setView(p).setPositiveButton(tr("应用","Apply"),null).setNeutralButton(tr("恢复取景与标记","Reset crop and marker"),(d,w)->{android.content.SharedPreferences.Editor e=getSharedPreferences("preferences",0).edit();e.remove("map_crop_focus_x").remove("map_crop_focus_y").remove("map_pin_size_percent").remove("map_pin_color").remove("map_pin_focus_x").remove("map_pin_focus_y").apply();render();}).setNegativeButton(tr("取消","Cancel"),null).create();dialog.show();dialog.getButton(-1).setOnClickListener(v->{String hex=color.getText().toString().trim();if(!WidgetColors.validHex(hex)){color.setError("#RRGGBB");return;}android.content.SharedPreferences.Editor e=getSharedPreferences("preferences",0).edit();e.putString("map_crop_focus_x",String.valueOf(cropX.getProgress())).putString("map_crop_focus_y",String.valueOf(cropY.getProgress())).putString("map_backdrop_zoom",String.valueOf(zoom.getProgress()+3)).putString("map_backdrop_blur",String.valueOf(blur.getProgress())).putString("map_backdrop_spread",String.valueOf(spread.getProgress())).putString("map_pin_size_percent",String.valueOf(size.getProgress()+25)).putString("map_pin_color",hex).putString("map_pin_focus_x",String.valueOf(x.getProgress())).putString("map_pin_focus_y",String.valueOf(y.getProgress())).apply();dialog.dismiss();render();});}

  private List<String> skinOrder(){List<String> values=new ArrayList<>();List<String> valid=Arrays.asList(UiSkin.pickerChoices());for(String key:AppPreferences.get(this,"skin_order","").split(","))if(valid.contains(key)&&!values.contains(key))values.add(key);for(String key:valid)if(!values.contains(key))values.add(key);return values;}
  private void vehicleAppearanceMenu(){new HapticDialogBuilder(this).setTitle(tr("车辆外观快捷设置","Vehicle appearance"))
    .setItems(new String[]{tr("切换皮肤","Change skin"),tr("当前主题车辆图片","Vehicle image for current theme"),tr("白天车辆图片","Day vehicle image"),tr("夜间车辆图片","Night vehicle image")},(d,w)->{if(w==0)skinDialog();else vehicleArtDialog(w==1?AppPreferences.dark(this):w==3);}).show();}
  private String openingLocationVin;private final long locationStart=SystemClock.elapsedRealtime();private boolean locationProbesScheduled;
  private final Runnable locationProbe3=()->probeOpeningLocation();
  private final Runnable locationProbe5=()->probeOpeningLocation();
  private void scheduleOpeningLocation(){if(!VehiclePowerPolicy.automaticReads(this)||!FirstUseConsent.accepted(this)||!model.hasSession()||model.selected==null)return;String vin=model.selected.vin;if(Objects.equals(vin,openingLocationVin)&&locationProbesScheduled)return;openingLocationVin=vin;locationProbesScheduled=true;displayHandler.removeCallbacks(locationProbe3);displayHandler.removeCallbacks(locationProbe5);displayHandler.postDelayed(locationProbe3,Math.max(0,3000-(SystemClock.elapsedRealtime()-locationStart)));displayHandler.postDelayed(locationProbe5,Math.max(0,5000-(SystemClock.elapsedRealtime()-locationStart)));}
  private void probeOpeningLocation(){if(!VehiclePowerPolicy.automaticReads(this)||!getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)||!model.hasSession()||model.selected==null||!Objects.equals(openingLocationVin,model.selected.vin)||!model.parkingName.isEmpty())return;model.readLastParkedLocation(true,(point,error)->{if(point!=null)model.resolveParkingName(point,()->{if(!model.parkingName.isEmpty()){displayHandler.removeCallbacks(locationProbe3);displayHandler.removeCallbacks(locationProbe5);}});});}
  private LinearLayout chargeHealthSlot(LinearLayout health,int height){LinearLayout slot=column();slot.setTag("health_charge_slot");slot.addView(health,new LinearLayout.LayoutParams(-1,-1));FrameLayout charging=new FrameLayout(this);charging.setTag("charging_section");ChargingPulseView pulse=new ChargingPulseView(this);charging.addView(pulse,new FrameLayout.LayoutParams(-1,-1));TextView state=liveText(()->tr("充电中","Charging")+" · "+DisplayMetric.CHARGE_POWER.value(this,model.snapshot,true,AppPreferences.english(this))+" · "+DisplayMetric.CHARGE_ETA.value(this,model.snapshot,true,AppPreferences.english(this)),height<=42?12:15,ink,true);state.setGravity(Gravity.CENTER);charging.addView(state,new FrameLayout.LayoutParams(-1,-1));slot.addView(charging,new LinearLayout.LayoutParams(-1,-1));click(charging,this::chargingDialog);bindDashboard(()->{boolean active=ChargingVisibility.shouldShow(model.snapshot);health.setVisibility(active?View.GONE:"behind".equals(AppPreferences.get(this,"all_good_position","below"))&&VehicleCondition.reported(model.snapshot,showTires())&&!VehicleCondition.problem(model.snapshot,showTires())?View.INVISIBLE:View.VISIBLE);charging.setVisibility(active?View.VISIBLE:View.GONE);pulse.setActive(active);slot.setVisibility(!active&&"behind".equals(AppPreferences.get(this,"all_good_position","below"))&&"offset_c".equals(AppPreferences.get(this,"all_good_lines","one"))&&VehicleCondition.reported(model.snapshot,showTires())&&!VehicleCondition.problem(model.snapshot,showTires())?View.GONE:View.VISIBLE);});return slot;}
  private LinearLayout simple2Card(){LinearLayout card=column();card.setPadding(dp(12),dp(8),dp(12),dp(8));GradientDrawable frame=bg(surface,20);frame.setStroke(dp(1),line);card.setBackground(frame);return card;}
  private void simple2Action(LinearLayout parent,String control,String label){
    LinearLayout tile=row();tile.setPadding(dp(8),dp(6),dp(8),dp(6));GradientDrawable frame=bg(surface,16);frame.setStroke(dp(1),line);tile.setBackground(frame);
    ImageView icon=new ImageView(this);icon.setTag("control_icon_"+control);icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setPadding(dp(8),dp(8),dp(8),dp(8));icon.setBackground(bg(viSoft(),12));tile.addView(icon,new LinearLayout.LayoutParams(dp(38),dp(38)));
    LinearLayout words=column();words.setPadding(dp(9),0,0,0);TextView title=text(label,13,ink,true);fitSingleLine(title,13);words.addView(title);TextView state=feedbackText(control,false);state.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);words.addView(state);tile.addView(words,new LinearLayout.LayoutParams(0,-2,1));
    bindDashboard(()->{String raw=controlRaw(control);icon.setImageResource(controlIcon(control,raw));boolean active="开启".equals(raw)||"未锁".equals(raw)||"已打开".equals(raw)||"未关严".equals(raw);icon.setColorFilter(active?orange:ink);tile.setEnabled(model.canControl(control));tile.setAlpha(tile.isEnabled()?1f:.5f);});
    click(tile,()->{if("signal".equals(control))signalMenu();else directControl(control);});installLockHold(tile,control);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(parent.getChildCount()>0)lp.leftMargin=dp(7);parent.addView(tile,lp);
  }
  private void buildSimple2(){
    if(getResources().getConfiguration().screenWidthDp>getResources().getConfiguration().screenHeightDp||getResources().getConfiguration().screenHeightDp<680){buildDashboard(true);return;}
    boolean compact=getResources().getConfiguration().screenHeightDp<720||getResources().getConfiguration().fontScale>1.2f;body.setPadding(0,0,0,0);body.setTag("simple2_dashboard");
    LinearLayout head=row();LinearLayout identity=column();identity.addView(liveText(()->vehicleDisplayName(model.snapshot),20,ink,true));identity.addView(parkingCaption());head.addView(identity,new LinearLayout.LayoutParams(0,-1,1));
    for(int icon:new int[]{R.drawable.ic_location,R.drawable.ic_refresh}){ImageView button=new ImageView(this);button.setImageResource(icon);button.setColorFilter(ink);button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);button.setPadding(dp(10),dp(10),dp(10),dp(10));GradientDrawable frame=bg(surface,24);frame.setStroke(dp(1),line);button.setBackground(frame);click(button,()->{if(icon==R.drawable.ic_location)locationDialog();else model.refreshUser(null);});head.addView(button,new LinearLayout.LayoutParams(dp(44),dp(44)));}body.addView(head,new LinearLayout.LayoutParams(-1,dp(52)));
    LinearLayout hero=simple2Card();LinearLayout energy=row();LinearLayout battery=column();TextView soc=liveText(()->model.snapshot==null?"--%":VehicleSnapshot.metric(model.snapshot.batteryPercent,"%"),compact?32:44,ink,true);soc.setTag("battery_soc");battery.addView(soc);battery.addView(text(tr("当前电量","Battery"),11,muted,false));energy.addView(battery,new LinearLayout.LayoutParams(0,-2,1));LinearLayout range=column();range.setGravity(Gravity.END);TextView km=liveText(()->model.snapshot==null?"-- km":VehicleSnapshot.metric(model.snapshot.rangeKm," km"),25,ink,true);km.setTag("battery_range");range.addView(km);range.addView(text(tr("预计续航","Estimated range"),11,muted,false));energy.addView(range,new LinearLayout.LayoutParams(0,-2,1));hero.addView(energy);
    ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);styleViProgress(bar);batteryBarVisibility(bar);bindDashboard(()->bar.setProgress(model.snapshot==null||model.snapshot.batteryPercent==null?0:model.snapshot.batteryPercent));hero.addView(bar,new LinearLayout.LayoutParams(-1,dp(5)));hero.addView(vehicleVisual(),new LinearLayout.LayoutParams(-1,0,1));body.addView(hero,new LinearLayout.LayoutParams(-1,0,1));gap(body,6);
    for(String[] controls:new String[][]{{"lock","windows"},{"trunk","signal"},{"climate","air"}}){LinearLayout line=row();for(String key:controls){String label=key.equals("lock")?tr("车门锁","Door lock"):key.equals("windows")?tr("车窗","Windows"):key.equals("trunk")?tr("后备箱","Trunk"):key.equals("signal")?tr("寻车","Find car"):key.equals("climate")?tr("驻车空调","Climate"):tr("空气净化","Air purification");simple2Action(line,key,label);}body.addView(line,new LinearLayout.LayoutParams(-1,dp(compact?56:64)));gap(body,6);}
    LinearLayout utilities=row();VehicleConditionButton condition=conditionButton(tr("车况","Status"));condition.setConditionIcon(R.drawable.ic_nav_vehicle_data_ref);utilities.addView(condition,new LinearLayout.LayoutParams(0,dp(58),1));TextView camp=utility(tr("露营模式","Camping"),false,false,this::campDialog);utilityIcon(camp,R.drawable.ic_camp);camp.setTag("camp_button");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(58),1);cp.leftMargin=dp(7);utilities.addView(camp,cp);bindDashboard(()->{boolean active=CampModeManager.active(this);camp.setTextColor(ink);tintUtilityIcon(camp);((UtilityButton)camp).feedback(ButtonFeedback.value(this,"camp",model.snapshot,CampModeManager.status(this)));condition.refreshService();});body.addView(utilities);gap(body,5);
    List<DisplayMetric> chosen=controlMetrics();chosen.removeIf(m->ChargingVisibility.isChargingMetric(m)||m==DisplayMetric.VEHICLE_STATUS||m==DisplayMetric.BATTERY||m==DisplayMetric.RANGE);if(chosen.size()>3)chosen=new ArrayList<>(chosen.subList(0,3));LinearLayout data=simple2Card();TextView all=button(tr("车辆数据 · 全部数据 ›","Vehicle data · all data ›"),()->{page=1;render();});all.setMinHeight(0);all.setTextSize(12);data.addView(all,new LinearLayout.LayoutParams(-1,dp(26)));addMetricGrid(data,model.snapshot,chosen,true);body.addView(data);dashboardReady=true;
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
    refresh.setRefreshing(model.busy && model.userRefreshing && model.activeAction.isEmpty());

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
      if (UiSkin.SIMPLE_2.equals(skin)) buildSimple2();
      else if (UiSkin.PS_DEFAULT.equals(skin)) buildDashboard(false);
      else if (UiSkin.nioLayout(this)) buildDashboard(true);
      else buildVoLikeDashboard(UiSkin.detailedView(this));
    } finally {
      body = outerBody;
    }
  }

  private void requestPullRefresh() {
    HorizontalSwipeRefreshLayout refresh = controlRefresh;
    if (model.busy) {
      if (refresh != null) refresh.setRefreshing(model.userRefreshing&&model.activeAction.isEmpty());
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
    // When signing in after an offline stop, ensure network recovery monitoring starts.
    if(!model.hasSession())lastCampRecoveryElapsed=0;
    if(model.hasSession()&&CampModeManager.needsRecovery(this)
        &&getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)){
      if(campNetworkCallback==null)monitorCampingNetwork();
      // A login or vehicle-selection callback can arrive after network validation.
      // The network callback may already be registered and never fire again.
      if(!model.busy&&model.lastVehicleReadOk&&model.selected!=null&&campConnectivity!=null){
        android.net.Network network=campConnectivity.getActiveNetwork();
        android.net.NetworkCapabilities capabilities=network==null?null:campConnectivity.getNetworkCapabilities(network);
        if(capabilities!=null&&capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED))
          recoverCampingOnNetwork();
      }
    }

    View contentRoot=findViewById(android.R.id.content);
    if(statusDockBinding!=null)statusDockBinding.run();
    if(model.progressOnly){TextView label=contentRoot.findViewWithTag("status_update");if(label!=null)label.setText(dashboardStamp());return;}
    if(page==2&&model.hasSession())return;
    if(parkingDialog!=null&&(!model.hasSession()||model.selected==null||!java.util.Objects.equals(parkingVin,model.selected.vin)))dismissParking();
    scheduleOpeningLocation();if(mapHost==null&&AppPreferences.visible(this,"map_backdrop",false)&&model.parkedLocation!=null){render();return;}updateBackdrop();
    if (dashboardReady && page == 0 && model.hasSession()) {
      for (Runnable binding : new java.util.ArrayList<>(dashboardBindings)) binding.run();
      applyButtonHierarchy(shell);applyButtonOpacity(shell);UiTypography.applyTree(shell);
      if (controlRefresh != null)
        controlRefresh.setRefreshing(model.busy && model.userRefreshing && model.activeAction.isEmpty());
    } else {View old=contentRoot.findViewWithTag("status_scroll");if(old instanceof ScrollView)statusScrollY=((ScrollView)old).getScrollY();render();}
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

  private LinearLayout connectionDock(){
    LinearLayout dock=row();dock.setTag("connection_command_dock");dock.setPadding(dp(18),0,dp(18),0);
    TextView connected=text("",10,ink,false);connected.setTag("vehicle_connection_state");connected.setGravity(Gravity.CENTER_VERTICAL);fitSingleLine(connected,10);dock.addView(connected,new LinearLayout.LayoutParams(-2,-1));
    TextView process=text("",9,muted,false);process.setTag("command_status_progress");process.setGravity(Gravity.CENTER_VERTICAL);fitSingleLine(process,9);dock.addView(process,new LinearLayout.LayoutParams(0,-1,1));
    TextView running=text("",9,muted,false);running.setTag("vehicle_running_state");running.setVisibility(View.GONE);dock.addView(running);
    statusDockBinding=()->{boolean live=model.vehicleDataConnected();String detail=commandDockText();connected.setText(getString(R.string.connection_with_separator,tr(live?"车辆已连接":"车辆未连接",live?"Vehicle connected":"Vehicle disconnected"),detail.isEmpty()?"":" · "));connected.setTextColor(live?ink:muted);process.setText(detail);};statusDockBinding.run();return dock;
  }

  private String commandDockText(){
    if(model.commandPending)return tr(model.operation==OperationState.SENDING?"发送中 ":"确认中 ",model.operation==OperationState.SENDING?"Sending ":"Verifying ")+model.readbackPercent+"%";
    if(model.quietRefresh||model.busy)return tr("正在刷新","Refreshing");
    switch(model.operation){
      case VEHICLE_CONFIRMED:return tr("执行成功","Successful");
      case SERVER_ACCEPTED:return tr("已发送 · 效果未确认","Sent · unverified");
      case UNCONFIRMED:return tr("指令已发送，请确认执行情况","Command sent. Please confirm execution.");
      case FAILED:return tr("执行失败","Failed");
      default:return model.snapshot!=null&&AppPreferences.visible(this,"show_update",true)?tr("更新 ","Updated ")+android.text.format.DateFormat.format("HH:mm",model.snapshot.timestampMillis):"";
    }
  }

  private String dashboardStamp() {
    VehicleSnapshot v = model.snapshot;
    String stamp = v == null || v.timestampMillis <= 0 ? tr("更新时间未知", "Update time unknown")
        : tr("更新 ", "Updated ") + android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis);
    if(model.quietRefresh)return tr("正在刷新 · ","Refreshing · ")+stamp;
    if(model.commandPending)return tr("执行中 ","In progress ")+model.readbackPercent+"%";
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

  private void applyHeroWeight(TextView view){
    int weight=500;try{weight=Integer.parseInt(AppPreferences.get(this,"hero_number_weight","700"));}catch(NumberFormatException ignored){}
    weight=Math.max(300,Math.min(900,weight));view.getPaint().setFakeBoldText(weight>=700);
    if(android.os.Build.VERSION.SDK_INT>=28)view.setTypeface(Typeface.create(regular,weight,false));
    else view.setTypeface(Typeface.create(weight<=300?"sans-serif-light":weight>=500?"sans-serif-medium":"sans-serif",weight>=700?Typeface.BOLD:Typeface.NORMAL));
  }
  private void visualControlsDialog(){LinearLayout p=detailPanel();
    int weight=700,opacity=100;try{weight=Integer.parseInt(AppPreferences.get(this,"hero_number_weight","700"));opacity=Integer.parseInt(AppPreferences.get(this,"button_background_percent","100"));}catch(NumberFormatException ignored){}
    SeekBar w=campSlider(p,tr("默认皮肤电量与续航粗细","Default skin battery / range weight"),300,900,Math.max(300,Math.min(900,weight)),null,null);
    SeekBar o=campSlider(p,tr("所有皮肤按钮背景不透明度 %","All skins button background opacity %"),0,100,Math.max(0,Math.min(100,opacity)),null,null);
    new HapticDialogBuilder(this).setTitle(tr("数字粗细与按钮背景","Number weight and button backgrounds")).setView(p).setPositiveButton(tr("保存","Save"),(d,x)->{AppPreferences.set(this,"hero_number_weight",String.valueOf(300+w.getProgress()));AppPreferences.set(this,"button_background_percent",String.valueOf(o.getProgress()));render();}).setNegativeButton(tr("取消","Cancel"),null).show();
  }
  private void applyButtonOpacity(View v){
    int value;try{value=Integer.parseInt(AppPreferences.get(this,"button_background_percent","100"));}catch(NumberFormatException e){value=100;}
    boolean control=v.isClickable()||v instanceof android.widget.CompoundButton;
    if(control&&v.getBackground()!=null&&!(v instanceof VehicleHeroView))v.getBackground().mutate().setAlpha(Math.max(0,Math.min(100,value))*255/100);
    if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)applyButtonOpacity(((ViewGroup)v).getChildAt(i));
  }
  private void batteryBarVisibility(ProgressBar bar){bar.setTag("vi_progress");bar.setVisibility(AppPreferences.visible(this,"show_battery_bar",true)?View.VISIBLE:View.GONE);}

  private void addBatterySummary(LinearLayout parent, boolean compact) {
    LinearLayout headline = row(); headline.setTag("battery_summary");
    LinearLayout soc = column();
    TextView percent = liveText(() -> model.snapshot == null ? "--%"
        : VehicleSnapshot.metric(model.snapshot.batteryPercent, "%"), compact ? 22 : 38, ink, true);
    percent.setTag("battery_soc");if(UiSkin.PS_DEFAULT.equals(UiSkin.get(this))){applyHeroWeight(percent);}soc.addView(percent);
    ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    batteryBarVisibility(bar); bar.setMax(100); styleViProgress(bar);
    if (nioColors()) bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(32,196,121)));
    else if (voLike()) bar.setProgressTintList(android.content.res.ColorStateList.valueOf(voAccent()));
    bindDashboard(() -> bar.setProgress(model.snapshot == null || model.snapshot.batteryPercent == null ? 0 : model.snapshot.batteryPercent));
    gap(soc, 3); soc.addView(bar, new LinearLayout.LayoutParams(-1, dp(3)));
    headline.addView(soc, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout range = column(); range.setGravity(Gravity.END);
    TextView km = liveText(() -> model.snapshot == null ? "-- km"
        : VehicleSnapshot.metric(model.snapshot.rangeKm, " km"), compact ? 21 : 30, ink, true);
    km.setTag("battery_range"); if(UiSkin.PS_DEFAULT.equals(UiSkin.get(this)))applyHeroWeight(km);range.addView(km);
    if (!compact) range.addView(text(tr("预计续航", "Estimated range"), 10, muted, false));
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1); rp.leftMargin = dp(20);
    headline.addView(range, rp); parent.addView(headline);
  }

  private void addChargingSection(LinearLayout parent, boolean compact) {
    FrameLayout section=new FrameLayout(this);section.setTag("charging_section_legacy");
    section.setPadding(dp(10),dp(5),dp(10),dp(5));
    GradientDrawable frame=bg(surface,voLike()?18:nioSkin()?12:8);frame.setStroke(dp(1),line);section.setBackground(frame);

    LinearLayout row=row();
    ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_charge);
    int chargeAccent=voLike()?voAccent():nioColors()?Color.rgb(32,196,121):accentText();
    icon.setColorFilter(chargeAccent);row.addView(icon,new LinearLayout.LayoutParams(dp(20),dp(20)));

    LinearLayout lines=column();lines.setPadding(dp(7),0,0,0);
    float fontScale=getResources().getConfiguration().fontScale;
    int chargeSp=fontScale>1.4f?11:compact?12:15;
    int detailSp=fontScale>1.3f?10:11;
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
      boolean active=AppPreferences.visible(this,"show_charging",true)&&ChargingVisibility.shouldShow(model.snapshot)&&parent.findViewWithTag("health_charge_slot")==null;
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
    click(location,this::locationDialog);bindDashboard(()->{location.setEnabled(!model.locationBusy&&!(model.busy&&!model.quietRefresh)&&!model.commandPending&&model.selected!=null);location.setAlpha(location.isEnabled()?1f:.45f);});
    ImageView refresh=new ImageView(this);refresh.setImageResource(R.drawable.ic_refresh);refresh.setColorFilter(ink);refresh.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    refresh.setPadding(dp(9),dp(9),dp(9),dp(9));refresh.setTag("refresh_vehicle");click(refresh,()->model.refreshUser(null));
    bindDashboard(()->{refresh.setEnabled(!(model.busy&&!model.quietRefresh));refresh.setAlpha(refresh.isEnabled()?1f:.45f);});
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
          LinearLayout stage=column();stage.setTag("volike_hero");LinearLayout content=row();VehicleHeroView car=new VehicleHeroView(this);car.setTag("polestar_vehicle");car.setContentDescription(tr("查看车门与车窗状态","View doors and windows"));click(car,this::carStatusDialog);car.setVisibility(AppPreferences.visible(this,"show_car",true)?View.VISIBLE:View.INVISIBLE);content.addView(car,new LinearLayout.LayoutParams(0,-1,1));content.addView(voDetailRail(true),new LinearLayout.LayoutParams(dp(142),-1));stage.addView(content,new LinearLayout.LayoutParams(-1,0,1));stage.addView(voStatusStrip(true),new LinearLayout.LayoutParams(-1,Math.max(dp(26),textLineHeight(14)+dp(4))));body.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        }else body.addView(voHero(false),new LinearLayout.LayoutParams(
            -1,dp(compact?150:170),1));
        addChargingSection(body,compact);
        if(!detailed)addVoMetricGrid(body,chosen,false,false);
        addVoQuickActions(body,false);
        addVoUtilities(body,false);
      }
      TextView footer=liveText(this::dashboardStamp,8,muted,false);footer.setGravity(Gravity.CENTER_VERTICAL);
      click(footer,()->info(tr("车况","Vehicle status"),dashboardStamp()));

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