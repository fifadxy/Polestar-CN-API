package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.os.Looper;
import io.github.polestarcn.companion.api.*;
import io.github.polestarcn.companion.auth.SecureSessionStore;
import io.grpc.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,shadows=ParkingLbsV079Test.Store.class)
public class ParkingLbsV079Test {
 @Implements(value=SecureSessionStore.class,isInAndroidSdk=false) public static class Store {
  static ChinaPolestarApi.Session current;static int clears;
  @Implementation public static ChinaPolestarApi.Session load(android.content.Context c){return current;}
  @Implementation public static void save(android.content.Context c,ChinaPolestarApi.Session s){current=s;}
  @Implementation public static void clear(android.content.Context c){current=null;clears++;}
 }
 private ChinaPolestarApi api;private VehicleCoordinator model;private FakeChannel channel;
 private static void set(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
 @Before public void start()throws Exception{
  Field instance=VehicleCoordinator.class.getDeclaredField("instance");instance.setAccessible(true);instance.set(null,null);
  Store.current=new ChinaPolestarApi.Session();Store.current.access="fixture";Store.current.expiresAt=System.currentTimeMillis()+3600000;Store.current.c3LbsHost="lbs.volvocars.com.cn";Store.current.c3Host="cars.volvocars.com.cn";Store.clears=0;
  RuntimeEnvironment.getApplication().getSharedPreferences("legal",0).edit().putBoolean("accepted_disclaimer_v1",true).commit();
  model=VehicleCoordinator.get(RuntimeEnvironment.getApplication());model.selected=new ChinaPolestarApi.VehicleInfo();model.selected.vin="VIN_FIXTURE_A";api=model.api;
  channel=new FakeChannel();set(api,"sharedChannel",channel);set(api,"sharedHost","lbs.volvocars.com.cn:443");
 }
 @Test public void lastParkedRpcUsesIndependentHostVinFieldOneAndFourSecondDeadline()throws Exception{
  LocationSnapshot p=api.readLastParkedLocation(model.selected.vin);assertEquals(120.5,p.longitude,.0001);assertEquals(30.25,p.latitude,.0001);assertEquals(1700000000000L,p.reportedAt);assertTrue(p.stale);
  assertEquals("dtlinternet.DtlInternetService/GetLastParkedLocation",channel.method);assertEquals(MethodDescriptor.MethodType.UNARY,channel.type);assertTrue(channel.deadline>0&&channel.deadline<=4000);
  assertEquals(model.selected.vin,new String((byte[])ProtoWire.decode(channel.request).get(1),java.nio.charset.StandardCharsets.UTF_8));assertEquals(model.selected.vin,channel.headerVin);
 }
 @Test public void lbsFailureDoesNotClearChinaSession()throws Exception{channel.fail=true;try{api.readLastParkedLocation(model.selected.vin);fail("expected service failure");}catch(Exception expected){}assertEquals(0,Store.clears);assertNotNull(api.session());}
 @Test public void trustedDiscoveryHostRejectsSpoofedOrMalformedCachedDestinations()throws Exception{
  Method trusted=ChinaPolestarApi.class.getDeclaredMethod("trustedLbsHost",String.class);trusted.setAccessible(true);
  assertEquals(true,trusted.invoke(null,"lbs.volvocars.com.cn"));assertEquals(true,trusted.invoke(null,"LBS.VOLVOCARS.COM.CN"));
  for(String host:new String[]{"volvocars.com.cn.evil.invalid","evil.invalid","https://lbs.volvocars.com.cn","lbs.volvocars.com.cn:443","user@lbs.volvocars.com.cn","lbs.volvocars.com.cn/path","-bad.volvocars.com.cn"})assertEquals(host,false,trusted.invoke(null,host));
 }
 static final class ManualExecutor extends AbstractExecutorService{
  final List<Runnable> pending=new ArrayList<>();public void execute(Runnable r){pending.add(r);}public void shutdown(){}public List<Runnable> shutdownNow(){return pending;}public boolean isShutdown(){return false;}public boolean isTerminated(){return false;}public boolean awaitTermination(long n,TimeUnit u){return true;}
  void first(){pending.remove(0).run();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 }
 @Test public void coordinatorLbsFailurePreservesVehicleSnapshotAndSession()throws Exception{
  ManualExecutor queue=new ManualExecutor();set(model,"io",queue);channel.fail=true;VehicleSnapshot before=new VehicleSnapshot("P2","masked",63,230,100,"空闲","全部关闭","全部关闭","已关闭","已上锁",System.currentTimeMillis(),"LIVE",false);model.snapshot=before;
  final boolean[] replied={false};model.readLastParkedLocation((p,error)->{assertNull(p);assertFalse(error.isEmpty());replied[0]=true;});queue.first();assertTrue(replied[0]);assertSame(before,model.snapshot);assertTrue(model.hasSession());assertEquals(0,Store.clears);assertFalse(model.locationBusy);
  String log=DiagnosticLog.export();assertFalse(log.contains("120.5"));assertFalse(log.contains("30.25"));assertFalse(log.contains(model.selected.vin));
 }
 @Test public void switchingVehicleDropsPendingPreviousLocation()throws Exception{
  ManualExecutor queue=new ManualExecutor();set(model,"io",queue);final boolean[] called={false};model.readLastParkedLocation((p,error)->called[0]=true);
  ChinaPolestarApi.VehicleInfo next=new ChinaPolestarApi.VehicleInfo();next.vin="VIN_FIXTURE_B";model.choose(next);queue.first();assertFalse(called[0]);assertFalse(model.locationBusy);
 }
 @Test public void logoutDropsPendingLocationAndKeepsNoPositionHistory()throws Exception{
  ManualExecutor queue=new ManualExecutor();set(model,"io",queue);final boolean[] called={false};model.readLastParkedLocation((p,error)->called[0]=true);model.logout();queue.first();assertFalse(called[0]);assertFalse(model.hasSession());assertNull(model.selected);assertNull(model.snapshot);assertEquals(1,Store.clears);
 }
 private static final class FakeChannel extends ManagedChannel{
  byte[] request;boolean fail;String method,headerVin;long deadline;MethodDescriptor.MethodType type;
  @Override @SuppressWarnings("unchecked") public <Q,S> ClientCall<Q,S> newCall(MethodDescriptor<Q,S> descriptor,CallOptions options){method=descriptor.getFullMethodName();type=descriptor.getType();deadline=options.getDeadline().timeRemaining(TimeUnit.MILLISECONDS);
   return new ClientCall<Q,S>(){Listener<S> listener;public void start(Listener<S> l,Metadata h){listener=l;headerVin=h.get(Metadata.Key.of("vin",Metadata.ASCII_STRING_MARSHALLER));}public void request(int n){}public void cancel(String m,Throwable t){}public void sendMessage(Q message){request=(byte[])message;}public void halfClose(){if(fail){listener.onClose(Status.UNAVAILABLE,new Metadata());return;}
    byte[] point=ProtoWire.concat(ProtoWire.fixed32Field(1,120.5f),ProtoWire.fixed32Field(2,30.25f),ProtoWire.bytesField(3,ProtoWire.varintField(1,1700000000)));listener.onHeaders(new Metadata());listener.onMessage((S)ProtoWire.concat(ProtoWire.bytesField(2,point),ProtoWire.varintField(3,1)));listener.onClose(Status.OK,new Metadata());}};
  }
  public String authority(){return "fixture";}public ManagedChannel shutdown(){return this;}public boolean isShutdown(){return false;}public boolean isTerminated(){return false;}public ManagedChannel shutdownNow(){return this;}public boolean awaitTermination(long t,TimeUnit u){return true;}
 }
}
