package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
@org.robolectric.annotation.Config(sdk=28)
@RunWith(RobolectricTestRunner.class)
public final class ControlApprovalHotfixTest {
 @Test public void defaultsToBiometricAndKeepsEveryActionInItsOwnFamily(){
  android.content.Context c=RuntimeEnvironment.getApplication();
  c.getSharedPreferences("preferences",0).edit().clear().commit();
  for(String s:new String[]{"lock","unlock","trunk-close","climate-start","air-stop","windows-open","honk","camp-start"})
   assertEquals(ControlApproval.BIOMETRIC,ControlApproval.mode(c,s));
  AppPreferences.set(c,ControlApproval.key("unlock"),ControlApproval.CONFIRM);
  assertEquals(ControlApproval.CONFIRM,ControlApproval.mode(c,"lock"));
  assertEquals(ControlApproval.BIOMETRIC,ControlApproval.mode(c,"climate-stop"));
  AppPreferences.set(c,ControlApproval.key("climate-start"),ControlApproval.DIRECT);
  assertEquals(ControlApproval.DIRECT,ControlApproval.mode(c,"climate-stop"));
  AppPreferences.set(c,ControlApproval.key("camp-stop"),ControlApproval.CONFIRM);
  assertEquals(ControlApproval.CONFIRM,ControlApproval.mode(c,"camp-start"));
 }
}
