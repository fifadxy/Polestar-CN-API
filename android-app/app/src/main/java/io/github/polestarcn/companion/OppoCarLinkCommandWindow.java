package io.github.polestarcn.companion;
import java.util.*;
/** Bounds repeated same-direction SDK writes, including momentary commands without telemetry. */
final class OppoCarLinkCommandWindow {
  private final LinkedHashMap<String,Long> sent=new LinkedHashMap<>();
  boolean admit(String key,long now){Long at=sent.get(key);if(at!=null&&(now<at||now-at<5000))return false;sent.put(key,now);if(sent.size()>16)sent.remove(sent.keySet().iterator().next());return true;}
}
