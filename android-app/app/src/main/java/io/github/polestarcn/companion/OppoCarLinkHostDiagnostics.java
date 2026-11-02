package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Last observed OPPO CarLink host/module load. Contains no key or vehicle secret. */
final class OppoCarLinkHostDiagnostics {
  private static final String PREF="oppo-carlink-host-diagnostics";
  private OppoCarLinkHostDiagnostics() {}

  static Bundle record(Context context,Bundle in) {
    if(in==null)throw new IllegalArgumentException("Missing CarLink host report");
    SharedPreferences.Editor e=context.getSharedPreferences(PREF,0).edit();
    e.putLong("seenAt",System.currentTimeMillis());
    e.putBoolean("ready",in.getBoolean("ready",false));
    for(String key:new String[]{"phase","hostVersion","moduleVersion","process","reason"}) {
      String value=in.getString(key,"");
      if(value==null)value="";
      e.putString(key,value.substring(0,Math.min(value.length(),160)));
    }
    e.apply();
    return status(context);
  }

  static Bundle status(Context context) {
    SharedPreferences p=context.getSharedPreferences(PREF,0);
    Bundle out=new Bundle();
    out.putLong("seenAt",p.getLong("seenAt",0L));
    out.putBoolean("ready",p.getBoolean("ready",false));
    for(String key:new String[]{"phase","hostVersion","moduleVersion","process","reason"})
      out.putString(key,p.getString(key,""));
    return out;
  }
}
