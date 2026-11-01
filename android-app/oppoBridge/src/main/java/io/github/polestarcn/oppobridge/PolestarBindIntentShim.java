package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.content.Intent;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/** Redirects only the synthetic POLESTAR provider's OEM account-bind launch to our local bridge UI. */
final class PolestarBindIntentShim {
  private PolestarBindIntentShim() {}

  static void install(OppoBridgeModule module) {
    int hooked=0;
    for(Method method:Activity.class.getDeclaredMethods()) {
      if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=void.class)continue;
      if(!"startActivity".equals(method.getName())&&!"startActivityForResult".equals(method.getName()))continue;
      Class<?>[] p=method.getParameterTypes();
      if(p.length==0||p[0]!=Intent.class)continue;
      method.setAccessible(true);
      final String id="psassistant_polestar_bind_redirect_"+method.getName()+"_"+p.length;
      module.hook(method).setId(id)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            List<Object> args=chain.getArgs();
            Intent intent=args!=null&&!args.isEmpty()&&args.get(0) instanceof Intent?(Intent)args.get(0):null;
            if(intent==null||!isPolestarBind(intent))return chain.proceed();
            try {
              Activity activity=(Activity)chain.getThisObject();
              Intent local=new Intent()
                  .setClassName("io.github.polestarcn.oppobridge",
                      "io.github.polestarcn.oppobridge.PolestarOfficialBindActivity");
              activity.startActivity(local);
              module.info("polestar_bind_redirect handled=true action="+String.valueOf(intent.getAction()));
              return null;
            } catch(Throwable failure) {
              module.warn("polestar_bind_redirect fallback="+failure.getClass().getSimpleName());
              return chain.proceed();
            }
          });
      hooked++;
    }
    module.info("polestar_bind_redirect hooks="+hooked);
  }

  private static boolean isPolestarBind(Intent intent) {
    String action=intent.getAction();
    boolean bindAction="oplus.intent.action.sceneservice.ACCOUNT_BIND".equals(action)
        ||"oplus.intent.action.sceneservice.REQUEST_SHOW_ACCOUNT_BINGDING_DIALOG".equals(action);
    if(!bindAction)return false;
    Object provider=null;
    try {
      android.os.Bundle extras=intent.getExtras();
      if(extras!=null)provider=extras.get("providerInfo");
    } catch(Throwable ignored) {}
    if(provider==null)return false;
    try {
      Object code=provider.getClass().getMethod("getSpCode").invoke(provider);
      return "POLESTAR".equalsIgnoreCase(String.valueOf(code));
    } catch(Throwable ignored) {
      return String.valueOf(provider).toUpperCase(java.util.Locale.ROOT).contains("POLESTAR");
    }
  }
}
