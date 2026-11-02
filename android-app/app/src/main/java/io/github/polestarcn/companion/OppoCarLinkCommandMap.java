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

  static String canonical(String instruction){String key=norm(instruction);return "CAR-HORN-AND-FLASH".equals(key)?"CAR-HORN&&DOUBLE-FLASH":key;}
  private static boolean open(String value){return "OPEN".equals(value)||"ON".equals(value)||"1".equals(value);}
  private static boolean close(String value){return "CLOSE".equals(value)||"OFF".equals(value)||"0".equals(value);}
  private static String norm(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replace('_','-');}
}
