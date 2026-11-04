package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;
import java.util.Locale;

/** Converts OPPO CAR control vocabulary into PsAssistant's verified vehicle commands. */
final class VehicleProtocolGateway {
  private VehicleProtocolGateway() {}

  static Bundle resolve(Context context, VehicleCoordinator coordinator, String request) {
    Bundle out=new Bundle();
    Bundle session=VirtualVehicleSessionManager.status(context,coordinator);
    out.putAll(session);
    if(!session.getBoolean("active",false))return reject(out,"virtual-session-not-active");
    VehicleSnapshot state=BuildConfig.OPPO_SDK_MOCK?OppoCarLinkMockFixture.snapshot():(coordinator==null?null:coordinator.snapshot);
    if(!PsAssistantBridgeProvider.commandStateReady(state,System.currentTimeMillis()))
      return reject(out,"fresh-vehicle-state-required");
    String command=resolveAction(request,state);
    if(command==null)return reject(out,"unsupported-or-unknown-state");
    out.putBoolean("accepted",true);out.putString("reason","resolved");
    out.putString("request",request==null?"":request);out.putString("command",command);
    out.putString("commandId",commandId(session.getString("sessionId",""),command,System.currentTimeMillis()));
    out.putLong("resolvedAt",System.currentTimeMillis());
    return out;
  }

  static String resolveAction(String request,VehicleSnapshot state) {
    if(request==null)return null;
    switch(request) {
      case "lock":case "unlock":case "climate-start":case "climate-stop":case "flash":case "honk":
      case "windows-open":case "windows-close":case "trunk-open":case "trunk-close":case "air-start":case "air-stop":case "honk-flash":return request;
      case "lock-toggle":
        String lock=norm(state==null?null:state.lock);
        if(lock.equals("LOCKED")||lock.equals("已锁")||lock.equals("已上锁"))return "unlock";
        if(lock.equals("UNLOCKED")||lock.equals("未锁")||lock.equals("已解锁")||lock.equals("未上锁"))return "lock";
        return null;
      case "climate-toggle":
        String climate=norm(state==null?null:state.climate);
        if(climate.equals("ACTIVE")||climate.equals("ON")||climate.equals("开启")||climate.equals("运行中"))return "climate-stop";
        if(climate.equals("INACTIVE")||climate.equals("OFF")||climate.equals("关闭")||climate.equals("未运行"))return "climate-start";
        return null;
      case "air-toggle":
        String air=norm(state==null?null:state.air);
        if(air.equals("开启")||air.equals("ON")||air.equals("ACTIVE"))return "air-stop";
        if(air.equals("关闭")||air.equals("OFF")||air.equals("INACTIVE"))return "air-start";
        return null;
      case "trunk-toggle":
        String trunk=norm(state==null?null:state.tailgate);
        if(trunk.equals("已打开")||trunk.equals("OPEN"))return "trunk-close";
        if(trunk.equals("已关闭")||trunk.equals("CLOSED"))return "trunk-open";
        return null;
      default:return null;
    }
  }

  private static Bundle reject(Bundle out,String reason){out.putBoolean("accepted",false);out.putString("reason",reason);return out;}
  private static String norm(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT);}
  private static String commandId(String session,String action,long now){return Integer.toHexString((session+"|"+action+"|"+(now/1000L)).hashCode());}
}
