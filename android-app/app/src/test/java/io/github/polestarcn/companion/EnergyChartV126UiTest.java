package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
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
    for(boolean dark:new boolean[]{false,true}){
      Bitmap background=popupGradient(dark);
      Bitmap first=drawAtScroll(bins,0,dark),panned=drawAtScroll(bins,160,dark);
      int axisRight=(int)EnergyChartPainter.plotLeft(1f);
      for(int x=0;x<axisRight;x++)for(int y=0;y<190;y++)
        assertEquals("Pinned axis must not move at x="+x+" y="+y,
            first.getPixel(x,y),panned.getPixel(x,y));
      for(int x:new int[]{0,3,6})for(int y:new int[]{43,88,140,178})
        assertEquals("Axis gutter should preserve dialog glass gradient",
            background.getPixel(x,y),panned.getPixel(x,y));
      boolean moved=false;
      for(int x=axisRight+20;x<400&&!moved;x+=8)for(int y=40;y<155;y+=8)
        if(first.getPixel(x,y)!=panned.getPixel(x,y)){moved=true;break;}
      assertTrue("Bars should scroll independently of the pinned axis",moved);
      first.recycle();panned.recycle();background.recycle();
    }
  }
  private static Bitmap popupGradient(boolean dark){
    Bitmap image=Bitmap.createBitmap(440,190,Bitmap.Config.ARGB_8888);
    Canvas c=new Canvas(image);Paint p=new Paint();
    p.setShader(new LinearGradient(0,0,0,190,
        dark?0xFF2A3036:Color.WHITE,dark?0xFF181D22:0xFFEDF1F4,Shader.TileMode.CLAMP));
    c.drawRect(0,0,440,190,p);return image;
  }
  private static Bitmap drawAtScroll(List<EnergyChartSeries.Bin> bins,int offset,boolean dark){
    Bitmap bg=popupGradient(dark);Bitmap image=bg.copy(Bitmap.Config.ARGB_8888,true);
    bg.recycle();Canvas c=new Canvas(image);c.save();c.translate(-offset,0);
    EnergyChartPainter.drawScrollableWithFloatingAxis(c,bins,0,EnergyChartSeries.BARS,
        .71,1,Color.BLACK,Color.GRAY,EnergyChartSeries.DAILY,1f,
        EnergyChartDisplay.HIDDEN,-1,offset,440);
    c.restore();return image;
  }
  @Test public void barTapCyclesFocusedAllGuidedThenHidden(){
    assertEquals(EnergyChartDisplay.LATEST,
        EnergyChartDisplay.afterBarTap(EnergyChartDisplay.LATEST,false));
    assertEquals(EnergyChartDisplay.ALL,
        EnergyChartDisplay.afterBarTap(EnergyChartDisplay.LATEST,true));
    assertEquals(EnergyChartDisplay.HIDDEN,
        EnergyChartDisplay.afterBarTap(EnergyChartDisplay.ALL,true));
    assertEquals(EnergyChartDisplay.LATEST,
        EnergyChartDisplay.afterBarTap(EnergyChartDisplay.HIDDEN,false));
    List<EnergyChartSeries.Bin> bins=new ArrayList<>();
    for(int i=0;i<8;i++){
      EnergyChartSeries.Bin b=new EnergyChartSeries.Bin(1735689600000L+i*86400000L);
      b.count=1;b.drive=i+1;bins.add(b);
    }
    Bitmap withGuide=Bitmap.createBitmap(900,190,Bitmap.Config.ARGB_8888);
    Bitmap noGuide=Bitmap.createBitmap(900,190,Bitmap.Config.ARGB_8888);
    EnergyChartPainter.draw(new Canvas(withGuide),bins,0,EnergyChartSeries.BARS,.71,1,
        Color.BLACK,Color.GRAY,EnergyChartSeries.DAILY,false,1f,EnergyChartDisplay.ALL,3);
    EnergyChartPainter.draw(new Canvas(noGuide),bins,0,EnergyChartSeries.BARS,.71,1,
        Color.BLACK,Color.GRAY,EnergyChartSeries.DAILY,false,1f,EnergyChartDisplay.ALL,-1);
    assertFalse("Selected bar should have dashed reference guide",withGuide.sameAs(noGuide));
    withGuide.recycle();noGuide.recycle();
  }

}
