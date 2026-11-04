package io.github.polestarcn.companion;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.widget.RemoteViews;

/** Lightweight, key-free VoLike widget: locally drawn static card, live data and verified controls. */
public final class VoLikeWidgetRenderer {
  public static final String CLASSIC="classic", SIMPLE="volike_simple", DETAILED="volike_detailed",
      POLESTAR="volike_polestar",PSEU="pseu";
  private VoLikeWidgetRenderer() {}
  public static String style(Context c) {
    String value=AppPreferences.get(c,"widget_design",CLASSIC);
    return SIMPLE.equals(value)||DETAILED.equals(value)||POLESTAR.equals(value)||PSEU.equals(value)?value:CLASSIC;
  }
  public static String label(Context c, String style) {
    boolean en=AppPreferences.english(c);
    if(PSEU.equals(style))return "PSeu";
    if(SIMPLE.equals(style)) return en?"VoLike Simple":"VoLike 简洁";
    if(DETAILED.equals(style)) return en?"VoLike Detailed":"VoLike 详细";
    if(POLESTAR.equals(style)) return en?"Polestar VI":"极星 VI";
    return en?"Classic / Custom data":"经典 / 自定义数据";
  }

  public static RemoteViews createViews(Context c,int id,WidgetRenderer.Size size,int w,int h,VehicleSnapshot s){return WidgetRenderer.createNativeViews(c.getApplicationContext(),id,size,w,h,null,s,style(c));}
  private static PendingIntent pending(Context c,int id,WidgetRenderer.Size size,String action){
    Intent i=new Intent(c,MainActivity.class);
    i.setAction("io.github.polestarcn.companion.WIDGET");
    i.setData(Uri.parse("psassistant://widget/"+id+"/"+size+"/"+action));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    if(!"open".equals(action))i.putExtra("widget_action",action);
    return PendingIntent.getActivity(c,0,i,
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  }
  public static Bitmap preview(Context context,VehicleSnapshot s,String style,int width,int height){
    Context c=context.getApplicationContext();float density=c.getResources().getDisplayMetrics().density;
    android.widget.RemoteViews remote=WidgetRenderer.createNativeViews(c,0,WidgetRenderer.Size.LARGE,width,height,null,s,style);
    android.view.View root=remote.apply(c,new android.widget.FrameLayout(c));int vw=Math.round(width*density),vh=Math.round(height*density);
    root.measure(android.view.View.MeasureSpec.makeMeasureSpec(vw,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(vh,android.view.View.MeasureSpec.EXACTLY));root.layout(0,0,vw,vh);
    int bw=Math.min(920,width*2),bh=Math.round(height*bw/(float)width);Bitmap bitmap=Bitmap.createBitmap(bw,bh,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.scale(bw/(float)vw,bw/(float)vw);root.draw(canvas);return bitmap;
  }
}
