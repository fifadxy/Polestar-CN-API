package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import org.junit.Test;

public final class CarStatusPopupLayoutTest {
  @Test public void collapsedPrimaryDataFitsAndDetailsCanScroll(){
    for(int height:new int[]{320,375,480,568,640,780,960}){
      int viewport=CarStatusPopupLayout.viewportHeightDp(height);
      int car=CarStatusPopupLayout.diagramHeightDp(height);
      assertTrue("Summary, timestamp and folded header must be visible",car+110<=viewport);
      assertTrue("Keep the four-door car visible",car>=95);
      assertTrue("Leave room for dialog chrome on compact devices",viewport<=height*.65f);
    }
  }
}
