package io.github.polestarcn.oppobridge;

import android.view.View;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Exact-build home view adapter, scoped to our CAR row and provider identity. */
final class NativeHomeCardHook {
  static void install(OppoBridgeModule module,ClassLoader loader) throws Exception {
    Class<?> host=loader.loadClass("com.oplus.mydevices.deviceui.devicecard.view.homecard.HomeCardView");
    Class<?> data=loader.loadClass("com.oplus.mydevices.domain.entities.cards.QuickCardDeviceData");
    Method bind=host.getDeclaredMethod("D",data);
    bind.setAccessible(true);
    Method content=host.getMethod("i",View.class);
    Method pkg=data.getMethod("getPackageName"),type=data.getMethod("getType"),id=data.getMethod("getId");
    Field boundId=host.getDeclaredField("j");boundId.setAccessible(true);
    Field engineContent=host.getDeclaredField("i");engineContent.setAccessible(true);
    module.hook(bind).setId("psassistant_native_home_card").setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain->{
      Object row=chain.getArgs().get(0);
      if(row!=null)try {
        String rowPkg=String.valueOf(pkg.invoke(row));
        String rowType=String.valueOf(type.invoke(row));
        String rowId=String.valueOf(id.invoke(row));
        if(rowId.startsWith("psassistant_"))
          module.info("native_home_card candidate pkg="+rowPkg+" type="+rowType+" id="+rowId);
        if(NativeVehicleHomeView.isOurCard(rowPkg,rowType,rowId)) {
          View view=(View)chain.getThisObject();
          // Fail closed on OTA changes; no generic name/parameter guessing.
          String version=view.getContext().getPackageManager().getPackageInfo("com.heytap.mydevices",0).versionName;
          if("17.4.15".equals(version)) {
            NativeVehicleHomeView nativeView=new NativeVehicleHomeView(view.getContext());
            nativeView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1,-2));
            content.invoke(view,nativeView);
            boundId.set(view,rowId);
            engineContent.setBoolean(view,true);
            module.info("native_home_card rendered=true");
            return null;
          }
        }
      }catch(Throwable failure){module.warn("native_home_card fallback="+failure.getClass().getSimpleName());}
      return chain.proceed();
    });
  }
}
