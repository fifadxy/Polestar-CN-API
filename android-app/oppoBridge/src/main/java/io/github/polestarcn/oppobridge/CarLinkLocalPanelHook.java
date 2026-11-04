package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Own-car panel inside an explicitly marked ControlRouter. OEM auth results are untouched. */
final class CarLinkLocalPanelHook {
  static final String ROUTER="com.heytap.opluscarlink.carcontrol.view.ControlRouterActivity";
  static final String MARKER="psassistant.local_panel";
  private static final Set<Activity> mounted=Collections.newSetFromMap(new WeakHashMap<Activity,Boolean>());
  private CarLinkLocalPanelHook(){}
  static boolean accepts(String activity,Intent intent){return ROUTER.equals(activity)&&intent!=null&&intent.getBooleanExtra(MARKER,false);}
  static void install(OppoBridgeModule module)throws Exception {
    Method resume=Activity.class.getDeclaredMethod("onResume");resume.setAccessible(true);
    module.hook(resume).setId("psassistant_carlink_local_panel_v3").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
      Object result=chain.proceed();Object receiver=chain.getThisObject();
      if(!(receiver instanceof Activity))return result;
      Activity a=(Activity)receiver;
      if(!accepts(a.getClass().getName(),a.getIntent()))return result;
      a.getWindow().getDecorView().post(()->{
        if(a.isFinishing()||a.isDestroyed()||mounted.contains(a))return;
        try{
          Bundle state=BridgeClient.virtualSession(a);
          if(!BridgeSettings.snapshot(a).getBoolean(BridgeSettings.MASTER,true)||!state.getBoolean("active",false)){
            report(a,false,"live-session-not-ready");return;
          }
          LinearLayout page=new LinearLayout(a);page.setOrientation(LinearLayout.VERTICAL);
          TextView title=new TextView(a);title.setText("Polestar · CarLink 本机车控");title.setTextSize(22);page.addView(title);
          TextView note=new TextView(a);note.setText("真实车辆接口 · 控制由 PsAssistant 执行；此页不代表 OPPO 云端绑定。");page.addView(note);
          page.addView(new NativeVehicleHomeView(a));ScrollView scroll=new ScrollView(a);scroll.addView(page);a.setContentView(scroll);
          mounted.add(a);report(a,true,"panel-mounted");
        }catch(RuntimeException failure){module.warn("carlink_local_panel="+failure.getClass().getSimpleName());report(a,false,failure.getClass().getSimpleName());}
      });return result;
    });
    module.info("carlink_local_panel hook-installed=true");
  }
  private static void report(Activity a,boolean ready,String reason){Bundle b=new Bundle();b.putBoolean("ready",ready);b.putString("reason",reason);b.putString("moduleVersion",BridgeBuild.VERSION_NAME);BridgeClient.call(a,"carlink-local-panel-report",b);}
}
