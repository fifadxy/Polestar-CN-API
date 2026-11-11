package io.github.polestarcn.companion;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Read-only automatic LSPosed failure triage. No raw logcat, user databases,
 * VIN, credentials, vehicle commands, or hidden package data are exported.
 */
final class OppoHookDoctor {
  static final String[] HOSTS={
      "com.heytap.mydevices","com.coloros.assistantscreen","com.oplus.assistantscreen",
      "com.heytap.opluscarlink","com.oplus.carlink","com.oppo.carlink",
      "com.coloros.sceneservice","com.oplus.sceneservice",
      "com.oplus.smartengine","com.oplus.cardservice","com.nearme.instant.platform"};
  private static final int REPORT_LIMIT=40_000;
  private OppoHookDoctor(){}

  static boolean allowed(String packageName){
    if(packageName==null)return false;
    for(String host:HOSTS)if(host.equals(packageName))return true;
    return false;
  }
  static String sanitized(String raw){
    if(raw==null)return "";
    String text=raw.replaceAll("(?i)(access[_-]?token|refresh[_-]?token|auth[_-]?code|password|secret|cookie|authorization)\\s*[=:]\\s*[^\\s;,]+","$1=[REDACTED]");
    text=text.replaceAll("(?i)\\b[A-HJ-NPR-Z0-9]{17}\\b","[VEHICLE-REDACTED]");
    text=text.replaceAll("(?i)\\b1[3-9][0-9]{9}\\b","[PHONE-REDACTED]");
    text=text.replaceAll("(?i)(virtualCarId|vehicleId|vin)\\s*[=:]\\s*[^\\s;,]+","$1=[REDACTED]");
    text=text.replaceAll("(?m)(Virtual Vehicle Session[^\\n]*?age=[^\\n]*?s)\\s*·[^\\n]*","$1 · [VEHICLE-ID-REDACTED]");
    text=text.replaceAll("(?m)(真实车辆只读状态\\s*·)\\s*[^\\n]*","$1 [VEHICLE-NAME-REDACTED]");
    return text.length()>REPORT_LIMIT?text.substring(0,REPORT_LIMIT)+"\\n[TRUNCATED]":text;
  }
  private static String version(Context c,String pkg){
    try{
      PackageInfo info=c.getPackageManager().getPackageInfo(pkg,0);
      return info.versionName==null?"installed":info.versionName;
    }catch(PackageManager.NameNotFoundException | SecurityException unavailable){
      return "not-installed-or-not-visible";
    }
  }
  private static String selectedMode(Context c){
    try{
      android.os.Bundle mode=c.getContentResolver().call(
          android.net.Uri.parse("content://io.github.polestarcn.companion.bridge-settings"),
          "snapshot",null,null);
      if(mode!=null)return mode.getString("compatibilityMode","stable");
    }catch(RuntimeException ignored){}
    return "unknown";
  }
  static String diagnosis(Context c){
    StringBuilder out=new StringBuilder("PsAssistant LSPosed Auto Diagnosis v1\n");
    out.append("App=").append(BuildConfig.VERSION_NAME).append(" Bridge=")
        .append("2.7.1")
        .append(" Android=").append(Build.VERSION.SDK_INT).append('\n');
    out.append("Device=").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append('\n');
    out.append("Mode=").append(selectedMode(c)).append('\n');
    out.append("Root=").append(OppoHookProcessManager.rootProbe()).append('\n');
    for(String host:HOSTS)out.append("HOST ").append(host).append(" version=")
        .append(version(c,host)).append('\n');
    out.append("\nHOOK mode observations (historical observations may be stale):\n")
        .append(OppoBridgeModeDiagnostics.description(c,
            selectedMode(c))).append('\n');
    out.append("\nBridge self-test (read-only):\n");
    try {out.append(io.github.polestarcn.oppobridge.BridgeTestOrchestrator.runReport(c));}
    catch(Exception ex){out.append("probe_error=").append(ex.getClass().getSimpleName());}
    String observed=OppoBridgeModeDiagnostics.description(c,selectedMode(c));
    out.append("\\nActionable diagnosis:\\n");
    if(!"ROOT_GRANTED".equals(OppoHookProcessManager.rootProbe()))
      out.append("[CHECK] Root unavailable; allow PsAssistant in Magisk only for authorized process management.\\n");
    if(observed.contains("尚无宿主加载回报"))
      out.append("[CHECK] Enable one LSPosed module, select OPPO host scopes, restart hosts and open MyDevices/CarLink.\\n");
    else if(observed.contains("需重启宿主"))
      out.append("[CHECK] Selected and installed Hook modes differ; restart the listed hosts.\\n");
    out.append("\nOfficial-vs-local path (separate evidence):\n");
    try{
      android.os.Bundle b=OppoDualPathStatus.snapshot(c);
      for(String key:new String[]{"officialPath","officialAuthorized","localVisible",
          "localCommandReady","cachedOnly","localLifecycle"})
        out.append(key).append('=').append(b.get(key)).append('\n');
    }catch(RuntimeException error){out.append("path_error=").append(error.getClass().getSimpleName()).append('\n');}
    try{
      android.os.Bundle path=OppoDualPathStatus.snapshot(c);
      if(!path.getBoolean("localVisible",false))
        out.append("[CHECK] Sign in to PsAssistant, select vehicle and enable OPPO integration.\\n");
      else if(!path.getBoolean("localCommandReady",false))
        out.append("[CHECK] Refresh real vehicle telemetry; cached state cannot authorize writes.\\n");
      if(!path.getBoolean("officialAuthorized",false))
        out.append("[INFO] OPPO official SDK authorization not confirmed; a local card is not OEM binding.\\n");
    }catch(RuntimeException ignored){}
    out.append("\nOperational checks:\n")
        .append("Fluid default=").append(OppoFluidPolicy.enabled(c))
        .append(" notification scenes=").append(OppoFluidPolicy.notify(c,"charging")).append('\n')
        .append("Dynamic refresh=").append(OppoAdaptiveRefresh.enabled(c)).append('\n')
        .append("Note: 'hook installed', card drawn and SDK authorized are distinct.\n")
        .append("No personal account, token, VIN, raw system logs or real car command included.\n");
    return sanitized(out.toString());
  }
  static File export(Context c,String content)throws Exception{
    File dir=new File(c.getCacheDir(),"oppo-research");
    if(!dir.isDirectory()&&!dir.mkdirs())throw new java.io.IOException("Cannot create report directory");
    File f=new File(dir,"PsAssistant-LSPosed-Diagnosis.txt");
    Files.write(f.toPath(),sanitized(content).getBytes(StandardCharsets.UTF_8));
    return f;
  }
}
