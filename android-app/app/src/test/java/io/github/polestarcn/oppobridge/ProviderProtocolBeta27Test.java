package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.content.*;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,shadows=ProviderProtocolBeta27Test.IdentityBinder.class)
public class ProviderProtocolBeta27Test {
  // Simulate Binder's documented identity switching; ShadowBinder otherwise stubs the native calls.
  @Implements(Binder.class) public static class IdentityBinder extends ShadowBinder {
    @Implementation protected static long clearCallingIdentity(){int old=Binder.getCallingUid();ShadowBinder.setCallingUid(android.os.Process.myUid());return old;}
    @Implementation protected static void restoreCallingIdentity(long old){ShadowBinder.setCallingUid((int)old);}
  }
  public static class Upstream extends ContentProvider {
    int uid,calls;boolean fail;Bundle snapshot=new Bundle();
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){uid=Binder.getCallingUid();calls++;if(fail)throw new IllegalStateException("test failure");Bundle b=new Bundle(snapshot);b.putBoolean("cardEnabled",true);return b;}
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){return null;}
    @Override public int delete(Uri u,String s,String[] a){return 0;}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
  }
  private Context c;private VehicleDeviceCardProvider provider;private Upstream upstream;private int host;
  @Before public void setup(){
    c=RuntimeEnvironment.getApplication();c.getSharedPreferences("vehicle-card",0).edit().clear().commit();
    upstream=new Upstream();ProviderInfo bridge=new ProviderInfo();bridge.authority=BridgeClient.URI.getAuthority();upstream.attachInfo(c,bridge);
    ShadowContentResolver.registerProviderInternal(bridge.authority,upstream);
    upstream.snapshot.putBoolean("available",true);upstream.snapshot.putLong("capturedAt",System.currentTimeMillis());
    provider=new VehicleDeviceCardProvider();ProviderInfo info=new ProviderInfo();info.authority=VehicleDeviceCardProvider.AUTHORITY;provider.attachInfo(c,info);
    host=android.os.Process.myUid()+900;shadowOf(c.getPackageManager()).setPackagesForUid(host,new String[]{"com.heytap.mydevices"});ShadowBinder.setCallingUid(host);
  }
  @After public void close(){ShadowBinder.reset();provider.shutdown();}
  @Test public void hostQueryRelaysAsProviderAndRestoresCaller(){
    try(Cursor rows=provider.query(VehicleDeviceCardProvider.URI.buildUpon().appendQueryParameter("version","2").build(),null,null,null,null)){
      assertEquals(1,rows.getCount());assertEquals("row-ready",rows.getExtras().getString("reason"));
    }
    assertEquals(android.os.Process.myUid(),upstream.uid);assertEquals(host,Binder.getCallingUid());assertEquals(2,upstream.calls);
  }
  @Test public void onlyAuthenticatedHostCanEnterRelay(){
    ShadowBinder.setCallingUid(host+1);
    try{provider.query(VehicleDeviceCardProvider.URI,null,null,null,null);fail();}catch(SecurityException expected){}
    assertEquals(0,upstream.calls);assertEquals(host+1,Binder.getCallingUid());
  }
  @Test public void missingSnapshotHasExplicitEmptyCursorReason(){
    upstream.snapshot.putBoolean("available",false);
    try(Cursor rows=provider.query(VehicleDeviceCardProvider.URI,null,null,null,null)){assertEquals(0,rows.getCount());assertEquals("no-snapshot",rows.getExtras().getString("reason"));}
  }
  @Test public void failureRestoresIdentityAndDoesNotBroadcastASecondWrite(){
    upstream.fail=true;Bundle answer=provider.call("card_show",null,null);
    assertFalse(answer.getBoolean("result"));assertEquals(host,Binder.getCallingUid());assertEquals(1,upstream.calls);
  }
  @Test public void oldMainAppCannotReceiveAWriteThatBypassesItsVerificationPolicy(){
    ShadowBinder.setCallingUid(android.os.Process.myUid());
    Bundle answer=BridgeClient.command(c,"unlock");
    assertFalse(answer.getBoolean("accepted"));assertEquals("main-app-update-required",answer.getString("reason"));assertEquals(1,upstream.calls);
  }
  @Test public void selectionMatchesTheRequestedColumnExactly(){
    assertTrue(VehicleDeviceCardProvider.matchesSelection("device_id = ?",new String[]{"id"},"id","mac"));
    assertFalse(VehicleDeviceCardProvider.matchesSelection("device_id = ?",new String[]{"mac"},"id","mac"));
    assertTrue(VehicleDeviceCardProvider.matchesSelection("device_mac=?",new String[]{"mac"},"id","mac"));
    try{VehicleDeviceCardProvider.matchesSelection("device_id != ?",new String[]{"id"},"id","mac");fail();}catch(IllegalArgumentException expected){}
  }
  @Test public void foreignUriNeverSelectsOwnDevice(){
    for(String path:new String[]{"content://other/device_info_table_new",VehicleDeviceCardProvider.URI+"/other"})
      try{provider.query(Uri.parse(path),null,null,null,null);fail();}catch(IllegalArgumentException expected){}
    assertEquals(host,Binder.getCallingUid());assertEquals(0,upstream.calls);
  }
  @Test public void bothPackagedAuthoritiesUseTheSameStrictTablePath(){
    VehicleDeviceCardProvider.requireUri(Uri.parse("content://"+BridgeIdentity.STANDALONE_AUTHORITY+"/device_info_table_new"));
    VehicleDeviceCardProvider.requireUri(Uri.parse("content://"+BridgeIdentity.INTEGRATED_AUTHORITY+"/device_info_table_new"));
    try{VehicleDeviceCardProvider.requireUri(Uri.parse("content://"+BridgeIdentity.INTEGRATED_AUTHORITY+"/other"));fail();}
    catch(IllegalArgumentException expected){}
  }
  @Test public void staleOrUnknownStateDoesNotPublishGuessingShortcuts()throws Exception {
    Bundle state=new Bundle();state.putLong("capturedAt",System.currentTimeMillis());state.putString("lock","未知");
    assertEquals(0,VehicleDeviceCardProvider.deviceData(state,"id","mac").getJSONArray("shortcuts").length());
    state.putString("lock","未锁");assertEquals(1,VehicleDeviceCardProvider.deviceData(state,"id","mac").getJSONArray("shortcuts").length());
    state.putLong("capturedAt",System.currentTimeMillis()-6*60_000L);assertEquals(0,VehicleDeviceCardProvider.deviceData(state,"id","mac").getJSONArray("shortcuts").length());
  }
}
