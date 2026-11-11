package io.github.polestarcn.oppobridge;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * User-confirmed, bounded root action for known OPPO car-card hosts only.
 * Never kills SystemUI, launcher, LSPosed, Magisk, PsAssistant, or Zygote.
 * No wildcard process name, shell text supplied by users, package-data clear,
 * permission changes, or bypass of OEM authentication.
 */
public final class BridgeHostRestarter {
  private static final String[] TARGETS={
      "com.heytap.mydevices",
      "com.coloros.assistantscreen","com.oplus.assistantscreen",
      "com.heytap.opluscarlink","com.oplus.carlink","com.oppo.carlink",
      "com.oplus.ocar","com.nearme.instant.platform"
  };
  private BridgeHostRestarter(){}

  public static String[] targets(){return TARGETS.clone();}
  public static boolean approved(String pkg){
    if(pkg==null)return false;
    for(String target:TARGETS)if(target.equals(pkg))return true;
    return false;
  }
  static String quote(String value){
    if(value==null||!value.matches("[A-Za-z0-9_.$/:]{1,250}"))
      throw new IllegalArgumentException("Unsafe host component");
    return "'"+value+"'";
  }
  static String command(String pkg,String launcher){
    if(!approved(pkg))throw new IllegalArgumentException("Host not allowlisted");
    String name=launcher==null?"":launcher;
    if(!name.isEmpty()&&!name.startsWith(pkg+"/"))
      throw new IllegalArgumentException("Launcher is not owned by host");
    // Hosts without launchers are allowed to restart on the next real host
    // access; never force-stop such packages into a persistent stopped state.
    return name.isEmpty()?"am kill --user current "+quote(pkg)
        :"am force-stop --user current "+quote(pkg)+" && am start --user current -n "+quote(name);
  }
  static String shell(String command)throws Exception{
    Process process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
    ByteArrayOutputStream bytes=new ByteArrayOutputStream(2048);
    Thread drain=new Thread(()->{
      try(InputStream input=process.getInputStream()){
        byte[] buf=new byte[512];int n;
        while((n=input.read(buf))!=-1) {
          int remaining=2048-bytes.size();
          if(remaining>0)bytes.write(buf,0,Math.min(remaining,n));
        }
      }catch(IOException ignored){}
    },"psa-bridge-root-drain");
    drain.setDaemon(true);drain.start();
    boolean finished=process.waitFor(8,TimeUnit.SECONDS);
    if(!finished){process.destroyForcibly();drain.join(300);throw new IOException("Timed out");}
    drain.join(600);
    if(process.exitValue()!=0)throw new IOException("Exit "+process.exitValue());
    return bytes.toString("UTF-8").trim();
  }
  /** Must be called from a background thread after a user gesture. */
  public static String rootStatus(){
    try{return "0".equals(shell("id -u").trim())?"ROOT_GRANTED":"ROOT_DENIED";}
    catch(Exception e){return "ROOT_UNAVAILABLE";}
  }
  /** No call to this method should be made on Activity.onCreate or from a Hook. */
  public static String restart(Context context){
    if(!"ROOT_GRANTED".equals(rootStatus()))
      return "Root 未授权；请在 Magisk 中允许 PsAssistant/Bridge 的 su 请求。";
    StringBuilder result=new StringBuilder("OPPO 宿主重载（用户确认）\n");
    int launched=0,refreshRequested=0,missing=0;
    for(String pkg:TARGETS){
      try {
        // Root pm path avoids Android 11+ package visibility false negatives.
        String installed=shell("pm path "+quote(pkg));
        if(!installed.startsWith("package:")){result.append(pkg).append(": 未安装\n");missing++;continue;}
        Intent intent=context.getPackageManager().getLaunchIntentForPackage(pkg);
        ComponentName component=intent==null?null:intent.getComponent();
        String launcher=component==null||!pkg.equals(component.getPackageName())
            ?null:component.flattenToString();
        shell(command(pkg,launcher));
        if(launcher!=null){result.append(pkg).append(": 已停止并重新启动入口\n");launched++;}
        else {result.append(pkg).append(": 已请求进程重载；无公开启动入口，待系统调用自动恢复\n");refreshRequested++;}
      }catch(Exception error){
        result.append(pkg).append(": 重载未完成 (").append(error.getClass().getSimpleName()).append(")\n");
      }
    }
    result.append("重新启动入口=").append(launched)
        .append("; 后台重载请求=").append(refreshRequested)
        .append("; 未安装=").append(missing)
        .append("\n请重新打开设备空间/负一屏检查 Hook 实时回报。进程重载不代表车辆绑定或远控已成功。");
    return result.toString();
  }
}
