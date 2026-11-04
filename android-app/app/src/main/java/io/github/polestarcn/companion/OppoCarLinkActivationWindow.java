package io.github.polestarcn.companion;
import java.util.List;
/** An SDK connection request may read once only after explicit opt-in. Never a timer. */
final class OppoCarLinkActivationWindow {
  private long last=-1;
  boolean admit(List<String> requested,String selected,boolean optedIn,boolean powerAllows,boolean busy,long now){
    if(!optedIn||!powerAllows||busy||requested==null||requested.size()>8
        ||selected==null||selected.isEmpty()||!requested.contains(selected)||now<0)return false;
    if(last>=0&&(now<last||now-last<5*60_000L))return false;
    last=now;return true;
  }
}
