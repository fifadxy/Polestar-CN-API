package io.github.polestarcn.companion;
import android.content.Context;
final class ChargingSweepSettings {
 final int style,period,strength,color;
 ChargingSweepSettings(Context c){style=EffectSettings.number(c,"charging_sweep_style",0,0,3);period=EffectSettings.number(c,"charging_sweep_period",5,1,30)*1000;strength=EffectSettings.number(c,"charging_sweep_strength",100,0,200);color=ConditionStyle.colorValue(AppPreferences.get(c,"charging_sweep_color","#FF6B00"),PsaDesign.ORANGE);}
}
