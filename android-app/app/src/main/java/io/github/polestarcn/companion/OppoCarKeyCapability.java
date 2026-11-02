package io.github.polestarcn.companion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import java.util.List;

/** Read-only discovery for OPPO Bluetooth-car-key system surfaces. */
public final class OppoCarKeyCapability {
  public static final String ACTION_BIND_BLUETOOTH_KEY =
      "oppo.intent.action.opluscarlink.BIND_BT_KEY";
  public static final String ACTION_VIEW_KEY_CARD_DETAIL =
      "com.nearme.wallet.key.KEY_DETAIL_ACTIVITY";
  public static final String VALUE_CARD_TYPE_BLUETOOTH_CAR_KEY = "6";

  private OppoCarKeyCapability() {}

  public static Intent bindIntent() { return new Intent(ACTION_BIND_BLUETOOTH_KEY); }
  public static Intent walletDetailIntent() { return new Intent(ACTION_VIEW_KEY_CARD_DETAIL); }

  public static Bundle status(Context context) {
    PackageManager pm=context.getPackageManager();
    Bundle out=new Bundle();
    out.putString("bindAction",ACTION_BIND_BLUETOOTH_KEY);
    out.putString("walletDetailAction",ACTION_VIEW_KEY_CARD_DETAIL);
    out.putString("walletCardType",VALUE_CARD_TYPE_BLUETOOTH_CAR_KEY);
    out.putBoolean("requiresOemKeyProvisioning",true);
    out.putBoolean("fakeBindingNotAKey",true);
    try {
      List<ResolveInfo> bind=pm.queryIntentActivities(bindIntent(),PackageManager.MATCH_DEFAULT_ONLY);
      out.putInt("bindHandlerCount",bind==null?0:bind.size());
      out.putBoolean("bindSurfaceAvailable",bind!=null&&!bind.isEmpty());
    } catch(RuntimeException failure) {
      out.putInt("bindHandlerCount",0); out.putBoolean("bindSurfaceAvailable",false);
    }
    try {
      List<ResolveInfo> detail=pm.queryIntentActivities(walletDetailIntent(),PackageManager.MATCH_DEFAULT_ONLY);
      out.putInt("walletDetailHandlerCount",detail==null?0:detail.size());
      out.putBoolean("walletDetailSurfaceAvailable",detail!=null&&!detail.isEmpty());
    } catch(RuntimeException failure) {
      out.putInt("walletDetailHandlerCount",0); out.putBoolean("walletDetailSurfaceAvailable",false);
    }
    out.putBoolean("systemBluetoothLe",pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    return out;
  }

  public static String describe(Context context) {
    Bundle s=status(context);
    return "OPPO 蓝牙车钥匙框架：绑定入口="+s.getBoolean("bindSurfaceAvailable",false)
        +" · 钱包详情="+s.getBoolean("walletDetailSurfaceAvailable",false)
        +" · BLE="+s.getBoolean("systemBluetoothLe",false)
        +"\n该框架只承接已由车企 App 完成的真实钥匙绑定；伪造已绑定状态不会产生可用于 Polestar PAKM 的密钥。";
  }
}
