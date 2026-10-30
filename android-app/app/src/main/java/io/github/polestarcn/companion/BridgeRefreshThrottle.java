package io.github.polestarcn.companion;
/** One explicit bridge refresh admission per minute, using monotonic time. */
final class BridgeRefreshThrottle {
  static final long INTERVAL_MS=60000;
  private long last=-1;
  synchronized long retryAfter(long now) {return last<0||now<last?0:Math.max(0,INTERVAL_MS-(now-last));}
  synchronized boolean admit(long now) {if(retryAfter(now)>0)return false;last=now;return true;}
}
