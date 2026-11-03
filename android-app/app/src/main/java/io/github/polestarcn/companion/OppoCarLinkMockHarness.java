package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Method;

/** Reflection bridge to the optional TestLab OCS mock AAR. */
final class OppoCarLinkMockHarness {
  private OppoCarLinkMockHarness(){}

  static boolean available(){return BuildConfig.OPPO_SDK_MOCK;}

  static Bundle status(){
    if(!available()){Bundle out=new Bundle();out.putBoolean("mock",false);return out;}
    try{
      Class<?> type=Class.forName("com.oplus.ocs.carlink.mock.MockCarLinkServer");
      Object value=type.getMethod("status").invoke(null);
      return value instanceof Bundle?(Bundle)value:Bundle.EMPTY;
    }catch(Throwable unavailable){Bundle out=new Bundle();out.putBoolean("mock",true);out.putString("error",unavailable.getClass().getSimpleName());return out;}
  }

  static void reset(){
    if(!available())return;
    OppoCarLinkMockFixture.reset();
    invoke("reset",new Class<?>[0],new Object[0]);
  }

  static void runFull(Context context,Runnable done){
    if(!available()){if(done!=null)done.run();return;}
    AppPreferences.set(context,"carlink_type_code","POLESTAR_2_TESTLAB");
    AppPreferences.set(context,"carlink_type_name","Polestar 2 TestLab");
    AppPreferences.set(context,"carlink_image_url","https://test.invalid/polestar2-testlab.png");
    OppoDeviceIntegration.setEnabled(context,true);
    OppoCarLinkRuntime runtime=OppoCarLinkRuntime.get(context);
    Handler main=new Handler(Looper.getMainLooper());
    runtime.start();
    main.postDelayed(runtime::authorize,120L);
    main.postDelayed(runtime::subscribe,320L);
    main.postDelayed(()->{invoke("requestCarInfo",new Class<?>[0],new Object[0]);invoke("requestCarStatus",new Class<?>[0],new Object[0]);invoke("showCard",new Class<?>[0],new Object[0]);},520L);
    main.postDelayed(()->issue("LOCK-CAR","OPEN"),820L);
    main.postDelayed(()->issue("AC-TEMP","OPEN"),1250L);
    main.postDelayed(()->issue("CAR-BOOT","OPEN"),1680L);
    main.postDelayed(()->{invoke("requestCarStatus",new Class<?>[0],new Object[0]);if(done!=null)done.run();},2150L);
  }

  static Object issue(String instruction,String value){
    Bundle state=status();String car=state.getString("currentCar","");
    if(car.isEmpty())return null;
    return invoke("issueControl",new Class<?>[]{String.class,String.class,String.class},new Object[]{car,instruction,value});
  }

  private static Object invoke(String name,Class<?>[] types,Object[] args){
    try{
      Class<?> type=Class.forName("com.oplus.ocs.carlink.mock.MockCarLinkServer");
      Method method=type.getMethod(name,types);return method.invoke(null,args);
    }catch(Throwable unavailable){return null;}
  }
}
