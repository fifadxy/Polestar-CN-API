package io.github.polestarcn.companion;

/** Never switch an active account during remote commands or camping; allow signed-out recovery. */
final class LoginSafetyGate {
  private LoginSafetyGate(){}
  static boolean canBegin(boolean signedIn,boolean busy,boolean commands,
      boolean campActive,boolean pendingStop,boolean campStopping){
    if(busy||commands)return false;
    return !signedIn||!(campActive||pendingStop||campStopping);
  }
}
