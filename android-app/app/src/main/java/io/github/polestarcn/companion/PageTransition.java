package io.github.polestarcn.companion;
import android.animation.*;import android.content.Context;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.view.View;import java.util.WeakHashMap;
/** One short native transition shared by navigation and its isolated preview. */
final class PageTransition {
 private static final WeakHashMap<View,ValueAnimator> running=new WeakHashMap<>();
 private PageTransition(){}
 static void slide(View v,int direction){Context c=v.getContext();PowerManager p=(PowerManager)c.getSystemService(Context.POWER_SERVICE);if(!AppPreferences.visible(c,"light_effects",true)||!AppPreferences.visible(c,"page_transition",true)||!ValueAnimator.areAnimatorsEnabled()||(p!=null&&p.isPowerSaveMode()))return;v.animate().cancel();v.setTranslationX(direction*c.getResources().getDisplayMetrics().density*42);v.setAlpha(.6f);v.animate().translationX(0).alpha(1).setDuration(230).setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();}
 static void stop(View v){v.animate().cancel();v.setTranslationX(0);v.setScaleX(1);v.setScaleY(1);ValueAnimator a=running.remove(v);if(a!=null)a.cancel();}
 static void play(View v){Context c=v.getContext();PowerManager p=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
  if(!AppPreferences.visible(c,"light_effects",true)||!AppPreferences.visible(c,"page_transition",true)||!ValueAnimator.areAnimatorsEnabled()||(p!=null&&p.isPowerSaveMode()))return;
  stop(v);boolean dark=AppPreferences.dark(c);GradientDrawable frost=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,dark?new int[]{0x5023272D,0x1923272D,Color.TRANSPARENT}:new int[]{0x4BF0F2F6,0x19F0F2F6,Color.TRANSPARENT});v.setForeground(frost);v.setAlpha(.8f);
  if(Build.VERSION.SDK_INT>=31){float d=c.getResources().getDisplayMetrics().density;v.setRenderEffect(RenderEffect.createBlurEffect(5*d,5*d,Shader.TileMode.CLAMP));}
  ValueAnimator a=ValueAnimator.ofFloat(0,1);running.put(v,a);a.setDuration(170);a.addUpdateListener(frame->{float phase=(Float)frame.getAnimatedValue();frost.setAlpha((int)(210*(1-phase)));v.setAlpha(.8f+.2f*phase);if(Build.VERSION.SDK_INT>=31&&phase>.32f)v.setRenderEffect(null);});a.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator animator){if(running.get(v)==animator)running.remove(v);v.setForeground(null);v.setAlpha(1);if(Build.VERSION.SDK_INT>=31)v.setRenderEffect(null);}});a.start();
 }
}
