package io.github.polestarcn.companion;
import android.content.Context;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.File;
final class BackgroundImages {
 static File file(Context c){return new File(c.getFilesDir(),AppPreferences.dark(c)?"background-night.jpg":"background-day.jpg");}
 static View wrap(Context c,View shell){File f=file(c);if(!AppPreferences.visible(c,"background_image_enabled",false)||!f.isFile())return shell;Bitmap bitmap=BitmapFactory.decodeFile(f.getAbsolutePath());if(bitmap==null)return shell;FrameLayout root=new FrameLayout(c);ImageView photo=new ImageView(c);photo.setImageBitmap(bitmap);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);root.addView(photo,new FrameLayout.LayoutParams(-1,-1));shell.setBackgroundColor(Color.TRANSPARENT);root.addView(shell,new FrameLayout.LayoutParams(-1,-1));return root;}
 private BackgroundImages(){}
}
