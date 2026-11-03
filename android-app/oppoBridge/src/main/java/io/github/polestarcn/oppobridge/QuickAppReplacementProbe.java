package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read-only exact-build probe for MyDevices 17.4.15 QuickApp widget replacement state.
 *
 * <p>The target provider runs in com.heytap.mydevices:cards. The probe records routing metadata
 * plus a bounded, credential-free summary of the final deviceList that is handed to the QuickApp
 * card. It never changes the OEM Bundle, RPK path, card binding, device list, or provider result.</p>
 */
final class QuickAppReplacementProbe {
  private static final String PROVIDER =
      "com.oplus.mydevices.quickapp.provider.DeviceQuickAppCardWidgetProvider";
  private static final String BRIDGE_PACKAGE="io.github.polestarcn.oppobridge";
  private static final String PSASSISTANT_PACKAGE="io.github.polestarcn.companion";

  private QuickAppReplacementProbe() {}

  static void install(OppoBridgeModule module, ClassLoader loader) throws Exception {
    Application app=currentApplication();
    if(app!=null) {
      String version=app.getPackageManager().getPackageInfo("com.heytap.mydevices",0).versionName;
      if(!"17.4.15".equals(version)) {
        module.info("quickapp_replacement_probe skipped hostVersion="+version);
        return;
      }
    }

    Class<?> type=loader.loadClass(PROVIDER);
    int hooked=0;
    for(Method method:type.getDeclaredMethods()) {
      Class<?>[] p=method.getParameterTypes();
      if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=Bundle.class
          ||p.length!=2||p[0]!=String.class||p[1]!=Bundle.class)continue;
      if(!"handleGetCardData".equals(method.getName())&&!"handleCardShow".equals(method.getName()))continue;
      method.setAccessible(true);
      final String methodName=method.getName();
      module.hook(method)
          .setId("psassistant_quickapp_replace_"+methodName)
          .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
          .intercept(chain->{
            List<Object> args=chain.getArgs();
            String widget=args!=null&&!args.isEmpty()?safe(args.get(0)):"";
            Bundle extras=args!=null&&args.size()>1&&args.get(1) instanceof Bundle?(Bundle)args.get(1):null;
            Object raw=chain.proceed();
            Bundle result=raw instanceof Bundle?(Bundle)raw:null;
            try {
              report(module,methodName,widget,extras,result);
            } catch(Throwable failure) {
              module.warn("quickapp_replacement_probe_failed "+failure.getClass().getSimpleName());
            }
            return raw;
          });
      hooked++;
    }
    if(hooked==0)throw new IllegalStateException("QuickApp replacement methods missing");
    module.info("quickapp_replacement_probe ready=true methods="+hooked);
  }

  private static void report(OppoBridgeModule module,String method,String widget,Bundle extras,Bundle result) {
    Application app=currentApplication();
    if(app==null)return;

    String oldCode=value(extras,"oldWidgetCode");
    String replaced=value(extras,"replacedWidgetCode");
    if(replaced.isEmpty()&&result!=null)replaced=value(result,"replacedWidgetCode");
    if(oldCode.isEmpty()&&result!=null)oldCode=value(result,"oldWidgetCode");

    String effective=!replaced.isEmpty()?replaced:!oldCode.isEmpty()?oldCode:widget;
    String path=cardPath(effective);

    Bundle in=new Bundle();
    in.putBoolean("ready",true);
    in.putString("phase","widget-final-data");
    in.putString("method",method);
    in.putString("widgetCode",bounded(widget,500));
    in.putString("oldWidgetCode",bounded(oldCode,500));
    in.putString("replacedWidgetCode",bounded(replaced,500));
    in.putString("cardPath",path);
    in.putString("reason","read-only final QuickApp deviceList observation; no raw vehicle/account payload persisted");
    in.putString("moduleVersion",BridgeBuild.VERSION_NAME);
    try {in.putString("hostVersion",app.getPackageManager().getPackageInfo("com.heytap.mydevices",0).versionName);}
    catch(Throwable ignored){in.putString("hostVersion","");}

    in.putBoolean("single4x2CarRegistry",true);
    in.putBoolean("single2x2CarRegistry",false);
    in.putBoolean("single4x2EventSummaryCar",false);
    in.putBoolean("single2x2EventSummaryCar",false);

    Bundle data=result!=null?result:extras;
    in.putString("resultKeys",bundleKeys(data));
    inspectCardData(data,in);
    if(!in.getBoolean("deviceListPresent",false)&&result!=extras)inspectCardData(extras,in);

    try {
      app.getContentResolver().call(BridgeClient.URI,"quickapp-runtime-report",null,in);
      module.info("quickapp_replacement method="+method+" cardPath="+path
          +" devices="+in.getInt("deviceCount",0)
          +" ourCard="+in.getBoolean("selectedIsOurCard",false)
          +" shortcuts="+in.getInt("selectedShortcutCount",0)
          +" replaced="+(!replaced.isEmpty()));
    } catch(Throwable ignored) {}
  }

  private static void inspectCardData(Bundle source,Bundle out) {
    if(source==null)return;
    putBooleanLike(source,out,"isAuth");
    putBooleanLike(source,out,"isBind");
    putBooleanLike(source,out,"hasDevice");
    Object raw=source.get("deviceList");
    if(raw==null)return;
    out.putBoolean("deviceListPresent",true);
    try {
      JSONArray list;
      if(raw instanceof String)list=new JSONArray((String)raw);
      else if(raw instanceof JSONArray)list=(JSONArray)raw;
      else return;
      out.putInt("deviceCount",Math.min(list.length(),999));
      if(list.length()==0)return;

      int selected=0;
      for(int i=0;i<Math.min(list.length(),16);i++) {
        JSONObject item=list.optJSONObject(i);
        if(item!=null&&isOurDevice(item)){selected=i;break;}
      }
      JSONObject device=list.optJSONObject(selected);
      if(device==null)return;
      out.putInt("selectedDeviceIndex",selected);
      out.putBoolean("selectedIsOurCard",isOurDevice(device));
      out.putString("selectedIdClass",idClass(first(device,"id","mDeviceId","deviceId")));
      out.putString("selectedType",bounded(first(device,"type","mDeviceType","deviceType"),80));
      out.putString("selectedPackage",bounded(first(device,"packageName","deviceAppPackage","mDeviceAppPackage"),160));
      Object data=device.opt("data");
      out.putBoolean("selectedHasData",data!=null&&data!=JSONObject.NULL&&String.valueOf(data).length()>0);

      JSONArray shortcuts=array(device.opt("shortcuts"));
      if(shortcuts==null)return;
      out.putInt("selectedShortcutCount",Math.min(shortcuts.length(),999));
      List<String> ids=new ArrayList<>();
      for(int i=0;i<Math.min(shortcuts.length(),16);i++) {
        JSONObject item=shortcuts.optJSONObject(i);
        if(item==null)continue;
        String id=first(item,"id","shortcutId");
        if(!id.isEmpty())ids.add(identifier(id,80));
      }
      out.putString("selectedShortcutIds",bounded(String.join("|",ids),600));
    } catch(Throwable ignored) {
      out.putString("deviceListParse","unavailable");
    }
  }

  private static JSONArray array(Object value) {
    if(value instanceof JSONArray)return (JSONArray)value;
    if(value instanceof String)try{return new JSONArray((String)value);}catch(Throwable ignored){}
    return null;
  }

  private static boolean isOurDevice(JSONObject item) {
    String id=first(item,"id","mDeviceId","deviceId");
    String pkg=first(item,"packageName","deviceAppPackage","mDeviceAppPackage");
    return id.startsWith("psassistant_")||BRIDGE_PACKAGE.equals(pkg)||PSASSISTANT_PACKAGE.equals(pkg);
  }

  private static String first(JSONObject item,String... keys) {
    if(item==null)return "";
    for(String key:keys) {
      Object value=item.opt(key);
      if(value!=null&&value!=JSONObject.NULL) {
        String text=String.valueOf(value).trim();
        if(!text.isEmpty()&&!"null".equalsIgnoreCase(text))return text;
      }
    }
    return "";
  }

  private static void putBooleanLike(Bundle source,Bundle out,String key) {
    if(!source.containsKey(key))return;
    Object value=source.get(key);
    if(value instanceof Boolean)out.putBoolean(key,(Boolean)value);
    else if(value instanceof Number)out.putBoolean(key,((Number)value).intValue()!=0);
    else if(value!=null)out.putBoolean(key,"true".equalsIgnoreCase(String.valueOf(value))
        ||"1".equals(String.valueOf(value)));
  }

  private static String bundleKeys(Bundle bundle) {
    if(bundle==null)return "";
    TreeSet<String> keys=new TreeSet<>(bundle.keySet());
    List<String> safe=new ArrayList<>();
    for(String key:keys) {
      String lower=key.toLowerCase(Locale.ROOT);
      if(lower.contains("token")||lower.contains("password")||lower.contains("cookie")
          ||lower.contains("account")||lower.contains("vin"))continue;
      safe.add(identifier(key,80));
      if(safe.size()>=40)break;
    }
    return bounded(String.join("|",safe),1200);
  }

  private static String idClass(String id) {
    if(id==null||id.isEmpty())return "missing";
    return id.startsWith("psassistant_")?"psassistant":"other";
  }

  private static String identifier(String value,int max) {
    if(value==null)return "";
    String clean=value.replace('|','_').replace('\n',' ').replace('\r',' ').trim();
    return clean.substring(0,Math.min(clean.length(),max));
  }

  private static String value(Bundle bundle,String key) {
    if(bundle==null||!bundle.containsKey(key))return "";
    Object value=bundle.get(key);
    return value==null?"":String.valueOf(value);
  }

  static String cardPath(String code) {
    if(code==null)return "";
    // Exact AssistantScreen 17.14.60 local config on the user's build:
    // 10000581 is the size-2 "自选设备" route (4x2); 10000580 is its compact sibling.
    if(code.startsWith("10000581"))return "SingleDeviceCard4x2";
    if(code.startsWith("10000580"))return "SingleDeviceCard2x2";
    for(String size:new String[]{"SingleDeviceCard4x2","SingleDeviceCard2x2","SingleDeviceCard2x1",
        "PackDeviceCard4x2","PackDeviceCard2x2","PackDeviceCard2x1",
        "LockScreenSingleCard2x1","LockScreenSingleCard1x1"})
      if(code.contains(size))return size;
    return "";
  }

  private static String safe(Object value){return value==null?"":String.valueOf(value);}
  private static String bounded(String value,int max) {
    if(value==null)return "";
    return value.substring(0,Math.min(value.length(),max));
  }

  private static Application currentApplication() {
    try {
      return (Application)Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch(Throwable ignored) {return null;}
  }
}
