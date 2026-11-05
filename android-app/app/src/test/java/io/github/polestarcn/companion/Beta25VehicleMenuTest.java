package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w360dp-h780dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class Beta25VehicleMenuTest {
  private AppRegressionTest base;private MainActivity a;private VehicleCoordinator model;
  @Before public void start()throws Exception{
    base=new AppRegressionTest();base.start();Field f=AppRegressionTest.class.getDeclaredField("activity");f.setAccessible(true);a=(MainActivity)f.get(base);
    model=VehicleCoordinator.get(a);f=VehicleCoordinator.class.getDeclaredField("signedIn");f.setAccessible(true);f.set(model,true);
    model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();model.selected.vin="BETA25-FIXTURE";
    AppPreferences.set(a,"language","zh");snapshot(false);
  }
  @After public void stop(){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();if(d!=null)d.dismiss();base.stop();}
  private void snapshot(boolean active){model.snapshot=new VehicleSnapshot("Polestar 2","masked",63,230,65845,"空闲","全部关闭","全部关闭","已关闭","已上锁",System.currentTimeMillis(),"LIVE",false,active?"开启":"关闭",active?"开启":"关闭");}
  private View render(int width,int height)throws Exception{
    Configuration c=new Configuration(a.getResources().getConfiguration());c.screenWidthDp=width;c.screenHeightDp=height;
    a.getResources().updateConfiguration(c,a.getResources().getDisplayMetrics());
    Method m=MainActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(a);
    View root=a.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);return root;
  }
  private VehicleHeroView hero(View v){if(v instanceof VehicleHeroView)return (VehicleHeroView)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){VehicleHeroView h=hero(g.getChildAt(i));if(h!=null)return h;}}return null;}
  private void touch(VehicleHeroView v,int action){MotionEvent e=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),action,10,10,0);v.onTouchEvent(e);e.recycle();}
  @Test public void holdFor1500msOpensAppearanceMenuAndOnlyMenuTogglesMinimal()throws Exception{
    UiSkin.set(a,UiSkin.PS_DEFAULT);UiDisplayMode.setMinimal(a,false);VehicleHeroView v=hero(render(360,780));assertNotNull(v);
    touch(v,MotionEvent.ACTION_DOWN);assertFalse(UiDisplayMode.minimal(a));
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(1500,TimeUnit.MILLISECONDS);
    AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d);assertTrue(d.isShowing());
    assertEquals("切换皮肤",d.getListView().getAdapter().getItem(0));
    assertEquals(5,d.getListView().getAdapter().getCount());assertFalse(UiDisplayMode.minimal(a));
    touch(v,MotionEvent.ACTION_UP);d.getListView().performItemClick(null,4,4);assertTrue(UiDisplayMode.minimal(a));
  }
  @Test public void cancelledHoldDoesNotOpenMenu()throws Exception{
    VehicleHeroView v=hero(render(360,780));assertNotNull(v);touch(v,MotionEvent.ACTION_DOWN);touch(v,MotionEvent.ACTION_CANCEL);
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(1600,TimeUnit.MILLISECONDS);assertNull(ShadowAlertDialog.getLatestAlertDialog());
  }
  @Test public void minimalControlsAreLargerCenteredAndHideInactiveFeedbackAcrossSkins()throws Exception{
    for(String skin:UiSkin.CHOICES){
      UiSkin.set(a,skin);UiDisplayMode.setMinimal(a,false);snapshot(false);View root=render(360,780);
      ImageView regular=root.findViewWithTag("control_icon_climate");assertNotNull(skin,regular);int before=regular.getLayoutParams().width;
      UiDisplayMode.setMinimal(a,true);root=render(360,780);ImageView icon=root.findViewWithTag("control_icon_climate");
      assertTrue(skin+" icon size",icon.getLayoutParams().width>before);
      for(String control:new String[]{"climate","air"}){TextView feedback=root.findViewWithTag("control_feedback_"+control);if(feedback!=null)assertEquals(skin,View.GONE,feedback.getVisibility());}
      View parent=(View)icon.getParent();assertEquals(skin+" centered",parent.getWidth()/2f,icon.getLeft()+icon.getWidth()/2f,1.1f);
      snapshot(true);root=render(360,780);TextView feedback=root.findViewWithTag("control_feedback_climate");if(feedback!=null)assertEquals(skin,View.GONE,feedback.getVisibility());
      if(UiSkin.PS_DEFAULT.equals(skin)||UiSkin.SIMPLE.equals(skin)||UiSkin.SIMPLE_2.equals(skin))export(root,skin);
    }
  }

  @Test public void smallAndLandscapeMinimalTilesKeepActiveContentInsideTheirBounds()throws Exception{
    UiDisplayMode.setMinimal(a,true);snapshot(true);
    for(String skin:UiSkin.CHOICES)for(int[] size:new int[][]{{320,568},{640,320}}){
      UiSkin.set(a,skin);View root=render(size[0],size[1]);
      View icon=root.findViewWithTag("control_icon_climate"),feedback=root.findViewWithTag("control_feedback_climate");
      assertNotNull(skin,icon);if(feedback!=null)assertEquals(skin,View.GONE,feedback.getVisibility());
      View tile=icon;while(!"control_tile".equals(tile.getTag())&&tile.getParent() instanceof View)tile=(View)tile.getParent();
      int[] start=new int[2],at=new int[2];tile.getLocationInWindow(start);
      for(View child:new View[]{icon}){child.getLocationInWindow(at);assertTrue(skin+" top",at[1]>=start[1]);assertTrue(skin+" bottom "+size[0]+"x"+size[1],at[1]+child.getHeight()<=start[1]+tile.getHeight());}
    }
  }
  private void export(View root,String name)throws Exception{
    File f=new File("build/reports/previews/beta25/"+name+"-minimal.png");f.getParentFile().mkdirs();Bitmap b=Bitmap.createBitmap(360,780,Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));try(FileOutputStream out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
  }
}
