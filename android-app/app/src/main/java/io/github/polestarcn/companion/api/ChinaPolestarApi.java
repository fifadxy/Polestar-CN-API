package io.github.polestarcn.companion.api;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import io.github.polestarcn.companion.DiagnosticLog;
import io.github.polestarcn.companion.VehicleSnapshot;
import io.github.polestarcn.companion.MileageSnapshot;
import io.github.polestarcn.companion.ExteriorSnapshot;
import io.github.polestarcn.companion.EnvironmentSnapshot;
import io.github.polestarcn.companion.ChargingSnapshot;
import io.github.polestarcn.companion.TireSnapshot;
import io.github.polestarcn.companion.VehicleHealthSnapshot;
import io.github.polestarcn.companion.auth.ClientSignatureProvider;
import io.github.polestarcn.companion.auth.SecureSessionStore;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class ChinaPolestarApi {
  private static final String GW = "https://gw.app.polestar-auto.com";
  private static final String DISCOVERY = "https://cnepmob.volvocars.com.cn/";
  private static final String ACCEPT_C3 = "application/volvo.cloud.cnepmob.v1+json";
  private static final long DEFAULT_TOKEN_LIFETIME_SECONDS = 14_400L;
  private static final Object REFRESH_LOCK=new Object();
  // Narrow lock never held during network traffic, so logout cannot hang on an offline server.
  private static final Object SESSION_STATE_LOCK=new Object();
  private static long sessionEpoch;

  private final Context context;
  private final ClientSignatureProvider signatureProvider;
  private ManagedChannel sharedChannel;
  private String sharedHost;
  private final Map<String,Long> targetSocUnavailableUntil=new java.util.concurrent.ConcurrentHashMap<>();
  private final Map<String, HealthCache> healthCache = new java.util.concurrent.ConcurrentHashMap<>();
  private static final class HealthCache {
    final Map<Integer, Object> fields; final long expires;
    HealthCache(Map<Integer, Object> fields, long expires) { this.fields = fields; this.expires = expires; }
  }
  public void invalidateHealth(String vin) {
    HealthCache previous = healthCache.get(vin);
    if (previous != null) healthCache.put(vin, new HealthCache(previous.fields, 0));
  }

  public synchronized void close() {
    if (sharedChannel != null) sharedChannel.shutdownNow();
    sharedChannel = null;
    sharedHost = null;
  }

  private synchronized ManagedChannel channel(String host, int port) {
    String key = host + ":" + port;
    if (sharedChannel == null || sharedChannel.isShutdown() || !key.equals(sharedHost)) {
      close();
      sharedChannel =
          OkHttpChannelBuilder.forAddress(host, port)
              .useTransportSecurity()
              .disableRetry()
              .idleTimeout(1, TimeUnit.MINUTES)
              .userAgent("grpc-java-okhttp/1.68.2")
              .build();
      sharedHost = key;
    }
    return sharedChannel;
  }

  public ChinaPolestarApi(Context context) {
    this.context = context.getApplicationContext();
    this.signatureProvider = new ClientSignatureProvider(this.context);
  }

  public static final class Session {
    public String access = "";
    public String refresh = "";
    public long expiresAt;
    public String c3Host;
    public String c3LbsHost;
    public long revision;
  }

  public static final class VehicleInfo {
    public String vin;
    public String model = "Polestar 2";
    public String modelName = "";
    public String modelYear = "";

    public String displayLabel() {
      String name = first(modelName, model, "Polestar");
      String year = first(modelYear);
      String suffix = vin == null || vin.length() < 4 ? "----" : vin.substring(vin.length() - 4);
      return name + (year.isEmpty() ? "" : " · " + year) + " · VIN…" + suffix;
    }
  }

  public Session login(String phone, String password) throws Exception {
    final long loginEpoch;
    synchronized(SESSION_STATE_LOCK){loginEpoch=sessionEpoch;}
    if (phone == null || phone.trim().isEmpty()) throw new IllegalArgumentException("请输入手机号");
    if (password == null || password.isEmpty()) throw new IllegalArgumentException("请输入密码");

    JSONObject body = new JSONObject();
    body.put("clientId", "polestar-app-android");
    body.put("phone", phone.trim());
    body.put("password", password);
    body.put("autocrt", true);

    JSONObject response = postAccount("/user/api/v2/oauth/login/password", body);
    int code = response.optInt("code", 200);
    JSONObject data = response.optJSONObject("data");
    if (data == null) data = response;
    if (code != 200 && code != 0) {
      throw new Exception(
          "账号登录失败：" + first(response.optString("msg"), response.optString("message"), "服务端拒绝请求"));
    }

    Session s = new Session();
    JSONObject global = firstObject(data, "globleToken", "globalToken", "global_token");
    s.access =
        global == null
            ? ""
            : first(
                global.optString("accessToken"),
                global.optString("access_token"),
                global.optString("token"));
    s.refresh =
        global == null
            ? ""
            : first(global.optString("refreshToken"), global.optString("refresh_token"));
    long expires =
        global == null
            ? 0L
            : firstLong(global.optLong("expiresIn", 0), global.optLong("expires_in", 0));
    if (s.access.isEmpty())
      s.access =
          first(
              scalar(data, "globleToken"),
              scalar(data, "globalToken"),
              scalar(data, "global_token"),
              data.optString("accessToken"),
              data.optString("access_token"),
              data.optString("token"),
              scalar(response, "globleToken"),
              scalar(response, "globalToken"),
              scalar(response, "global_token"));
    if (s.refresh.isEmpty())
      s.refresh =
          first(
              data.optString("refreshToken"),
              data.optString("refresh_token"),
              response.optString("refreshToken"),
              response.optString("refresh_token"));
    if (expires <= 0)
      expires =
          firstLong(
              data.optLong("expiresIn", 0),
              data.optLong("expires_in", 0),
              response.optLong("expiresIn", 0),
              response.optLong("expires_in", 0),
              DEFAULT_TOKEN_LIFETIME_SECONDS);
    if (s.access.isEmpty()) throw new Exception("登录成功响应中没有 C3 global token");
    s.expiresAt = System.currentTimeMillis() + Math.max(60L, expires) * 1000L;
    s.revision = System.currentTimeMillis();
    synchronized(SESSION_STATE_LOCK){
      if(sessionEpoch!=loginEpoch)throw new IllegalStateException("Login cancelled by sign-out");
      healthCache.clear();targetSocUnavailableUntil.clear();
      SecureSessionStore.save(context,s);
    }
    DiagnosticLog.add("China login accepted; session stored securely");
    return s;
  }

  public void logout() {
    // No wait for an in-flight HTTP refresh. Invalidate its right to save before clearing.
    synchronized(SESSION_STATE_LOCK){
      sessionEpoch++;
      SecureSessionStore.clear(context);
    }
    healthCache.clear();targetSocUnavailableUntil.clear();close();
    DiagnosticLog.add("China session cleared");
  }

  public Session session() {
    return SecureSessionStore.load(context);
  }

  public Session refreshIfNeeded(Session original) throws Exception {
    if (original == null) throw new Exception("尚未登录");
    if (System.currentTimeMillis() + 120_000L < original.expiresAt) return original;
    synchronized (REFRESH_LOCK) {
      final long refreshEpoch;
      synchronized(SESSION_STATE_LOCK){refreshEpoch=sessionEpoch;}
      Session current = SecureSessionStore.load(context);
      if (current == null) throw new Exception("尚未登录");
      if (System.currentTimeMillis() + 120_000L < current.expiresAt) return current;
      if (current.refresh == null || current.refresh.isEmpty())
        throw new Exception("访问令牌已过期且没有可用的 refresh token，请重新登录");

      JSONObject body = new JSONObject().put("refreshToken", current.refresh);
      JSONObject response = postAccount("/user/api/v2/oauth/refresh-global-token", body);
      int code = response.optInt("code", 200);
      JSONObject data = response.optJSONObject("data");
      if (data == null) data = response;
      if (code != 200 && code != 0)
        throw new Exception(
            "刷新令牌被服务端拒绝："
                + first(response.optString("msg"), response.optString("message"), "未提供原因"));

      JSONObject global = firstObject(data, "globleToken", "globalToken", "global_token");
      String access =
          global == null
              ? ""
              : first(
                  global.optString("accessToken"),
                  global.optString("access_token"),
                  global.optString("token"));
      if (access.isEmpty())
        access =
            first(
                scalar(data, "globleToken"),
                scalar(data, "globalToken"),
                scalar(data, "global_token"),
                data.optString("accessToken"),
                data.optString("access_token"),
                data.optString("token"),
                response.optString("accessToken"),
                response.optString("access_token"),
                response.optString("token"));
      if (access.isEmpty()) throw new Exception("刷新登录令牌响应中未找到 C3 global token");
      String refresh =
          global == null
              ? ""
              : first(global.optString("refreshToken"), global.optString("refresh_token"));
      refresh =
          first(
              refresh,
              data.optString("refreshToken"),
              data.optString("refresh_token"),
              current.refresh);
      long expires =
          global == null
              ? 0L
              : firstLong(global.optLong("expiresIn", 0), global.optLong("expires_in", 0));
      if (expires <= 0)
        expires =
            firstLong(
                data.optLong("expiresIn", 0),
                data.optLong("expires_in", 0),
                response.optLong("expiresIn", 0),
                response.optLong("expires_in", 0),
                DEFAULT_TOKEN_LIFETIME_SECONDS);
      current.access = access;
      current.refresh = refresh;
      current.expiresAt = System.currentTimeMillis() + Math.max(60L, expires) * 1000L;
      current.revision=System.currentTimeMillis();
      synchronized(SESSION_STATE_LOCK){
        if(sessionEpoch!=refreshEpoch)throw new IllegalStateException("Session ended during refresh");
        SecureSessionStore.save(context,current);
      }
      DiagnosticLog.add("China access token refreshed");
      return current;
    }
  }

  public List<VehicleInfo> discoverVehicles() throws Exception {
    Session s = refreshIfNeeded(requireSession());
    ensureC3Host(s);
    Object value =
        request(
            "https://" + s.c3Host + "/car-information/car",
            "GET",
            "application/json",
            null,
            s.access);
    ArrayList<VehicleInfo> out = new ArrayList<>();
    collectVehicles(value, out);
    if (out.isEmpty()) throw new Exception("账号下没有发现车辆");
    return out;
  }

  public VehicleSnapshot readVehicle(VehicleInfo info) throws Exception {
    Session s = refreshIfNeeded(requireSession());
    ensureC3Host(s);
    String vin = info.vin;
    Map<Integer, Object> battery =
        ProtoWire.nested(
            stateRpc(s, vin, "services.vehiclestates.battery.BatteryService", "GetLatestBattery"),
            3);
    Integer soc = ProtoWire.integer(battery, 2);
    Integer range = ProtoWire.integer(battery, 4);
    String charging =
        ProtoWire.enumText(
            battery, 7, "未指定", "充电中", "空闲", "计划中", "放电中", "错误", "智能充电", "已完成", "智能充电暂停");
    long capturedAt = ProtoWire.timestampMillis(battery, 1);
    if (capturedAt > System.currentTimeMillis() + 300_000L) capturedAt = 0;

    ChargingSnapshot chargeDetails=ChargingSnapshot.fromData(battery);
    if(System.currentTimeMillis()>=targetSocUnavailableUntil.getOrDefault(vin,0L))try {
      Map<Integer,Object> targetReply=ProtoWire.decode(invoke(s,vin,"chronos.services.v1.TargetSocService","GetLatestTargetSoc",
          ProtoWire.bytesField(1,ProtoWire.concat(ProtoWire.stringField(1,java.util.UUID.randomUUID().toString()),ProtoWire.stringField(2,vin)))));
      Object replyVin=targetReply.get(2);if(replyVin instanceof byte[]&&!vin.equals(new String((byte[])replyVin,java.nio.charset.StandardCharsets.UTF_8)))throw new Exception("Target SoC vehicle mismatch");
      chargeDetails=chargeDetails.withTarget(targetReply);targetSocUnavailableUntil.remove(vin);
    }catch(Exception unavailable){if(targetSocUnavailableUntil.size()>=16)targetSocUnavailableUntil.clear();targetSocUnavailableUntil.put(vin,System.currentTimeMillis()+300_000L);DiagnosticLog.add("Target SoC optional read unavailable");}


    MileageSnapshot mileage = MileageSnapshot.unknown();
    try {
      mileage = MileageSnapshot.fromReply(stateRpc(
          s, vin, "services.vehiclestates.odometer.OdometerService", "GetLatestOdometer"));
    } catch (Exception e) {
      // A missing optional mileage response must not discard valid battery/exterior data.
      DiagnosticLog.add("Mileage state unavailable");
    }
    mileage = mileage.withConsumption(battery);

    String lock = "未知";
    String doors = "状态未知";
    String windows = "状态未知";
    String tailgate = "状态未知";
    ExteriorSnapshot individualExterior = ExteriorSnapshot.unknown();
    try {
      Map<Integer, Object> exterior =
          ProtoWire.nested(
              stateRpc(
                  s, vin, "services.vehiclestates.exterior.ExteriorService", "GetLatestExterior"),
              3);
      Integer lockValue = ProtoWire.integer(exterior, 2);
      if (lockValue != null) lock = lockValue == 2 ? "已上锁" : lockValue == 1 ? "未锁" : "未知";
      individualExterior = ExteriorSnapshot.fromData(exterior);
      doors = individualExterior.summary(false);
      windows = individualExterior.summary(true);
      tailgate = ProtoWire.enumText(exterior, 12, "未知", "已打开", "已关闭", "未关严");
    } catch (Exception e) {
      DiagnosticLog.add("Exterior read failed: " + safe(e));
    }

    Map<Integer, Object> health = readHealth(s, vin);
    TireSnapshot tires = TireSnapshot.fromData(health);
    VehicleHealthSnapshot vehicleHealth = VehicleHealthSnapshot.fromData(health, tires);

    EnvironmentSnapshot environment = EnvironmentSnapshot.fromData(
        readFeatureData(s, vin, true), readFeatureData(s, vin, false));
    return new VehicleSnapshot(
        first(info.modelName, info.model, "Polestar"),
        maskVin(vin),
        soc,
        range,
        mileage.wholeKm(),
        charging,
        doors,
        windows,
        tailgate,
        lock,
        capturedAt,
        "LIVE",
        false,
        environment.climate,
        environment.air).withMileage(mileage).withExterior(individualExterior).withEnvironment(environment)
        .withCharging(chargeDetails).withTires(tires).withHealth(vehicleHealth);
  }

  private Map<Integer, Object> readHealth(Session s, String vin) {
    long now = System.currentTimeMillis();
    HealthCache cache = healthCache.get(vin);
    if (cache != null && now < cache.expires) return cache.fields;
    try {
      byte[] body;
      try {body=invoke(s,vin,"services.vehiclestates.health.HealthService","GetLatestHealth",ProtoWire.stringField(2,vin));}
      catch(Exception latest){
        if(latest.getMessage()==null||!latest.getMessage().contains("UNIMPLEMENTED"))throw latest;
        body=invoke(s,vin,"services.vehiclestates.health.HealthService","GetHealth",ProtoWire.stringField(2,vin),true);
      }
      Map<Integer,Object> reply=ProtoWire.decode(body);
      Object returned = reply.get(2);
      if (returned instanceof byte[] && !vin.equals(new String((byte[]) returned, StandardCharsets.UTF_8)))
        throw new IllegalStateException("Health vehicle mismatch");
      Map<Integer, Object> fields = ProtoWire.nested(reply, 3);
      if(fields.isEmpty())throw new IllegalStateException("Health empty reply");
      healthCache.put(vin, new HealthCache(fields, now + 5 * 60_000L));
      return fields;
    } catch (Exception e) {
      // Optional Health failure must not erase independently reported battery/climate data.
      Map<Integer, Object> fields = cache == null ? Collections.emptyMap() : cache.fields;
      healthCache.put(vin, new HealthCache(fields, now + 60_000L));
      DiagnosticLog.add("Optional Health unavailable; retained original report time");
      return fields;
    }
  }

  private String readFeature(Session s, String vin, boolean climate) {
    return ProtoWire.enumText(readFeatureData(s, vin, climate), climate ? 2 : 6, "未知", "开启", "关闭", "处理中");
  }

  private Map<Integer, Object> readFeatureData(Session s, String vin, boolean climate) {
    try {
      Map<Integer, Object> fields =
          ProtoWire.nested(
              stateRpc(
                  s,
                  vin,
                  climate
                      ? "services.vehiclestates.parkingclimatization.ParkingClimatizationService"
                      : "services.vehiclestates.precleaning.PreCleaningService",
                  climate ? "GetLatestParkingClimatization" : "GetLatestPreCleaning"),
              3);
      return fields;
    } catch (Exception e) {
      DiagnosticLog.add("Optional state unavailable: " + (climate ? "climate" : "air"));
      return java.util.Collections.emptyMap();
    }
  }

  public boolean commandReached(String vin, String action) throws Exception {
    Session s = refreshIfNeeded(requireSession());
    ensureC3Host(s);
    if (action.startsWith("climate")) {
      String value = readFeature(s, vin, true);
      return (action.equals("climate-start") ? "开启" : "关闭").equals(value);
    }
    if (action.startsWith("air")) {
      String value = readFeature(s, vin, false);
      return (action.equals("air-start") ? "开启" : "关闭").equals(value);
    }
    Map<Integer, Object> fields =
        ProtoWire.nested(
            stateRpc(
                s, vin, "services.vehiclestates.exterior.ExteriorService", "GetLatestExterior"),
            3);
    if (action.startsWith("window")) {
      boolean targetOpen = action.equals("windows-open");
      for (int field = 7; field <= 10; field++) {
        Integer value = ProtoWire.integer(fields, field);
        if (value == null || value != (targetOpen ? 1 : 2)) return false;
      }
      return true;
    }
    Integer value = ProtoWire.integer(fields, action.startsWith("trunk") ? 12 : 2);
    return value != null
        && value == (action.equals("lock") || action.equals("trunk-close") ? 2 : 1);
  }

  public byte[] windows(String vin, boolean open) throws Exception {
    // Reference APK and upstream Hisingen expose WindowControl. Field 2 uses open/close enum 1/2,
    // matching TailgateControl. China execution still requires device verification.
    return control(
        vin,
        "WindowControl",
        ProtoWire.concat(baseControl(vin), ProtoWire.varintField(2, open ? 1 : 2)));
  }

  public byte[] honkFlash(String vin, int action) throws Exception {
    // Verified against upstream Hisingen: HonkFlash uses one field-2 enum.
    // 0 = honk + flash, 1 = honk only, 2 = flash only.
    if (action < 0 || action > 2) throw new IllegalArgumentException("Unsupported HonkFlash action");
    return control(
        vin,
        "HonkFlash",
        ProtoWire.concat(baseControl(vin), ProtoWire.varintField(2, action)));
  }

  public byte[] tailgate(String vin, boolean open) throws Exception {
    return control(
        vin,
        "TailgateControl",
        ProtoWire.concat(baseControl(vin), ProtoWire.varintField(2, open ? 1 : 2)));
  }

  public byte[] lock(String vin) throws Exception {
    return control(vin, "Lock", ProtoWire.concat(baseControl(vin), ProtoWire.varintField(2, 0)));
  }

  public byte[] purify(String vin, boolean start) throws Exception {
    return control(
        vin,
        "PreCleaning",
        ProtoWire.concat(baseControl(vin), ProtoWire.varintField(2, start ? 1 : 0)));
  }

  public byte[] unlock(String vin) throws Exception {
    return control(vin, "Unlock", baseControl(vin));
  }

  public byte[] climateStop(String vin) throws Exception {
    return control(vin, "ClimatizationStop", baseControl(vin));
  }

  public byte[] climateStart(String vin, float temperatureC) throws Exception {
    io.github.polestarcn.companion.ClimateTarget.validate(temperatureC);
    byte[] body =
        ProtoWire.concat(
            baseControl(vin), ProtoWire.varintField(2, 1), ProtoWire.fixed32Field(3, temperatureC));
    return control(vin, "ClimatizationStart", body);
  }

  private byte[] control(String vin, String method, byte[] body) throws Exception {
    Session s = refreshIfNeeded(requireSession());
    ensureC3Host(s);
    DiagnosticLog.add("Remote command sending: " + method + " VIN..." + suffix(vin));
    byte[] result = invoke(s, vin, "invocation.InvocationService", method, body);
    DiagnosticLog.add("Remote command server accepted: " + method + " bytes=" + result.length);
    return result;
  }

  private byte[] baseControl(String vin) {
    return ProtoWire.bytesField(1, ProtoWire.stringField(1, vin));
  }

  private Map<Integer, Object> stateRpc(Session s, String vin, String service, String method)
      throws Exception {
    return ProtoWire.decode(invoke(s, vin, service, method, ProtoWire.stringField(2, vin)));
  }

  private byte[] invoke(Session s, String vin, String service, String method, byte[] request)
      throws Exception {
    return invoke(s, vin, service, method, request, false);
  }

  private byte[] invoke(Session s, String vin, String service, String method, byte[] request,
      boolean firstStream) throws Exception {
    return invokeOnHost(s,s.c3Host,vin,service,method,request,firstStream);
  }
  private byte[] invokeOnHost(Session s,String host,String vin,String service,String method,byte[] request,boolean firstStream) throws Exception {
    if (host == null || host.isEmpty()) throw new Exception("C3 host missing");
    String cleanHost = host.replace("https://", "").replace("http://", "");
    int port = 443;
    int colon = cleanHost.lastIndexOf(':');
    if (colon > 0) {
      try {
        port = Integer.parseInt(cleanHost.substring(colon + 1));
        cleanHost = cleanHost.substring(0, colon);
      } catch (NumberFormatException ignored) {
      }
    }

    MethodDescriptor.Marshaller<byte[]> marshaller =
        new MethodDescriptor.Marshaller<byte[]>() {
          @Override
          public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value == null ? new byte[0] : value);
          }

          @Override
          public byte[] parse(InputStream stream) {
            try {
              return readAll(stream);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        };
    MethodDescriptor<byte[], byte[]> descriptor =
        MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(firstStream ? MethodDescriptor.MethodType.SERVER_STREAMING : MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(service, method))
            .setRequestMarshaller(marshaller)
            .setResponseMarshaller(marshaller)
            .build();

    Metadata headers = new Metadata();
    headers.put(
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + s.access);
    headers.put(Metadata.Key.of("vin", Metadata.ASCII_STRING_MARSHALLER), vin);

    ManagedChannel channel = channel(cleanHost, port);
    try {
      Channel authenticated =
          ClientInterceptors.intercept(channel, MetadataUtils.newAttachHeadersInterceptor(headers));
      if (firstStream) {
        io.grpc.Context.CancellableContext readContext = io.grpc.Context.current().withCancellation();
        io.grpc.Context previous = readContext.attach();
        try {
          java.util.Iterator<byte[]> replies = ClientCalls.blockingServerStreamingCall(authenticated,
              descriptor, CallOptions.DEFAULT.withDeadlineAfter(4, TimeUnit.SECONDS), request);
          return replies.hasNext() ? replies.next() : new byte[0];
        } finally {
          readContext.detach(previous);
          readContext.cancel(null);
        }
      }
      return ClientCalls.blockingUnaryCall(
          authenticated,
          descriptor,
          CallOptions.DEFAULT.withDeadlineAfter((service.endsWith(".TargetSocService")||service.endsWith(".HealthService")||service.equals("dtlinternet.DtlInternetService"))?4:12, TimeUnit.SECONDS),
          request);
    } catch (StatusRuntimeException e) {
      throw new Exception(
          "C3 " + e.getStatus().getCode() + ": " + first(e.getStatus().getDescription(), "无描述"), e);
    }
  }

  public io.github.polestarcn.companion.LocationSnapshot readLastParkedLocation(String vin) throws Exception {
    Session session=refreshIfNeeded(requireSession());ensureC3LbsHost(session);
    Map<Integer,Object> reply=ProtoWire.decode(invokeOnHost(session,session.c3LbsHost,vin,
        "dtlinternet.DtlInternetService","GetLastParkedLocation",ProtoWire.stringField(1,vin),false));
    io.github.polestarcn.companion.LocationSnapshot location=io.github.polestarcn.companion.LocationSnapshot.fromReply(reply);
    if(location==null)throw new Exception("Location unavailable");return location;
  }
  private static boolean trustedLbsHost(String host) {
    return host!=null && host.matches("(?i)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+volvocars\\.com\\.cn");
  }
  private void ensureC3LbsHost(Session session) throws Exception {
    if(trustedLbsHost(session.c3LbsHost))return;
    session.c3LbsHost=null;
    Object value=request(DISCOVERY,"GET",null,null,null,ACCEPT_C3);
    JSONObject lbs=value instanceof JSONObject?((JSONObject)value).optJSONObject("c3Lbs"):null;
    String host=lbs==null?"":lbs.optString("grpcHost","");
    if(!trustedLbsHost(host))throw new Exception("LBS host unavailable");
    session.c3LbsHost=host;SecureSessionStore.save(context,session);
  }

  private void ensureC3Host(Session s) throws Exception {
    if (s.c3Host != null && !s.c3Host.isEmpty()) return;
    Object value = request(DISCOVERY, "GET", null, null, null, ACCEPT_C3);
    if (!(value instanceof JSONObject)) throw new Exception("C3 服务发现返回格式异常");
    JSONObject c3 = ((JSONObject) value).optJSONObject("c3");
    String host = c3 == null ? "" : c3.optString("grpcHost", "");
    if (host.isEmpty()) throw new Exception("C3 服务发现没有返回主服务");
    s.c3Host = host.replace("https://", "").replace("http://", "");
    SecureSessionStore.save(context, s);
    DiagnosticLog.add("C3 host discovered");
  }

  private JSONObject postAccount(String path, JSONObject body) throws Exception {
    String url = signedUrl(path);
    Object value = request(url, "POST", "application/json", body.toString(), null);
    if (!(value instanceof JSONObject)) throw new Exception("账号服务返回格式异常");
    return (JSONObject) value;
  }

  @android.annotation.SuppressLint("HardwareIds") // Required by the authorized China client compatibility contract.
  private String signedUrl(String path) throws Exception {
    String androidId =
        Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    if (androidId == null || androidId.isEmpty() || "null".equalsIgnoreCase(androidId))
      throw new Exception("无法读取本机 Android ID，登录请求未发送");

    LinkedHashMap<String, String> params = new LinkedHashMap<>();
    params.put("appId", "polestar-app-android");
    params.put("timestamp", Long.toString(System.currentTimeMillis()));
    params.put("nonce", UUID.randomUUID().toString());
    params.put("deviceId", androidId);
    params.put("deviceModel", Build.MODEL == null ? "Android" : Build.MODEL);
    params.put("brand", Build.BRAND == null ? "android" : Build.BRAND.toLowerCase(Locale.ROOT));
    params.put("clientType", "PHONE");
    params.put(
        "systemVer", Build.DISPLAY == null ? "Android " + Build.VERSION.RELEASE : Build.DISPLAY);
    params.put("appVer", "4.25.0");

    ArrayList<String> keys = new ArrayList<>(params.keySet());
    Collections.sort(keys);
    StringBuilder canonical = new StringBuilder();
    for (String key : keys) {
      if (canonical.length() > 0) canonical.append('&');
      canonical.append(rfc3986(key)).append('=').append(rfc3986(params.get(key)));
    }
    String secret = signatureProvider.resolve();
    byte[] digest =
        MessageDigest.getInstance("MD5")
            .digest((secret + canonical + secret).getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder();
    for (byte b : digest) hex.append(String.format(Locale.US, "%02x", b & 0xff));
    params.put("sign", hex.toString());

    StringBuilder query = new StringBuilder();
    for (Map.Entry<String, String> e : params.entrySet()) {
      if (query.length() > 0) query.append('&');
      query.append(rfc3986(e.getKey())).append('=').append(rfc3986(e.getValue()));
    }
    return GW + path + "?" + query;
  }

  private Object request(String url, String method, String contentType, String body, String token)
      throws Exception {
    return request(url, method, contentType, body, token, "application/json");
  }

  private Object request(
      String url, String method, String contentType, String body, String token, String accept)
      throws Exception {
    long start = System.currentTimeMillis();
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setRequestMethod(method);
    c.setConnectTimeout(12_000);
    c.setReadTimeout(18_000);
    c.setRequestProperty("Accept", accept == null ? "application/json" : accept);
    c.setRequestProperty(
        "User-Agent", "PolestarAppCN/RCS/4.25.0b262 Android/" + Build.VERSION.RELEASE);
    if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
    if (body != null) {
      c.setDoOutput(true);
      c.setRequestProperty("Content-Type", contentType == null ? "application/json" : contentType);
      try (OutputStream out = c.getOutputStream()) {
        out.write(body.getBytes(StandardCharsets.UTF_8));
      }
    }
    int status = c.getResponseCode();
    InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
    byte[] bytes = in == null ? new byte[0] : readAll(in);
    c.disconnect();
    DiagnosticLog.add(
        "API "
            + method
            + " "
            + new URL(url).getPath()
            + " status="
            + status
            + " elapsed="
            + (System.currentTimeMillis() - start)
            + "ms bytes="
            + bytes.length);
    String text = new String(bytes, StandardCharsets.UTF_8);
    if (status < 200 || status >= 300) throw new Exception("HTTP " + status);
    if (text.isEmpty()) return new JSONObject();
    Object parsed = new JSONTokener(text).nextValue();
    if (parsed instanceof JSONObject) return parsed;
    if (parsed instanceof JSONArray) return new JSONObject().put("items", parsed);
    throw new Exception("服务返回的数据格式无法识别");
  }

  private Session requireSession() throws Exception {
    Session s = SecureSessionStore.load(context);
    if (s == null || s.access == null || s.access.isEmpty()) throw new Exception("尚未登录中国区车辆账户");
    return s;
  }

  private static void collectVehicles(Object node, List<VehicleInfo> out) {
    if (node instanceof JSONObject) {
      JSONObject o = (JSONObject) node;
      String vin =
          first(
              o.optString("vin"),
              o.optString("vehicleVin"),
              o.optString("vehicleIdentificationNumber"));
      if (vin.matches("[A-HJ-NPR-Z0-9]{17}")) {
        boolean exists = false;
        for (VehicleInfo v : out)
          if (vin.equals(v.vin)) {
            exists = true;
            break;
          }
        if (!exists) {
          VehicleInfo v = new VehicleInfo();
          v.vin = vin;
          v.model = first(o.optString("model"), "Polestar 2");
          v.modelName = o.optString("modelName", "");
          v.modelYear = first(o.optString("modelYear"), o.optString("year"));
          out.add(v);
        }
      }
      java.util.Iterator<String> keys = o.keys();
      while (keys.hasNext()) collectVehicles(o.opt(keys.next()), out);
    } else if (node instanceof JSONArray) {
      JSONArray a = (JSONArray) node;
      for (int i = 0; i < a.length(); i++) collectVehicles(a.opt(i), out);
    }
  }

  private static String rfc3986(String value) throws Exception {
    return URLEncoder.encode(value == null ? "" : value, "UTF-8")
        .replace("+", "%20")
        .replace("%7E", "~");
  }

  private static byte[] readAll(InputStream in) throws Exception {
    try (InputStream stream = in;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = stream.read(buf)) >= 0) if (n > 0) out.write(buf, 0, n);
      return out.toByteArray();
    }
  }

  private static String first(String... values) {
    if (values == null) return "";
    for (String value : values)
      if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim()))
        return value;
    return "";
  }

  private static JSONObject firstObject(JSONObject source, String... keys) {
    if (source == null) return null;
    for (String key : keys) {
      JSONObject value = source.optJSONObject(key);
      if (value != null) return value;
    }
    return null;
  }

  private static String scalar(JSONObject source, String key) {
    if (source == null) return "";
    Object value = source.opt(key);
    return value instanceof String || value instanceof Number ? String.valueOf(value) : "";
  }

  private static String stateSummary(int[] values, String label) {
    if (values == null || values.length == 0) return label + "状态未知";
    boolean allKnown = true;
    int open = 0;
    for (int value : values) {
      if (value < 0) allKnown = false;
      else if (value == 1) open++;
    }
    if (!allKnown) return label + "状态不完整";
    return open == 0 ? "全部关闭" : open + " 个开启";
  }

  private static long firstLong(long... values) {
    for (long v : values) if (v > 0) return v;
    return 0L;
  }

  private static String maskVin(String vin) {
    return vin == null || vin.length() < 4 ? "VIN…" : "VIN…" + vin.substring(vin.length() - 4);
  }

  private static String suffix(String vin) {
    return vin == null || vin.length() < 4 ? "----" : vin.substring(vin.length() - 4);
  }

  private static String safe(Exception e) {
    String m = e.getMessage();
    return m == null ? e.getClass().getSimpleName() : trim(m, 180);
  }

  private static String trim(String s, int max) {
    return s == null ? "" : (s.length() <= max ? s : s.substring(0, max));
  }
}
