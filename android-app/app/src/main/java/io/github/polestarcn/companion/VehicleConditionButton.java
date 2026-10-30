package io.github.polestarcn.companion;
import android.content.Context;
import java.util.function.Supplier;
/** Native, bounded condition label; icon and primary label remain available in all display modes. */
public final class VehicleConditionButton extends CenteredStatusButton {
 private final Supplier<VehicleSnapshot> snapshot;
 public VehicleConditionButton(Context c){this(c,()->null);}
 public VehicleConditionButton(Context c,Supplier<VehicleSnapshot> source){super(c);snapshot=source;}
 public void setConditionIcon(int res){glyph(res);reserveIconRow(res!=0);}
 public static String displayMode(Context c){String value=AppPreferences.get(c,"condition_display","percent");return value.equals("percent")||value.equals("text")?value:"bar";}
 public void refreshService(){String mode=displayMode(getContext()),base=title();Integer percent=mode.equals("text")?null:ServiceReading.cyclePercent(getContext(),snapshot.get());
  String fullDetail=percent==null?"":AppPreferences.tr(getContext(),"保养周期 ","Service cycle ")+percent+"%";
  String detail=percent==null?VehicleCondition.summary(getContext(),snapshot.get(),AppPreferences.visible(getContext(),"show_tire_status",true)):percent+"%";
  // Percent-only removes the bar, not the native title or the PS Default icon slot.
  contents(detail,mode.equals("bar")?percent:null);
  setContentDescription(base+(percent==null?"":" · "+fullDetail)+AppPreferences.tr(getContext()," · 点击展开车况"," · Expand vehicle condition"));
 }
}
