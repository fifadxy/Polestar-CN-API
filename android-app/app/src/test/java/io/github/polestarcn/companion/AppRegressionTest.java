package io.github.polestarcn.companion;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.github.polestarcn.companion.api.ProtoWire;
import java.io.*;
import java.lang.reflect.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class AppRegressionTest {
  private MainActivity activity;
  private org.robolectric.android.controller.ActivityController<MainActivity> controller;

  @Before
  public void start() throws Exception {
    Context c = RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences", 0).edit().clear().commit();
    c.getSharedPreferences("cn_vehicle_session_secure", 0).edit().clear().commit();
    c.getSharedPreferences("vehicle_snapshot_display_v1", 0).edit().clear().commit();
    c.getSharedPreferences("camp_status", 0).edit().clear().commit();
    c.getSharedPreferences("camp_mode_secure", 0).edit().clear().commit();
    c.getSharedPreferences("climate_runtime_v1", 0).edit().clear().commit();
    Field f = VehicleCoordinator.class.getDeclaredField("instance");
    f.setAccessible(true);
    f.set(null, null);
    org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("legal",0).edit().putBoolean("accepted_disclaimer_v1",true).commit();
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    activity = controller.get();
  }

  @After
  public void stop() {
    controller.pause().stop().destroy();
  }

  @Test
  public void firstLaunchShowsRealCredentialsWithoutRegionDialog() {
    assertNull(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
    assertEquals(2, count(activity.getWindow().getDecorView(), EditText.class));
    assertFalse(AppPreferences.enabled(activity));
    assertEquals(Region.CHINA, AppState.region(activity));
    assertEquals(OperationState.NOT_SENT, VehicleCoordinator.get(activity).operation);
  }

  @Test public void controlColorsFollowVehicleStateRatherThanPendingTap() throws Exception {
    AppPreferences.set(activity,"theme","light"); UiSkin.set(activity,UiSkin.DETAILED);
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn"); signedIn.setAccessible(true); signedIn.setBoolean(c, true);
    c.snapshot = new VehicleSnapshot("fixture", "masked", 60, 250, 12000, "空闲", "全部关闭",
        "全部关闭", "已打开", "未锁", System.currentTimeMillis(), "LIVE", false, "开启", "开启");
    Method render = MainActivity.class.getDeclaredMethod("render"); render.setAccessible(true); render.invoke(activity);
    java.util.List<View> circles=new java.util.ArrayList<>();collectTagged(activity.getWindow().getDecorView(),"volike_control_circle",circles);
    assertEquals(4,circles.size());
    int orange=0,green=0;
    for(View circle:circles){int color=drawableColor(circle);if(color==android.graphics.Color.rgb(255,107,0))orange++;if(color==android.graphics.Color.parseColor("#5E8D7A"))green++;}
    assertEquals("unlock and open trunk use warning orange",2,orange);assertEquals("active climate and air use VoLike green",2,green);

    c.snapshot = new VehicleSnapshot("fixture", "masked", 60, 250, 12000, "空闲", "全部关闭",
        "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(), "LIVE", false, "关闭", "关闭");
    c.busy = true; c.activeAction = "climate-start"; c.operation = OperationState.SENDING;
    render.invoke(activity);
    circles.clear();collectTagged(activity.getWindow().getDecorView(),"volike_control_circle",circles);
    for(View circle:circles)assertEquals("pending command does not fake active vehicle state",
        android.graphics.Color.parseColor("#F1F0EC"),drawableColor(circle));
    c.busy = false;
  }
  private static int drawableColor(View view){
    return ((android.graphics.drawable.GradientDrawable)view.getBackground()).getColor().getDefaultColor();
  }
  private static void collectTagged(View view,String tag,java.util.List<View> out){
    if(tag.equals(view.getTag()))out.add(view);
    if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collectTagged(((ViewGroup)view).getChildAt(i),tag,out);
  }

  @Test
  @Config(qualifiers = "w320dp-h568dp-mdpi")
  public void controlTilesFitSmallPortrait() throws Exception {
    checkLayout(320, 568);
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-mdpi")
  public void controlTilesFitTypicalPortrait() throws Exception {
    checkLayout(360, 640);
  }

  @Test
  @Config(qualifiers = "w640dp-h320dp-mdpi")
  public void controlTilesFitLandscape() throws Exception {
    checkLayout(640, 320);
  }

  @Test
  @Config(qualifiers = "w360dp-h640dp-mdpi")
  public void darkChineseLayoutFits() throws Exception {
    AppPreferences.set(activity, "language", "zh");
    AppPreferences.set(activity, "theme", "dark");
    checkLayout(360, 640);
  }

  @Test
  @Config(sdk = 26)
  public void fingerprintFailureAndCancelNeverSendCommand() throws Exception {
    org.robolectric.Shadows.shadowOf(activity.getPackageManager())
        .setSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT, true);
    android.hardware.fingerprint.FingerprintManager manager =
        activity.getSystemService(android.hardware.fingerprint.FingerprintManager.class);
    org.robolectric.shadows.ShadowFingerprintManager fingerprint =
        org.robolectric.Shadows.shadowOf(manager);
    fingerprint.setIsHardwareDetected(true);
    fingerprint.setHasEnrolledFingerprints(true);
    VehicleCoordinator model = VehicleCoordinator.get(activity);
    model.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    model.selected.vin = "TEST-FIXTURE";
    Method verify =
        MainActivity.class.getDeclaredMethod("authenticate", String.class, String.class);
    verify.setAccessible(true);
    verify.invoke(activity, "Unlock", "unlock");
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    fingerprint.authenticationFails();
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertFalse(model.busy);
    assertEquals(OperationState.NOT_SENT, model.operation);
    Field prompt = MainActivity.class.getDeclaredField("biometric");
    prompt.setAccessible(true);
    ((androidx.biometric.BiometricPrompt) prompt.get(activity)).cancelAuthentication();
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertFalse(model.busy);
    assertEquals(OperationState.NOT_SENT, model.operation);
  }

  private void checkLayout(int width, int height) throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.snapshot =
        new VehicleSnapshot(
            "Polestar 2",
            "VIN…TEST",
            60,
            250,
            12000,
            "空闲",
            "全部关闭",
            "全部关闭",
            "已关闭",
            "已上锁",
            System.currentTimeMillis(),
            "LIVE",
            false,
            "关闭",
            "关闭");
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    Field auth = VehicleCoordinator.class.getDeclaredField("signedIn");
    auth.setAccessible(true);
    auth.setBoolean(c, true);
    Method method = MainActivity.class.getDeclaredMethod("render");
    method.setAccessible(true);
    method.invoke(activity);
    Field f = MainActivity.class.getDeclaredField("body");
    f.setAccessible(true);
    LinearLayout body = (LinearLayout) f.get(activity);
    View root = (View) body.getParent();
    root.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, width, height);
    assertEquals(0, count(body, ScrollView.class));
    assertEquals(4, countTappableTiles(body));
    verifyTiles(body, body.getHeight());
    android.graphics.Bitmap bitmap =
        android.graphics.Bitmap.createBitmap(
            width, height, android.graphics.Bitmap.Config.ARGB_8888);
    root.draw(new android.graphics.Canvas(bitmap));
    File directory = new File("build/reports/previews");
    directory.mkdirs();
    try (FileOutputStream out =
        new FileOutputStream(
            new File(
                directory,
                "control-"
                    + width
                    + "x"
                    + height
                    + "-"
                    + (AppPreferences.dark(activity) ? "dark" : "light")
                    + "-"
                    + (AppPreferences.english(activity) ? "en" : "zh")
                    + ".png"))) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    bitmap.recycle();
  }

  private static int countTappableTiles(View v) {
    int total = "control_tile".equals(v.getTag()) ? 1 : 0;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) total += countTappableTiles(g.getChildAt(i));
    }
    return total;
  }

  private static void verifyTiles(View view, int viewportHeight) {
    if ("control_tile".equals(view.getTag())) {
      LinearLayout tile = (LinearLayout) view;
      assertTrue("control target at least 48 dp", tile.getHeight() >= 48);
      assertTrue("control text fits", tile.getChildAt(tile.getChildCount()-1).getBottom() <= tile.getHeight());
      android.graphics.Rect bounds = new android.graphics.Rect();
      assertTrue("control remains on screen", tile.getGlobalVisibleRect(bounds));
      assertEquals("complete control remains on screen", tile.getHeight(), bounds.height());
    }
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) verifyTiles(group.getChildAt(i), viewportHeight);
    }
  }

  @Test @Config(qualifiers = "w320dp-h568dp-mdpi")
  public void nineSelectedFieldsStillFitOneScreen() throws Exception {
    AppPreferences.set(activity, "home_metrics", "odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,tm_consumption,aqi,temperature");
    checkLayout(320, 568);
  }

  @Test @Config(qualifiers = "w320dp-h568dp-mdpi")
  public void nineFieldsAndLargerTextKeepControlsVisible() throws Exception {
    android.content.res.Configuration configuration = new android.content.res.Configuration(activity.getResources().getConfiguration());
    configuration.fontScale = 1.3f;
    android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
    metrics.scaledDensity = metrics.density * 1.3f;
    activity.getResources().updateConfiguration(configuration, metrics);
    AppPreferences.set(activity, "home_metrics", "odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,tm_consumption,aqi,temperature");
    checkLayout(320, 568);
  }

  @Test @Config(qualifiers = "w320dp-h568dp-mdpi")
  public void twelveFieldsAndLargerTextKeepCompactControlsVisible() throws Exception {
    android.content.res.Configuration configuration = new android.content.res.Configuration(activity.getResources().getConfiguration());
    configuration.fontScale = 1.3f;
    android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
    metrics.scaledDensity = metrics.density * 1.3f;
    activity.getResources().updateConfiguration(configuration, metrics);
    AppPreferences.set(activity, "home_metrics", "odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,tm_consumption,aqi,temperature,charge_power,charge_current,charge_eta");
    checkLayout(320, 568);
    View tile = findTagged(activity.getWindow().getDecorView(), "control_tile");
    assertTrue(tile.getHeight() >= 60);
  }

  @Test @Config(qualifiers = "w640dp-h320dp-mdpi")
  public void twelveFieldsFitLandscape() throws Exception {
    AppPreferences.set(activity, "home_metrics", "odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,tm_consumption,aqi,temperature,charge_power,charge_current,charge_eta");
    checkLayout(640, 320);
  }

  @Test @Config(sdk = 26, qualifiers = "w360dp-h640dp-mdpi")
  public void allFourTilesGoDirectlyToBiometricsAndCancelWithoutCommand() throws Exception {
    org.robolectric.Shadows.shadowOf(activity.getPackageManager())
        .setSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT, true);
    org.robolectric.shadows.ShadowFingerprintManager fingerprint = org.robolectric.Shadows.shadowOf(
        activity.getSystemService(android.hardware.fingerprint.FingerprintManager.class));
    fingerprint.setIsHardwareDetected(true); fingerprint.setHasEnrolledFingerprints(true);
    for (String skin : new String[]{UiSkin.SIMPLE,UiSkin.DETAILED,UiSkin.VOLIKE_POLESTAR,UiSkin.NIO_LIKE,UiSkin.NIO_LIKE_2}) {
    UiSkin.set(activity,skin);
    checkLayout(360, 640);
    java.util.List<View> tiles = new java.util.ArrayList<>(); collectTiles(activity.getWindow().getDecorView(), tiles);
    Field pending = MainActivity.class.getDeclaredField("authPending"); pending.setAccessible(true);
    Field prompt = MainActivity.class.getDeclaredField("biometric"); prompt.setAccessible(true);
    for (View tile : tiles) {
      tile.performClick(); org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
      assertTrue("tap opens biometric prompt directly", pending.getBoolean(activity));
      assertNull("no action-selection dialog", org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
      View cancel = null;
      for (androidx.fragment.app.Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
        if (fragment instanceof androidx.fragment.app.DialogFragment) {
          Dialog dialog = ((androidx.fragment.app.DialogFragment) fragment).getDialog();
          if (dialog != null) cancel = dialog.findViewById(android.R.id.button2);
        }
      }
      assertNotNull("biometric cancel control", cancel); cancel.performClick();
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1, java.util.concurrent.TimeUnit.SECONDS);
      assertFalse(pending.getBoolean(activity));
      assertFalse(VehicleCoordinator.get(activity).busy);
      assertEquals(OperationState.NOT_SENT, VehicleCoordinator.get(activity).operation);
    }
    }
  }

  @Test public void hapticSwitchAndFallbackStrength() {
    android.os.Vibrator vibrator = (android.os.Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
    org.robolectric.shadows.ShadowVibrator shadow = org.robolectric.Shadows.shadowOf(vibrator);
    shadow.setHasVibrator(true); shadow.setHasAmplitudeControl(true);
    android.provider.Settings.System.putInt(activity.getContentResolver(), android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
    AppPreferences.set(activity, "haptics", "off"); Haptics.tap(activity); assertFalse(shadow.isVibrating());
    AppPreferences.set(activity, "haptics", "on"); AppPreferences.set(activity, "haptic_strength", "20");
    Haptics.tap(activity); assertTrue(shadow.isVibrating());
    vibrator.cancel(); AppPreferences.set(activity, "haptics", "off"); Haptics.tap(activity); assertFalse(shadow.isVibrating());
    shadow.setHasAmplitudeControl(false); AppPreferences.set(activity, "haptics", "on");
    Haptics.tap(activity); assertEquals(10L, shadow.getMilliseconds());
    AppPreferences.set(activity, "haptic_strength", "80"); Haptics.tap(activity);
    assertEquals(22L, shadow.getMilliseconds());
  }

  @Test @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void exportThreeNativePagePreviewsAndOpeningBadges() throws Exception {
    AppPreferences.set(activity, "language", "zh"); AppPreferences.set(activity, "theme", "dark");
    AppPreferences.set(activity, "home_metrics", "odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,tm_consumption,temperature,aqi,charge_power,charge_current,charge_eta");
    checkLayout(360, 780);
    VehicleCoordinator model = VehicleCoordinator.get(activity);
    model.snapshot = new VehicleSnapshot("Polestar 2", "masked", 63, 230, 65845, "充电中", "全部关闭", "全部关闭",
        "已关闭", "未锁", System.currentTimeMillis(), "LIVE", false, "开启", "关闭")
        .withMileage(new MileageSnapshot(65_845_000L, 100.5, 25.25, 45.0, 35.0, 18.5, 21.5, 1, 1))
        .withEnvironment(new EnvironmentSnapshot("开启", "关闭", 23.0, 22.0, 3.0, 4, null, null, 1, 1))
        .withCharging(new ChargingSnapshot(1, 2, 4, 180, 6000.0, 29.0))
        .withExterior(new ExteriorSnapshot(new int[]{1, 2, 2, 2}, new int[]{2, 2, 3, -1}));
    Method render = MainActivity.class.getDeclaredMethod("render"); render.setAccessible(true); render.invoke(activity);
    Field field = MainActivity.class.getDeclaredField("body"); field.setAccessible(true);
    View root = (View) ((View) field.get(activity)).getParent();
    root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(780, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 360, 780);
    assertEquals("车况",((TextView)findTagged(root,"exterior_button")).getText().toString());
    View status=findTagged(root,"vehicle_status_button");assertNotNull(status);
    assertTrue(status.getContentDescription().toString().contains("请检查车辆"));
    assertTrue(ExteriorView.hasOpening(model.snapshot));
    savePage(root, null, "v062-home.png");
    Method doors = MainActivity.class.getDeclaredMethod("exteriorDialog"); doors.setAccessible(true); doors.invoke(activity);
    View exterior = activity.getWindow().getDecorView();
    assertEquals(1, count(exterior, ExteriorView.class));
    savePage(exterior, null, "v062-exterior.png"); activity.onBackPressed();
    Method camp = MainActivity.class.getDeclaredMethod("campDialog"); camp.setAccessible(true); camp.invoke(activity);
    AlertDialog camping = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    assertTrue(camping.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
    savePage(root, camping, "v062-camping.png"); camping.dismiss();
    activity.getSharedPreferences("camp_status", 0).edit().putString("phase", "STOPPING").commit();
    render.invoke(activity);
    View campButton = findTagged(activity.getWindow().getDecorView(), "camp_button");
    assertEquals(android.graphics.Color.parseColor("#79B79F"),
        ((android.graphics.drawable.GradientDrawable) campButton.getBackground()).getColor().getDefaultColor());
  }

  private static void savePage(View root, AlertDialog dialog, String name) throws Exception {
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(1080, 2340, android.graphics.Bitmap.Config.ARGB_8888);
    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap); canvas.scale(3f, 3f); root.draw(canvas);
    if (dialog != null) {
      canvas.drawColor(0x99000000);
      View decor = dialog.getWindow().getDecorView();
      clearPreviewScrollIndicators(decor);
      decor.measure(View.MeasureSpec.makeMeasureSpec(336, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(744, View.MeasureSpec.AT_MOST));
      decor.layout(0, 0, 336, decor.getMeasuredHeight());
      canvas.save(); canvas.translate(12, (780 - decor.getMeasuredHeight()) / 2f); decor.draw(canvas); canvas.restore();
    }
    File dir = new File("build/reports/previews"); dir.mkdirs();
    try (FileOutputStream out = new FileOutputStream(new File(dir, name))) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    bitmap.recycle();
  }
  private static void clearPreviewScrollIndicators(View view) {
    view.setScrollIndicators(0);
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
      clearPreviewScrollIndicators(((ViewGroup) view).getChildAt(i));
  }
  private static View findTagged(View view, String tag) {
    if (tag.equals(view.getTag())) return view;
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
      View found = findTagged(((ViewGroup) view).getChildAt(i), tag); if (found != null) return found;
    }
    return null;
  }
  private static void collectTiles(View view, java.util.List<View> tiles) {
    if ("control_tile".equals(view.getTag())) tiles.add(view);
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
      collectTiles(((ViewGroup) view).getChildAt(i), tiles);
  }

  @Test
  public void campingDefaultsAndBatteryThresholdAreEnforcedInUi() throws Exception {
    assertEquals(31, CampModeManager.interval(activity));
    assertEquals(10, CampModeManager.minimum(activity));
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.snapshot = new VehicleSnapshot("test", "masked", 9, null, null, "未知", "未知", "未知",
        "未知", "未知", 0, "CACHED", false);
    Method method = MainActivity.class.getDeclaredMethod("campDialog"); method.setAccessible(true); method.invoke(activity);
    AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
    assertEquals(3, count(dialog.getWindow().getDecorView(), SeekBar.class));
    AppPreferences.set(activity, "camp_interval", "999");
    AppPreferences.set(activity, "camp_minimum", "1");
    assertEquals(60, CampModeManager.interval(activity)); assertEquals(5, CampModeManager.minimum(activity));
  }

  @Test
  public void widgetsRenderEverySelectedFieldAndKeep48DpControlTargets() throws Exception {
    VehicleSnapshot v = new VehicleSnapshot("Polestar 2", "masked", 63, 230, 65845,
        "充电中", "全部关闭", "全部关闭", "已关闭", "未锁", System.currentTimeMillis(), "LIVE", false)
        .withMileage(new MileageSnapshot(65_845_000L, 100.5, 25.25, 45.0, 35.0, 18.5, 21.5, 1000, 1000))
        .withCharging(new ChargingSnapshot(1, 2, 4, 180, 6000.0, 29.0));
    VehicleSnapshotStore.saveChina(activity, v);
    for (WidgetRenderer.Size size : WidgetRenderer.Size.values()) {
      int width = size == WidgetRenderer.Size.MINI || size == WidgetRenderer.Size.SQUARE ? 150 : 300;
      int height = size == WidgetRenderer.Size.MINI ? 88 : size == WidgetRenderer.Size.WIDE ? 70
          : size == WidgetRenderer.Size.DATA ? 260 : 170;
      RemoteViews remote = WidgetRenderer.createViews(activity, 100 + size.ordinal(), size);
      View root = remote.apply(activity, new FrameLayout(activity));
      root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, width, height);
      assertTrue(root.findViewById(R.id.widget_action_lock).getHeight() >= 48);
      assertTrue(root.findViewById(R.id.widget_action_climate).getHeight() >= 48);
      int[] ids = {R.id.widget_metric0, R.id.widget_metric1, R.id.widget_metric2, R.id.widget_metric3,
          R.id.widget_metric4, R.id.widget_metric5, R.id.widget_metric6, R.id.widget_metric7};
      for (int i = 0; i < WidgetRenderer.capacity(size); i++) {
        TextView field = root.findViewById(ids[i]);
        assertEquals(View.VISIBLE, field.getVisibility());
        assertFalse(field.getText().toString().isEmpty());
        assertTrue("widget field has width", field.getWidth() >= 30);
        if (field.getLayout().getHeight() > field.getHeight())
          System.err.println("WIDGET_OVERFLOW size=" + size + " field=" + i
              + " text=" + field.getText() + " layout=" + field.getLayout().getHeight()
              + " view=" + field.getHeight() + " width=" + field.getWidth());
        assertTrue("widget text fits height: " + size + "/" + i + " " + field.getLayout().getHeight() + "/" + field.getHeight(), field.getLayout().getHeight() <= field.getHeight());
      }
      android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
      root.draw(new android.graphics.Canvas(bitmap));
      File directory = new File("build/reports/previews"); directory.mkdirs();
      try (FileOutputStream out = new FileOutputStream(new File(directory, "widget-" + size + ".png"))) {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
      }
      bitmap.recycle();
    }
  }

  @Test
  public void englishAndChineseSwitchWithoutChangingWidgetLanguage() {
    AppPreferences.set(activity, "language", "en");
    assertEquals("On", AppPreferences.status(activity, "开启"));
    AppPreferences.set(activity, "language", "zh");
    assertEquals("开启", AppPreferences.status(activity, "开启"));
    assertEquals(activity.getString(R.string.app_name) + " · 4×2", activity.getString(R.string.widget_standard_name));
  }

  @Test
  public void cacheRestorationIncludesRealClimateAndAir() {
    VehicleSnapshot v =
        new VehicleSnapshot(
            "Polestar 2",
            "VIN…TEST",
            64,
            280,
            4000,
            "充电中",
            "全部关闭",
            "全部关闭",
            "已关闭",
            "已上锁",
            1000,
            "LIVE",
            false,
            "开启",
            "关闭");
    VehicleSnapshotStore.saveChina(activity, v);
    VehicleSnapshot copy = VehicleSnapshotStore.loadChina(activity);
    assertEquals("CACHED", copy.source);
    assertEquals("开启", copy.climate);
    assertEquals("关闭", copy.air);
    assertEquals(Integer.valueOf(64), copy.batteryPercent);
  }

  @Test
  public void mileageCachePreservesTripDecimalsAndNullValues() {
    VehicleSnapshot v = new VehicleSnapshot("Polestar 2", "masked", 64, 280, null,
        "空闲", "全部关闭", "全部关闭", "已关闭", "已上锁", 1000, "LIVE", false)
        .withMileage(new MileageSnapshot(12_345_678L, 127.5, 43.25));
    VehicleSnapshotStore.saveChina(activity, v);
    VehicleSnapshot copy = VehicleSnapshotStore.loadChina(activity);
    assertEquals("CACHED", copy.source);
    assertEquals(Long.valueOf(12_345_678L), copy.mileage.totalMeters);
    assertEquals(127.5, copy.mileage.manualKm, 0.0001);
    assertEquals(43.25, copy.mileage.automaticKm, 0.0001);
    VehicleSnapshotStore.saveChina(activity, v.withMileage(MileageSnapshot.unknown()));
    copy = VehicleSnapshotStore.loadChina(activity);
    assertNull(copy.mileage.totalMeters);
    assertNull(copy.mileage.automaticKm);
  }

  @Test public void completeTelemetrySurvivesCacheAndWidgetLanguageIsIndependent() {
    VehicleSnapshot v = new VehicleSnapshot("fixture", "masked", 63, 230, 65845,
        "充电中", "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(), "LIVE", false)
        .withMileage(new MileageSnapshot(65_845_000L, 100.5, 25.25, 45.0, 35.0, 18.5, 21.5, 1000, 1000))
        .withEnvironment(new EnvironmentSnapshot("关闭", "关闭", -4.5, 22.0, 3.0, 4, 25, 8, 1000, 1000))
        .withExterior(new ExteriorSnapshot(new int[]{1,2,3,-1}, new int[]{2,1,2,2}))
        .withCharging(new ChargingSnapshot(1, 2, 4, 180, 6000.0, 29.0));
    VehicleSnapshotStore.saveChina(activity, v);
    VehicleSnapshot copy = VehicleSnapshotStore.loadChina(activity);
    assertEquals(-4.5, copy.environment.temperatureC, 0.001);
    assertEquals(Integer.valueOf(4), copy.environment.aqi);
    assertEquals(3.0, copy.environment.pm25UgM3, 0.001);
    assertArrayEquals(new int[]{1,2,3,-1}, copy.exterior.states(false));
    assertEquals(29.0, copy.chargingDetails.currentAmps, 0.001);
    assertEquals("3 h", copy.chargingDetails.timeToFull(true));
    assertEquals(21.5, copy.mileage.automaticConsumption, 0.001);
    AppPreferences.set(activity, "language", "zh");
    AppPreferences.set(activity, "widget_language", "en");
    AppPreferences.set(activity, WidgetRenderer.metricKey(WidgetRenderer.Size.MINI), "charge_connection");
    View widget = WidgetRenderer.createViews(activity, 1234, WidgetRenderer.Size.MINI)
        .apply(activity, new FrameLayout(activity));
    assertTrue(widget.findViewById(R.id.widget_charging_section).getContentDescription().toString().contains("Connected"));
    assertFalse(AppPreferences.english(activity)); assertTrue(AppPreferences.widgetEnglish(activity));
  }

  @Test
  public void previousMileageCacheIsInvalidatedWithoutLosingBattery() {
    activity.getSharedPreferences("vehicle_snapshot_display_v1", 0).edit()
        .putString("china", "{\"name\":\"Polestar 2\",\"batteryPercent\":64,\"odometerKm\":17,\"capturedAt\":1000}")
        .commit();
    VehicleSnapshot copy = VehicleSnapshotStore.loadChina(activity);
    assertEquals(Integer.valueOf(64), copy.batteryPercent);
    assertNull(copy.odometerKm);
    assertNull(copy.mileage.totalMeters);
  }

  @Test
  public void notificationContainsStatesAndDataUpdateTime() throws Exception {
    VehicleSnapshotStore.saveChina(
        activity,
        new VehicleSnapshot(
            "Polestar 2",
            "VIN…TEST",
            64,
            280,
            4000,
            "充电中",
            "全部关闭",
            "全部关闭",
            "已关闭",
            "已上锁",
            1000,
            "LIVE",
            false,
            "开启",
            "关闭"));
    Method m = ResidentService.class.getDeclaredMethod("notification", Context.class);
    m.setAccessible(true);
    Notification n = (Notification) m.invoke(null, activity);
    String title = n.extras.getString(Notification.EXTRA_TITLE),
        text = n.extras.getString(Notification.EXTRA_TEXT);
    assertTrue(title.contains("64%"));
    assertFalse(text.contains("Token"));
    assertTrue(text.contains("Data updated"));
    assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility);
    assertTrue((n.flags & Notification.FLAG_ONGOING_EVENT) != 0);
  }

  @Test
  @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void bothSkinsExposeTemplateStylePullToRefresh() throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);

    for (String skin : new String[]{UiSkin.SIMPLE, UiSkin.DETAILED}) {
      UiSkin.set(activity, skin);
      c.busy = false;
      c.activeAction = "";
      render.invoke(activity);
      View tagged = findTagged(activity.getWindow().getDecorView(), "control_pull_refresh");
      assertNotNull("pull-to-refresh exists for " + skin, tagged);
      assertTrue(tagged instanceof HorizontalSwipeRefreshLayout);
      assertTrue(((HorizontalSwipeRefreshLayout) tagged).isEnabled());
      assertFalse(((HorizontalSwipeRefreshLayout) tagged).isRefreshing());
    }
  }

  @Test
  @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void pullRefreshSpinnerTracksVehicleRefreshNotRemoteCommand() throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);

    c.busy = true;
    c.activeAction = "";
    render.invoke(activity);
    HorizontalSwipeRefreshLayout refresh =
        (HorizontalSwipeRefreshLayout) findTagged(
            activity.getWindow().getDecorView(), "control_pull_refresh");
    assertTrue(refresh.isRefreshing());

    c.activeAction = "lock";
    render.invoke(activity);
    refresh =
        (HorizontalSwipeRefreshLayout) findTagged(
            activity.getWindow().getDecorView(), "control_pull_refresh");
    assertFalse("remote command does not masquerade as data refresh", refresh.isRefreshing());
    c.busy = false;
    c.activeAction = "";
  }

  @Test
  public void horizontalSwipeRefreshLayoutDoesNotInterceptClearHorizontalGesture() {
    HorizontalSwipeRefreshLayout layout = new HorizontalSwipeRefreshLayout(activity);
    FrameLayout child = new FrameLayout(activity);
    layout.addView(child, new SwipeRefreshLayout.LayoutParams(300, 500));
    long now = android.os.SystemClock.uptimeMillis();
    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 20f, 50f, 0);
    MotionEvent move = MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_MOVE, 180f, 58f, 0);
    layout.onInterceptTouchEvent(down);
    assertFalse(layout.onInterceptTouchEvent(move));
    down.recycle();
    move.recycle();
  }

  @Test
  @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void bothControlSkinsFitWithoutContentScrollingInLightAndDark() throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);

    for (String theme : new String[]{"light", "dark"}) {
      AppPreferences.set(activity, "theme", theme);
      for (String skin : new String[]{UiSkin.SIMPLE, UiSkin.DETAILED}) {
        UiSkin.set(activity, skin);
        render.invoke(activity);
        View refresh = findTagged(activity.getWindow().getDecorView(), "control_pull_refresh");
        assertNotNull(refresh);
        assertEquals("control page itself is fixed for " + skin + "/" + theme,
            0, count(refresh, ScrollView.class));
      }
    }
  }

  @Test
  @Config(qualifiers = "w640dp-h320dp-mdpi")
  public void bothStylesUseFixedLandscapeLayoutsWithoutClippedControls() throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.selected.modelName = "Polestar 2 Long Range Dual Motor";
    c.selected.modelYear = "2024";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "开启", "关闭")
        .withEnvironment(new EnvironmentSnapshot("开启", "关闭", 23.0, 22.0,
            3.0, 4, 18, null, 1, 1));
    AppPreferences.set(activity, "home_metrics", "temperature,target_temperature,vehicle_status");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);

    for (String skin : new String[]{UiSkin.SIMPLE, UiSkin.DETAILED}) {
      UiSkin.set(activity, skin);
      render.invoke(activity);
      View root = activity.getWindow().getDecorView();
      root.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 640, 320);
      View content = findTagged(root, "control_pull_content");
      assertNotNull(content);
      assertEquals(0, count(content, ScrollView.class));
      android.graphics.Rect visible = new android.graphics.Rect();
      assertTrue(content.getGlobalVisibleRect(visible));
      assertTrue("fixed landscape content stays within viewport for " + skin,
          visible.bottom <= 320);
      assertStaticVehicleSummary(root);
    }
  }

  @Test
  public void tireUiUsesItpmsWarningStateWithoutDisplayingKpa() {
    TireSnapshot tyres = new TireSnapshot(
        new Double[]{250.0, 251.0, 249.0, 252.0},
        new int[]{TireSnapshot.OK, TireSnapshot.OK, TireSnapshot.OK, TireSnapshot.OK}, 1);
    assertTrue(tyres.allGood());
    assertEquals("正常", tyres.label(0, false));
    assertFalse(tyres.label(0, false).contains("kPa"));
    assertFalse(tyres.label(0, true).contains("kPa"));

    TireSnapshot partial = new TireSnapshot(
        new Double[]{250.0, 251.0, 249.0, 252.0},
        new int[]{TireSnapshot.OK, TireSnapshot.UNKNOWN, TireSnapshot.OK, TireSnapshot.OK}, 1);
    assertFalse("partial iTPMS state must not be called ALL GOOD", partial.allGood());
    assertEquals("UNKNOWN", partial.summary(true));
  }

  @Test
  public void campAndWidgetSlidersUsePolestarViStyling() throws Exception {
    Method camp = MainActivity.class.getDeclaredMethod("campDialog");
    camp.setAccessible(true);
    camp.invoke(activity);
    AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    assertNotNull(findTagged(dialog.getWindow().getDecorView(), "vi_slider"));
    dialog.dismiss();

    Method widget = MainActivity.class.getDeclaredMethod(
        "widgetSlider", LinearLayout.class, String.class, String.class, int.class);
    widget.setAccessible(true);
    LinearLayout panel = new LinearLayout(activity);
    panel.setOrientation(LinearLayout.VERTICAL);
    widget.invoke(activity, panel, "Opacity", "widget_opacity", 58);
    assertNotNull(findTagged(panel, "vi_slider"));
  }

  @Test
  @Config(qualifiers = "w320dp-h568dp-mdpi")
  public void bothControlSkinsKeepTheirLastControlInsideSmallScreen() throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true); signedIn.setBoolean(c, true);
    Method render = MainActivity.class.getDeclaredMethod("render"); render.setAccessible(true);
    for (String skin : new String[]{UiSkin.SIMPLE, UiSkin.DETAILED}) {
      UiSkin.set(activity, skin);
      render.invoke(activity);
      View content = findTagged(activity.getWindow().getDecorView(), "control_pull_content");
      assertNotNull(content);
      View root = activity.getWindow().getDecorView();
      root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(568, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 320, 568);
      ViewGroup group = (ViewGroup) content;
      View last = group.getChildAt(group.getChildCount() - 1);
      assertTrue("last control stays in page for " + skin, last.getBottom() <= content.getHeight());
    }
  }

  @Test
  public void legacySkinValuesMigrateToTwoStyleModel() {
    AppPreferences.set(activity, "ui_skin", "latest");
    assertEquals(UiSkin.SIMPLE, UiSkin.get(activity));
    AppPreferences.set(activity, "ui_skin", "ev_concept");
    assertEquals(UiSkin.SIMPLE, UiSkin.get(activity));
    AppPreferences.set(activity, "ui_skin", "current");
    assertEquals(UiSkin.DETAILED, UiSkin.get(activity));
  }

  @Test
  public void climateUsesVehicleReportedRemainingMinutesBeforeLocalEstimate() {
    AppPreferences.set(activity, "language", "zh");
    ClimateRuntime.stopped(activity);
    VehicleSnapshot live = new VehicleSnapshot("Polestar 2", "masked", 60, 250, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "开启", "关闭")
        .withEnvironment(new EnvironmentSnapshot("开启", "关闭", 23.0, 22.0,
            null, null, 18, null, System.currentTimeMillis(), System.currentTimeMillis()));
    assertEquals("运行中 · 剩余18分钟", ClimateRuntime.label(activity, live, "vehicle"));
    assertTrue(ClimateRuntime.detail(activity, live, "vehicle").contains("剩余 18分钟"));
    assertTrue(ClimateRuntime.detail(activity, live, "vehicle").contains("车内 23.0 °C"));
    assertTrue(ClimateRuntime.detail(activity, live, "vehicle").contains("设定 22.0 °C"));

    ClimateRuntime.started(activity, "vehicle", System.currentTimeMillis() - 5L * 60L * 1000L);
    VehicleSnapshot fallback = new VehicleSnapshot("Polestar 2", "masked", 60, 250, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "开启", "关闭")
        .withEnvironment(new EnvironmentSnapshot("开启", "关闭", 23.0, 22.0,
            null, null, null, null, 1, 1));
    assertTrue(ClimateRuntime.label(activity, fallback, "vehicle").contains("估算剩余"));
  }

  @Test
  @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void bothVehicleControlStylesExposeStaticSummaryAndSelectedMetrics() throws Exception {
    AppPreferences.set(activity, "language", "zh");
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.selected.modelName = "Polestar 2 Long Range Dual Motor";
    c.selected.modelYear = "2024";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "开启", "关闭")
        .withEnvironment(new EnvironmentSnapshot("开启", "关闭", 23.0, 22.0,
            3.0, 4, 18, null, 1, 1));
    AppPreferences.set(activity, "home_metrics", "temperature,target_temperature,vehicle_status");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);

    for (String skin : new String[]{UiSkin.SIMPLE, UiSkin.DETAILED}) {
      UiSkin.set(activity, skin);
      render.invoke(activity);
      View root = activity.getWindow().getDecorView();
      root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(780, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 360, 780);
      assertStaticVehicleSummary(root);
      assertTrue(hasText(root, "车内 °C"));
      assertTrue(hasText(root, "目标 °C"));
    }

    UiSkin.set(activity, UiSkin.DETAILED);
    render.invoke(activity);
    View root = activity.getWindow().getDecorView();
    root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(780, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 360, 780);
    View car = findTagged(root, "polestar_vehicle");
    View windows = findTagged(root, "windows_control_button");
    View bodyButton = findTagged(root, "exterior_button");
    View vehicleStatus = findTagged(root, "vehicle_status_button");
    View camp = findTagged(root, "camp_button");
    View signal = findTagged(root, "signal_control_button");
    assertNotNull(car); assertNotNull(windows); assertNotNull(bodyButton);
    assertNotNull(vehicleStatus); assertNotNull(camp); assertNotNull(signal);
    int carBottom = absoluteTop(car) + car.getHeight();
    assertTrue(absoluteTop(windows) >= carBottom);
    assertTrue(absoluteTop(bodyButton) >= carBottom);
    assertTrue(absoluteTop(vehicleStatus) >= carBottom);
    assertTrue(absoluteTop(camp) >= carBottom);
    assertTrue(absoluteTop(signal) >= carBottom);
  }

  @Test
  @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void primaryVehicleHeaderHidesYearAndVinButSecondaryMenuShowsThem() throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "LPSVSE1K9ML123456";
    c.selected.model = "Polestar 2";
    c.selected.modelName = "Polestar 2 2021";
    c.selected.modelYear = "2021";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    UiSkin.set(activity, UiSkin.DETAILED);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);
    render.invoke(activity);
    View root = activity.getWindow().getDecorView();
    assertTrue(hasText(root, "Polestar 2"));
    assertFalse(hasText(root, "2021"));
    assertFalse(hasText(root, "LPSVSE1K9ML123456"));

    Method info = MainActivity.class.getDeclaredMethod("vehicleInfoDialog");
    info.setAccessible(true);
    info.invoke(activity);
    AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    assertTrue(hasText(dialog.getWindow().getDecorView(), "2021"));
    assertTrue(hasText(dialog.getWindow().getDecorView(), "LPSVSE1K9ML123456"));
    dialog.dismiss();
  }

  @Test
  @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void tyreDetailIsHiddenByDefaultBehindAggregateVehicleStatus() throws Exception {
    AppPreferences.set(activity, "language", "zh");
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    TireSnapshot tyres = new TireSnapshot(
        new Double[]{null, null, null, null},
        new int[]{TireSnapshot.OK, TireSnapshot.OK, TireSnapshot.OK, TireSnapshot.OK}, 1);
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭")
        .withTires(tyres)
        .withHealth(new VehicleHealthSnapshot(
            VehicleHealthSnapshot.OK, VehicleHealthSnapshot.OK, VehicleHealthSnapshot.OK, 1));
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    AppPreferences.set(activity, "experimental_tire", "off");
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);
    render.invoke(activity);
    View root = activity.getWindow().getDecorView();
    assertNotNull(findTagged(root, "vehicle_status_button"));
    assertNull("standalone tyre button is not on control page", findTagged(root, "tire_button"));

    Method dialogMethod = MainActivity.class.getDeclaredMethod("vehicleStatusDialog");
    dialogMethod.setAccessible(true);
    dialogMethod.invoke(activity);
    View page=activity.getWindow().getDecorView();
    assertNotNull(page.findViewWithTag("status_scroll"));
    assertTrue(hasText(page,"12V"));assertTrue(hasText(page,"玻璃水"));assertTrue(hasText(page,"胎压告警"));
    assertNotNull(page.findViewWithTag("disclosure_header_status_tyres"));

  }

  @Test
  public void healthWarningMappingMatchesGlobalHisingenFields() {
    java.util.Map<Integer, Object> health = new java.util.LinkedHashMap<>();
    health.put(13, 2L);
    health.put(38, 1L);
    TireSnapshot tyres = new TireSnapshot(
        new Double[]{null, null, null, null},
        new int[]{TireSnapshot.OK, TireSnapshot.OK, TireSnapshot.OK, TireSnapshot.OK}, 1);
    VehicleHealthSnapshot state = VehicleHealthSnapshot.fromData(health, tyres);
    assertEquals(VehicleHealthSnapshot.WARNING, state.washerFluid);
    assertEquals(VehicleHealthSnapshot.OK, state.lowVoltageBattery);
    assertEquals(VehicleHealthSnapshot.OK, state.tirePressure);
    assertTrue(state.hasProblem());
  }

  @Test
  @Config(qualifiers = "w320dp-h568dp-mdpi")
  public void twelveDynamicMetricsRemainOnSingleScreenInBothSkins() throws Exception {
    AppPreferences.set(activity, "home_metrics",
        "vehicle_status,odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,"
            + "tm_consumption,aqi,temperature,charge_power,charge_current");
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true);
    signedIn.setBoolean(c, true);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true);
    for (String skin : new String[]{UiSkin.SIMPLE, UiSkin.DETAILED}) {
      UiSkin.set(activity, skin);
      render.invoke(activity);
      View root = activity.getWindow().getDecorView();
      root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(568, View.MeasureSpec.EXACTLY));
      root.layout(0, 0, 320, 568);
      View content = findTagged(root, "control_pull_content");
      assertNotNull(content);
      assertEquals(0, count(content, ScrollView.class));
      ViewGroup group = (ViewGroup) content;
      View last = group.getChildAt(group.getChildCount() - 1);
      assertTrue(last.getBottom() <= content.getHeight());
      assertStaticVehicleSummary(root);
      View charging = findTagged(root, "charging_section");
      assertNotNull(charging);
      assertEquals("idle charge telemetry stays hidden", View.GONE, charging.getVisibility());
    }
  }

  @Test
  @Config(qualifiers = "w360dp-h780dp-mdpi")
  public void controlPageDoesNotExposeLanguageShortcut() throws Exception {
    VehicleCoordinator c = VehicleCoordinator.get(activity);
    c.selected = new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    c.selected.vin = "TEST-FIXTURE";
    c.snapshot = new VehicleSnapshot("Polestar 2", "masked", 64, 260, 12000, "空闲",
        "全部关闭", "全部关闭", "已关闭", "已上锁", System.currentTimeMillis(),
        "LIVE", false, "关闭", "关闭");
    Field signedIn = VehicleCoordinator.class.getDeclaredField("signedIn");
    signedIn.setAccessible(true); signedIn.setBoolean(c, true);
    UiSkin.set(activity, UiSkin.DETAILED);
    Method render = MainActivity.class.getDeclaredMethod("render");
    render.setAccessible(true); render.invoke(activity);
    assertFalse(hasExactText(activity.getWindow().getDecorView(), "中文"));
    assertFalse(hasExactText(activity.getWindow().getDecorView(), "EN"));
  }

  @Test public void standaloneClientConfigNeverRequiresAnotherInstalledApk() throws Exception {
    io.github.polestarcn.companion.auth.ClientSignatureProvider provider=
        new io.github.polestarcn.companion.auth.ClientSignatureProvider(activity);
    if(provider.isReady()){
      assertEquals(32,provider.resolve().length());
      assertTrue(provider.source()!=io.github.polestarcn.companion.auth.ClientSignatureProvider.Source.MISSING);
    }else{
      assertEquals(io.github.polestarcn.companion.auth.ClientSignatureProvider.Source.MISSING,provider.source());
      try{
        provider.resolve();
        fail("missing build configuration must be explicit");
      }catch(IllegalStateException expected){
        assertTrue(expected.getMessage().contains("verification build"));
      }
    }
  }

  @Test
  public void decorativeGlowClassIsNotShipped() throws Exception {
    try {
      Class.forName("io.github.polestarcn.companion.ViAmbientGlowView", false,
          MainActivity.class.getClassLoader());
      fail("Decorative glow implementation must be removed from this build");
    } catch (ClassNotFoundException expected) {
      // The removed class cannot allocate an animator or retain rendering work.
    }
  }

  private static void assertStaticVehicleSummary(View root) {
    assertNull("decorative glow is absent", findTagged(root, "vi_ambient_glow"));
    View car = findTagged(root, "polestar_vehicle");
    View summary = findTagged(root, "vehicle_status_button");
    assertNotNull(car);
    assertTrue("vehicle summary pairs an icon and text", summary instanceof LinearLayout);
    assertTrue(((LinearLayout) summary).getChildCount() >= 2);
    assertTrue(count(summary, TextView.class) >= 1);
    assertTrue("vehicle summary is below the vehicle image",
        absoluteTop(summary) >= absoluteTop(car) + car.getHeight());
    View progress = findTagged(root, "vi_progress");
    assertTrue("both styles retain a battery bar", progress instanceof ProgressBar);
    android.graphics.Rect visible = new android.graphics.Rect();
    assertTrue("vehicle summary is visible", summary.getGlobalVisibleRect(visible));
    assertEquals("entire vehicle summary remains visible", summary.getHeight(), visible.height());
  }

  private static boolean hasText(View view, String text) {
    if (view instanceof TextView && ((TextView) view).getText().toString().contains(text)) return true;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++)
        if (hasText(group.getChildAt(i), text)) return true;
    }
    return false;
  }

  private static boolean hasExactText(View view, String text) {
    if (view instanceof TextView && text.equals(((TextView) view).getText().toString())) return true;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++)
        if (hasExactText(group.getChildAt(i), text)) return true;
    }
    return false;
  }

  private static int absoluteTop(View view) {
    int top = 0;
    View current = view;
    while (current != null) {
      top += current.getTop();
      android.view.ViewParent parent = current.getParent();
      current = parent instanceof View ? (View) parent : null;
    }
    return top;
  }

  private static int count(View v, Class<?> type) {
    int n = type.isInstance(v) ? 1 : 0;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) n += count(g.getChildAt(i), type);
    }
    return n;
  }
}
