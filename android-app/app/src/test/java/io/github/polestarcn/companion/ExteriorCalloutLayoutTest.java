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
  @Test public void uprightCarClearsCalloutsAndFrontRearLabels(){
    for(float width:new float[]{272f,320f,420f})for(float height:new float[]{150f,190f,240f,320f}){
      float card=Math.min(width*.42f,Math.max(68f,Math.min(116f,width*.30f)));
      float half=ExteriorCalloutLayout.carHalfLength(width,height,1f,.53f,card,1.25f);
      assertTrue("Car must fit in center lane",2*half*.53f<=width-2*card-17f);
      assertTrue("Front marker must clear roof artwork",height*.5f-half>=25f);
      assertTrue("Rear marker must clear roof artwork",height*.5f+half<=height-25f);
      assertTrue("A visible car is still required",half>0f);
    }
  }
  @Test public void vehicleBodyIsApproximatelyTwentyPercentLargerWhenSpacePermits(){
    float w=360f,h=236f,ratio=.53f;
    float card=Math.min(w*.42f,Math.max(68f,Math.min(116f,w*.30f)));
    float original=Math.min(h*.33f,w*.38f);
    float current=ExteriorCalloutLayout.carHalfLength(w,h,1f,ratio,card,1f);
    assertEquals("Normal portrait view has room for the full 20% zoom",original*1.2f,current,.1f);
  }
  @Test public void pixelDensityScalesProportionally(){
    float[] a=ExteriorCalloutLayout.rows(150,1f,1f);
    float[] b=ExteriorCalloutLayout.rows(450,3f,1f);
    for(int i=0;i<a.length;i++)assertEquals(a[i]*3,b[i],0.01f);
  }
}
