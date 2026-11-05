package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Read-only host activation report: a selected mode is not an active mode until host restarts. */
public final class OppoBridgeModeDiagnostics {
  private static final String PREFS="oppo-bridge-mode-diagnostics-v1";
  private OppoBridgeModeDiagnostics(){}

  public static boolean validHost(String pkg){
    if(pkg==null)return false;
    return pkg.equals("com.heytap.mydevices")||pkg.equals("com.coloros.assistantscreen")
        ||pkg.equals("com.oplus.assistantscreen")||pkg.equals("com.heytap.opluscarlink")
        ||pkg.equals("com.oplus.carlink")||pkg.equals("com.oppo.carlink")
        ||pkg.equals("com.coloros.sceneservice")||pkg.equals("com.oplus.sceneservice")
        ||pkg.equals("com.oplus.ocar")||pkg.equals("com.oplus.smartengine")||pkg.equals("com.oplus.cardservice")
        ||pkg.equals("com.nearme.instant.platform");
  }
  public static Bundle record(Context c,Bundle report){
    Bundle out=new Bundle();
    if(report==null)return out;
    String pkg=report.getString("packageName","");
    String proc=report.getString("process","");
    String mode=report.getString("mode","");
    if(!validHost(pkg)||!proc.equals(pkg)&&!proc.startsWith(pkg+":"))return out;
    if(!mode.equals("stable")&&!mode.equals("new")&&!mode.equals("mock"))return out;
    long now=System.currentTimeMillis();
    // Persist only process diagnostics; no vehicle identity, account or auth metadata.
    c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
       .putString(pkg+"|"+proc,mode).putLong(pkg+"|"+proc+"|at",now).apply();
    out.putBoolean("accepted",true);return out;
  }
  public static Bundle status(Context c){
    Bundle result=new Bundle();
    SharedPreferences prefs=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    for(String pkg:new String[]{"com.heytap.mydevices","com.coloros.assistantscreen",
        "com.oplus.assistantscreen","com.heytap.opluscarlink","com.oplus.carlink",
        "com.oppo.carlink","com.coloros.sceneservice","com.oplus.sceneservice"}){
      for(String process:new String[]{pkg,pkg+":cards"}){
        String key=pkg+"|"+process;
        long observed=prefs.getLong(key+"|at",0);
        if(observed>0&&observed<=System.currentTimeMillis()){
          result.putString(key,prefs.getString(key,"unknown"));result.putLong(key+"|at",observed);
        }
      }
    }
    return result;
  }
  public static String description(Context c,String selected){
    StringBuilder out=new StringBuilder();Bundle b=status(c);long now=System.currentTimeMillis();
    for(String key:b.keySet())if(!key.endsWith("|at")){
      long at=b.getLong(key+"|at",0);boolean recent=at>0&&now-at<6*60*60_000L;
      String seen=b.getString(key,"?");
      out.append(key).append(" = ").append(seen)
         .append(recent?"（已观测）":"（历史记录）")
         .append(selected.equals(seen)?"":" ⚠与已选择模式不同，需重启宿主")
         .append('\n');
    }
    if(out.length()==0)out.append("尚无宿主加载回报；请在 LSPosed 激活并重启目标应用后检查。");
    return out.toString();
  }
}
