package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
public final class ParkingPreviewV124Test {
  @Test public void previewHasUsefulMapAreaOnSmallAndLargePortraitScreens(){
    for(int[] screen:new int[][]{{320,568},{360,640},{360,780},{440,956}}){
      int[] viewport=DialogWindowGeometry.parkingPreview(screen[0],screen[1],1);
      int map=ParkingPreviewLayout.mapHeightDp(screen[1],true);
      assertTrue(viewport[0]>screen[0]*.88);
      assertEquals("No fixed-height lower blank region",android.view.ViewGroup.LayoutParams.WRAP_CONTENT,viewport[1]);
      assertTrue("Interactive map viewport must remain available",map>=52);
      assertTrue("Map remains useful without inflating the window",map<=320);
    }
  }
  @Test public void smallMapSettingsStayCompactAndUnchanged(){
    int[] settings=DialogWindowGeometry.map(360,640,1);
    int[] parking=DialogWindowGeometry.parkingPreview(360,640,1);
    assertTrue(settings[0]<parking[0]);
    assertEquals(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,parking[1]);
    assertTrue(settings[0]*settings[1]<360*640/2);
  }
}
