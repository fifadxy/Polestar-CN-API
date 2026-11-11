package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.InsetDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class EnergyChartV126UiTest {
  @Test public void coloredSelectedTabsHaveIndependentInsetBounds(){
    Context c=RuntimeEnvironment.getApplication();
    AppPreferences.set(c,"language","zh");AppPreferences.set(c,"energy_chart_value_labels","latest");
    EnergySummaryPanel panel=new EnergySummaryPanel(c,null,null);
    LinearLayout metrics=(LinearLayout)panel.getChildAt(10);
    assertEquals(4,metrics.getChildCount());
    for(int i=0;i<4;i++){
      TextView tab=(TextView)metrics.getChildAt(i);
      assertTrue("Buttons must use inset backgrounds",tab.getBackground() instanceof InsetDrawable);
      assertTrue(tab.getMaxLines()==1);
      assertNotNull("Each control must have separate margins",tab.getLayoutParams());
    }
    assertTrue(metrics.getChildAt(0).isSelected());
    assertTrue(metrics.getChildAt(3).performClick());
    assertFalse(metrics.getChildAt(0).isSelected());
    assertTrue(metrics.getChildAt(3).isSelected());
    panel.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1700,View.MeasureSpec.AT_MOST));
    panel.layout(0,0,320,panel.getMeasuredHeight());
    for(int i=1;i<4;i++){
      assertTrue("Tab layouts must not overlap",metrics.getChildAt(i-1).getRight()<metrics.getChildAt(i).getLeft());
    }
  }
  @Test public void modeCyclesLatestAllAndHiddenAndRespectsOffState(){
    Context c=RuntimeEnvironment.getApplication();
    AppPreferences.set(c,"language","zh");AppPreferences.set(c,"energy_chart_value_labels","latest");
    EnergySummaryPanel panel=new EnergySummaryPanel(c,null,null);
    LinearLayout modes=panel.findViewWithTag("energy_style_controls");
    assertEquals(4,modes.getChildCount());
    TextView toggle=panel.findViewWithTag("energy_value_labels_toggle");
    assertNotNull(toggle);
    assertTrue(toggle.getText().toString().contains("最新"));
    assertTrue(toggle.performClick());assertTrue(toggle.getText().toString().contains("全部"));
    assertTrue(toggle.performClick());assertTrue(toggle.getText().toString().contains("隐藏"));
    assertFalse(toggle.isSelected());
    assertTrue(toggle.performClick());assertTrue(toggle.getText().toString().contains("最新"));
    assertEquals("latest",AppPreferences.get(c,"energy_chart_value_labels",""));
  }
  @Test public void labelOverlapWithOrangeBarRequiresContrastingText(){
    assertTrue(EnergyChartPainter.intersects(10,10,75,25,40,0,60,100));
    assertFalse(EnergyChartPainter.intersects(10,10,30,25,40,0,60,100));
    assertFalse(EnergyChartPainter.intersects(10,10,40,25,40,0,60,100));
  }
  @Test public void historyCalloutsChangePaintButNotSampleValues(){
    List<EnergyChartSeries.Bin> bins=new ArrayList<>();
    for(int i=0;i<3;i++){
      EnergyChartSeries.Bin bin=new EnergyChartSeries.Bin(System.currentTimeMillis()+i*86400000L);
      bin.count=2;bin.drive=i+1;bins.add(bin);
    }
    Bitmap latest=Bitmap.createBitmap(600,190,Bitmap.Config.ARGB_8888);
    Bitmap hidden=Bitmap.createBitmap(600,190,Bitmap.Config.ARGB_8888);
    EnergyChartPainter.draw(new Canvas(latest),bins,0,EnergyChartSeries.BARS,.7,1,Color.BLACK,Color.GRAY,
        EnergyChartSeries.DAILY,false,1f,EnergyChartDisplay.LATEST,-1);
    EnergyChartPainter.draw(new Canvas(hidden),bins,0,EnergyChartSeries.BARS,.7,1,Color.BLACK,Color.GRAY,
        EnergyChartSeries.DAILY,false,1f,EnergyChartDisplay.HIDDEN,-1);
    assertFalse("VI leader and value must be visible by default",latest.sameAs(hidden));
    assertEquals(1d,bins.get(0).drive,.0001);
    assertEquals(3d,bins.get(2).drive,.0001);
    latest.recycle();hidden.recycle();
  }
  @Test public void floatingYAxisRemainsStationaryWhenChartIsPanned(){
    List<EnergyChartSeries.Bin> bins=new ArrayList<>();
    for(int i=0;i<15;i++){
      EnergyChartSeries.Bin bin=new EnergyChartSeries.Bin(1735689600000L+i*86400000L);
      bin.count=1;bin.drive=i+1;bins.add(bin);
    }
    Bitmap first=drawAtScroll(bins,0),panned=drawAtScroll(bins,160);
    int axisRight=(int)EnergyChartPainter.plotLeft(1f);
    for(int x=0;x<axisRight;x++)for(int y=0;y<190;y++)
      assertEquals("Floating Y-axis must not scroll with the plot at x="+x+" y="+y,
          first.getPixel(x,y),panned.getPixel(x,y));
    boolean seriesMoved=false;
    for(int x=axisRight+20;x<400&&!seriesMoved;x+=8)for(int y=40;y<155;y+=8)
      if(first.getPixel(x,y)!=panned.getPixel(x,y)){seriesMoved=true;break;}
    assertTrue("Chart bars should scroll independently of the pinned axis",seriesMoved);
    first.recycle();panned.recycle();
  }
  private static Bitmap drawAtScroll(List<EnergyChartSeries.Bin> bins,int offset){
    Bitmap image=Bitmap.createBitmap(440,190,Bitmap.Config.ARGB_8888);
    Canvas canvas=new Canvas(image);
    canvas.drawColor(Color.WHITE);
    canvas.save();canvas.translate(-offset,0);
    EnergyChartPainter.draw(canvas,bins,0,EnergyChartSeries.BARS,.7,1,Color.BLACK,Color.GRAY,
        EnergyChartSeries.DAILY,false,1f,EnergyChartDisplay.HIDDEN,-1);
    EnergyChartPainter.drawFloatingYAxis(canvas,bins,0,.7,1,Color.GRAY,1f,Color.WHITE,offset);
    canvas.restore();
    return image;
  }

}
