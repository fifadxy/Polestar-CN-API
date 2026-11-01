package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.content.Intent;
import android.content.ContextWrapper;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/** Redirects only the synthetic POLESTAR provider's OEM account-bind launch to our local bridge UI. */
final class PolestarBindIntentShim {
  private PolestarBindIntentShim() {}

  static void install(OppoBridgeModule module) {
    int hooked=0;
    hooked+=hookStartActivityMethods(module,Activity.class);
    hooked+=hookStartActivityMethods(module,ContextWrapper.class);
    module.info("polestar_bind_redirect hooks="+hooked);
  }

  private static int hookStartActivityMethods(OppoBridgeModule module,Class<?> owner) {
    int hooked=0;
    for(Method method:owner.getDeclaredMethods()) {
      if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=void.class)continue;
      if(!"startActivity".equals(method.getName())&&!owner.equals(Activity.class))continue;
      if(owner.equals(Activity.class)&&!"startActivity".equals(method.getName())
          &&!"startActivityForResult".equals(method.getName()))continue;
      Class<?>[] p=method.getParameterTypes();
      if(p.length==0||p[0]!=Intent.class)continue;
      method.setAccessible(true);
      final String id="psassistant_polestar_bind_redirect_"+owner.getSimpleName()+"_"
          +method.getName()+"_"+p.length;
      module.hook(method).setId(id)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            List<Object> args=chain.getArgs();
            Intent intent=args!=null&&!args.isEmpty()&&args.get(0) instanceof Intent?(Intent)args.get(0):null;
            if(intent==null||!isPolestarBind(intent))return chain.proceed();
            try {
              Object caller=chain.getThisObject();
              android.content.Context context=caller instanceof android.content.Context
                  ?(android.content.Context)caller:null;
              if(context==null)return chain.proceed();
              Intent local=new Intent()
                  .setClassName("io.github.polestarcn.oppobridge",
                      "io.github.polestarcn.oppobridge.PolestarOfficialBindActivity")
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(local);
              module.info("polestar_bind_redirect handled=true owner="+owner.getSimpleName()
                  +" action="+String.valueOf(intent.getAction()));
              return null;
            } catch(Throwable failure) {
              module.warn("polestar_bind_redirect fallback="+failure.getClass().getSimpleName());
              return chain.proceed();
            }
          });
      hooked++;
    }
    return hooked;
  }

  private static boolean isPolestarBind(Intent intent) {
    String action=intent.getAction();
    boolean bindAction="oplus.intent.action.sceneservice.ACCOUNT_BIND".equals(action)
        ||"oplus.intent.action.sceneservice.REQUEST_SHOW_ACCOUNT_BINGDING_DIALOG".equals(action);
    if(!bindAction)return false;
    android.os.Bundle extras=null;
    try { extras=intent.getExtras(); } catch(Throwable ignored) {}
    if(extras==null)return false;

    for(String key:new String[]{"spCode","providerCode","serviceProviderCode","brandCode"}) {
      Object value=extras.get(key);
      if("POLESTAR".equalsIgnoreCase(String.valueOf(value)))return true;
    }

    Object provider=extras.get("providerInfo");
    if(provider!=null) {
      try {
        Object code=provider.getClass().getMethod("getSpCode").invoke(provider);
        if("POLESTAR".equalsIgnoreCase(String.valueOf(code)))return true;
      } catch(Throwable ignored) {
        if(String.valueOf(provider).toUpperCase(java.util.Locale.ROOT).contains("POLESTAR"))return true;
      }
    }

    for(String key:extras.keySet()) {
      String lower=key==null?"":key.toLowerCase(java.util.Locale.ROOT);
      if(!(lower.contains("provider")||lower.contains("brand")||lower.contains("sp")))continue;
      Object value=extras.get(key);
      String text=String.valueOf(value);
      if(text.length()<=160&&text.toUpperCase(java.util.Locale.ROOT).contains("POLESTAR"))return true;
    }
    return false;
  }
}
