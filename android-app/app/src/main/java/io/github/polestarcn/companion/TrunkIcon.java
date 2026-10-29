package io.github.polestarcn.companion;
import android.content.Context;
/** Selected geometry shares native theme tint with every control surface. */
final class TrunkIcon {
 static int drawable(Context c){return drawable(c,false);}
 static int drawable(Context c,boolean open){String style=AppPreferences.get(c,"trunk_icon","detail");return "door".equals(style)?(open?R.drawable.ic_unlock:R.drawable.ic_lock):"car".equals(style)?R.drawable.ic_tailgate:R.drawable.ic_tailgate_detail;}
 private TrunkIcon(){}
}
