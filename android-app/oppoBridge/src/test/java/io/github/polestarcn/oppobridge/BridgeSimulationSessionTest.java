package io.github.polestarcn.oppobridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public final class BridgeSimulationSessionTest {
  @Test public void simulationExercisesCardAndFourRoutesWithoutVehicleCall() {
    Context context = RuntimeEnvironment.getApplication();
    BridgeSimulationSession.Result result = BridgeSimulationSession.run(context);
    assertTrue(result.passed);
    assertTrue(result.text.contains("不发车、不写OPPO鉴权"));
    assertTrue(result.text.contains("卡片控制项 · 4/4"));
    assertFalse(result.text.contains("lastCommandSent=true"));
  }

  @Test public void consolidatedSettingsCanDisableAndRestoreMasterSwitch() {
    Context context = RuntimeEnvironment.getApplication();
    BridgeSettings.updateLocal(context, BridgeSettings.MASTER, false);
    Bundle disabled = BridgeSettings.localSnapshot(context);
    assertFalse(disabled.getBoolean(BridgeSettings.MASTER, true));
    BridgeSettings.enableRecommendedDefaults(context);
    Bundle restored = BridgeSettings.localSnapshot(context);
    assertTrue(restored.getBoolean(BridgeSettings.MASTER, false));
    assertTrue(restored.getBoolean(BridgeSettings.DIRECT, false));
  }
}
