package io.github.polestarcn.companion;
/** Clock-safe TTL policy, independently testable without Android runtime. */
public final class RefreshPlanner {
 private RefreshPlanner(){}
 public static boolean fresh(long last,long now,long ttlMs){
   return last>0&&now>=last&&ttlMs>0&&now-last<ttlMs;
 }
 public static boolean shouldRead(long last,long now,long ttlMs){
   return !fresh(last,now,ttlMs);
 }
}
