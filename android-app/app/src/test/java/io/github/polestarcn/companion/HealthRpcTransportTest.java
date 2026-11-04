package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import io.github.polestarcn.companion.api.ChinaPolestarApi;
import io.github.polestarcn.companion.api.ProtoWire;
import io.grpc.*;
import java.lang.reflect.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk = 28)
public final class HealthRpcTransportTest {
  private ChinaPolestarApi api;
  private final String vin = "VIN_FIXTURE";
  private FakeChannel channel;
  private ChinaPolestarApi.Session session;
  @Before public void setup() throws Exception {
    api = new ChinaPolestarApi(RuntimeEnvironment.getApplication()); channel = new FakeChannel();
    session = new ChinaPolestarApi.Session(); session.access = "fixture"; session.c3Host = "fixture.invalid";
    Field field = ChinaPolestarApi.class.getDeclaredField("sharedChannel"); field.setAccessible(true); field.set(api, channel);
    field = ChinaPolestarApi.class.getDeclaredField("sharedHost"); field.setAccessible(true); field.set(api, "fixture.invalid:443");
    channel.payload = ProtoWire.concat(ProtoWire.stringField(2, vin), ProtoWire.bytesField(3,
        ProtoWire.concat(ProtoWire.varintField(9, 1), ProtoWire.varintField(10, 1), ProtoWire.varintField(11, 1), ProtoWire.varintField(12, 1))));
  }
  private TireSnapshot read(String target) throws Exception {
    Method method = ChinaPolestarApi.class.getDeclaredMethod("readHealth", ChinaPolestarApi.Session.class, String.class);
    method.setAccessible(true); return TireSnapshot.fromData((java.util.Map<Integer,Object>) method.invoke(api, session, target));
  }
  @Test public void healthReadsLatestUnaryWithAShortDeadline() throws Exception {
    assertTrue(read(vin).allGood()); assertEquals(MethodDescriptor.MethodType.UNARY, channel.type);
    assertEquals(vin, new String((byte[]) ProtoWire.decode(channel.request).get(2), java.nio.charset.StandardCharsets.UTF_8));
    assertTrue(channel.deadlineMillis > 0 && channel.deadlineMillis <= 4000);
  }
  @Test public void healthCacheAvoidsRepeatedPollsButManualRefreshInvalidatesIt() throws Exception {
    assertTrue(read(vin).allGood()); assertTrue(read(vin).allGood()); assertEquals(1, channel.calls);
    api.invalidateHealth(vin); read(vin); assertEquals(2, channel.calls);
  }
  @Test public void missingServiceAndOtherVehicleNeverBecomeAllGood() throws Exception {
    assertFalse(read("DIFFERENT_VEHICLE").allGood());
    channel.unimplemented = true; assertFalse(read(vin).allGood()); assertTrue(channel.cancelled);
  }
  @Test public void latestUnimplementedFallsBackToOneStreamingReply() throws Exception {
    channel.failLatest=true;assertTrue(read(vin).allGood());assertEquals(2,channel.calls);
    assertEquals(MethodDescriptor.MethodType.SERVER_STREAMING,channel.type);assertTrue(channel.cancelled);
  }
  private static final class FakeChannel extends ManagedChannel {
    byte[] payload, request; boolean cancelled, unimplemented, failLatest; int calls; long deadlineMillis;
    MethodDescriptor.MethodType type;
    @Override @SuppressWarnings("unchecked") public <Q, S> ClientCall<Q, S> newCall(MethodDescriptor<Q, S> descriptor, CallOptions options) {
      calls++; type = descriptor.getType(); deadlineMillis = options.getDeadline().timeRemaining(TimeUnit.MILLISECONDS);
      ClientCall<Q, S> call = new ClientCall<Q, S>() {
        Listener<S> listener;
        public void start(Listener<S> listener, Metadata headers) { this.listener = listener; }
        public void request(int n) {}
        public void cancel(String message, Throwable cause) { cancelled = true; }
        public void halfClose() {
          if (unimplemented || failLatest && descriptor.getFullMethodName().endsWith("/GetLatestHealth")) listener.onClose(Status.UNIMPLEMENTED, new Metadata());
          else { listener.onHeaders(new Metadata()); listener.onMessage((S) payload);
            if(descriptor.getType()==MethodDescriptor.MethodType.UNARY)listener.onClose(Status.OK,new Metadata()); }
        }
        public void sendMessage(Q value) { request = (byte[]) value; }
      };
      io.grpc.Context.current().addListener(context -> call.cancel("context cancelled", null), Runnable::run);
      return call;
    }
    public String authority() { return "fixture.invalid"; }
    public ManagedChannel shutdown() { return this; }
    public boolean isShutdown() { return false; }
    public boolean isTerminated() { return false; }
    public ManagedChannel shutdownNow() { return this; }
    public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
  }
}
