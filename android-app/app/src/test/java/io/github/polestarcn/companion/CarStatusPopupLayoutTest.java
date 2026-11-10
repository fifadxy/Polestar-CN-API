package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import org.junit.Test;

public final class CarStatusPopupLayoutTest {
  @Test public void everyViewportKeepsDiagramAndActionTogetherWithoutScrolling(){
    for(int height:new int[]{320,375,480,568,640,780,960}){
      int available=CarStatusPopupLayout.viewportHeightDp(height);
      int car=CarStatusPopupLayout.diagramHeightDp(height);
      assertTrue("Leave room for labels and full-details button",car+90<=available);
      assertTrue("Keep the car visible",car>=96);
      assertTrue("Fit fixed viewport within the device",available<=height*.65f);
    }
  }
}
