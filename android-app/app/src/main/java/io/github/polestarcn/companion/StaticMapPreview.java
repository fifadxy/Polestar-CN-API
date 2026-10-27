package io.github.polestarcn.companion;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.function.Consumer;
/** User-requested Amap preview, called only from the parking detail dialog after local opt-in. */
public final class StaticMapPreview {
  private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"PsaMapPreview");t.setDaemon(true);return t;});
  private StaticMapPreview(){}
  public static Uri uri(LocationSnapshot location,String key){
    String point=String.format(Locale.US,"%.6f,%.6f",location.longitude,location.latitude);
    return Uri.parse("https://restapi.amap.com/v3/staticmap").buildUpon()
      .appendQueryParameter("location",point).appendQueryParameter("zoom","15").appendQueryParameter("size","640*360")
      .appendQueryParameter("scale","1").appendQueryParameter("markers","mid,0xFF6B00,A:"+point).appendQueryParameter("key",key).build();
  }
  public static Future<?> load(LocationSnapshot location,String key,Consumer<Bitmap> complete){
    return IO.submit(()->{Bitmap image=null;HttpURLConnection connection=null;
      try{connection=(HttpURLConnection)new URL(uri(location,key).toString()).openConnection();connection.setConnectTimeout(6000);connection.setReadTimeout(8000);
        if(connection.getResponseCode()!=200)throw new java.io.IOException("map unavailable");
        try(InputStream in=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
          byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted()||bytes.size()+n>2*1024*1024)throw new java.io.IOException("map unavailable");bytes.write(buffer,0,n);}
          byte[] data=bytes.toByteArray();image=BitmapFactory.decodeByteArray(data,0,data.length);
        }
      }catch(Exception ignored){/* Never expose the key or coordinates in an error or log. */}
      finally{if(connection!=null)connection.disconnect();}
      Bitmap result=image;new Handler(Looper.getMainLooper()).post(()->complete.accept(result));
    });
  }
}
