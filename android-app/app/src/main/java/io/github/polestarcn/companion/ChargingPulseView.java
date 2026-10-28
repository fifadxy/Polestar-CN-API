package io.github.polestarcn.companion;
import android.animation.ValueAnimator;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.os.PowerManager;
import android.os.Build;
import android.view.View;
import android.view.animation.LinearInterpolator;
/** A 4.8-second full-card sweep behind charging text; paused when hidden or animations disabled. */
public final class ChargingPulseView extends View {
 private final Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);private final Matrix matrix=new Matrix();
 private ValueAnimator animator;private LinearGradient shader;private float phase;private long lastFrame;private int accent=PsaDesign.ORANGE;private boolean active,registered,hostActive=true;
 private final BroadcastReceiver power=new BroadcastReceiver(){public void onReceive(Context c,Intent i){sync();}};
 private final SharedPreferences.OnSharedPreferenceChangeListener prefs=(p,key)->sync();
 public ChargingPulseView(Context c){super(c);setTag("charging_pulse");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 public void setAccentColor(int color){if(color!=accent){accent=color;shader=null;invalidate();}}
 public void setActive(boolean value){active=value;setVisibility(value?VISIBLE:GONE);sync();}
 void setHostActive(boolean value){hostActive=value;sync();}
 private boolean powerSaving(){PowerManager p=(PowerManager)getContext().getSystemService(Context.POWER_SERVICE);return p!=null&&p.isPowerSaveMode();}
 private void sync(){boolean animate=hostActive&&active&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&(AppPreferences.visible(getContext(),"light_effects",true)&&AppPreferences.visible(getContext(),"charging_animation",true))&&ValueAnimator.areAnimatorsEnabled()&&!powerSaving();if(!animate){stop();return;}if(animator!=null)return;
  animator=ValueAnimator.ofFloat(0,1);animator.setDuration(4800);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());animator.addUpdateListener(a->{long now=SystemClock.uptimeMillis();if(now-lastFrame<48)return;lastFrame=now;phase=(Float)a.getAnimatedValue();invalidate();});animator.start();}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();IntentFilter f=new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);if(Build.VERSION.SDK_INT>=33)getContext().registerReceiver(power,f,Context.RECEIVER_NOT_EXPORTED);else getContext().registerReceiver(power,f);registered=true;getContext().getSharedPreferences("preferences",0).registerOnSharedPreferenceChangeListener(prefs);sync();}
 @Override protected void onDetachedFromWindow(){stop();if(registered){getContext().unregisterReceiver(power);registered=false;}getContext().getSharedPreferences("preferences",0).unregisterOnSharedPreferenceChangeListener(prefs);super.onDetachedFromWindow();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);sync();}
 @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);sync();}
 private void stop(){if(animator!=null){animator.cancel();animator=null;}phase=0;invalidate();}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){shader=null;}
 @Override protected void onDraw(Canvas c){if(animator==null||!active||getWidth()==0)return;float spread=getWidth()*.32f;
  if(shader==null){shader=new LinearGradient(-spread,0,spread,0,new int[]{Color.TRANSPARENT,Color.argb(18,Color.red(accent),Color.green(accent),Color.blue(accent)),Color.argb(42,Color.red(accent),Color.green(accent),Color.blue(accent)),Color.TRANSPARENT},new float[]{0,.36f,.53f,1},Shader.TileMode.CLAMP);glow.setShader(shader);}
  matrix.setTranslate(-spread+phase*(getWidth()+2*spread),0);shader.setLocalMatrix(matrix);float radius=12*getResources().getDisplayMetrics().density;c.drawRoundRect(0,0,getWidth(),getHeight(),radius,radius,glow);
 }
}
