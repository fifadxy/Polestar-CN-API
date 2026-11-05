package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import io.github.libxposed.api.XposedInterface;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** NEW-mode feature-verified in-memory patch. Never writes APK/RPK or bypasses permissions. */
final class AdaptiveQuickAppRpkPatch {
  private AdaptiveQuickAppRpkPatch(){}

  static void install(OppoBridgeModule module) throws Exception {
    Method open=ZipFile.class.getDeclaredMethod("getInputStream",ZipEntry.class);
    open.setAccessible(true);
    module.hook(open).setId("psassistant_beta46_rpk_structural_car_gate")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
          List<Object> args=chain.getArgs();
          ZipEntry entry=args!=null&&args.size()==1&&args.get(0) instanceof ZipEntry
              ?(ZipEntry)args.get(0):null;
          if(entry==null||!supportedEntry(entry.getName())
              ||entry.getSize()>QuickAppCarCapability.MAX_INDEX_SIZE)
            return chain.proceed();
          Application app=currentApplication();
          if(app==null)return chain.proceed();
          String rpkMode=BridgeSettings.rpkModeForCurrentHost();
          if(BridgeSettings.RPK_EXACT.equals(rpkMode)
              &&entry.getSize()!=QuickAppCarCapability.INDEX_SIZE)return chain.proceed();
          Object raw=chain.proceed();
          if(!(raw instanceof InputStream))return raw;
          byte[] source;
          try(InputStream in=(InputStream)raw){source=readBounded(in,QuickAppCarCapability.MAX_INDEX_SIZE);}
          catch(Throwable failure){module.warn("beta46_rpk_read_fallback="+failure.getClass().getSimpleName());
            return chain.proceed();}
          byte[] patched=BridgeSettings.RPK_EXACT.equals(rpkMode)
              ?QuickAppCarCapability.patchExact(source):QuickAppCarCapability.patchFeature(source);
          if(patched==null){
            report(app,false,"structural-gate-rejected",source);
            return new ByteArrayInputStream(source);
          }
          report(app,true,rpkMode,source);
          return new ByteArrayInputStream(patched);
        });
    module.info("beta46_rpk_gate installed feature-or-exact opt-in");
  }

  static boolean supportedEntry(String name){
    return QuickAppCarCapability.INDEX_ENTRY.equals(name)
        ||"cards/SingleDeviceCard2x2/index.js".equals(name);
  }
  private static byte[] readBounded(InputStream input,int max) throws Exception {
    ByteArrayOutputStream out=new ByteArrayOutputStream(32*1024);
    byte[] data=new byte[8192];int n;
    while((n=input.read(data))!=-1){if(out.size()+n>max)throw new IllegalStateException("RPK index too large");
      out.write(data,0,n);}
    return out.toByteArray();
  }
  private static void report(Application app,boolean applied,String reason,byte[] original){
    try {
      Bundle b=new Bundle();b.putBoolean("ready",true);b.putString("phase","beta46-rpk-structural");
      b.putBoolean("rpkPatchApplied",applied);b.putString("rpkPatchReason",reason);
      b.putLong("rpkPatchAt",System.currentTimeMillis());
      b.putString("rpkEntrySha256",QuickAppCarCapability.sha256(original));
      BridgeClient.call(app,"quickapp-runtime-report",b);
    }catch(Throwable ignored){}
  }
  private static Application currentApplication(){
    try{return (Application)Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}
  }
}
