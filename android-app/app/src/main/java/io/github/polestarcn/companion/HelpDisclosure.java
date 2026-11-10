package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
/** Consistent description followed by a VI-orange circled question icon. */
public final class HelpDisclosure {
  private HelpDisclosure(){}
  private static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
  private static final class QuestionDrawable extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int px;
    QuestionDrawable(Context c){px=dp(c,14);setBounds(0,0,px,px);}
    @Override public void draw(Canvas canvas){
      float cx=getBounds().centerX(),cy=getBounds().centerY(),radius=px*.43f;
      p.setColor(PsaDesign.ORANGE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,px*.09f));
      canvas.drawCircle(cx,cy,radius,p);
      p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);
      p.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD));
      p.setTextSize(px*.66f);Paint.FontMetrics fm=p.getFontMetrics();
      canvas.drawText("?",cx,cy-(fm.ascent+fm.descent)/2,p);
    }
    @Override public int getIntrinsicWidth(){return px;}
    @Override public int getIntrinsicHeight(){return px;}
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  }
  public static void add(LinearLayout parent,Context c,String title,String detail){
    String heading=title==null||title.trim().isEmpty()?AppPreferences.tr(c,"说明","Info"):title;
    LinearLayout group=new LinearLayout(c);group.setOrientation(LinearLayout.VERTICAL);
    TextView question=new TextView(c);
    // A trailing compound drawable does not enlarge the font layout metrics as ImageSpan can.
    // WRAP_CONTENT keeps the orange circled question directly beside the label.
    question.setText(heading);
    question.setCompoundDrawablesWithIntrinsicBounds(null,null,new QuestionDrawable(c),null);
    question.setCompoundDrawablePadding(dp(c,5));
    question.setIncludeFontPadding(false);
    question.setTag("help_explanation_trigger");question.setTextSize(13);
    question.setTypeface(PsaDesign.regular());question.setTextColor(PsaDesign.muted(AppPreferences.dark(c)));
    question.setMinHeight(dp(c,MenuSpacing.dp(c)<=2?30:32));
    question.setGravity(Gravity.CENTER_VERTICAL);
    question.setContentDescription(heading+"？");
    TextView body=new TextView(c);body.setText(detail);body.setTextSize(12);body.setTag("help_explanation_body");
    body.setTypeface(PsaDesign.regular());body.setTextColor(PsaDesign.muted(AppPreferences.dark(c)));
    body.setPadding(dp(c,2),0,dp(c,2),dp(c,6));body.setVisibility(View.GONE);
    question.setOnClickListener(v->{Haptics.tap(c);boolean open=body.getVisibility()!=View.VISIBLE;
      body.setVisibility(open?View.VISIBLE:View.GONE);
      question.setContentDescription(heading+"？ · "+AppPreferences.tr(c,open?"已展开":"已收起",open?"expanded":"collapsed"));});
    group.addView(question,new LinearLayout.LayoutParams(-2,-2));
    group.addView(body);parent.addView(group);
  }
}
