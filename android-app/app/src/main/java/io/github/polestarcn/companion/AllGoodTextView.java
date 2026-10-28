package io.github.polestarcn.companion;
import android.animation.ValueAnimator;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
/** Native text with one subtle 6.4-second sweep, ~12fps; no layers/bitmaps/network work. */
public final class AllGoodTextView extends TextView {
 private ValueAnimator animator;private LinearGradient shader;private final Matrix matrix=new Matrix();
 private float phase;private int shaderColor;private long lastFrame;private boolean hostActive=true,registered;
 private final BroadcastReceiver power=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){sync();}};
 public AllGoodTextView(Context c){super(c);}
 @Override public void setText(CharSequence value,BufferType type){super.setText(value,type);sync();}
 private boolean good(){String value=getText()==null?"":getText().toString().trim();return "All Good！".equalsIgnoreCase(value)||"All Good!".equalsIgnoreCase(value)||"ALL GOOD".equalsIgnoreCase(value)||"ALL\nGOOD".equalsIgnoreCase(value);}
 private boolean powerSaving(){PowerManager p=(PowerManager)getContext().getSystemService(Context.POWER_SERVICE);return p!=null&&p.isPowerSaveMode();}
 private void sync(){boolean enabled=hostActive&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&good()&&AppPreferences.visible(getContext(),"light_effects",true)&&AppPreferences.visible(getContext(),"all_good_shimmer",true)&&ValueAnimator.areAnimatorsEnabled()&&!powerSaving();if(!enabled){stop();return;}if(animator!=null)return;
  animator=ValueAnimator.ofFloat(0,1);animator.setDuration(6400);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());animator.addUpdateListener(a->{long now=SystemClock.uptimeMillis();if(now-lastFrame<80)return;lastFrame=now;phase=(Float)a.getAnimatedValue();invalidate();});animator.start();}
 private void stop(){if(animator!=null){animator.cancel();animator=null;}if(getPaint()!=null)getPaint().setShader(null);phase=0;invalidate();}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();IntentFilter f=new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);if(Build.VERSION.SDK_INT>=33)getContext().registerReceiver(power,f,Context.RECEIVER_NOT_EXPORTED);else getContext().registerReceiver(power,f);registered=true;sync();}
 @Override protected void onDetachedFromWindow(){stop();if(registered){getContext().unregisterReceiver(power);registered=false;}super.onDetachedFromWindow();}
 @Override protected void onWindowVisibilityChanged(int v){super.onWindowVisibilityChanged(v);sync();}
 @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);sync();}
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);shader=null;}
 public static void host(View v,boolean active){if(v==null)return;if(!active)PageTransition.stop(v);if(v instanceof AllGoodTextView){AllGoodTextView t=(AllGoodTextView)v;t.hostActive=active;t.sync();}if(v instanceof ChargingPulseView)((ChargingPulseView)v).setHostActive(active);if(v instanceof VehicleHeroView)((VehicleHeroView)v).setEffectsHostActive(active);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)host(((ViewGroup)v).getChildAt(i),active);}
 @Override protected void onDraw(Canvas c){if(animator==null||getWidth()==0){getPaint().setShader(null);super.onDraw(c);return;}int base=getCurrentTextColor();float spread=getWidth()*.26f;
  if(shader==null||base!=shaderColor){shaderColor=base;float mix=AppPreferences.dark(getContext())?.68f:.48f;int highlight=Color.rgb((int)(Color.red(base)+(255-Color.red(base))*mix),(int)(Color.green(base)+(255-Color.green(base))*mix),(int)(Color.blue(base)+(255-Color.blue(base))*mix));shader=new LinearGradient(-spread,0,spread,0,new int[]{base,base,highlight,base,base},new float[]{0,.30f,.50f,.70f,1},Shader.TileMode.CLAMP);}
  matrix.setTranslate(-spread+phase*(getWidth()+2*spread),0);shader.setLocalMatrix(matrix);getPaint().setShader(shader);super.onDraw(c);getPaint().setShader(null);
 }
}
