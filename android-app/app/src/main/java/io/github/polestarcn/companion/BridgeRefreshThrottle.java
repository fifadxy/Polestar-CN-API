package io.github.polestarcn.companion;
/** One explicit bridge refresh admission per minute, using monotonic time. */
final class BridgeRefreshThrottle {
  static final long INTERVAL_MS=60000;
  private long last=-1;
  synchronized long retryAfter(long now){return retryAfter(now,INTERVAL_MS);}
  synchronized long retryAfter(long now,long windowMs){
    long bounded=Math.max(30_000L,Math.min(300_000L,windowMs));
    return last<0||now<last?0:Math.max(0,bounded-(now-last));
  }
  synchronized boolean admit(long now){return admit(now,INTERVAL_MS);}
  synchronized boolean admit(long now,long windowMs){
    if(retryAfter(now,windowMs)>0)return false;
    last=now;return true;
  }
}
