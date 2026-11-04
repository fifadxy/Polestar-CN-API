package io.github.polestarcn.companion;
import java.util.Locale;
/** Network errors are pauses, while definitively rejected refresh credentials require sign-in. */
public final class CampRecovery {
  private CampRecovery(){}
  public static String classify(Exception e){
    if(e==null)return "CHECK_UNAVAILABLE";
    String reason=String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
    if(reason.contains("http 401")||reason.contains("http 403")
        ||reason.contains("刷新令牌被服务端拒绝")||reason.contains("refresh token")
        ||reason.contains("尚未登录")||reason.contains("令牌已过期"))return "LOGIN_REQUIRED";
    if(reason.contains("network")||reason.contains("unavailable")||reason.contains("timeout")
        ||reason.contains("timed out")||reason.contains("socket")||reason.contains("connect")
        ||reason.contains("unknownhost")||reason.contains("deadline")||reason.contains("网络"))
      return "WAITING_FOR_NETWORK";
    return "CHECK_UNAVAILABLE";
  }
}
