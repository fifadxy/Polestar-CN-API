package io.github.polestarcn.oppobridge;

import android.os.Bundle;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Native AssistantScreen four-button contract, independent from the Xposed runtime.
 * Flash and horn are momentary commands and must never be persisted as switches. */
final class AssistantScreenMenuSpec {
  private AssistantScreenMenuSpec(){}

  static List<Object> build(Constructor<?> ctor, Bundle state, String scheme) throws Exception {
    ArrayList<Object> out=new ArrayList<>(4);
    boolean locked=hasValue(state,"lock","已上锁","已锁","LOCKED");
    boolean climate=hasValue(state,"climate","开启","运行中","ON","ACTIVE","RUNNING");
    out.add(ctor.newInstance("1","车锁","车锁",0,0,locked,
        scheme+"://command?action=lock-toggle"));
    out.add(ctor.newInstance("2","空调","空调",0,0,climate,
        scheme+"://command?action=climate-toggle"));
    out.add(ctor.newInstance("3","闪灯","闪灯",0,0,false,
        scheme+"://command?action=flash"));
    out.add(ctor.newInstance("4","鸣笛","鸣笛",0,0,false,
        scheme+"://command?action=honk"));
    return out;
  }

  private static boolean hasValue(Bundle state,String field,String... values){
    String raw=state==null?"":state.getString(field,"");
    for(String expected:values)
      if(raw.trim().toUpperCase(Locale.ROOT).equals(expected.toUpperCase(Locale.ROOT)))return true;
    return false;
  }
}
