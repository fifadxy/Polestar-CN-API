package io.github.polestarcn.companion;
import android.content.Context;import java.util.Locale;
/** Screen presentation only; vehicle coordinates and location requests remain untouched. */
final class MapMarkerSettings {
 final int size,focusX,focusY,cropX,cropY;final String color;
 MapMarkerSettings(Context c){size=EffectSettings.number(c,"map_pin_size_percent",100,25,300);focusX=EffectSettings.number(c,"map_pin_focus_x",50,0,100);focusY=EffectSettings.number(c,"map_pin_focus_y",42,0,100);cropX=EffectSettings.number(c,"map_crop_focus_x",50,0,100);cropY=EffectSettings.number(c,"map_crop_focus_y",50,0,100);String value=AppPreferences.get(c,"map_pin_color","#FF6B00");color=WidgetColors.validHex(value)?value.toUpperCase(Locale.ROOT):"#FF6B00";}
 String signature(){return size+":"+color+":"+focusX+":"+focusY+":"+cropX+":"+cropY;}
}
