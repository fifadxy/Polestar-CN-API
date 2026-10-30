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
    if(!preview&&"behind".equals(AppPreferences.get(getContext(),"all_good_position","below")))scale*="offset_c".equals(AppPreferences.get(getContext(),"all_good_lines","one"))?.76f:.82f;
    imageBounds.set(-iw*scale/2,-ih*scale/2,iw*scale/2,ih*scale/2);bounds.set(imageBounds);VehicleArt.effectBounds(art,bounds);
  }
  private float verticalOffset(int rotation){if(preview||!"behind".equals(AppPreferences.get(getContext(),"all_good_position","below")))return 0;float desired=getHeight()*("offset_c".equals(AppPreferences.get(getContext(),"all_good_lines","one"))?.42f:"two".equals(AppPreferences.get(getContext(),"all_good_lines","one"))?.30f:.08f);float projected=rotation==0?imageBounds.height():imageBounds.width();return Math.min(desired,Math.max(0,(getHeight()-getPaddingTop()-getPaddingBottom()-projected)/2));}
  private boolean offsetC(){return !preview&&"behind".equals(AppPreferences.get(getContext(),"all_good_position","below"))&&"offset_c".equals(AppPreferences.get(getContext(),"all_good_lines","one"));}
  private String backdropMessage="";private String[] backdropLines=new String[0];
  private void drawGoodBackdrop(Canvas canvas){android.content.Context c=getContext();boolean problem=coordinator!=null&&VehicleCondition.problem(coordinator.snapshot,AppPreferences.visible(c,"show_tire_status",true));boolean offset=offsetC(),two=offset||"two".equals(AppPreferences.get(c,"all_good_lines","one"));String text=ConditionStyle.message(c,problem);if(!problem&&!c.getSharedPreferences("preferences",0).contains("condition_good_text")&&two)text=offset?"All\nGood!":"ALL\nGOOD！";if(!text.equals(backdropMessage)){backdropMessage=text;backdropLines=text.split("\n",3);}backdropText.setColor(ConditionStyle.color(c));backdropText.setTypeface(ConditionStyle.typeface(c));backdropText.setFakeBoldText(false);backdropText.setTextAlign(android.graphics.Paint.Align.LEFT);
    float spacing=EffectSettings.number(c,"condition_line_spacing",100,50,200)/100f,stretch=EffectSettings.number(c,"condition_stretch",100,50,200)/100f;int custom=EffectSettings.number(c,"condition_size",0,0,100);float size=custom>0?custom*getResources().getDisplayMetrics().scaledDensity:Math.min(getWidth()*(offset?.25f:two?.27f:.22f),getHeight()*(two?.43f:.72f));backdropText.setTextSize(size);float width=0;for(String line:backdropLines)width=Math.max(width,backdropText.measureText(line)*stretch);float limit=getWidth()*(offset?.64f:.94f);float block=size*(1+Math.max(0,backdropLines.length-1)*spacing);float fit=Math.min(1,Math.min(limit/Math.max(1,width),getHeight()*.8f/Math.max(1,block)));size*=fit;width*=fit;block*=fit;backdropText.setTextSize(size);
    boolean customX=c.getSharedPreferences("preferences",0).contains("condition_focus_x"),customY=c.getSharedPreferences("preferences",0).contains("condition_focus_y");float x=customX?getWidth()*.03f+Math.max(0,getWidth()*.94f-width)*EffectSettings.number(c,"condition_focus_x",50,0,100)/100f:two?getWidth()*.04f:(getWidth()-width)/2;float y=customY?size*.82f+Math.max(0,getHeight()*.94f-block)*EffectSettings.number(c,"condition_focus_y",50,0,100)/100f:Math.max(getHeight()*(offset?.29f:two?.23f:.39f),size*.82f);canvas.save();canvas.translate(x,y);canvas.scale(stretch,1);for(int i=0;i<backdropLines.length;i++)canvas.drawText(backdropLines[i],0,size*spacing*i,backdropText);canvas.restore();}
  @Override protected void onDraw(Canvas canvas){
    if(getDrawable()==null)return;if(mode==EffectStateController.Mode.OFF)sync();int rotation=VehicleArt.portraitRotation(getContext());geometry(rotation);
    float time=(SystemClock.uptimeMillis()-epoch)/1000f,density=getResources().getDisplayMetrics().density;
    if(!preview&&coordinator!=null&&"behind".equals(AppPreferences.get(getContext(),"all_good_position","below"))&&!ChargingVisibility.shouldShow(coordinator.snapshot)&&(VehicleCondition.reported(coordinator.snapshot,AppPreferences.visible(getContext(),"show_tire_status",true))||VehicleCondition.problem(coordinator.snapshot,AppPreferences.visible(getContext(),"show_tire_status",true)))){drawGoodBackdrop(canvas);}

    canvas.save();canvas.translate(getPaddingLeft()+(getWidth()-getPaddingLeft()-getPaddingRight())/2f+(offsetC()?Math.min(getWidth()*.08f,Math.max(0,(getWidth()-imageBounds.width())/2)):0),getPaddingTop()+(getHeight()-getPaddingTop()-getPaddingBottom())/2f+verticalOffset(rotation));canvas.rotate(rotation);
    if(mode==EffectStateController.Mode.CHARGING&&!bounds.isEmpty()){
      boolean right=VehicleArt.frontAtRight(art);float px=bounds.left+bounds.width()*(right?.13f:.87f),py=bounds.top+bounds.height()*.78f;
      particles.drawLayer(canvas,bounds,px,py,time,density,settings,lowRam?45:150,1,0);
    }
    if(mode!=EffectStateController.Mode.OFF&&mode!=EffectStateController.Mode.CHARGING&&!bounds.isEmpty())airflow.drawLayer(canvas,bounds,VehicleArt.frontAtRight(art),time,density,settings,mode,lowRam?24:120,1,0);
    canvas.save();canvas.translate(imageBounds.left,imageBounds.top);canvas.scale(imageBounds.width()/getDrawable().getIntrinsicWidth(),imageBounds.height()/getDrawable().getIntrinsicHeight());getDrawable().setBounds(0,0,getDrawable().getIntrinsicWidth(),getDrawable().getIntrinsicHeight());getDrawable().draw(canvas);canvas.restore();
    if(mode==EffectStateController.Mode.CHARGING&&!bounds.isEmpty())particles.drawLayer(canvas,bounds,bounds.left+bounds.width()*(VehicleArt.frontAtRight(art)?.13f:.87f),bounds.top+bounds.height()*.78f,time,density,settings,lowRam?45:150,1,1);
    if(mode!=EffectStateController.Mode.OFF&&mode!=EffectStateController.Mode.CHARGING&&!bounds.isEmpty())airflow.drawLayer(canvas,bounds,VehicleArt.frontAtRight(art),time,density,settings,mode,lowRam?24:120,1,1);
    if(mode!=EffectStateController.Mode.OFF&&mode!=EffectStateController.Mode.CHARGING&&settings.windowGlow)windows.draw(canvas,imageBounds,art,time,mode,settings.windowStrength,1f,settings.windowRange,settings.windowPeriod);
    canvas.restore();
    if(isPressed()){touchGlow.setColor(0x24FFFFFF);canvas.drawRoundRect(0,0,getWidth(),getHeight(),12*density,12*density,touchGlow);backdropText.setTextAlign(android.graphics.Paint.Align.CENTER);backdropText.setColor(PsaDesign.muted(AppPreferences.dark(getContext())));backdropText.setTypeface(PsaDesign.regular());backdropText.setFakeBoldText(false);backdropText.setTextSize(10*density);canvas.drawText(AppPreferences.tr(getContext(),"长按1.1s选择车辆图片","Hold 1.1s to choose vehicle"),getWidth()/2f,getHeight()-8*density,backdropText);}
  }
  private final Handler touchHandler=new Handler(Looper.getMainLooper());private float downX,downY;private boolean holdFired,moved;private Runnable holdAction;
  void setVehicleHold(Runnable action){holdAction=action;}
  private final android.graphics.Paint backdropText=new android.graphics.Paint(3),touchGlow=new android.graphics.Paint(3);
  private final Runnable hold=()->{holdFired=true;Haptics.vehicle(getContext());if(holdAction!=null)holdAction.run();};
  @Override public boolean performClick(){return super.performClick();}
  @Override public boolean onTouchEvent(android.view.MotionEvent e){switch(e.getActionMasked()){
    case android.view.MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();holdFired=false;moved=false;Haptics.vehicle(getContext());invalidate();touchHandler.postDelayed(hold,1100);break;
    case android.view.MotionEvent.ACTION_MOVE:float slop=android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();if(Math.abs(e.getX()-downX)>slop||Math.abs(e.getY()-downY)>slop){moved=true;touchHandler.removeCallbacks(hold);}break;
    case android.view.MotionEvent.ACTION_UP:touchHandler.removeCallbacks(hold);setPressed(false);if(!holdFired&&!moved)performClick();return true;
    case android.view.MotionEvent.ACTION_CANCEL:touchHandler.removeCallbacks(hold);setPressed(false);return true;
  }return super.onTouchEvent(e);}
  boolean effectsRunning(){return mode!=EffectStateController.Mode.OFF&&queued;}
}
