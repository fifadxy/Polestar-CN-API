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
      POLESTAR="volike_polestar";
  private VoLikeWidgetRenderer() {}
  public static String style(Context c) {
    String value=AppPreferences.get(c,"widget_design",CLASSIC);
    return SIMPLE.equals(value)||DETAILED.equals(value)||POLESTAR.equals(value)?value:CLASSIC;
  }
  public static String label(Context c, String style) {
    boolean en=AppPreferences.english(c);
    if(SIMPLE.equals(style)) return en?"VoLike Simple":"VoLike 简洁";
    if(DETAILED.equals(style)) return en?"VoLike Detailed":"VoLike 详细";
    if(POLESTAR.equals(style)) return en?"Polestar VI":"极星 VI";
    return en?"Classic / Custom data":"经典 / 自定义数据";
  }

  public static RemoteViews createViews(Context c,int id,WidgetRenderer.Size size,int w,int h,VehicleSnapshot s) {
    RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.widget_volike);
    String current=style(c);
    views.setImageViewBitmap(R.id.widget_bg,preview(c,s,current,w,h));
    int ink=AppPreferences.dark(c)?Color.WHITE:Color.rgb(35,37,39);
    int accent=POLESTAR.equals(current)?PsaDesign.ORANGE:Color.rgb(94,141,122);
    views.setInt(R.id.volike_widget_lock_icon,"setColorFilter",ink);
    views.setInt(R.id.volike_widget_climate_icon,"setColorFilter",ink);
    views.setInt(R.id.volike_widget_charge_icon,"setColorFilter",
        ChargingVisibility.shouldShow(s)?accent:ink);
    views.setInt(R.id.volike_widget_location_icon,"setColorFilter",ink);
    views.setContentDescription(R.id.widget_root,"Polestar 2 "
        +(s==null?"--":VehicleSnapshot.metric(s.batteryPercent,"%"))+" "
        +(s==null?"--":VehicleSnapshot.metric(s.rangeKm," km"))
        +(ChargingVisibility.shouldShow(s)?" Charging":""));
    views.setOnClickPendingIntent(R.id.widget_root,pending(c,id,size,"open"));
    views.setOnClickPendingIntent(R.id.volike_widget_lock,pending(c,id,size,"lock-choice"));
    views.setOnClickPendingIntent(R.id.volike_widget_climate,pending(c,id,size,"climate-choice"));
    views.setOnClickPendingIntent(R.id.volike_widget_charge,pending(c,id,size,"open"));
    views.setOnClickPendingIntent(R.id.volike_widget_location,pending(c,id,size,"location"));
    return views;
  }
  private static PendingIntent pending(Context c,int id,WidgetRenderer.Size size,String action){
    Intent i=new Intent(c,MainActivity.class);
    i.setAction("io.github.polestarcn.companion.WIDGET");
    i.setData(Uri.parse("psassistant://widget/"+id+"/"+size+"/"+action));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    if(!"open".equals(action))i.putExtra("widget_action",action);
    return PendingIntent.getActivity(c,0,i,
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  }
  public static Bitmap preview(Context context,VehicleSnapshot s,String style,int width,int height) {
    int w=Math.max(180,Math.min(920,width*2)), h=Math.max(116,Math.min(640,height*2));
    Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    Canvas canvas=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    float sx=w/420f,sy=h/220f;canvas.save();canvas.scale(sx,sy);
    boolean dark=AppPreferences.dark(context),shortCard=SIMPLE.equals(style)||height<125;
    boolean polestar=POLESTAR.equals(style);
    int bg=dark?Color.rgb(29,31,33):polestar?Color.rgb(244,244,242):Color.rgb(249,247,244);
    int ink=dark?Color.WHITE:Color.rgb(23,25,26),muted=dark?0xFFAAADB1:0xFF777A7C;
    int accent=polestar?PsaDesign.ORANGE:dark?0xFF79B79F:0xFF5E8D7A;
    p.setColor(bg);canvas.drawRoundRect(2,2,418,218,28,28,p);
    text(canvas,p,"Polestar 2",14,20,33,muted,false);
    text(canvas,p,s==null?"--%":VehicleSnapshot.metric(s.batteryPercent,"%"),34,20,85,ink,true);
    text(canvas,p,s==null?"-- km":VehicleSnapshot.metric(s.rangeKm," km"),22,137,85,ink,true);
    float progress=s==null||s.batteryPercent==null?0:Math.max(0,Math.min(100,s.batteryPercent));
    p.setColor(dark?0xFF494D4D:0xFFD9DADD);canvas.drawRoundRect(20,102,400,111,4.5f,4.5f,p);
    p.setColor(accent);canvas.drawRoundRect(20,102,20+380*progress/100f,111,4.5f,4.5f,p);
    boolean charging=ChargingVisibility.shouldShow(s);
    text(canvas,p,charging?(AppPreferences.widgetEnglish(context)?"CHARGING":"充电中"):
        (AppPreferences.widgetEnglish(context)?"Vehicle ready":"车辆已连接"),12,20,133,charging?accent:muted,false);
    if(!shortCard) {
      Bitmap car=BitmapFactory.decodeResource(context.getResources(),R.drawable.vehicle_hero);
      if(car!=null) {
        float x=182,y=125,maxWidth=215,maxHeight=45;
        float k=Math.min(maxWidth/car.getWidth(),maxHeight/car.getHeight());
        float cw=car.getWidth()*k,ch=car.getHeight()*k;
        p.setAlpha(225);canvas.drawBitmap(car,null,new RectF(x,y,x+cw,y+ch),p);
        car.recycle();p.setAlpha(255);
      }
      if(DETAILED.equals(style)||POLESTAR.equals(style)){
        text(canvas,p,AppPreferences.widgetEnglish(context)?"Last update":"车辆状态",10,20,155,muted,false);
        if(s!=null)text(canvas,p,s.lock==null?"--":AppPreferences.status(context,s.lock),12,20,174,ink,false);
      }
    }
    canvas.restore();
    return bitmap;
  }
  private static void text(Canvas canvas,Paint paint,String value,float sp,float x,float y,int color,boolean bold) {
    paint.setShader(null);paint.setColor(color);paint.setTypeface(bold?PsaDesign.display():PsaDesign.regular());
    paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(sp);canvas.drawText(value,x,y,paint);
  }
}
