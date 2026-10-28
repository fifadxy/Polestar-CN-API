package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import io.github.polestarcn.companion.api.ProtoWire;
import java.util.*;
import org.junit.Test;
public class TripV078Test {
  private final long at=1_700_000_000_000L;
  private MileageSnapshot odo(){return new MileageSnapshot(100000L,100.0,50.0,50.0,25.0,null,null,at,0);}
  private Map<Integer,Object> battery(Double ta,Double energy,long time){
    Map<Integer,Object> map=new HashMap<>();map.put(1,ProtoWire.varintField(1,time/1000));map.put(3,18.5);map.put(13,19.3);
    if(ta!=null)map.put(12,ta);if(energy!=null)map.put(15,energy);return map;
  }
  @Test public void taEnergyRequiresIndependentTripEnergyAndAlignedReport(){
    MileageSnapshot m=odo().withConsumption(battery(20.0,10000.0,at));assertEquals(20.0,m.automaticConsumption,.001);assertFalse(m.automaticConsumptionEstimated);
    assertEquals(18.5,m.manualConsumption,.001);assertEquals(19.3,m.sinceChargeConsumption,.001);
  }
  @Test public void missingAverageCanUseLabelledEnergyDistanceEstimate(){
    MileageSnapshot m=odo().withConsumption(battery(null,10000.0,at));assertEquals(20.0,m.automaticConsumption,.001);assertTrue(m.automaticConsumptionEstimated);
  }
  @Test public void capacityAndConflictingEnergyAreNeverRelabelledAsTa(){
    assertNull(odo().withConsumption(battery(78.0,null,at)).automaticConsumption);
    assertNull(odo().withConsumption(battery(78.0,10000.0,at)).automaticConsumption);
    assertNull(odo().withConsumption(battery(20.0,10000.0,at+180000)).automaticConsumption);
    assertNull(odo().withConsumption(battery(20.0,Double.NaN,at)).automaticConsumption);
  }
  @Test public void tripTimeIsEstimatedAndStoppedOrMissingSpeedStaysUnknown(){
    assertEquals(120.0,MileageSnapshot.estimatedMinutes(100.0,50.0),.001);
    assertEquals("2h 0m",MileageSnapshot.durationText(120.0,true));
    assertNull(MileageSnapshot.estimatedMinutes(5.0,0.0));assertNull(MileageSnapshot.estimatedMinutes(null,50.0));
    assertEquals("--",MileageSnapshot.durationText(null,true));
  }
  @Test public void sinceChargeFieldsNeverBecomeDrivingTime(){
    byte[] data=ProtoWire.concat(ProtoWire.fixed32Field(7,50.5f),ProtoWire.varintField(8,40));
    MileageSnapshot m=MileageSnapshot.fromReply(ProtoWire.decode(ProtoWire.bytesField(3,data)));
    assertEquals(50.5,m.sinceChargeKm,.001);assertEquals(40.0,m.sinceChargeAverageSpeed,.001);
    assertNull(m.automaticAverageSpeed);assertNull(m.manualKm);
  }
}
