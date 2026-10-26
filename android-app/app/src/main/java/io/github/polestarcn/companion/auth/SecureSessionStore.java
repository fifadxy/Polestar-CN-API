package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import io.github.polestarcn.companion.api.ChinaPolestarApi;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

public final class SecureSessionStore {
  private static final String PREF = "cn_vehicle_session_secure";
  private static final String KEY_BLOB = "session_blob_v1";
  private static final String ALIAS = "polestar_cn_vehicle_session_aes_v1";

  private SecureSessionStore() {}

  public static long refreshedAt(Context context) {
    return context.getSharedPreferences(PREF, 0).getLong("token_refreshed_at", 0);
  }

  public static boolean hasSession(Context context) {
    return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).contains(KEY_BLOB);
  }

  public static void save(Context context, ChinaPolestarApi.Session s) throws Exception {
    JSONObject o = new JSONObject();
    o.put("access", s.access);
    o.put("refresh", s.refresh);
    o.put("expiresAt", s.expiresAt);
    o.put("c3Host", s.c3Host == null ? "" : s.c3Host);
    o.put("revision", s.revision);
    byte[] ivAndCipher = encrypt(o.toString().getBytes(StandardCharsets.UTF_8));
    context
        .getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_BLOB, Base64.encodeToString(ivAndCipher, Base64.NO_WRAP))
        .putLong("token_refreshed_at", s.revision)
        .apply();
  }

  public static ChinaPolestarApi.Session load(Context context) {
    String blob =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_BLOB, null);
    if (blob == null || blob.isEmpty()) return null;
    try {
      byte[] plain = decrypt(Base64.decode(blob, Base64.NO_WRAP));
      JSONObject o = new JSONObject(new String(plain, StandardCharsets.UTF_8));
      ChinaPolestarApi.Session s = new ChinaPolestarApi.Session();
      s.access = o.optString("access", "");
      s.refresh = o.optString("refresh", "");
      s.expiresAt = o.optLong("expiresAt", 0L);
      s.c3Host = emptyToNull(o.optString("c3Host", ""));
      s.revision = o.optLong("revision", 0L);
      if (s.access.isEmpty()) return null;
      return s;
    } catch (Exception e) {
      clear(context);
      return null;
    }
  }

  public static void clear(Context context) {
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
  }

  static byte[] encrypt(byte[] plain) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key());
    byte[] iv = cipher.getIV();
    byte[] encrypted = cipher.doFinal(plain);
    byte[] out = new byte[1 + iv.length + encrypted.length];
    out[0] = (byte) iv.length;
    System.arraycopy(iv, 0, out, 1, iv.length);
    System.arraycopy(encrypted, 0, out, 1 + iv.length, encrypted.length);
    return out;
  }

  static byte[] decrypt(byte[] blob) throws Exception {
    if (blob.length < 14) throw new IllegalArgumentException("bad session blob");
    int ivLen = blob[0] & 0xff;
    if (ivLen < 12 || 1 + ivLen >= blob.length) throw new IllegalArgumentException("bad iv");
    byte[] iv = new byte[ivLen];
    System.arraycopy(blob, 1, iv, 0, ivLen);
    byte[] encrypted = new byte[blob.length - 1 - ivLen];
    System.arraycopy(blob, 1 + ivLen, encrypted, 0, encrypted.length);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
    return cipher.doFinal(encrypted);
  }

  private static SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    java.security.Key existing = store.getKey(ALIAS, null);
    if (existing instanceof SecretKey) return (SecretKey) existing;

    KeyGenerator generator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
    KeyGenParameterSpec spec =
        new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build();
    generator.init(spec);
    return generator.generateKey();
  }

  private static String emptyToNull(String s) {
    return s == null || s.isEmpty() ? null : s;
  }
}
