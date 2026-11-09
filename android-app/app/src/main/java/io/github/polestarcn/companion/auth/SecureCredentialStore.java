package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Opt-in credential vault, separate from token storage, inaccessible from app IPC. */
public final class SecureCredentialStore {
  private static final String PREF="psa_cn_saved_login_v1";
  private static final String KEY="encrypted_account_v1";
  private static final String ALIAS="psa_cn_user_auth_aes_256_v1";
  private static final byte[] AAD="PsAssistant/China/SavedCredentials/v1".getBytes(StandardCharsets.UTF_8);
  private static final int MAX_BLOB=12000;
  private SecureCredentialStore(){}
  public static final class Credentials implements AutoCloseable {
    public final String phone;
    private char[] password;
    private Credentials(String p,char[] secret){phone=p;password=secret;}
    public String password(){if(password==null)throw new IllegalStateException("Credential has been closed");return new String(password);}
    @Override public void close(){if(password!=null){Arrays.fill(password,'\0');password=null;}}
  }
  public static boolean hasSaved(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).contains(KEY);}
  public static void save(Context c,String phone,char[] password)throws Exception{
    if(phone==null||phone.trim().length()<4||phone.trim().length()>40||password==null||password.length==0||password.length>4096)
      throw new IllegalArgumentException("Invalid credentials");
    JSONObject o=new JSONObject();o.put("schema",1);o.put("phone",phone.trim());o.put("password",new String(password));
    byte[] plain=o.toString().getBytes(StandardCharsets.UTF_8);
    try{
      byte[] secure=encryptWithKey(key(),plain);
      if(secure.length>MAX_BLOB)throw new IllegalArgumentException("Oversized encrypted credential");
      boolean written=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
          .putString(KEY,Base64.encodeToString(secure,Base64.NO_WRAP)).commit();
      Arrays.fill(secure,(byte)0);
      if(!written)throw new IllegalStateException("Unable to save credentials securely");
    }finally{Arrays.fill(plain,(byte)0);}
  }
  public static Credentials load(Context c)throws Exception{
    String saved=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,null);
    if(saved==null||saved.isEmpty())return null;
    if(saved.length()>MAX_BLOB*2)throw new IllegalStateException("Credential data invalid");
    byte[] ciphertext=Base64.decode(saved,Base64.NO_WRAP),plain=null;
    try{
      plain=decryptWithKey(key(),ciphertext);
      JSONObject data=new JSONObject(new String(plain,StandardCharsets.UTF_8));
      if(data.optInt("schema")!=1)throw new IllegalArgumentException("Credential schema unsupported");
      String phone=data.optString("phone",""),password=data.optString("password","");
      if(phone.isEmpty()||password.isEmpty())throw new IllegalArgumentException("Credential incomplete");
      return new Credentials(phone,password.toCharArray());
    }finally{
      Arrays.fill(ciphertext,(byte)0);
      if(plain!=null)Arrays.fill(plain,(byte)0);
    }
  }
  public static void clear(Context c){
    c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear().commit();
    try{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);store.deleteEntry(ALIAS);}
    catch(Exception ignored){}
  }
  static byte[] encryptWithKey(SecretKey key,byte[] plain)throws Exception{
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key);c.updateAAD(AAD);
    byte[] iv=c.getIV(),ct=c.doFinal(plain);
    byte[] out=new byte[2+iv.length+ct.length];out[0]=1;out[1]=(byte)iv.length;
    System.arraycopy(iv,0,out,2,iv.length);System.arraycopy(ct,0,out,2+iv.length,ct.length);
    Arrays.fill(ct,(byte)0);return out;
  }
  static byte[] decryptWithKey(SecretKey key,byte[] blob)throws Exception{
    if(blob==null||blob.length<30||blob.length>MAX_BLOB||blob[0]!=1)throw new IllegalArgumentException("Invalid credential envelope");
    int size=blob[1]&0xff;
    if(size!=12||blob.length<=size+18)throw new IllegalArgumentException("Invalid credential nonce");
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(blob,2,2+size)));
    cipher.updateAAD(AAD);
    return cipher.doFinal(blob,2+size,blob.length-2-size);
  }
  private static SecretKey key()throws Exception{
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    java.security.Key existing=ks.getKey(ALIAS,null);
    if(existing instanceof SecretKey)return (SecretKey)existing;
    KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    KeyGenParameterSpec.Builder spec=new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256).setUserAuthenticationRequired(true);
    if(Build.VERSION.SDK_INT>=30){
      spec.setUserAuthenticationParameters(60,KeyProperties.AUTH_BIOMETRIC_STRONG|KeyProperties.AUTH_DEVICE_CREDENTIAL);
    }else{
      spec.setUserAuthenticationValidityDurationSeconds(60);
    }
    generator.init(spec.build());return generator.generateKey();
  }
}
