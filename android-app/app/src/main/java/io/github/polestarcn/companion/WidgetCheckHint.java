package io.github.polestarcn.companion;

import android.content.Context;

/** VI-orange Check hint near the Widget's last-update timestamp. */
final class WidgetCheckHint {
  static final String PREF="widget_check_hint";
  private WidgetCheckHint(){}
  static boolean visible(Context c,VehicleSnapshot s){
    return AppPreferences.visible(c,PREF,true)
        &&s!=null&&!s.isDemo&&VehicleCondition.problem(s,AppPreferences.visible(c,"show_tire_status",true));
  }
}
