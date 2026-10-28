package io.github.polestarcn.companion;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * SwipeRefreshLayout that preserves horizontal gestures, matching the reference APK interaction.
 * A horizontal move beyond touch slop is not intercepted; vertical pull gestures remain available.
 */
public final class HorizontalSwipeRefreshLayout extends SwipeRefreshLayout {
  private float downX, downY;
  private Runnable rightSwipe;
  public void setOnRightSwipe(Runnable action){rightSwipe=action;}
  @Override public boolean dispatchTouchEvent(MotionEvent event){
    if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();}
    if(event.getActionMasked()==MotionEvent.ACTION_UP&&rightSwipe!=null&&event.getX()-downX>Math.max(64*getResources().getDisplayMetrics().density,touchSlop*3)&&Math.abs(event.getY()-downY)<Math.abs(event.getX()-downX)*.45f){
      MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();rightSwipe.run();return true;
    }return super.dispatchTouchEvent(event);
  }
  private boolean horizontalGesture;
  private final int touchSlop;

  public HorizontalSwipeRefreshLayout(Context context) {
    this(context, null);
  }

  public HorizontalSwipeRefreshLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    track(event);
    if (horizontalGesture) return false;
    return super.onInterceptTouchEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    track(event);
    if (horizontalGesture) return false;
    return super.onTouchEvent(event);
  }

  private void track(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        downX = event.getX();
        downY = event.getY();
        horizontalGesture = false;
        break;
      case MotionEvent.ACTION_MOVE:
        float dx = Math.abs(event.getX() - downX);
        float dy = Math.abs(event.getY() - downY);
        if (dx > touchSlop && dx > dy * 1.15f) horizontalGesture = true;
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        horizontalGesture = false;
        break;
      default:
        break;
    }
  }
}
