package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class DialogFirstFrameV122Test {
  @Test public void allDialogsAreCenteredBeforeTheirFirstRenderedFrame() {
    Activity a=Robolectric.buildActivity(Activity.class).setup().get();
    for(boolean compact:new boolean[]{false,true}) {
      HapticDialogBuilder b=new HapticDialogBuilder(a);
      if(compact)b.compactMapWindow();
      AlertDialog dialog=b.setTitle("Dialog").setView(new TextView(a)).create();
      WindowManager.LayoutParams before=dialog.getWindow().getAttributes();
      assertEquals(Gravity.CENTER,before.gravity);
      assertEquals(R.style.PsaDialogFadeAnimation,before.windowAnimations);
      final int screenWidth=a.getResources().getDisplayMetrics().widthPixels;
      final int screenHeight=a.getResources().getDisplayMetrics().heightPixels;
      final float density=a.getResources().getDisplayMetrics().density;
      final int[] desired=compact?DialogWindowGeometry.map(screenWidth,screenHeight,density)
          :new int[]{Math.min((int)(360*density),(int)(screenWidth*.86f)),
            WindowManager.LayoutParams.WRAP_CONTENT};
      dialog.show();
      View decor=dialog.getWindow().getDecorView();
      decor.getViewTreeObserver().dispatchOnPreDraw();
      WindowManager.LayoutParams settled=dialog.getWindow().getAttributes();
      assertEquals(desired[0],settled.width);
      assertEquals(desired[1],settled.height);
      assertEquals(Gravity.CENTER,settled.gravity);
      assertEquals(0f,decor.getTranslationX(),.001f);
      decor.getViewTreeObserver().dispatchOnPreDraw();
      WindowManager.LayoutParams second=dialog.getWindow().getAttributes();
      assertEquals(settled.width,second.width);
      assertEquals(settled.height,second.height);
      assertEquals(settled.gravity,second.gravity);
      dialog.dismiss();
    }
    a.finish();
  }
}
