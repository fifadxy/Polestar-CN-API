package io.github.polestarcn.companion.auth;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.Test;

/** Hardware-backed auth is validated on devices; GCM envelope is verified here. */
public final class SecureCredentialStoreTest {
  private SecretKey key()throws Exception{KeyGenerator g=KeyGenerator.getInstance("AES");g.init(256);return g.generateKey();}
  @Test public void cipherTextNeverContainsPlainCredentialsAndIsRandomized()throws Exception{
    SecretKey secret=key();
    byte[] plaintext="{\"phone\":\"13800000000\",\"password\":\"TEST-LONG-PASSWORD-ABC\"}".getBytes(StandardCharsets.UTF_8);
    byte[] ciphertext=SecureCredentialStore.encryptWithKey(secret,plaintext);
    assertEquals(1,ciphertext[0]);assertEquals(12,ciphertext[1]);
    assertFalse(new String(ciphertext,StandardCharsets.ISO_8859_1).contains("13800000000"));
    assertFalse(new String(ciphertext,StandardCharsets.ISO_8859_1).contains("TEST-LONG-PASSWORD-ABC"));
    assertArrayEquals(plaintext,SecureCredentialStore.decryptWithKey(secret,ciphertext));
    assertFalse(Arrays.equals(ciphertext,SecureCredentialStore.encryptWithKey(secret,plaintext)));
  }
  @Test public void corruptedEnvelopeAndAuthenticationTagAreRejected()throws Exception{
    SecretKey k=key();byte[] encrypted=SecureCredentialStore.encryptWithKey(k,"sensitive".getBytes(StandardCharsets.UTF_8));
    encrypted[encrypted.length-1]^=1;
    try{SecureCredentialStore.decryptWithKey(k,encrypted);fail("GCM tag must detect modification");}
    catch(javax.crypto.AEADBadTagException expected){}
    encrypted[0]=0;
    try{SecureCredentialStore.decryptWithKey(k,encrypted);fail("Version header must be checked");}
    catch(IllegalArgumentException expected){}
  }
  @Test public void independentKeysCannotDecryptEachOther()throws Exception{
    byte[] ciphertext=SecureCredentialStore.encryptWithKey(key(),"secret".getBytes(StandardCharsets.UTF_8));
    try{SecureCredentialStore.decryptWithKey(key(),ciphertext);fail("Independent Keystore key required");}
    catch(javax.crypto.AEADBadTagException expected){}
  }
}
