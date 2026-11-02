package io.github.polestarcn.oppobridge;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

final class BridgeClient {
  static final Uri URI = Uri.parse("content://io.github.polestarcn.companion.bridge");

  private BridgeClient() {}

  static Bundle capabilities(Context context) {
    return call(context, "capabilities", null);
  }

  static Bundle snapshot(Context context) {
    return call(context, "snapshot", null);
  }

  static Bundle refresh(Context context) {
    return call(context, "refresh", null);
  }

  static Bundle command(Context context, String action) {
    if(capabilities(context).getInt("authorizationPolicyVersion",0)!=1)return failure("main-app-update-required");
    Bundle in = new Bundle();
    in.putString("action", action);
    Bundle result=call(context,"command",in);
    android.app.PendingIntent approval=result.getParcelable("authorizationIntent");
    if(approval!=null)try {
      android.app.ActivityOptions options=android.app.ActivityOptions.makeBasic();
      if(android.os.Build.VERSION.SDK_INT>=34)options.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
      approval.send(context,0,null,null,null,null,options.toBundle());
    }catch(android.app.PendingIntent.CanceledException|RuntimeException unavailable){
      result.putBoolean("accepted",false);result.putString("reason","open-main-app-for-authorization");
    }
    return result;
  }

  static Bundle call(Context context, String method, Bundle extras) {
    ContentResolver resolver = context.getContentResolver();
    try {
      Bundle result = resolver.call(URI, method, null, extras);
      if (result != null) return result;
    } catch (SecurityException rejected) {
      return failure("provider-access-denied");
    } catch (RuntimeException providerUnavailable) {
      // Existing complete PsAssistant installs may predate PsAssistantBridgeProvider.
      // In that case the LSPosed scope injects RuntimeBridgeClient's receiver into
      // the already installed PsAssistant process and reuses its live session.
    }
    if(context.getPackageManager().resolveContentProvider(URI.getAuthority(),0)!=null)return failure("provider-call-unavailable");
    return RuntimeBridgeClient.call(context, method, extras);
  }
  private static Bundle failure(String reason){Bundle out=new Bundle();out.putBoolean("available",false);out.putBoolean("accepted",false);out.putString("reason",reason);return out;}
}
