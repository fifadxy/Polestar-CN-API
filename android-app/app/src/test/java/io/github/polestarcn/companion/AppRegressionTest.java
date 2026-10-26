package io.github.polestarcn.companion;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
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
    Field f = VehicleCoordinator.class.getDeclaredField("instance");
    f.setAccessible(true);
    f.set(null, null);
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
    int tiles = 0;
    for (int i = 0; i < body.getChildCount(); i++) {
      View child = body.getChildAt(i);
      if (!(child instanceof LinearLayout)) continue;
      LinearLayout row = (LinearLayout) child;
      for (int j = 0; j < row.getChildCount(); j++) {
        View tile = row.getChildAt(j);
        if (!(tile instanceof LinearLayout) || tile.getContentDescription() == null) continue;
        assertTrue("tile touch height", tile.getHeight() >= 48);
        LinearLayout inner = (LinearLayout) tile;
        assertTrue(
            "tile text fully visible",
            inner.getChildAt(inner.getChildCount() - 1).getBottom() <= tile.getHeight());
        assertTrue("tile within viewport", child.getBottom() <= body.getHeight());
        tiles++;
      }
    }
    assertEquals(4, tiles);
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

  @Test
  public void englishAndChineseSwitchWithoutChangingWidgetLanguage() {
    AppPreferences.set(activity, "language", "en");
    assertEquals("On", AppPreferences.status(activity, "开启"));
    AppPreferences.set(activity, "language", "zh");
    assertEquals("开启", AppPreferences.status(activity, "开启"));
    assertEquals("Polestar · Standard", activity.getString(R.string.widget_standard_name));
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
  public void notificationContainsStatesAndTokenTime() throws Exception {
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
    assertTrue(text.contains("Token"));
    assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility);
    assertTrue((n.flags & Notification.FLAG_ONGOING_EVENT) != 0);
  }

  @Test
  public void invalidReferenceCannotImport() throws Exception {
    File temp = File.createTempFile("invalid", ".apk", activity.getCacheDir());
    try {
      Method m =
          io.github.polestarcn.companion.auth.ReferenceSignatureProvider.class.getDeclaredMethod(
              "extract", File.class);
      m.setAccessible(true);
      try {
        m.invoke(
            new io.github.polestarcn.companion.auth.ReferenceSignatureProvider(activity), temp);
        fail("accepted invalid APK");
      } catch (InvocationTargetException expected) {
        assertTrue(expected.getCause() instanceof IllegalStateException);
      }
    } finally {
      temp.delete();
    }
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
