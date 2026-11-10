package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import java.util.List;

/** Experimental main-vehicle background. Vehicle data is read once from local samples, never fetched here. */
final class EnergyBackdropView extends View {
  static final String ENABLED="energy_backdrop_enabled", STYLE="energy_backdrop_style", STEP="energy_backdrop_step", METRIC="energy_backdrop_metric";
  private final String vehicle;
  private List<EnergyChartSeries.Bin> data;
  private final int style,step,metric;
  private int serial;
  EnergyBackdropView(Context c,String vehicleId){super(c);vehicle=EnergyLog.validVehicle(vehicleId)?vehicleId:null;
    style=EnergyChartSeries.style(AppPreferences.get(c,STYLE,"curve"));
    step=EnergyChartSeries.granularity(AppPreferences.get(c,STEP,"day"));
    String chosen=AppPreferences.get(c,METRIC,"drive");
    metric="climate".equals(chosen)?1:"distance".equals(chosen)?2:"cost".equals(chosen)?3:0;
    setTag("energy_experimental_backdrop");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    setClickable(false);setFocusable(false);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
  }
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();final int id=++serial;if(vehicle==null)return;
    Context app=getContext().getApplicationContext();
    EnergyLog.WORK.execute(()->{
      try {EnergyLog.Report r=EnergyLog.get(app).readRecentDays(vehicle,31);
        List<EnergyChartSeries.Bin> grouped=EnergyChartSeries.aggregate(r.points,step);
        post(()->{if(id!=serial||!isAttachedToWindow())return;data=grouped;invalidate();});
      }catch(Exception ignored){} // Experimental visual must never interfere with vehicle controls.
    });
  }
  @Override protected void onDetachedFromWindow(){serial++;super.onDetachedFromWindow();}
  @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
    if(data==null||data.isEmpty())return;
    Context c=getContext();EnergyChartPainter.draw(canvas,data,metric,style,EnergyLog.perPercent(c),EnergyLog.price(c),
        PsaDesign.ink(AppPreferences.dark(c)),PsaDesign.muted(AppPreferences.dark(c)),step,true,getResources().getDisplayMetrics().density);
  }
}
