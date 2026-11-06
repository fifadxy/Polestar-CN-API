package io.github.polestarcn.companion;

import android.content.Context;

/** A remote command is reported as successful only after a matching LIVE vehicle readback. */
final class CommandResultLabel {
  private CommandResultLabel(){}
  static String confirmed(Context c,String action){
    if(action==null)return AppPreferences.tr(c,"执行成功","Confirmed");
    switch(action){
      case "lock":return AppPreferences.tr(c,"已上锁","Locked");
      case "unlock":return AppPreferences.tr(c,"已解锁","Unlocked");
      case "climate-start":return AppPreferences.tr(c,"空调已开启","Climate started");
      case "climate-stop":return AppPreferences.tr(c,"空调已关闭","Climate stopped");
      case "air-start":return AppPreferences.tr(c,"净化已开启","Purification started");
      case "air-stop":return AppPreferences.tr(c,"净化已关闭","Purification stopped");
      case "trunk-open":return AppPreferences.tr(c,"尾门已打开","Tailgate opened");
      case "trunk-close":return AppPreferences.tr(c,"尾门已关闭","Tailgate closed");
      case "windows-open":return AppPreferences.tr(c,"车窗已打开","Windows opened");
      case "windows-close":return AppPreferences.tr(c,"车窗已关闭","Windows closed");
      default:return AppPreferences.tr(c,"执行成功","Confirmed");
    }
  }
}
