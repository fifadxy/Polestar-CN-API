package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.app.Application;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** NEW-mode AssistantScreen action interception limited to own custom URI and host process.
 * No optimistic vehicle states: a command is only accepted/queued until server readback completes.
 */
final class AdaptiveShelfShortcutInterceptor {
  private static final ExecutorService IO=Executors.newSingleThreadExecutor();
  private static final Handler MAIN=new Handler(Looper.getMainLooper());
  private static final Set<String> PENDING=new HashSet<>();
  private static final String[] COMMANDS={"lock-toggle","climate-toggle","air-toggle","trunk-toggle",
    "lock","unlock","climate-start","climate-stop","air-start","air-stop",
    "trunk-open","trunk-close","windows-open","windows-close","flash","honk","honk-flash"};
  private AdaptiveShelfShortcutInterceptor(){}

  static String requestedAction(Intent i){
    if(i==null||!Intent.ACTION_VIEW.equals(i.getAction()))return null;
    Uri u=i.getData();if(u==null)return null;
    String scheme=u.getScheme(),host=u.getHost();
    if(!"command".equals(host)||!("psassistant-companion".equals(scheme)||"psassistant-oppo-bridge".equals(scheme)))return null;
    String action=u.getQueryParameter("action");
    if(action==null||action.length()>40)return null;
    for(String allowed:COMMANDS)if(allowed.equals(action))return action;
    return null;
  }
  static void install(OppoBridgeModule module){
    // Called only from com.coloros.assistantscreen's main process and NEW mode.
    for(Class<?> cls:new Class<?>[]{Activity.class,ContextWrapper.class}){
      for(Method method:cls.getDeclaredMethods()){
        if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=void.class)continue;
        if(!"startActivity".equals(method.getName())&& !"startActivityForResult".equals(method.getName()))continue;
        if(method.getParameterCount()==0||method.getParameterTypes()[0]!=Intent.class)continue;
        try {
          method.setAccessible(true);
          module.hook(method).setId("psassistant_beta46_shelf_click_"+cls.getSimpleName()+"_"+method.getName()+"_"+method.getParameterCount())
              .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
              .intercept(chain->{
                List<Object> args=chain.getArgs();
                String requested=args!=null&&!args.isEmpty()&&args.get(0) instanceof Intent
                    ?requestedAction((Intent)args.get(0)):null;
                if(requested==null)return chain.proceed();
                Application app=currentApplication();
                if(app==null)return chain.proceed();
                // This intercept is not an OEM permission or binding check.
                dispatch(app,requested);
                return null;
              });
        }catch(Throwable ignored){module.warn("beta46_shelf_click_signature_changed="+method.getName());}
      }
    }
  }
  private static void dispatch(Application app,String request){
    synchronized(PENDING){if(!PENDING.add(request))return;}
    IO.execute(()->{
      try {
        Bundle state=BridgeClient.snapshot(app);
        if(!freshAndReal(state)){
          toast(app,"极星车控：正在刷新真实车况…");
          BridgeClient.refresh(app);
          long deadline=android.os.SystemClock.elapsedRealtime()+12_000L;
          while(android.os.SystemClock.elapsedRealtime()<deadline&&!freshAndReal(state)){
            try{Thread.sleep(1100L);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();return;}
            state=BridgeClient.snapshot(app);
          }
        }
        if(!freshAndReal(state)){
          toast(app,"车况未确认，请打开 PsAssistant 操作");return;
        }
        // Compare against a second read immediately before dispatch to prevent cross-VIN
        // or stale toggle direction if vehicle selection changes during the refresh wait.
        Bundle latest=BridgeClient.snapshot(app);
        if(!freshAndReal(latest)||!state.getString("vehicleKey","").equals(latest.getString("vehicleKey",""))){
          toast(app,"当前车辆已变化，操作取消");return;
        }
        String action=VehicleShortcutActivity.resolveToggle(request,latest);
        if(action==null){toast(app,"车辆状态不足，未执行操作");return;}
        Bundle result=BridgeClient.command(app,action);
        if(result.getBoolean("mock",false))toast(app,"模拟测试：未操作真实车辆");
        else if(result.getBoolean("accepted",false))toast(app,"指令已受理，等待车辆回读确认");
        else toast(app,"指令未受理，请打开主程序确认："+result.getString("reason","unknown"));
      }catch(Throwable ignored){toast(app,"控车桥接未完成，请在 PsAssistant 中确认");}
      finally{synchronized(PENDING){PENDING.remove(request);}}
    });
  }
  static boolean freshAndReal(Bundle state){
    if(state==null||!state.getBoolean("available",false)||state.getBoolean("demo",false)
        ||!state.getBoolean("cardEnabled",true)||state.getString("vehicleKey","").isEmpty())return false;
    long at=state.getLong("capturedAt",0),now=System.currentTimeMillis();
    return at>0&&at<=now&&now-at<=300_000L;
  }
  private static void toast(Application app,String message){MAIN.post(()->Toast.makeText(app,message,Toast.LENGTH_SHORT).show());}
  private static Application currentApplication(){
    try{return (Application)Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}
  }
}
