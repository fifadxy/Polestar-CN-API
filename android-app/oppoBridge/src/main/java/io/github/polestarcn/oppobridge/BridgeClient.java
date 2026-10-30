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
    Bundle in = new Bundle();
    in.putString("action", action);
    return call(context, "command", in);
  }

  static Bundle call(Context context, String method, Bundle extras) {
    ContentResolver resolver = context.getContentResolver();
    Bundle result = resolver.call(URI, method, null, extras);
    return result == null ? Bundle.EMPTY : result;
  }
}
