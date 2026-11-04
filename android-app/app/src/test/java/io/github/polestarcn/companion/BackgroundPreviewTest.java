package io.github.polestarcn.companion;
import static org.junit.Assert.*;import android.graphics.*;import android.content.*;import android.widget.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BackgroundPreviewTest {
 @Test public void blankMapPixelsAreRejectedAndActualVariationAccepted(){Bitmap b=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);assertFalse(ParkingBackdrop.hasDetail(b));Canvas c=new Canvas(b);Paint p=new Paint();p.setColor(Color.BLACK);c.drawRect(10,10,45,90,p);assertTrue(ParkingBackdrop.hasDetail(b));b.recycle();}
 @Test public void localDayNightFilesAreSeparateAndMissingPhotoKeepsNormalBackground(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();AppPreferences.set(c,"theme","light");String day=BackgroundImages.file(c).getPath();AppPreferences.set(c,"theme","dark");assertNotEquals(day,BackgroundImages.file(c).getPath());FrameLayout shell=new FrameLayout(c);assertSame(shell,BackgroundImages.wrap(c,shell));}
}
