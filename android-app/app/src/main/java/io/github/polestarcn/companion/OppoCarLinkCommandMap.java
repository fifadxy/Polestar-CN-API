package io.github.polestarcn.companion;

import java.util.Locale;

/** Maps OPPO CarLink remote-control instructions onto existing PsAssistant commands. */
public final class OppoCarLinkCommandMap {
  private OppoCarLinkCommandMap() {}

  public static String toPsAssistantAction(String instruction,String value) {
    String key=norm(instruction),state=norm(value);
    if("CAR-DOOR".equals(key)||"LOCK-CAR".equals(key)) {
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
      if(!state.isEmpty()) return "climate-start";
    }
    return null;
  }

  private static boolean open(String value){return "OPEN".equals(value)||"ON".equals(value)||"1".equals(value);}
  private static boolean close(String value){return "CLOSE".equals(value)||"OFF".equals(value)||"0".equals(value);}
  private static String norm(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT);}
}
