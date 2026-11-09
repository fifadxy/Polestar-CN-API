package io.github.polestarcn.companion;
import java.util.concurrent.atomic.AtomicLong;
/** In-memory aggregate counters, without phone, VIN, password, token or raw response. */
public final class TelemetryDiagnostics {
  private TelemetryDiagnostics(){}
  private static final AtomicLong fullOk=new AtomicLong(),fullFail=new AtomicLong();
  private static final AtomicLong scopedOk=new AtomicLong(),scopedFail=new AtomicLong();
  private static final AtomicLong loginOk=new AtomicLong(),loginFail=new AtomicLong();
  private static final AtomicLong durations=new AtomicLong(),requests=new AtomicLong();
  public static void recordFull(boolean ok,long ms){record(ok,ms,fullOk,fullFail);}
  public static void recordScoped(boolean ok,long ms){record(ok,ms,scopedOk,scopedFail);}
  public static void recordLogin(boolean ok,long ms){record(ok,ms,loginOk,loginFail);}
  private static void record(boolean ok,long ms,AtomicLong pass,AtomicLong fail){
    (ok?pass:fail).incrementAndGet();requests.incrementAndGet();
    durations.addAndGet(Math.max(0,Math.min(ms,120_000)));
  }
  public static String report(){
    long n=requests.get();
    return "Session login OK/fail: "+loginOk.get()+"/"+loginFail.get()
      +"\nFull telemetry OK/fail: "+fullOk.get()+"/"+fullFail.get()
      +"\nScoped telemetry OK/fail: "+scopedOk.get()+"/"+scopedFail.get()
      +"\nAverage request duration: "+(n==0?0:durations.get()/n)+"ms"
      +"\nCounters reset when app process restarts; no account or vehicle identifiers are recorded.";
  }
  public static void reset(){fullOk.set(0);fullFail.set(0);scopedOk.set(0);scopedFail.set(0);loginOk.set(0);loginFail.set(0);durations.set(0);requests.set(0);}
}
