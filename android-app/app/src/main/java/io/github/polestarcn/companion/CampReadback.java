package io.github.polestarcn.companion;
import java.util.function.BooleanSupplier;
/** Blocking camping checks share the foreground policy: stop at the first verified state. */
final class CampReadback {
 interface Check {boolean read() throws Exception;}
 interface Waiter {void waitFor(long milliseconds) throws InterruptedException;}
 static boolean verify(Check check,BooleanSupplier cancelled,Waiter waiter)throws InterruptedException{
  long previous=0;
  for(long at:CommandReadback.CHECKPOINT_MS){
   if(cancelled.getAsBoolean())return false;
   waiter.waitFor(at-previous);previous=at;
   if(cancelled.getAsBoolean())return false;
   try{if(check.read())return true;}catch(InterruptedException e){throw e;}catch(Exception unavailable){DiagnosticLog.add("Camping readback unavailable; waiting for next slot");}
  }
  return false;
 }
 private CampReadback(){}
}
