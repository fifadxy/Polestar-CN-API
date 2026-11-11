package io.github.polestarcn.companion;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Exact process-name root restart, never a device or SystemUI reboot. */
final class OppoHookProcessManager {
  private OppoHookProcessManager(){}
  static String command(){
    StringBuilder b=new StringBuilder("changed=0; ");
    for(String host:OppoHookDoctor.HOSTS){
      if(!OppoHookDoctor.allowed(host))throw new IllegalStateException("Unlisted host");
      // Fixed app package names, no user-controlled shell argument or wildcards.
      b.append("for p in $(pidof ").append(host).append(" 2>/dev/null); do ")
          .append("case \"$p\" in *[!0-9]*|'') continue;; esac; ")
          .append("kill -TERM \"$p\" 2>/dev/null && changed=$((changed+1)); done; ");
    }
    b.append("echo restarted_processes=$changed");
    return b.toString();
  }
  private static String run(String shell,int seconds)throws Exception{
    Process process=new ProcessBuilder("su","-c",shell).redirectErrorStream(true).start();
    StringBuilder output=new StringBuilder();
    Thread reader=new Thread(()->{
      try(BufferedReader lines=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){
        String line;while((line=lines.readLine())!=null){
          if(output.length()<2048)output.append(line,0,Math.min(line.length(),256)).append('\n');
        }
      }catch(Exception ignored){}
    },"psa-su-stdout");
    reader.setDaemon(true);reader.start();
    if(!process.waitFor(seconds,TimeUnit.SECONDS)){process.destroyForcibly();throw new java.io.IOException("su timed out");}
    reader.join(500);
    if(process.exitValue()!=0)throw new java.io.IOException("su refused or command failed");
    return output.toString().trim();
  }
  static String rootProbe(){
    try{return "0".equals(run("id -u",3))?"ROOT_GRANTED":"ROOT_UNAVAILABLE";}
    catch(Exception e){return "ROOT_UNAVAILABLE:"+e.getClass().getSimpleName();}
  }
  static String restart(){
    if(!"ROOT_GRANTED".equals(rootProbe()))return "Root未授权；未终止任何进程";
    try{return run(command(),12)+"\n仅重启命中的OPPO宿主进程；未停止SystemUI或PsAssistant。请重新打开设备空间/负一屏再运行诊断。";}
    catch(Exception e){return "进程重启失败："+e.getClass().getSimpleName();}
  }
}
