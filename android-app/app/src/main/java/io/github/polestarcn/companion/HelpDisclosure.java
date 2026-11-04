package io.github.polestarcn.companion;
import android.content.Context;import android.view.*;import android.widget.*;
/** Supplemental text is collapsed by default. No network or dialogs are required to read it. */
public final class HelpDisclosure {
 private HelpDisclosure(){}
 public static void add(LinearLayout parent,Context c,String title,String detail){LinearLayout group=new LinearLayout(c);group.setOrientation(LinearLayout.VERTICAL);TextView question=new TextView(c);question.setText(c.getString(R.string.help_label,title));question.setTextSize(13);question.setTypeface(PsaDesign.regular());question.setTextColor(PsaDesign.muted(AppPreferences.dark(c)));question.setMinHeight(Math.round(44*c.getResources().getDisplayMetrics().density));question.setGravity(Gravity.CENTER_VERTICAL);TextView body=new TextView(c);body.setText(detail);body.setTextSize(12);body.setTypeface(PsaDesign.regular());body.setTextColor(PsaDesign.muted(AppPreferences.dark(c)));body.setVisibility(View.GONE);question.setOnClickListener(v->{Haptics.tap(c);boolean open=body.getVisibility()!=View.VISIBLE;body.setVisibility(open?View.VISIBLE:View.GONE);question.setContentDescription(c.getString(R.string.help_state,title,AppPreferences.tr(c,open?"已展开":"已收起",open?"expanded":"collapsed")));});question.setContentDescription(title);group.addView(question);group.addView(body);parent.addView(group);}
}
