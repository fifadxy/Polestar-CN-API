package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.animation.ValueAnimator;
import android.widget.ImageView;
/** A single immutable HD PNG with efficient real-time visual overlays; image geometry never stretches. */
final class VehicleHeroView extends ImageView {
  private final Bitmap roof;
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  private final VehicleEffectRenderer renderer;
  private VehicleEffectSettings.Mode preview;
  private VehicleEffectSettings.Mode live=VehicleEffectSettings.Mode.NONE;
  VehicleHeroView(Context context){
    super(context);
    int resource=VehicleArt.dashboard(context);
    setImageResource(resource);setScaleType(ScaleType.FIT_CENTER);setAdjustViewBounds(false);
    // Reuse the BitmapDrawable already loaded by ImageView; do not decode the large PNG twice.
    android.graphics.drawable.Drawable drawable=getDrawable();
    roof=drawable instanceof android.graphics.drawable.BitmapDrawable
        ?((android.graphics.drawable.BitmapDrawable)drawable).getBitmap()
        :BitmapFactory.decodeResource(getResources(),resource);
    renderer=new VehicleEffectRenderer(context);renderer.configure(context);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
  }
  private void syncMode(){
    VehicleEffectSettings.Mode requested=preview==null?live:preview;
    // The animation master switch must apply to settings preview and the dashboard equally.
    if(!AppPreferences.visible(getContext(),"effect_enabled",true))requested=VehicleEffectSettings.Mode.NONE;
    renderer.mode(requested);
    invalidate();
  }
  void setSnapshot(VehicleSnapshot snapshot){
    live=VehicleEffectSettings.vehicle(getContext(),snapshot);syncMode();
  }
  void preview(VehicleEffectSettings.Mode mode){preview=mode;renderer.configure(getContext());syncMode();}
  void resetPreview(){preview=null;renderer.configure(getContext());syncMode();}
  void reloadEffectSettings(){renderer.configure(getContext());syncMode();}
  @Override protected void onDraw(Canvas canvas){
    if(roof==null){super.onDraw(canvas);return;}
    int width=getWidth()-getPaddingLeft()-getPaddingRight(),height=getHeight()-getPaddingTop()-getPaddingBottom();
    if(width<=0||height<=0)return;
    int rotation=VehicleArt.portraitRotation(getContext());
    float scale=rotation==0?Math.min(width/(float)roof.getWidth(),height/(float)roof.getHeight()):
        Math.min(width/(float)roof.getHeight(),height/(float)roof.getWidth());
    canvas.save();canvas.translate(getPaddingLeft()+width/2f,getPaddingTop()+height/2f);
    canvas.rotate(rotation);
    RectF bounds=new RectF(-roof.getWidth()*scale/2f,-roof.getHeight()*scale/2f,
        roof.getWidth()*scale/2f,roof.getHeight()*scale/2f);
    renderer.draw(canvas,bounds,false);
    paint.setAlpha(255);canvas.drawBitmap(roof,null,bounds,paint);
    renderer.draw(canvas,bounds,true);
    canvas.restore();
    if(renderer.active()&&isShown()&&getWindowVisibility()==VISIBLE&&ValueAnimator.areAnimatorsEnabled()
        &&AppPreferences.visible(getContext(),"light_effects",true))postInvalidateDelayed(34);
  }
}
