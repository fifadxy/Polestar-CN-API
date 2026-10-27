package com.iwall.tech.security;

import android.content.Context;

/**
 * Minimal JNI declaration surface required by libglble.so JNI_OnLoad.
 *
 * Method names/signatures are recovered from the user-provided APK's DEX and
 * the native RegisterNatives table. This class contains no OEM implementation
 * and the V2 research UI does not invoke these methods.
 */
public final class GLAlgorithmJni {
  public GLAlgorithmJni() {}

  public native byte[][] aes_decrypt(int mode, byte[] data, byte[] key, byte[] iv);
  public native byte[][] aes_encrypt(int mode, byte[] data, byte[] key, byte[] iv);
  public native byte[][] aes_gcm_decrypt(byte[] data, byte[] key, byte[] iv);
  public native byte[][] aes_gcm_encrypt(byte[] data, byte[] key, byte[] iv);
  public native short crc16(byte[] data);
  public native int crc32(byte[] data);
  public native byte crc8(byte[] data);
  public native byte[] getDeviceFingerprint(Context context);
  public native byte[] hmac_sha1(byte[] data, byte[] key);
  public native byte[] hmac_sha256(byte[] data, byte[] key);
  public native byte[] sha1(byte[] data);
  public native byte[] sha256(byte[] data);
  public native String version();
}
