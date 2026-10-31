package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** One process-local export, survives setup activity recreation; never a persistent service. */
final class OppoResearchExportJob {
  interface Listener { void changed(State state); }
  static final class State {
    final boolean running;final String message;final File file;
    State(boolean running,String message,File file){this.running=running;this.message=message;this.file=file;}
  }
  private static final Handler MAIN=new Handler(Looper.getMainLooper());
  private static volatile State state=new State(false,"点击生成适配资料包，完成后可分享或另存。",null);
  private static volatile Listener listener;
  private OppoResearchExportJob() {}
  static void observe(Listener next){listener=next;dispatch();}
  static void remove(Listener previous){if(listener==previous)listener=null;}
  static State current(){return state;}
  private static void dispatch(){MAIN.post(()->{Listener current=listener;if(current!=null)current.changed(state);});}
  private static void publish(State next){state=next;dispatch();}
  static synchronized boolean start(Context context) {
    if(state.running)return false;
    Context app=context.getApplicationContext();publish(new State(true,"准备生成适配资料包…",null));
    new Thread(()->{
      File temporary=null;
      try {
        File directory=new File(app.getCacheDir(),"oppo-research");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create export directory");
        temporary=new File(directory,"research.zip.part");
        OppoHostApkExport.Result result;
        try(OutputStream output=new FileOutputStream(temporary)) {
          result=OppoLightResearchExport.write(app,output,message->publish(new State(true,message,null)));
        }
        File file=new File(directory,"PsAssistant-OPPO-light.zip");
        Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);
        publish(new State(false,(result.complete?"轻量资料包已生成":"轻量资料包已生成，部分组件不可见；详情已标注")+
            "；"+file.length()+" 字节（上限 2 MB），可分享分析。",file));
      } catch(Exception failure) {
        if(temporary!=null)temporary.delete();
        publish(new State(false,"生成失败："+failure.getClass().getSimpleName()+"。请确认剩余存储空间后重试。",null));
      }
    },"oppo-research-export").start();return true;
  }
}
