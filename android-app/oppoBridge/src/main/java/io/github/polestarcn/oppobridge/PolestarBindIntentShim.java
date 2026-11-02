package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

/** Redirects only a synthetic POLESTAR vehicle/account bind launch to our local bind UI. */
final class PolestarBindIntentShim {
  private PolestarBindIntentShim() {}

  static void install(OppoBridgeModule module) {
    int hooked=0;
    hooked+=hookStartActivityMethods(module,Activity.class);
    hooked+=hookStartActivityMethods(module,ContextWrapper.class);
    module.info("polestar_bind_redirect generation=2 hooks="+hooked);
  }

  private static int hookStartActivityMethods(OppoBridgeModule module,Class<?> owner) {
    int hooked=0;
    for(Method method:owner.getDeclaredMethods()) {
      if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=void.class)continue;
      if(owner.equals(Activity.class)) {
        if(!"startActivity".equals(method.getName())&&!"startActivityForResult".equals(method.getName()))continue;
      } else if(!"startActivity".equals(method.getName()))continue;
      Class<?>[] p=method.getParameterTypes();
      if(p.length==0||p[0]!=Intent.class)continue;
      method.setAccessible(true);
      final String id="psassistant_polestar_bind_redirect_v2_"+owner.getSimpleName()+"_"+method.getName()+"_"+p.length;
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
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  .putExtra("sourceAction",safe(intent.getAction(),120))
                  .putExtra("sourceData",safe(String.valueOf(intent.getData()),180));
              context.startActivity(local);
              module.info("polestar_bind_redirect handled=true owner="+owner.getSimpleName()
                  +" action="+String.valueOf(intent.getAction())+" data="+String.valueOf(intent.getData()));
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

  static boolean isPolestarBind(Intent intent) {
    if(intent==null||!hasPolestarMarker(intent))return false;
    String action=upper(intent.getAction());
    Uri data=intent.getData();
    String uri=upper(data==null?"":data.toString());
    if(action.contains("ACCOUNT_BIND")||action.contains("BINGDING")||action.contains("BIND_BT_KEY")
        ||action.contains("CARLINK")||action.contains("VEHICLE"))return true;
    if(uri.contains("CARLINK.HEYTAP.COM")&&(uri.contains("OPERATION=BIND")||uri.contains("BIND")))return true;
    // OEM builds occasionally use explicit activities with only provider extras. If the payload is
    // unmistakably the synthetic POLESTAR row, allow the local route rather than sending an invalid
    // provider code into SceneService/CarLink.
    return action.isEmpty()&&data==null;
  }

  private static boolean hasPolestarMarker(Intent intent) {
    String data=upper(intent.getData()==null?"":intent.getData().toString());
    if(data.contains("POLESTAR"))return true;
    android.os.Bundle extras=null;
    try {extras=intent.getExtras();}catch(Throwable ignored) {}
    if(extras==null)return false;
    for(String key:new String[]{"spCode","providerCode","serviceProviderCode","brandCode","companyCode","carBrand"}) {
      Object value=extras.get(key);if(isPolestar(value))return true;
    }
    for(String key:new String[]{"providerInfo","provider","brand","carInfo"}) {
      Object value=extras.get(key);if(value==null)continue;
      try {Object code=value.getClass().getMethod("getSpCode").invoke(value);if(isPolestar(code))return true;}
      catch(Throwable ignored) {}
      try {Object name=value.getClass().getMethod("getName").invoke(value);if(isPolestar(name))return true;}
      catch(Throwable ignored) {}
      if(isPolestar(value))return true;
    }
    for(String key:extras.keySet()) {
      String lower=key==null?"":key.toLowerCase(Locale.ROOT);
      if(!(lower.contains("provider")||lower.contains("brand")||lower.contains("sp")||lower.contains("car")))continue;
      Object value=extras.get(key);String text=String.valueOf(value);
      if(text.length()<=240&&isPolestar(text))return true;
    }
    return false;
  }

  private static boolean isPolestar(Object value) {
    String text=upper(String.valueOf(value));return text.contains("POLESTAR")||text.contains("极星");
  }
  private static String upper(String value){return value==null?"":value.toUpperCase(Locale.ROOT);}
  private static String safe(String value,int max){if(value==null||"null".equals(value))return "";return value.substring(0,Math.min(max,value.length()));}
}