package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Caller-side account-bind result shim for the exact AssistantScreen process.
 *
 * <p>When PsAssistant has an authenticated real vehicle and the local bridge is not explicitly disabled, only the Bundle returned to
 * AssistantScreen is extended with POLESTAR. The real SceneService provider/database is untouched.</p>
 */
final class SceneAccountBindResultShim {
  private static final String AUTHORITY="com.oplus.sceneservice.AccountSDKInitProvider";
  private static final String METHOD="method_get_account_bind_list";

  private SceneAccountBindResultShim() {}

  static void install(OppoBridgeModule module) throws Exception {
    Method call=ContentResolver.class.getMethod("call",Uri.class,String.class,String.class,Bundle.class);
    module.hook(call)
        .setId("psassistant_polestar_bind_result")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain->{
          List<Object> args=chain.getArgs();
          Uri uri=args!=null&&args.size()>0&&args.get(0) instanceof Uri?(Uri)args.get(0):null;
          String method=args!=null&&args.size()>1?String.valueOf(args.get(1)):"";
          Object raw=chain.proceed();
          if(uri==null||!AUTHORITY.equals(uri.getAuthority())||!METHOD.equals(method)
              ||!(raw instanceof Bundle))return raw;
          try {
            Application app=currentApplication();
            if(app==null)return raw;
            Bundle state=BridgeClient.virtualSession(app);
            if(!state.getBoolean("active",false))return raw;
            Bundle out=new Bundle((Bundle)raw);
            ArrayList<String> list=out.getStringArrayList("accountBindList");
            if(list==null)list=new ArrayList<>();
            else list=new ArrayList<>(list);
            boolean found=false;for(String code:list)if("POLESTAR".equalsIgnoreCase(code))found=true;
            if(!found)list.add("POLESTAR");
            out.putStringArrayList("accountBindList",list);
            out.putInt("accountBindCount",Math.max(out.getInt("accountBindCount",0),list.size()));
            module.info("scene_account_bind_result polestar=true count="+list.size());
            return out;
          } catch(Throwable failure) {
            module.warn("scene_account_bind_result fallback="+failure.getClass().getSimpleName());
            return raw;
          }
        });
  }

  private static Application currentApplication() {
    try{return (Application)Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }
}