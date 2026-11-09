package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
public class ExteriorCalloutLayoutTest {
  @Test public void fourCardsNeverOverlapAtCompactViewportHeights(){
    for(int height:new int[]{95,100,120,150,220,320}){
      for(float scale:new float[]{1f,1.15f,1.25f,1.5f}){
        float[] a=ExteriorCalloutLayout.rows(height,1f,scale);
        assertTrue(a[0]>=0);
        assertTrue(a[1]>=a[0]+a[2]);
        assertTrue(a[1]+a[2]<=height);
      }
    }
  }
  @Test public void pixelDensityScalesProportionally(){
    float[] a=ExteriorCalloutLayout.rows(150,1f,1f);
    float[] b=ExteriorCalloutLayout.rows(450,3f,1f);
    for(int i=0;i<a.length;i++)assertEquals(a[i]*3,b[i],0.01f);
  }
}
