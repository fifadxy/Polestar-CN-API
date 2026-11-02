package io.github.polestarcn.companion;
import android.content.Context;import android.content.SharedPreferences;import android.os.Bundle;
/** Last observed OPPO CarLink LSPosed host/module load. */
final class OppoCarLinkHostDiagnostics {
  private static final String PREF="oppo-carlink-host-diagnostics";private OppoCarLinkHostDiagnostics(){}
  static Bundle record(Context context,Bundle in){if(in==null)throw new IllegalArgumentException("Missing CarLink host report");SharedPreferences.Editor e=context.getSharedPreferences(PREF,0).edit();e.putLong("seenAt",System.currentTimeMillis());e.putBoolean("ready",in.getBoolean("ready",false));e.putLong("versionCode",in.getLong("versionCode",-1L));for(String key:new String[]{"phase","hostVersion","moduleVersion","process","reason","classes"}){String v=in.getString(key,"");if(v==null)v="";e.putString(key,v.substring(0,Math.min(v.length(),1200)));}e.apply();return status(context);}
  static Bundle status(Context context){SharedPreferences p=context.getSharedPreferences(PREF,0);Bundle out=new Bundle();out.putLong("seenAt",p.getLong("seenAt",0L));out.putBoolean("ready",p.getBoolean("ready",false));out.putLong("versionCode",p.getLong("versionCode",-1L));for(String key:new String[]{"phase","hostVersion","moduleVersion","process","reason","classes"})out.putString(key,p.getString(key,""));return out;}
}