package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Bounded runtime observations of MyDevices QuickApp widget replacement state. */
final class OppoQuickAppRuntimeDiagnostics {
  private static final String PREF="oppo-quickapp-runtime-diagnostics";
  private OppoQuickAppRuntimeDiagnostics() {}

  static Bundle record(Context context, Bundle in) {
    if(in==null)throw new IllegalArgumentException("Missing QuickApp runtime report");
    SharedPreferences.Editor e=context.getSharedPreferences(PREF,0).edit();
    e.putLong("seenAt",System.currentTimeMillis());
    e.putBoolean("ready",in.getBoolean("ready",false));
    for(String key:new String[]{"hostVersion","moduleVersion","phase","method","widgetCode",
        "oldWidgetCode","replacedWidgetCode","cardPath","reason"}) {
      if(!in.containsKey(key))continue;
      String value=in.getString(key,"");
      if(value==null)value="";
      e.putString(key,value.substring(0,Math.min(value.length(),600)));
    }
    for(String key:new String[]{"single4x2CarRegistry","single2x2CarRegistry",
        "single4x2EventSummaryCar","single2x2EventSummaryCar"})
      if(in.containsKey(key))e.putBoolean(key,in.getBoolean(key,false));
    e.apply();
    return status(context);
  }

  static Bundle status(Context context) {
    SharedPreferences p=context.getSharedPreferences(PREF,0);
    Bundle out=new Bundle();
    out.putLong("seenAt",p.getLong("seenAt",0L));
    out.putBoolean("ready",p.getBoolean("ready",false));
    for(String key:new String[]{"hostVersion","moduleVersion","phase","method","widgetCode",
        "oldWidgetCode","replacedWidgetCode","cardPath","reason"})
      out.putString(key,p.getString(key,""));
    for(String key:new String[]{"single4x2CarRegistry","single2x2CarRegistry",
        "single4x2EventSummaryCar","single2x2EventSummaryCar"})
      out.putBoolean(key,p.getBoolean(key,false));
    return out;
  }
}
