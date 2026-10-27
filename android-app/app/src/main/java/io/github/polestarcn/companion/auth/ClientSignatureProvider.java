package io.github.polestarcn.companion.auth;
import android.content.Context;
import android.util.Base64;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
/** Client configuration is provided by the private build, never discovered from installed apps. */
public final class ClientSignatureProvider {
 private final Context context;private volatile String cached;
 public ClientSignatureProvider(Context context){this.context=context.getApplicationContext();}
 public boolean isReady(){try{resolve();return true;}catch(Exception missing){return false;}}
 public String resolve() throws Exception {
  if(cached!=null)return cached;
  synchronized(this){
   if(cached!=null)return cached;
   try(InputStream in=context.getAssets().open("client-signing.bin")){
    byte[] buffer=new byte[65];int count=0,n;while(count<buffer.length&&(n=in.read(buffer,count,buffer.length-count))!=-1)count+=n;
    String value=new String(buffer,0,count,StandardCharsets.US_ASCII).trim();
    if(value.matches("[0-9A-F]{32}")){cached=value;return cached;}
   }catch(java.io.IOException missing){/* Developer verification builds may omit private client config. */}
   // Preserve already imported configuration when upgrading an older installation.
   String old=context.getSharedPreferences("reference_import",0).getString("encrypted",null);
   if(old!=null)try{String value=new String(SecureSessionStore.decrypt(Base64.decode(old,Base64.NO_WRAP)),StandardCharsets.UTF_8);if(value.matches("[0-9A-F]{32}")){cached=value;return cached;}}catch(Exception unavailable){}
   throw new IllegalStateException("This verification build has no account client configuration. Install the complete signed APK.");
  }
 }
}
