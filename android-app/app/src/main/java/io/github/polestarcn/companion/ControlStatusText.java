package io.github.polestarcn.companion;

import android.content.Context;

/** Short, stable control-state wording shared by the dashboard and utility buttons. */
public final class ControlStatusText {
  private ControlStatusText() {}

  public static String value(Context c,String control,VehicleSnapshot s,String fallback){
    if("camp".equals(control)){
      if(CampModeManager.active(c)||"STOPPING".equals(CampModeManager.phase(c)))
        return CampModeManager.remainingCaption(c);
      if(CampModeManager.pendingStop(c))return AppPreferences.tr(c,"待确认","Verify");
      return AppPreferences.tr(c,"未托管","Not managed");
    }
    if(s==null)return fallback==null||fallback.isEmpty()?"--":fallback;
    String raw=controlRaw(control,s);
    if("lock".equals(control)){
      if("已上锁".equals(raw)||"已锁".equals(raw))return AppPreferences.tr(c,"已锁","Locked");
      if("未锁".equals(raw))return AppPreferences.tr(c,"未锁","Unlocked");
    }
    if("climate".equals(control)||"air".equals(control)){
      if("开启".equals(raw))return AppPreferences.tr(c,"运行中","Running");
      if("关闭".equals(raw))return AppPreferences.tr(c,"未运行","Not running");
    }
    if("windows".equals(control)){
      if("全部关闭".equals(raw)||"已关闭".equals(raw))return AppPreferences.tr(c,"已关","Closed");
      if("已打开".equals(raw)||"未关严".equals(raw)||raw.contains("个开启"))
        return AppPreferences.tr(c,"未关","Open");
    }
    return AppPreferences.status(c,raw);
  }

  private static String controlRaw(String control,VehicleSnapshot s){
    if("windows".equals(control))return s.windows;
    if("lock".equals(control))return s.lock;
    if("trunk".equals(control))return s.tailgate;
    if("climate".equals(control))return s.climate;
    if("air".equals(control))return s.air;
    return "";
  }
}