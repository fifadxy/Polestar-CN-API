package io.github.polestarcn.companion;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
/** Last observed host reports, not proof that a module is currently enabled. */
final class OppoHostDiagnostics {
  private static final String PREF="oppo-host-diagnostics";
  static Bundle record(Context c,Bundle in) {
    if(in==null)throw new IllegalArgumentException("Missing host report");
    String process=in.getString("process","");
    String prefix="com.heytap.mydevices".equals(process)?"main_":"com.heytap.mydevices:cards".equals(process)?"cards_":null;
    if(prefix==null)throw new IllegalArgumentException("Unsupported host process");
    String phase=in.getString("phase","");
    if(!phase.equals("hooks-installed")&&!phase.equals("card-added")&&!phase.equals("install-failed"))throw new IllegalArgumentException("Unsupported host phase");
    SharedPreferences.Editor e=c.getSharedPreferences(PREF,0).edit();
    e.putString(prefix+"phase",phase).putBoolean(prefix+"ready",in.getBoolean("ready",false)).putLong(prefix+"seenAt",System.currentTimeMillis());
    for(String key:new String[]{"hostVersion","moduleVersion","reason"}) {
      String value=in.getString(key,"");e.putString(prefix+key,value==null?"":value.substring(0,Math.min(value.length(),120)));
    }
    e.apply();return status(c);
  }
  static Bundle status(Context c) {
    SharedPreferences p=c.getSharedPreferences(PREF,0);Bundle out=new Bundle();
    for(String prefix:new String[]{"main_","cards_"}) {
      out.putLong(prefix+"seenAt",p.getLong(prefix+"seenAt",0));out.putBoolean(prefix+"ready",p.getBoolean(prefix+"ready",false));
      for(String key:new String[]{"phase","hostVersion","moduleVersion","reason"})out.putString(prefix+key,p.getString(prefix+key,""));
    }
    return out;
  }
}
