package io.github.polestarcn.companion;
import android.content.Context;
/** Per-action user choice. Existing installations keep strong biometrics by default. */
public final class ControlAuthorization {
 public static final String[] ACTIONS={"unlock","lock","trunk-open","trunk-close","climate-start","climate-stop","air-start","air-stop","windows-open","windows-close","honk","flash","honk-flash","camp-start","camp-stop"};
 public static final String[] MODES={"biometric","confirm","direct"};
 private ControlAuthorization(){}
 public static String mode(Context c,String action){String value=AppPreferences.get(c,"control_action_auth_"+action,"biometric");for(String mode:MODES)if(mode.equals(value))return mode;return "biometric";}
 public static String label(Context c,String mode){return AppPreferences.tr(c,"direct".equals(mode)?"直接发送":"confirm".equals(mode)?"二次确认":"生物识别","direct".equals(mode)?"Send directly":"confirm".equals(mode)?"Confirm dialog":"Biometrics");}
 public static String actionLabel(Context c,String action){String[] zh={"解锁车门","锁定车门","打开尾箱","关闭尾箱","开启空调","关闭空调","开启净化","关闭净化","打开车窗","关闭车窗","鸣笛","闪灯","鸣笛并闪灯","开启露营","结束露营"};String[] en={"Unlock doors","Lock doors","Open trunk","Close trunk","Start climate","Stop climate","Start purification","Stop purification","Open windows","Close windows","Honk","Flash","Honk and flash","Start camping","End camping"};int i=java.util.Arrays.asList(ACTIONS).indexOf(action);return i<0?action:AppPreferences.tr(c,zh[i],en[i]);}
}
