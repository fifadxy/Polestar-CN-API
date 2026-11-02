package io.github.polestarcn.companion;
import android.content.Context;import android.content.Intent;import android.content.pm.PackageManager;import android.content.pm.ResolveInfo;import android.os.Bundle;import java.util.List;
/** Read-only discovery for OPPO Bluetooth-car-key system surfaces. */
public final class OppoCarKeyCapability {
  public static final String ACTION_BIND_BLUETOOTH_KEY="oppo.intent.action.opluscarlink.BIND_BT_KEY";
  public static final String ACTION_VIEW_KEY_CARD_DETAIL="com.nearme.wallet.key.KEY_DETAIL_ACTIVITY";
  public static final String VALUE_CARD_TYPE_BLUETOOTH_CAR_KEY="6";
  private OppoCarKeyCapability() {}
  public static Intent bindIntent(){return new Intent(ACTION_BIND_BLUETOOTH_KEY);}
  public static Intent walletDetailIntent(){return new Intent(ACTION_VIEW_KEY_CARD_DETAIL);}
  public static Bundle status(Context context){PackageManager pm=context.getPackageManager();Bundle out=new Bundle();out.putString("bindAction",ACTION_BIND_BLUETOOTH_KEY);out.putString("walletDetailAction",ACTION_VIEW_KEY_CARD_DETAIL);out.putString("walletCardType",VALUE_CARD_TYPE_BLUETOOTH_CAR_KEY);out.putBoolean("requiresOemKeyProvisioning",true);out.putBoolean("fakeBindingNotAKey",true);
    try{List<ResolveInfo> x=pm.queryIntentActivities(bindIntent(),PackageManager.MATCH_DEFAULT_ONLY);out.putInt("bindHandlerCount",x==null?0:x.size());out.putBoolean("bindSurfaceAvailable",x!=null&&!x.isEmpty());}catch(RuntimeException e){out.putBoolean("bindSurfaceAvailable",false);out.putInt("bindHandlerCount",0);}
    try{List<ResolveInfo> x=pm.queryIntentActivities(walletDetailIntent(),PackageManager.MATCH_DEFAULT_ONLY);out.putInt("walletDetailHandlerCount",x==null?0:x.size());out.putBoolean("walletDetailSurfaceAvailable",x!=null&&!x.isEmpty());}catch(RuntimeException e){out.putBoolean("walletDetailSurfaceAvailable",false);out.putInt("walletDetailHandlerCount",0);}
    out.putBoolean("systemBluetoothLe",pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));return out;}
}