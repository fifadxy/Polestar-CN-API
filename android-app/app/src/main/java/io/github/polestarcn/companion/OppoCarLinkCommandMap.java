package io.github.polestarcn.companion;

import java.util.Locale;

/** Maps OPPO CarLink remote-control instructions onto existing PsAssistant commands. */
public final class OppoCarLinkCommandMap {
  private OppoCarLinkCommandMap() {}

  public static String toPsAssistantAction(String instruction,String value) {
    String key=canonical(instruction),state=norm(value);
    if("LOCK-CAR".equals(key))return open(state)||"LOCK".equals(state)?"lock":null;
    if("CAR-DOOR".equals(key)) {
      if(open(state)||"UNLOCK".equals(state)) return "unlock";
      if(close(state)||"LOCK".equals(state)) return "lock";
      return null;
    }
    if("ALL-WINDOW".equals(key)) {
      if(open(state)) return "windows-open";
      if(close(state)) return "windows-close";
      return null;
    }
    if("CAR-BOOT".equals(key)) {
      if(open(state)) return "trunk-open";
      if(close(state)) return "trunk-close";
      return null;
    }
    if("CAR-HORN".equals(key)) return "honk";
    if("DOUBLE-FLASH".equals(key)) return "flash";
    if("CAR-HORN&&DOUBLE-FLASH".equals(key)) return "honk-flash";
    if("AC-TEMP".equals(key)) {
      if(close(state)||"OFF".equals(state)) return "climate-stop";
      if(open(state)) return "climate-start";
    }
    return null;
  }

  /** Exact uploaded bridge vocabulary. Never guess an operation from an arbitrary substring. */
  /** Normalizes the exact MD event vocabulary without guessing a missing target state.
   * Native SDK actions, local 1001-1004 events, and MyDevices four-button IDs
   * must converge on the same PsAssistant command and authorization path.
   */
  public static String bridgeAction(int event,android.os.Bundle extras){
    android.os.Bundle b=extras==null?android.os.Bundle.EMPTY:extras;
    String raw=b.getString("sdk_cmd_name","");
    if(raw==null||raw.trim().isEmpty())raw=b.getString("action","");
    String name=raw==null?"":raw.trim().toLowerCase(Locale.ROOT).replace('_','-');
    if(!name.isEmpty())switch(name){
      case "lock":case "unlock":case "climate-start":case "climate-stop":
      case "windows-open":case "windows-close":case "trunk-open":case "trunk-close":
      case "air-start":case "air-stop":case "air-toggle":case "trunk-toggle":
      case "lock-toggle":case "climate-toggle":case "flash":case "honk":
      case "honk-flash":return name;
      case "climate-on":case "hvac-on":return "climate-start";
      case "climate-off":case "hvac-off":return "climate-stop";
      case "window-close":case "close-window":return "windows-close";
      case "window-open":case "window-vent":return "windows-open";
      case "find-car":return "honk-flash";
      case "chesuo":return "lock-toggle";
      case "kongtiao":return "climate-toggle";
      case "shanguang":return "flash";
      case "mingdi":return "honk";
      case "car-horn":return "honk";
      case "double-flash":return "flash";
      case "car-horn&&double-flash":case "car-horn-and-flash":return "honk-flash";
      case "ac-temp":case "lock-car":case "car-door":
      case "all-window":case "car-boot":
        if(!b.containsKey("action_target_state")||!(b.get("action_target_state") instanceof Boolean))return null;
        return toPsAssistantAction(name,b.getBoolean("action_target_state")?"OPEN":"CLOSE");
      default:return null;
    }
    if(event==1004||event==4)return "honk-flash";
    if(!b.containsKey("action_target_state")||!(b.get("action_target_state") instanceof Boolean))return null;
    boolean on=b.getBoolean("action_target_state");
    switch(event){
      case 1:case 1001:return on?"lock":"unlock";
      case 2:case 1002:return on?"climate-start":"climate-stop";
      case 3:case 1003:return on?"windows-close":"windows-open";
      default:return null;
    }
  }

  static String canonical(String instruction){String key=norm(instruction);return "CAR-HORN-AND-FLASH".equals(key)?"CAR-HORN&&DOUBLE-FLASH":key;}
  private static boolean open(String value){return "OPEN".equals(value)||"ON".equals(value)||"1".equals(value);}
  private static boolean close(String value){return "CLOSE".equals(value)||"OFF".equals(value)||"0".equals(value);}
  private static String norm(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replace('_','-');}
}
