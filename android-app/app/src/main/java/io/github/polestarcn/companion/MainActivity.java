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
import io.github.polestarcn.companion.auth.ReferenceSignatureProvider;
import java.util.*;

/** Control is the launch page; the region chooser and all sample telemetry have been removed. */
public final class MainActivity extends FragmentActivity {
  private VehicleCoordinator model;
  private final Runnable listener = this::render;
  private LinearLayout shell, body;
  private int page = 0, ink, muted, paper, surface, line;
  private final int orange = Color.rgb(232, 132, 69);
  private Typeface regular, bold;
  private boolean authPending;
  private BiometricPrompt biometric;
  private EditText phone, password;
  private String inputPhone = "", inputPassword = "";
  private static final int PICK_APK = 32, NOTIFICATIONS = 33;

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
    if (model.hasSession() && !model.busy) model.refresh(null);
    if (AppPreferences.enabled(this))
      try {
        ResidentService.apply(this);
      } catch (RuntimeException e) {
        DiagnosticLog.add("Resident start restricted");
      }
  }

  @Override
  protected void onDestroy() {
    model.remove(listener);
    if (biometric != null) biometric.cancelAuthentication();
    super.onDestroy();
  }

  @Override
  protected void onStop() {
    if (biometric != null && authPending) biometric.cancelAuthentication();
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
    t.setOnClickListener(v -> r.run());
    t.setFocusable(true);
    t.setClickable(true);
    return t;
  }

  private void palette() {
    boolean dark = AppPreferences.dark(this);
    ink = Color.parseColor(dark ? "#F4F4F2" : "#18191A");
    muted = Color.parseColor(dark ? "#AAAAA7" : "#626560");
    paper = Color.parseColor(dark ? "#111213" : "#F5F5F2");
    surface = Color.parseColor(dark ? "#242628" : "#FFFFFF");
    line = Color.parseColor(dark ? "#343638" : "#DCDDD7");
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
    LinearLayout header = row();
    header.setPadding(dp(20), 0, dp(12), 0);
    ImageView brand = new ImageView(this);
    brand.setImageResource(
        AppPreferences.dark(this) ? R.drawable.brand_light : R.drawable.brand_dark);
    brand.setScaleType(ImageView.ScaleType.FIT_CENTER);
    brand.setContentDescription("Polestar");
    header.addView(brand, new LinearLayout.LayoutParams(dp(30), dp(30)));
    TextView name = text("Polestar Assistant", 16, ink, false);
    LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1);
    np.leftMargin = dp(12);
    header.addView(name, np);
    TextView language = button(AppPreferences.english(this) ? "EN" : "中文", this::languageDialog);
    header.addView(language, new LinearLayout.LayoutParams(dp(58), dp(48)));
    shell.addView(header, new LinearLayout.LayoutParams(-1, dp(56)));
    body = column();
    body.setPadding(dp(18), 0, dp(18), 0);
    shell.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
    if (page == 2) settings();
    else if (!model.hasSession()) login();
    else if (page == 1) details();
    else controls();
    LinearLayout nav = row();
    nav.setPadding(dp(14), dp(6), dp(14), dp(6));
    String[] names = {tr("车辆控制", "Control"), tr("车况详情", "Details"), tr("设置", "Settings")};
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
    boolean compact =
        getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density
            < 500;
    VehicleSnapshot v = model.snapshot;
    LinearLayout vehicle = row();
    TextView title = text(v == null ? tr("连接车辆", "Connect vehicle") : v.name, 20, ink, false);
    vehicle.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    TextView refresh = button(model.busy ? "…" : "↻", () -> model.refresh(null));
    refresh.setContentDescription(tr("刷新车辆状态", "Refresh vehicle"));
    refresh.setEnabled(!model.busy);
    vehicle.addView(refresh, new LinearLayout.LayoutParams(dp(48), dp(48)));
    title.setOnClickListener(w -> chooseVehicle());
    body.addView(vehicle, new LinearLayout.LayoutParams(-1, dp(compact ? 36 : 52)));
    LinearLayout metrics = row();
    TextView soc =
        text(v == null ? "—%" : VehicleSnapshot.metric(v.batteryPercent, "%"), 36, orange, false);
    metrics.addView(soc, new LinearLayout.LayoutParams(0, -1, 1));
    soc.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout ranges = column();
    ranges.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
    TextView range =
        text(v == null ? "— km" : VehicleSnapshot.metric(v.rangeKm, " km"), 23, orange, false);
    ranges.addView(range);
    ranges.addView(
        text(
            v == null ? tr("等待车辆数据", "Waiting for vehicle") : status(v.charging),
            12,
            muted,
            false));
    metrics.addView(ranges, new LinearLayout.LayoutParams(0, -1, 1));
    body.addView(metrics, new LinearLayout.LayoutParams(-1, dp(compact ? 44 : 66)));
    ImageView car = new ImageView(this);
    String modelName = v == null ? "" : v.name;
    if (modelName.contains("2")) car.setImageResource(R.drawable.vehicle_top);
    else
      car.setImageResource(
          AppPreferences.dark(this) ? R.drawable.brand_light : R.drawable.brand_dark);
    car.setScaleType(ImageView.ScaleType.FIT_CENTER);
    car.setPadding(dp(14), dp(8), dp(14), dp(8));
    car.setContentDescription(
        modelName.contains("2") ? "Polestar 2" : tr("车辆品牌标志", "Vehicle brand symbol"));
    if (!compact) body.addView(car, new LinearLayout.LayoutParams(-1, 0, 1.3f));
    LinearLayout first = row(), second = compact ? first : row();
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, 0, 1);
    rp.topMargin = dp(8);
    body.addView(first, rp);
    if (!compact) {
      LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1);
      sp.topMargin = dp(8);
      body.addView(second, sp);
    }
    tile(
        first,
        tr("门锁", "Door lock"),
        v == null ? tr("未知", "Unknown") : status(v.lock),
        "lock",
        () ->
            actionDialog(
                tr("门锁", "Door lock"),
                new String[] {tr("解锁车门", "Unlock doors"), tr("锁定车门", "Lock doors")},
                new String[] {"unlock", "lock"}));
    tile(
        first,
        tr("尾箱", "Tailgate"),
        v == null ? tr("未知", "Unknown") : status(v.tailgate),
        "trunk",
        () ->
            actionDialog(
                tr("尾箱", "Tailgate"),
                new String[] {tr("打开尾箱", "Open tailgate"), tr("关闭尾箱", "Close tailgate")},
                new String[] {"trunk-open", "trunk-close"}));
    tile(
        second,
        tr("空调", "Climate"),
        v == null ? tr("未知", "Unknown") : status(v.climate),
        "climate",
        () ->
            actionDialog(
                tr("驻车空调", "Parking climate"),
                new String[] {tr("启动 · 22°C", "Start · 22°C"), tr("关闭空调", "Stop climate")},
                new String[] {"climate-start", "climate-stop"}));
    tile(
        second,
        tr("空气净化", "Air purification"),
        v == null ? tr("未知", "Unknown") : status(v.air),
        "air",
        () ->
            actionDialog(
                tr("空气净化", "Air purification"),
                new String[] {tr("开启净化", "Start purification"), tr("关闭净化", "Stop purification")},
                new String[] {"air-start", "air-stop"}));
    TextView message =
        text(
            model.message.isEmpty()
                ? tr("点击卡片控制 · 需指纹验证", "Tap a tile · Biometric verification required")
                : model.message,
            12,
            muted,
            false);
    message.setMaxLines(2);
    message.setGravity(Gravity.CENTER_VERTICAL);
    body.addView(message, new LinearLayout.LayoutParams(-1, dp(compact ? 28 : 40)));
    String age =
        v == null
            ? ""
            : (v.source.equals("CACHED") ? tr("缓存", "Cached") : tr("读取", "Read"))
                + " · "
                + android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis);
    TextView stamp = text(age, 11, muted, false);
    if (!compact) body.addView(stamp, new LinearLayout.LayoutParams(-1, dp(20)));
  }

  private void tile(LinearLayout row, String label, String state, String icon, Runnable action) {
    LinearLayout t = column();
    t.setGravity(Gravity.CENTER_VERTICAL);
    t.setPadding(dp(14), dp(6), dp(12), dp(6));
    t.setBackground(bg(surface, 8));
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -1, 1);
    tp.setMargins(0, 0, row.getChildCount() < 3 ? dp(8) : 0, 0);
    row.addView(t, tp);
    ImageView image = new ImageView(this);
    int id =
        icon.equals("lock")
            ? R.drawable.ic_lock
            : icon.equals("trunk")
                ? R.drawable.ic_tailgate
                : icon.equals("climate") ? R.drawable.ic_climate : R.drawable.ic_air;
    image.setImageResource(id);
    image.setColorFilter(ink);
    t.addView(image, new LinearLayout.LayoutParams(dp(22), dp(22)));
    gap(t, 5);
    TextView a = text(label, 14, ink, false);
    a.setMaxLines(1);
    t.addView(a);
    TextView b = text(state, 12, muted, false);
    b.setMaxLines(1);
    t.addView(b);
    t.setEnabled(!model.busy && model.selected != null);
    t.setAlpha(t.isEnabled() ? 1f : .55f);
    t.setOnClickListener(w -> action.run());
    t.setContentDescription(label + ", " + state);
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
    new AlertDialog.Builder(this)
        .setTitle(tr("选择车辆", "Select vehicle"))
        .setItems(labels, (d, w) -> model.choose(model.cars.get(w)))
        .show();
  }

  private void actionDialog(String title, String[] labels, String[] actions) {
    if (model.busy || authPending || model.selected == null) return;
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setItems(labels, (d, w) -> authenticate(labels[w], actions[w]))
        .setNegativeButton(tr("取消", "Cancel"), null)
        .show();
  }

  private void authenticate(String label, String action) {
    if (authPending || model.busy || model.selected == null) return;
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
    final String vin = model.selected.vin;
    authPending = true;
    biometric =
        new BiometricPrompt(
            this,
            command -> runOnUiThread(command),
            new BiometricPrompt.AuthenticationCallback() {
              @Override
              public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                runOnUiThread(
                    () -> {
                      authPending = false;
                      if (model.selected != null && vin.equals(model.selected.vin) && !model.busy)
                        model.command(action);
                    });
              }

              @Override
              public void onAuthenticationError(int code, CharSequence error) {
                runOnUiThread(
                    () -> {
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
    detailRow(p, tr("累计里程", "Odometer"), VehicleSnapshot.metric(v.odometerKm, " km"));
    detailRow(p, tr("充电", "Charging"), status(v.charging));
    detailRow(p, tr("门锁", "Door lock"), status(v.lock));
    detailRow(p, tr("车门", "Doors"), status(v.doors));
    detailRow(p, tr("车窗", "Windows"), status(v.windows));
    detailRow(p, tr("尾箱", "Tailgate"), status(v.tailgate));
    detailRow(p, tr("空调", "Climate"), status(v.climate));
    detailRow(p, tr("净化", "Purification"), status(v.air));
    detailRow(
        p,
        tr("更新时间", "Last update"),
        android.text.format.DateFormat.format("MM-dd HH:mm", v.timestampMillis).toString());
    gap(p, 14);
    p.addView(button(tr("切换车辆", "Select vehicle"), this::chooseVehicle));
  }

  private void languageDialog() {
    new AlertDialog.Builder(this)
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

  private void settings() {
    LinearLayout p = scrollBody();
    p.addView(text(tr("设置", "Settings"), 24, ink, false));
    gap(p, 20);
    p.addView(button(tr("语言 / Language", "Language / 语言"), this::languageDialog));
    gap(p, 8);
    p.addView(
        button(
            tr("显示模式", "Appearance"),
            () ->
                new AlertDialog.Builder(this)
                    .setTitle(tr("显示模式", "Appearance"))
                    .setItems(
                        new String[] {tr("跟随系统", "System"), tr("浅色", "Light"), tr("深色", "Dark")},
                        (d, w) -> {
                          AppPreferences.set(
                              this, "theme", new String[] {"system", "light", "dark"}[w]);
                          render();
                        })
                    .show()));
    gap(p, 16);
    Switch keep = new Switch(this);
    keep.setText(tr("常驻车辆状态通知", "Persistent vehicle notification"));
    keep.setTextColor(ink);
    keep.setTextSize(15);
    keep.setMinHeight(dp(52));
    keep.setChecked(AppPreferences.enabled(this));
    keep.setOnCheckedChangeListener(
        (b, on) -> {
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
                "显示电量、充电、空调／净化及 Token 刷新时间。静默通知；系统约每 30 分钟刷新，省电限制可能延迟。锁屏隐藏详细内容。",
                "Shows battery, charging, climate / air and Token refresh time. Silent; Android"
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
              new AlertDialog.Builder(this)
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
            "v0.4.0-beta1 · " + tr("独立开发的非官方客户端", "Independent unofficial client"),
            12,
            muted,
            false));
    p.addView(text("Lato · SIL Open Font License", 12, muted, false));
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
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(s)
        .setPositiveButton(tr("知道了", "OK"), null)
        .show();
  }
}
