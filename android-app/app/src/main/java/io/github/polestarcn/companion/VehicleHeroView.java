package io.github.polestarcn.companion;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.*;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.*;
import android.view.Choreographer;
import android.widget.ImageView;

/** Native overlays share the existing FIT_CENTER car. PNG opacity and geometry stay intact. */
final class VehicleHeroView extends androidx.appcompat.widget.AppCompatImageView implements Choreographer.FrameCallback {
  private final ChargingParticleRenderer particles=new ChargingParticleRenderer();
  private final AirflowRenderer airflow=new AirflowRenderer();
  private final WindowGlowRenderer windows=new WindowGlowRenderer();
  private final RectF bounds=new RectF(),imageBounds=new RectF();
  private final android.graphics.Rect visibleRect=new android.graphics.Rect();
  private final boolean preview,lowRam;
  private final int art;
  private boolean hostActive=true,queued,registered;
  private long epoch,checkedAt,lastFrameNanos;
  private int previewAirDirection=-1;private int previewStyle=-1;private boolean previewAbsorb;
  private EffectSettings settings;
  private EffectStateController.Mode testMode=EffectStateController.Mode.CHARGING,mode=EffectStateController.Mode.OFF;
  private VehicleCoordinator coordinator;
  private final Runnable changed=()->sync();
  private final SharedPreferences.OnSharedPreferenceChangeListener preferences=(p,key)->{settings=new EffectSettings(getContext());if(previewStyle>=0)settings=settings.previewCharge(previewStyle,previewAbsorb);if(previewAirDirection>=0)settings=settings.previewAir(previewAirDirection==1);sync();invalidate();};
  private final BroadcastReceiver power=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){sync();}};

  VehicleHeroView(Context context){this(context,false);}
  VehicleHeroView(Context context,boolean simulation){
    super(context);preview=simulation;art=VehicleArt.dashboard(context);
    setImageResource(art);setScaleType(ScaleType.FIT_CENTER);setAdjustViewBounds(false);
    settings=new EffectSettings(context);
    ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    lowRam=am!=null&&am.isLowRamDevice();
  }
  void setSnapshot(VehicleSnapshot snapshot){sync();invalidate();}
  void setPreviewMode(EffectStateController.Mode value){testMode=value;epoch=SystemClock.uptimeMillis();sync();invalidate();}
  void setPreviewCharge(int style,boolean absorb){previewStyle=style;previewAbsorb=absorb;settings=new EffectSettings(getContext());if(style>=0)settings=settings.previewCharge(style,absorb);setPreviewMode(EffectStateController.Mode.CHARGING);}
  void setPreviewAir(int direction){previewAirDirection=direction;settings=new EffectSettings(getContext());if(direction>=0)settings=settings.previewAir(direction==1);invalidate();}
  void setEffectsHostActive(boolean active){hostActive=active;sync();}
  private boolean allowed(){PowerManager p=(PowerManager)getContext().getSystemService(Context.POWER_SERVICE);return hostActive&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&getGlobalVisibleRect(visibleRect)&&ValueAnimator.areAnimatorsEnabled()&&(p==null||!p.isPowerSaveMode());}
  private void sync(){
    if(settings==null)return;
    EffectStateController.Mode next=EffectStateController.Mode.OFF;
    if(allowed()){
      if(preview){if(settings.master&&settings.enabled(testMode))next=testMode;}
      else if(coordinator!=null)next=EffectStateController.resolve(coordinator.snapshot,settings,System.currentTimeMillis());
    }
    boolean different=mode!=next;mode=next;
    if(mode==EffectStateController.Mode.OFF){Choreographer.getInstance().removeFrameCallback(this);queued=false;if(different)invalidate();}
    else if(!queued){if(epoch==0)epoch=SystemClock.uptimeMillis();queued=true;Choreographer.getInstance().postFrameCallbackDelayed(this,1);}
  }
  @Override public void doFrame(long nanos){queued=false;if(SystemClock.uptimeMillis()-checkedAt>=500){checkedAt=SystemClock.uptimeMillis();sync();}if(mode!=EffectStateController.Mode.OFF&&hostActive&&isShown()){if(!lowRam||nanos-lastFrameNanos>=30_000_000L){lastFrameNanos=nanos;invalidate();}if(!queued){queued=true;Choreographer.getInstance().postFrameCallbackDelayed(this,1);}}else sync();}
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();settings=new EffectSettings(getContext());if(!preview){coordinator=VehicleCoordinator.get(getContext());coordinator.listen(changed);}getContext().getSharedPreferences("preferences",0).registerOnSharedPreferenceChangeListener(preferences);IntentFilter filter=new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);if(Build.VERSION.SDK_INT>=33)getContext().registerReceiver(power,filter,Context.RECEIVER_NOT_EXPORTED);else getContext().registerReceiver(power,filter);registered=true;sync();}
  @Override protected void onDetachedFromWindow(){hostActive=false;touchHandler.removeCallbacksAndMessages(null);sync();if(coordinator!=null){coordinator.remove(changed);coordinator=null;}getContext().getSharedPreferences("preferences",0).unregisterOnSharedPreferenceChangeListener(preferences);if(registered){getContext().unregisterReceiver(power);registered=false;}super.onDetachedFromWindow();}
  @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);sync();}
  @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);sync();}
  private void geometry(int rotation){
    if(getDrawable()==null)return;float iw=getDrawable().getIntrinsicWidth(),ih=getDrawable().getIntrinsicHeight();if(iw<=0||ih<=0)return;
    float w=getWidth()-getPaddingLeft()-getPaddingRight(),h=getHeight()-getPaddingTop()-getPaddingBottom();
    float scale=rotation==0?Math.min(w/iw,h/ih):Math.min(w/ih,h/iw);
    if(!preview&&"behind".equals(AppPreferences.get(getContext(),"all_good_position","below")))scale*=.82f;
    imageBounds.set(-iw*scale/2,-ih*scale/2,iw*scale/2,ih*scale/2);bounds.set(imageBounds);VehicleArt.effectBounds(art,bounds);
  }
  private float verticalOffset(int rotation){if(preview||!"behind".equals(AppPreferences.get(getContext(),"all_good_position","below")))return 0;float desired=getHeight()*("two".equals(AppPreferences.get(getContext(),"all_good_lines","one"))?.30f:.08f);float projected=rotation==0?imageBounds.height():imageBounds.width();return Math.min(desired,Math.max(0,(getHeight()-getPaddingTop()-getPaddingBottom()-projected)/2));}
  @Override protected void onDraw(Canvas canvas){
    if(getDrawable()==null)return;if(mode==EffectStateController.Mode.OFF)sync();int rotation=VehicleArt.portraitRotation(getContext());geometry(rotation);
    float time=(SystemClock.uptimeMillis()-epoch)/1000f,density=getResources().getDisplayMetrics().density;
    if(!preview&&coordinator!=null&&"behind".equals(AppPreferences.get(getContext(),"all_good_position","below"))&&!ChargingVisibility.shouldShow(coordinator.snapshot)&&VehicleCondition.reported(coordinator.snapshot,AppPreferences.visible(getContext(),"show_tire_status",true))&&!VehicleCondition.problem(coordinator.snapshot,AppPreferences.visible(getContext(),"show_tire_status",true))){backdropText.setColor(AppPreferences.dark(getContext())?0xFF707477:0xFFB7B9BA);backdropText.setTypeface(PsaDesign.regular());backdropText.setTextAlign(android.graphics.Paint.Align.CENTER);boolean two="two".equals(AppPreferences.get(getContext(),"all_good_lines","one"));backdropText.setTypeface(PsaDesign.display());backdropText.setFakeBoldText(true);float size=Math.min(getWidth()*(two?.27f:.22f),getHeight()*(two?.50f:.72f));String first=two?"ALL":"ALL GOOD！";backdropText.setTextSize(size);float width=backdropText.measureText(first);if(width>getWidth()*.94f)backdropText.setTextSize(size*getWidth()*.94f/width);if(two){backdropText.setTextAlign(android.graphics.Paint.Align.LEFT);canvas.drawText("ALL",getWidth()*.04f,Math.max(getHeight()*.23f,backdropText.getTextSize()*.82f),backdropText);canvas.drawText("GOOD！",getWidth()*.04f,Math.max(getHeight()*.23f,backdropText.getTextSize()*.82f)+backdropText.getTextSize()*.94f,backdropText);}else canvas.drawText(first,getWidth()/2f,Math.max(getHeight()*.39f,backdropText.getTextSize()*.82f),backdropText);}
    canvas.save();canvas.translate(getPaddingLeft()+(getWidth()-getPaddingLeft()-getPaddingRight())/2f,getPaddingTop()+(getHeight()-getPaddingTop()-getPaddingBottom())/2f+verticalOffset(rotation));canvas.rotate(rotation);
    if(mode==EffectStateController.Mode.CHARGING&&!bounds.isEmpty()){
      boolean right=VehicleArt.frontAtRight(art);float px=bounds.left+bounds.width()*(right?.13f:.87f),py=bounds.top+bounds.height()*.78f;
      particles.draw(canvas,bounds,px,py,time,density,settings,lowRam?45:150,1);
    }
    canvas.save();canvas.translate(imageBounds.left,imageBounds.top);canvas.scale(imageBounds.width()/getDrawable().getIntrinsicWidth(),imageBounds.height()/getDrawable().getIntrinsicHeight());getDrawable().setBounds(0,0,getDrawable().getIntrinsicWidth(),getDrawable().getIntrinsicHeight());getDrawable().draw(canvas);canvas.restore();
    if(mode!=EffectStateController.Mode.OFF&&mode!=EffectStateController.Mode.CHARGING&&!bounds.isEmpty())airflow.draw(canvas,bounds,VehicleArt.frontAtRight(art),time,density,settings,mode,lowRam?24:120,1);
    if(mode!=EffectStateController.Mode.OFF&&mode!=EffectStateController.Mode.CHARGING&&settings.windowGlow)windows.draw(canvas,imageBounds,art,time,mode,settings.windowStrength,settings.windowSpeed);
    canvas.restore();
    if(isPressed()){touchGlow.setColor(0x24FFFFFF);canvas.drawRoundRect(0,0,getWidth(),getHeight(),12*density,12*density,touchGlow);backdropText.setTextAlign(android.graphics.Paint.Align.CENTER);backdropText.setColor(PsaDesign.muted(AppPreferences.dark(getContext())));backdropText.setTypeface(PsaDesign.regular());backdropText.setFakeBoldText(false);backdropText.setTextSize(10*density);canvas.drawText(AppPreferences.tr(getContext(),"长按1.5s选择车辆图片","Hold 1.5s to choose vehicle"),getWidth()/2f,getHeight()-8*density,backdropText);}
  }
  private final Handler touchHandler=new Handler(Looper.getMainLooper());private float downX,downY;private boolean holdFired,moved;private Runnable holdAction;
  void setVehicleHold(Runnable action){holdAction=action;}
  private final android.graphics.Paint backdropText=new android.graphics.Paint(3),touchGlow=new android.graphics.Paint(3);
  private final Runnable hold=()->{holdFired=true;Haptics.tap(getContext());if(holdAction!=null)holdAction.run();};
  @Override public boolean performClick(){return super.performClick();}
  @Override public boolean onTouchEvent(android.view.MotionEvent e){switch(e.getActionMasked()){
    case android.view.MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();holdFired=false;moved=false;Haptics.tap(getContext());invalidate();touchHandler.postDelayed(hold,1500);break;
    case android.view.MotionEvent.ACTION_MOVE:float slop=android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();if(Math.abs(e.getX()-downX)>slop||Math.abs(e.getY()-downY)>slop){moved=true;touchHandler.removeCallbacks(hold);}break;
    case android.view.MotionEvent.ACTION_UP:touchHandler.removeCallbacks(hold);setPressed(false);if(!holdFired&&!moved)performClick();return true;
    case android.view.MotionEvent.ACTION_CANCEL:touchHandler.removeCallbacks(hold);setPressed(false);return true;
  }return super.onTouchEvent(e);}
  boolean effectsRunning(){return mode!=EffectStateController.Mode.OFF&&queued;}
}
