package io.github.polestarcn.companion;

import android.content.ClipData;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Drag handles plus accessible up/down alternatives. Edits are saved only on Apply. */
public final class MetricOrderView extends LinearLayout {
  private final List<DisplayMetric> values;
  public MetricOrderView(Context c){this(c,java.util.Collections.emptyList());}
 public MetricOrderView(Context c,List<DisplayMetric> source){super(c);setOrientation(VERTICAL);values=new ArrayList<>(source);setTag("metric_order");rebuild();
    setOnDragListener((v,e)->{if(!(e.getLocalState() instanceof DisplayMetric))return false;
      if(e.getAction()==DragEvent.ACTION_DROP){int from=values.indexOf(e.getLocalState()),target=values.size()-1;for(int i=0;i<getChildCount();i++)if(e.getY()<getChildAt(i).getY()+getChildAt(i).getHeight()/2f){target=i;break;}move(from,target);}
      if(e.getAction()==DragEvent.ACTION_DRAG_LOCATION&&getParent() instanceof ScrollView){ScrollView scroll=(ScrollView)getParent();float y=e.getY()-scroll.getScrollY();if(y<48)scroll.smoothScrollBy(0,-24);if(y>scroll.getHeight()-48)scroll.smoothScrollBy(0,24);}
      return true;});
  }
  public List<DisplayMetric> values(){return new ArrayList<>(values);}
  public void move(int from,int to){if(from<0||to<0||from>=values.size()||to>=values.size()||from==to)return;DisplayMetric value=values.remove(from);values.add(to,value);Haptics.tap(getContext());rebuild();}
  private int px(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
  private TextView label(String text){TextView v=new TextView(getContext());v.setText(text);v.setTextColor(PsaDesign.ink(AppPreferences.dark(getContext())));v.setTextSize(14);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(px(8),px(8),px(8),px(8));v.setMinHeight(Math.round(48*getResources().getDisplayMetrics().density));return v;}
  private void rebuild(){removeAllViews();for(DisplayMetric metric:values){
    LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);TextView handle=label("≡");handle.setTag("drag_"+metric.id);handle.setContentDescription(AppPreferences.tr(getContext(),"拖动排序 ","Drag to reorder ")+metric.label(AppPreferences.english(getContext())));handle.setFocusable(true);
    View.OnLongClickListener begin=v->{Haptics.tap(getContext());return v.startDragAndDrop(ClipData.newPlainText("metric",metric.id),new DragShadowBuilder(row),metric,0);};handle.setOnLongClickListener(begin);row.addView(handle,new LayoutParams(px(48),-2));
    TextView title=label(metric.label(AppPreferences.english(getContext())));title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(title,new LayoutParams(0,-2,1));
    for(int direction:new int[]{-1,1}){TextView arrow=label(direction<0?"↑":"↓");arrow.setGravity(Gravity.CENTER);arrow.setTag((direction<0?"up_":"down_")+metric.id);arrow.setContentDescription(AppPreferences.tr(getContext(),direction<0?"上移":"下移",direction<0?"Move up":"Move down"));arrow.setEnabled(values.indexOf(metric)+direction>=0&&values.indexOf(metric)+direction<values.size());arrow.setOnClickListener(v->move(values.indexOf(metric),values.indexOf(metric)+direction));row.addView(arrow,new LayoutParams(px(44),-2));}addView(row,new LayoutParams(-1,-2));}
  }
}
