package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import io.github.polestarcn.companion.api.ProtoWire;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class HealthLocationV078Test {
 private Context c;
 @Before public void start(){c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();}
 private Map<Integer,Object> health(){Map<Integer,Object> values=new HashMap<>();for(int field:new int[]{5,6,7,8,9,10,11,12,13,14,38})values.put(field,1L);
  values.put(2,931L);values.put(3,75L);values.put(4,14959L);values.put(1,ProtoWire.varintField(1,1700000000));return values;}
 private VehicleSnapshot snapshot(VehicleHealthSnapshot h){return new VehicleSnapshot("Polestar 2","masked",63,230,65845,"空闲","全部关闭","全部关闭","已关闭","已上锁",1700000000000L,"LIVE",false).withHealth(h);}
 @Test public void latestReferenceHealthMapsAllChecksAndRemainingServiceUnits(){
  Map<Integer,Object> fields=health();VehicleHealthSnapshot h=VehicleHealthSnapshot.fromData(fields,TireSnapshot.fromData(fields));
  assertEquals(1,h.maintenance);assertEquals(1,h.brakeFluid);assertEquals(1,h.coolant);assertEquals(1,h.oil);assertEquals(1,h.exteriorLights);
  assertEquals(Integer.valueOf(931),h.serviceHoursLeft);assertEquals(Integer.valueOf(75),h.serviceDaysLeft);assertEquals(Integer.valueOf(14959),h.serviceKmLeft);assertFalse(h.hasProblem());
  assertEquals("931 h",DisplayMetric.SERVICE_HOURS_LEFT.value(c,snapshot(h),true,true));
 }
 @Test public void unknownAndOmittedChecksNeverBecomeMeasuredNormal(){
  Map<Integer,Object> fields=health();fields.remove(8);fields.put(6,0L);fields.put(7,99L);fields.put(14,0L);
  VehicleHealthSnapshot h=VehicleHealthSnapshot.fromData(fields,TireSnapshot.fromData(fields));assertEquals(-1,h.oil);assertEquals(-1,h.brakeFluid);assertEquals(-1,h.coolant);assertEquals(-1,h.exteriorLights);
  assertEquals("All Good!",VehicleCondition.summary(snapshot(h),true,true));
 }
 @Test public void eachExtendedWarningReachesUnifiedVehicleCondition(){
  for(int field:new int[]{5,6,7,8,14}){Map<Integer,Object> fields=health();fields.put(field,2L);VehicleHealthSnapshot h=VehicleHealthSnapshot.fromData(fields,TireSnapshot.fromData(fields));assertTrue("field "+field,h.hasProblem());assertTrue(VehicleCondition.problem(snapshot(h),true));}
  Map<Integer,Object> fields=health();fields.put(46,ProtoWire.varintField(1,2));assertTrue(VehicleHealthSnapshot.fromData(fields,TireSnapshot.fromData(fields)).hasProblem());
 }
 @Test public void vehicleServiceReadingsOverrideLocalRecordsButPercentagesUseConfiguredCycles(){
  VehicleSnapshot v=snapshot(VehicleHealthSnapshot.fromData(health(),TireSnapshot.unknown()));
  assertEquals("75 d",DisplayMetric.SERVICE_DAYS_LEFT.value(c,v,true,true));assertEquals("14959 km",DisplayMetric.SERVICE_KM_LEFT.value(c,v,true,true));
  assertEquals("21%",DisplayMetric.SERVICE_TIME_PERCENT.value(c,v,true,true));assertEquals("75%",DisplayMetric.SERVICE_KM_PERCENT.value(c,v,true,true));
  AppPreferences.set(c,"maintenance_interval_days","750");assertEquals("10%",DisplayMetric.SERVICE_TIME_PERCENT.value(c,v,true,true));
  assertEquals(v.health.reportedAt,DisplayMetric.SERVICE_TIME_PERCENT.reportedAt(v));assertEquals(v.mileage.reportedAt,DisplayMetric.TA_DISTANCE.reportedAt(v));
 }
 @Test public void fullHealthCacheKeepsSourceTimeAndCounters(){
  VehicleSnapshot v=snapshot(VehicleHealthSnapshot.fromData(health(),TireSnapshot.unknown()));VehicleSnapshotStore.saveChina(c,v);VehicleSnapshot copy=VehicleSnapshotStore.loadChina(c);
  assertEquals(v.health.serviceHoursLeft,copy.health.serviceHoursLeft);assertEquals(v.health.serviceDaysLeft,copy.health.serviceDaysLeft);assertEquals(v.health.serviceKmLeft,copy.health.serviceKmLeft);
  assertEquals(v.health.brakeFluid,copy.health.brakeFluid);assertEquals(v.health.oil,copy.health.oil);assertEquals(v.health.reportedAt,copy.health.reportedAt);
 }
 private LocationSnapshot point(double lon,double lat){Map<Integer,Object> p=new HashMap<>();p.put(1,lon);p.put(2,lat);p.put(3,ProtoWire.varintField(1,1700000000));Map<Integer,Object> reply=new HashMap<>();reply.put(2,ProtoWire.concat(ProtoWire.fixed32Field(1,(float)lon),ProtoWire.fixed32Field(2,(float)lat),ProtoWire.bytesField(3,ProtoWire.varintField(1,1700000000))));reply.put(3,1L);return LocationSnapshot.fromReply(reply);}
 @Test public void parkedLocationPreservesAxisTimeAndVehicleStaleFlag(){LocationSnapshot p=point(120.5,30.25);assertEquals(120.5,p.longitude,.0001);assertEquals(30.25,p.latitude,.0001);assertTrue(p.stale);assertEquals(1700000000000L,p.reportedAt);assertNull(point(181,30));assertNull(point(120,91));assertNull(point(Double.NaN,30));assertNull(point(0,0));}
 @Test public void keylessMapUriHasExplicitAmapDestinationAndNoVehicleIdentity(){
  android.net.Uri uri=ParkingMapLinks.webUri(point(120.5,30.25),"Last parked location");assertEquals("https",uri.getScheme());assertEquals("uri.amap.com",uri.getHost());
  assertEquals("120.500000,30.250000",uri.getQueryParameter("position"));assertEquals("gaode",uri.getQueryParameter("coordinate"));assertEquals("0",uri.getQueryParameter("callnative"));
  assertNull(uri.getQueryParameter("key"));assertFalse(uri.toString().contains("vin"));
 }
 @Test public void displayCacheDoesNotPersistPreciseParkingCoordinates(){
  VehicleSnapshotStore.saveChina(c,snapshot(VehicleHealthSnapshot.unknown()));String cache=c.getSharedPreferences("vehicle_snapshot_display_v1",0).getString("china","");
  assertFalse(cache.contains("latitude"));assertFalse(cache.contains("longitude"));assertFalse(cache.contains("parkedLocation"));
 }
}
