package io.github.polestarcn.companion;
import android.content.Context;
/** Shares the native condition label layout so all default utility icons use the same baseline. */
final class UtilityButton extends CenteredStatusButton {
 UtilityButton(Context c){super(c);}
 void setUtilityIcon(int resource){glyph(resource);reserveIconRow(true);}
 void feedback(String value){contents(AppPreferences.visible(getContext(),"show_button_feedback",true)?value:"",null);}
}
