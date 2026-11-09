package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class V120AccountSecurityTest {
  @Test public void optInDefaultsOffAndCredentialKeysCannotBeExported(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    assertFalse(AppPreferences.visible(c,"remember_credentials_optin",false));
    for(String key:new String[]{"remember_credentials_optin","psa_cn_saved_login_v1","encrypted_account_v1",
        "session_blob_v1","password","accessToken","refreshToken"})
      assertFalse(key,SettingsBackup.allowed(key));
  }
  @Test public void resetSettingsDisablesFuturePasswordOptInWithoutExportingSecrets(){
    Context c=RuntimeEnvironment.getApplication();
    AppPreferences.set(c,"remember_credentials_optin","on");
    assertTrue(AppPreferences.visible(c,"remember_credentials_optin",false));
    SettingsBackup.reset(c);
    assertFalse(AppPreferences.visible(c,"remember_credentials_optin",false));
    assertFalse(SettingsBackup.allowed("remember_credentials_optin"));
  }
  @Test public void safeAggregateMetricsAreProcessLocalAndUnidentified(){
    TelemetryDiagnostics.reset();
    TelemetryDiagnostics.recordLogin(true,500);
    TelemetryDiagnostics.recordScoped(false,1000);
    String report=TelemetryDiagnostics.report();
    assertTrue(report.contains("1/0"));assertTrue(report.contains("0/1"));
    assertTrue(report.contains("750ms"));
    assertFalse(report.contains("password"));assertFalse(report.contains("VIN="));
    TelemetryDiagnostics.reset();assertTrue(TelemetryDiagnostics.report().contains("0ms"));
  }
  @Test public void signOutRecoveryCanLogInButActiveCampAccountSwitchCannot(){
    assertTrue(LoginSafetyGate.canBegin(false,false,false,true,true,true));
    assertFalse(LoginSafetyGate.canBegin(true,false,false,true,false,false));
    assertFalse(LoginSafetyGate.canBegin(true,false,false,false,true,false));
    assertFalse(LoginSafetyGate.canBegin(true,false,false,false,false,true));
    assertFalse(LoginSafetyGate.canBegin(false,true,false,false,false,false));
    assertFalse(LoginSafetyGate.canBegin(false,false,true,false,false,false));
    assertTrue(LoginSafetyGate.canBegin(true,false,false,false,false,false));
  }
  @Test public void loginErrorMessagesCannotExposePlainCredentials(){
    String sanitized=VehicleCoordinator.safe(new Exception("password=SECRET-PASS refresh_token=SECRET-TOKEN"));
    assertFalse(sanitized.contains("SECRET-PASS"));
    assertFalse(sanitized.contains("SECRET-TOKEN"));
    assertTrue(sanitized.contains("[redacted]"));
  }
  @Test public void diagnosticLogRedactsAccountSecretsAndTokens(){
    DiagnosticLog.add("password=ABC12345 accessToken=SECRET123 refresh_token=SECRET456");
    String exported=DiagnosticLog.export();
    for(String forbidden:new String[]{"ABC12345","SECRET123","SECRET456"})assertFalse(exported.contains(forbidden));
    assertTrue(exported.contains("[redacted]"));
  }
}
