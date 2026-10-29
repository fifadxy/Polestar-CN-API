package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import io.github.polestarcn.companion.api.ChinaPolestarApi;
import org.junit.Test;
public final class CampRecoveryGateTest {
 private static final long NOW=1800000000000L;
 private final CampSession camp=new CampSession("camp1","MATCHINGVIN","Vehicle",NOW+3600000,31,10,true);
 private ChinaPolestarApi.VehicleInfo vehicle(String vin){ChinaPolestarApi.VehicleInfo v=new ChinaPolestarApi.VehicleInfo();v.vin=vin;return v;}
 private VehicleSnapshot state(long t,String source,boolean demo){return new VehicleSnapshot("Vehicle","masked",75,250,1000,"空闲","全部关闭","全部关闭","已关闭","已上锁",t,source,demo,"开启","关闭");}
 @Test public void verifiedSameVehicleCanRecover(){assertTrue(CampRecoveryGate.permits(camp,vehicle("MATCHINGVIN"),state(NOW,"LIVE",false),true,true,NOW));}
 @Test public void wrongAccountOrVehicleNeverIssuesOldVinWrite(){assertFalse(CampRecoveryGate.permits(camp,vehicle("OTHER"),state(NOW,"LIVE",false),true,true,NOW));}
 @Test public void staleUnknownAndDemoNeverRecover(){assertFalse(CampRecoveryGate.permits(camp,vehicle("MATCHINGVIN"),state(NOW-1000000,"LIVE",false),true,true,NOW));assertFalse(CampRecoveryGate.permits(camp,vehicle("MATCHINGVIN"),state(NOW,"CACHE",false),true,true,NOW));assertFalse(CampRecoveryGate.permits(camp,vehicle("MATCHINGVIN"),state(NOW,"LIVE",true),true,true,NOW));}
 @Test public void authMissingOrFailedReadNeverRecover(){assertFalse(CampRecoveryGate.permits(camp,vehicle("MATCHINGVIN"),state(NOW,"LIVE",false),false,true,NOW));assertFalse(CampRecoveryGate.permits(camp,vehicle("MATCHINGVIN"),state(NOW,"LIVE",false),true,false,NOW));}
}
