package io.github.polestarcn.companion;

/** Independently guarded vehicle-control families. Only the explicit lock policy may resend its outstanding original action once. */
public final class CommandSlots {
  private CommandSlots() {}
  public static String group(String action) {
    if(action==null)return "unknown";
    if(action.equals("lock")||action.equals("unlock"))return "lock";
    if(action.startsWith("trunk"))return "trunk";
    if(action.startsWith("climate"))return "climate";
    if(action.startsWith("air"))return "air";
    if(action.startsWith("windows"))return "windows";
    if(action.equals("honk")||action.equals("flash")||action.equals("honk-flash")||action.equals("signal"))return "signal";
    return action;
  }
}
