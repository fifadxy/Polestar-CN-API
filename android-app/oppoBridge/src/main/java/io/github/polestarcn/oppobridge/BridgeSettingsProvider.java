package io.github.polestarcn.oppobridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

/** Exported read-only settings snapshot; mutation remains restricted to the module UID. */
public final class BridgeSettingsProvider extends ContentProvider {
  @Override public boolean onCreate() { return true; }

  @Override public Bundle call(String method, String arg, Bundle extras) {
    if (BridgeSettings.METHOD_SNAPSHOT.equals(method))
      return BridgeSettings.localSnapshot(contextOrThrow());
    if (BridgeSettings.METHOD_UPDATE.equals(method)) {
      if (Binder.getCallingUid() != Process.myUid()) throw new SecurityException("Module UID required");
      if (extras == null) throw new IllegalArgumentException("Missing settings payload");
      String key = extras.getString("key", "");
      BridgeSettings.updateLocal(contextOrThrow(), key, extras.getBoolean("value"));
      Bundle out = BridgeSettings.localSnapshot(contextOrThrow());
      out.putBoolean("accepted", true);
      return out;
    }
    throw new IllegalArgumentException("Unsupported settings method");
  }

  private android.content.Context contextOrThrow() {
    android.content.Context context = getContext();
    if (context == null) throw new IllegalStateException("No provider context");
    return context;
  }

  @Override public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) { return null; }
  @Override public String getType(Uri uri) { return null; }
  @Override public Uri insert(Uri uri, ContentValues values) { return null; }
  @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
  @Override public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) { return 0; }
}
