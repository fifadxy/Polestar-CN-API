package io.github.polestarcn.companion;
import android.content.Context;
/** User-selected per-command-family authorization. Does not bypass session or first-use consent. */
public final class ControlApproval {
  private ControlApproval(){}
  public static final String BIOMETRIC="biometric",CONFIRM="confirm",DIRECT="direct";
  public static final String[] GROUPS={"lock","climate","air","trunk","windows","signal","camp"};
  public static String group(String action) {
    if(action!=null&&action.startsWith("camp-"))return "camp";
    return CommandSlots.group(action);
  }
  public static String key(String action){return "control_auth_"+group(action);}
  public static String mode(Context c,String action) {
    String result=AppPreferences.get(c,key(action),BIOMETRIC);
    return CONFIRM.equals(result)||DIRECT.equals(result)?result:BIOMETRIC;
  }
  public static boolean requiresBiometrics(Context c,String action){return BIOMETRIC.equals(mode(c,action));}
}
