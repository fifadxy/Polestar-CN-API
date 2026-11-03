package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Exact-build, in-memory release of MyDevices' already-shipped CAR shortcuts.
 *
 * <p>No APK/RPK file is modified. The hook applies only to one entry with a verified size and
 * SHA-256, and returns the original bytes unchanged on every mismatch.</p>
 */
final class QuickAppRpkPatch {
  private static final AtomicBoolean REPORTED = new AtomicBoolean();

  private QuickAppRpkPatch() {}

  static void install(OppoBridgeModule module) throws Exception {
    Method getInputStream=ZipFile.class.getDeclaredMethod("getInputStream",ZipEntry.class);
    getInputStream.setAccessible(true);
    module.hook(getInputStream)
        .setId("psassistant_mydevices_17415_rpk_car_gate")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain->{
          List<Object> args=chain.getArgs();
          ZipEntry entry=args==null||args.isEmpty()||!(args.get(0) instanceof ZipEntry)
              ?null:(ZipEntry)args.get(0);
          if(entry==null||!QuickAppCarCapability.INDEX_ENTRY.equals(entry.getName())
              ||entry.getSize()!=QuickAppCarCapability.INDEX_SIZE)
            return chain.proceed();

          Application app=currentApplication();
          if(app==null||!experimentEnabled(app))return chain.proceed();
          Object raw=chain.proceed();
          if(!(raw instanceof InputStream))return raw;
          byte[] source;
          try(InputStream input=(InputStream)raw) {
            source=readExact(input,QuickAppCarCapability.INDEX_SIZE);
          } catch(Throwable failure) {
            module.warn("quickapp_rpk_patch read="+failure.getClass().getSimpleName());
            return raw;
          }
          byte[] patched=QuickAppCarCapability.patchExact(source);
          if(patched==null) {
            report(app,module,false,QuickAppCarCapability.sha256(source),"exact-gate-mismatch");
            return new ByteArrayInputStream(source);
          }
          report(app,module,true,QuickAppCarCapability.sha256(source),"exact-memory-patch");
          return new ByteArrayInputStream(patched);
        });
    module.info("quickapp_rpk_patch ready=true entry="+QuickAppCarCapability.INDEX_ENTRY);
  }

  private static boolean experimentEnabled(Application app) {
    try {return BridgeClient.call(app,"oppo-oem-car-status",null).getBoolean("enabled",false);}
    catch(Throwable ignored){return false;}
  }

  private static byte[] readExact(InputStream input,int expected) throws Exception {
    ByteArrayOutputStream out=new ByteArrayOutputStream(expected+128);
    byte[] block=new byte[8192];int total=0,read;
    while((read=input.read(block))!=-1) {
      total+=read;
      if(total>expected)throw new IllegalStateException("RPK entry larger than exact gate");
      out.write(block,0,read);
    }
    if(total!=expected)throw new IllegalStateException("RPK entry size changed");
    return out.toByteArray();
  }

  private static void report(Application app,OppoBridgeModule module,boolean applied,String sha,String reason) {
    if(!REPORTED.compareAndSet(false,true))return;
    try {
      Bundle in=new Bundle();
      in.putBoolean("ready",true);in.putString("phase","rpk-memory-patch");
      in.putString("moduleVersion",BridgeBuild.VERSION_NAME);in.putString("hostVersion",QuickAppCarCapability.HOST_VERSION);
      in.putBoolean("rpkPatchApplied",applied);in.putInt("rpkPatchCount",applied?1:0);
      in.putLong("rpkPatchAt",System.currentTimeMillis());in.putString("rpkPatchReason",reason);
      in.putString("rpkEntrySha256",sha);
      BridgeClient.call(app,"quickapp-runtime-report",in);
      module.info("quickapp_rpk_patch applied="+applied+" reason="+reason+" sha="+sha);
    } catch(Throwable ignored) {}
  }

  private static Application currentApplication() {
    try {return (Application)Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }
}
