package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Display-only per-skin, per-button editor. Never touches commands, state, or VIN.
 * Percent geometry is bounded by the original weighted slot so controls cannot
 * overlap neighbours. No preference is written until the user taps Save.
 */
final class SkinButtonLayout {
  static final String PREFIX="skin_button_";
  static final String[] CONTROLS={"lock","climate","air","trunk","windows","signal","camp","body"};
  static final String[] FIELDS={"width","height","stroke","radius","icon","label",
      "position_x","position_y","focus_x","focus_y"};
  static final int[] LOW={60,60,0,0,50,60,-24,-24,-24,-24};
  static final int[] HIGH={100,100,6,28,190,180,24,24,24,24};
  static final int[] DEFAULTS={100,100,1,12,100,100,0,0,0,0};
  private SkinButtonLayout(){}

  static String key(String skin,String control,String field){
    if(!in(UiSkin.CHOICES,skin)||!in(CONTROLS,control)||!in(FIELDS,field))
      throw new IllegalArgumentException("Unrecognized skin/button/property");
    return PREFIX+skin+"_"+control+"_"+field;
  }
  private static boolean in(String[] list,String value){
    for(String item:list)if(item.equals(value))return true;return false;
  }
  static int index(String field){
    for(int i=0;i<FIELDS.length;i++)if(FIELDS[i].equals(field))return i;
    throw new IllegalArgumentException("Unrecognized property");
  }
  static int clamp(int value,int i){return Math.max(LOW[i],Math.min(HIGH[i],value));}
  static int read(Context c,String skin,String control,String field){
    int i=index(field);
    return EffectSettings.number(c,key(skin,control,field),DEFAULTS[i],LOW[i],HIGH[i]);
  }
  static int current(Context c,String control,String field){
    return read(c,UiSkin.get(c),control,field);
  }
  private static boolean customized(Context c,String control,String field){
    return c.getSharedPreferences("preferences",0).contains(key(UiSkin.get(c),control,field));
  }
  static int[] values(Context c,String skin,String control){
    int[] result=new int[FIELDS.length];
    for(int i=0;i<FIELDS.length;i++)result[i]=read(c,skin,control,FIELDS[i]);
    return result;
  }
  static void store(Context c,String skin,String control,int[] values){
    if(values==null||values.length!=FIELDS.length)throw new IllegalArgumentException("Bad button values");
    SharedPreferences.Editor edit=c.getSharedPreferences("preferences",0).edit();
    // An unchanged slider must not override an existing skin's original radius/stroke.
    for(int i=0;i<FIELDS.length;i++){
      int value=clamp(values[i],i);String pref=key(skin,control,FIELDS[i]);
      if(value==DEFAULTS[i])edit.remove(pref);
      else edit.putString(pref,String.valueOf(value));
    }
    edit.apply();
  }
  static void clear(Context c,String skin,String control){
    SharedPreferences.Editor edit=c.getSharedPreferences("preferences",0).edit();
    for(String field:FIELDS)edit.remove(key(skin,control,field));
    edit.apply();
  }
  static void clearSkin(Context c,String skin){
    for(String control:CONTROLS)clear(c,skin,control);
  }
  private static int dp(Context c,int value){
    return Math.round(value*c.getResources().getDisplayMetrics().density);
  }
  static int strokePx(Context c,String control,int originalDp){
    return dp(c,customized(c,control,"stroke")?current(c,control,"stroke"):originalDp);
  }
  static float iconScale(View v){
    String control=controlOf(v);
    return control==null?1f:current(v.getContext(),control,"icon")/100f;
  }
  static float labelScale(View v){
    String control=controlOf(v);
    return control==null?1f:current(v.getContext(),control,"label")/100f;
  }
  static int focus(View v,boolean horizontal){
    String control=controlOf(v);
    if(control==null)return 0;
    return dp(v.getContext(),current(v.getContext(),control,horizontal?"focus_x":"focus_y"));
  }
  static String controlOf(View v){
    for(int i=0;v!=null&&i<12;i++){
      Object found=v.getTag(R.id.psa_skin_button_control);
      if(found instanceof String&&in(CONTROLS,(String)found))return (String)found;
      ViewParent parent=v.getParent();
      v=parent instanceof View?(View)parent:null;
    }
    return null;
  }

  /** A weighted slot remains exactly the original size. Only the visible card resizes. */
  static void mount(LinearLayout parent,View controlView,String control,
      LinearLayout.LayoutParams original){
    controlView.setTag(R.id.psa_skin_button_control,control);
    FrameLayout slot=new FrameLayout(parent.getContext());
    slot.setTag("skin_button_slot_"+control);
    slot.setClipChildren(true);
    parent.addView(slot,original);
    slot.addView(controlView,new FrameLayout.LayoutParams(-1,-1,Gravity.TOP|Gravity.LEFT));
    slot.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->
        sizeSlot(slot,controlView,control));
    decorate(controlView,control);
  }
  private static void sizeSlot(FrameLayout slot,View card,String control){
    int sw=slot.getWidth(),sh=slot.getHeight();if(sw<=0||sh<=0)return;
    Context c=slot.getContext();
    int w=Math.min(sw,Math.max(Math.min(sw,dp(c,40)),
        Math.round(sw*current(c,control,"width")/100f)));
    int h=Math.min(sh,Math.max(Math.min(sh,dp(c,40)),
        Math.round(sh*current(c,control,"height")/100f)));
    int left=(sw-w)/2+Math.max(-(sw-w)/2,Math.min((sw-w)/2,dp(c,current(c,control,"position_x"))));
    int top=(sh-h)/2+Math.max(-(sh-h)/2,Math.min((sh-h)/2,dp(c,current(c,control,"position_y"))));
    FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)card.getLayoutParams();
    if(lp.width!=w||lp.height!=h||lp.leftMargin!=left||lp.topMargin!=top||
        lp.gravity!=(Gravity.TOP|Gravity.LEFT)){
      lp.width=w;lp.height=h;lp.leftMargin=left;lp.topMargin=top;
      lp.gravity=Gravity.TOP|Gravity.LEFT;card.setLayoutParams(lp);
    }
  }

  /** Preserve existing active/error colors; only modify the requested shape attributes. */
  static void decorate(View card,String control){
    Context c=card.getContext(); Drawable drawable=card.getBackground();
    if(drawable instanceof GradientDrawable){
      GradientDrawable g=(GradientDrawable)drawable;
      if(customized(c,control,"radius"))g.setCornerRadius(dp(c,current(c,control,"radius")));
      // A runtime state binding may later recolor the border, while retaining this width.
      if(customized(c,control,"stroke"))
        g.setStroke(dp(c,current(c,control,"stroke")),PsaDesign.line(AppPreferences.dark(c)));
    } else if(customized(c,control,"stroke")&&current(c,control,"stroke")>0){
      GradientDrawable border=new GradientDrawable();
      border.setColor(Color.TRANSPARENT);
      border.setCornerRadius(dp(c,current(c,control,"radius")));
      border.setStroke(dp(c,current(c,control,"stroke")),PsaDesign.line(AppPreferences.dark(c)));
      card.setBackground(border);
    }
  }
  /** Runs after ButtonAppearance.apply, which restores global sizing from baselines. */
  static void apply(View v){
    String control=controlOf(v);if(control==null)return;
    String tag=v.getTag() instanceof String?(String)v.getTag():"";
    boolean icon=v instanceof ImageView&&tag.startsWith("control_icon_");
    boolean text=v instanceof TextView&&(tag.startsWith("control_title_")
        ||tag.startsWith("control_feedback_")||tag.startsWith("control_support_")
        ||tag.equals("volike_control_label")||tag.equals("shortcut_label")
        ||tag.equals("shortcut_feedback"));
    if(icon){
      ViewGroup.LayoutParams lp=v.getLayoutParams();
      if(lp!=null&&lp.width>0&&lp.height>0){
        int[] original=(int[])v.getTag(R.id.psa_size_baseline);
        if(original==null){original=new int[]{lp.width,lp.height};
          v.setTag(R.id.psa_size_baseline,original);}
        float common=UiDisplayMode.minimal(v.getContext())?
            MinimalIconAppearance.sizePercent(v.getContext())/100f:ButtonAppearance.iconScale(v.getContext());
        float percent=common*iconScale(v);
        int max=dp(v.getContext(),76);
        int width=Math.max(1,Math.min(max,Math.round(original[0]*percent)));
        int height=Math.max(1,Math.min(max,Math.round(original[1]*percent)));
        ViewParent parent=v.getParent();
        if(parent instanceof View){
          int pw=((View)parent).getWidth()-((View)parent).getPaddingLeft()-((View)parent).getPaddingRight();
          int ph=((View)parent).getHeight()-((View)parent).getPaddingTop()-((View)parent).getPaddingBottom();
          if(pw>0)width=Math.min(width,pw);
          if(ph>0)height=Math.min(height,ph);
        }
        if(lp.width!=width||lp.height!=height){lp.width=width;lp.height=height;v.setLayoutParams(lp);}
      }
    }
    if(text&&!(v instanceof CenteredStatusButton)){
      TextView label=(TextView)v;
      Object normalBaseline=label.getTag(R.id.psa_size_baseline);
      Float base=normalBaseline instanceof Float?(Float)normalBaseline:
          (Float)label.getTag(R.id.psa_text_baseline);
      if(base==null){base=label.getTextSize();label.setTag(R.id.psa_text_baseline,base);}
      if(customized(v.getContext(),control,"label")){
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(label,0);
        float normalScale=normalBaseline instanceof Float?ButtonAppearance.textScale(v.getContext()):1f;
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX,base*normalScale*labelScale(v));
      }
    }
    if(icon||text){
      v.setTranslationX(focus(v,true));
      v.setTranslationY(focus(v,false));
    }
  }

  /** A self-contained, offline preview. It never reads car state or initiates commands. */
  static final class Preview extends View {
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String skin,control;
    private int[] chosen;
    Preview(Context c,String skin,String control){
      super(c);this.skin=skin;this.control=control;chosen=values(c,skin,control);
      setContentDescription("Skin button layout preview");
    }
    void update(int[] next){chosen=next.clone();invalidate();}
    @Override protected void onDraw(Canvas canvas){
      super.onDraw(canvas);
      float d=getResources().getDisplayMetrics().density;
      float cx=getWidth()/2f,cy=getHeight()/2f;
      pen.setStyle(Paint.Style.FILL);pen.setColor(AppPreferences.dark(getContext())?0xff22272c:0xffeef0f1);
      canvas.drawRoundRect(0,0,getWidth(),getHeight(),12*d,12*d,pen);
      float maxW=getWidth()-24*d,maxH=getHeight()-18*d;
      float w=maxW*chosen[0]/100f,h=maxH*chosen[1]/100f;
      float offX=Math.max(-(maxW-w)/2,Math.min((maxW-w)/2,chosen[6]*d));
      float offY=Math.max(-(maxH-h)/2,Math.min((maxH-h)/2,chosen[7]*d));
      RectF r=new RectF(cx-w/2+offX,cy-h/2+offY,cx+w/2+offX,cy+h/2+offY);
      pen.setColor(AppPreferences.dark(getContext())?0xff32383e:Color.WHITE);
      canvas.drawRoundRect(r,chosen[3]*d,chosen[3]*d,pen);
      if(chosen[2]>0){
        pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(chosen[2]*d);
        pen.setColor(PsaDesign.ORANGE);canvas.drawRoundRect(r,chosen[3]*d,chosen[3]*d,pen);
        pen.setStyle(Paint.Style.FILL);
      }
      float textX=r.centerX()+chosen[8]*d,textY=r.centerY()+chosen[9]*d;
      pen.setTextAlign(Paint.Align.CENTER);pen.setColor(PsaDesign.ORANGE);
      float glyph=Math.min(15*d,Math.max(5*d,10*d*chosen[4]/100f));
      canvas.drawCircle(textX,textY-13*d,glyph/2,pen);
      pen.setColor(AppPreferences.dark(getContext())?Color.WHITE:0xff222222);
      pen.setTextSize(Math.min(18*d,11*d*chosen[5]/100f));
      String label=control.equals("air")?"PM2.5·12 /AQI·38":
          control.equals("lock")?"已锁":control.equals("climate")?"空调":
          control.equals("camp")?"露营模式":control.equals("body")?"保养":
          control.equals("trunk")?"尾箱":control.equals("windows")?"车窗":"寻车";
      canvas.drawText(label,textX,textY+15*d,pen);
      pen.setTextAlign(Paint.Align.LEFT);
    }
  }
}
