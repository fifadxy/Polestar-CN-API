package io.github.polestarcn.companion;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
/** Local component preview. Never subscribes to vehicle data or sends commands. */
final class SettingsPreviewView extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {
 private final TextView caption,widgetSample;private final UtilityButton climate;private final VehicleConditionButton service;
 private final SharedPreferences preferences;
 SettingsPreviewView(Context c){super(c);setOrientation(VERTICAL);int p=dp(6);setPadding(p,p,p,p);setTag("settings_live_preview");preferences=c.getSharedPreferences("preferences",0);
  caption=new TextView(c);caption.setText(AppPreferences.tr(c,"实时预览 · 电量 63%","Live preview · Battery 63%"));caption.setTextSize(13);addView(caption);
  LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER);climate=new UtilityButton(c);climate.setText(AppPreferences.tr(c,"空调","Climate"));climate.setUtilityIcon(R.drawable.ic_climate);climate.feedback("12m");service=new VehicleConditionButton(c);service.setText(AppPreferences.tr(c,"保养","Service"));service.setConditionIcon(R.drawable.ic_service_reference);
  row.addView(climate,new LayoutParams(0,dp(58),1));row.addView(service,new LayoutParams(0,dp(58),1));addView(row);
  widgetSample=new TextView(c);widgetSample.setText(AppPreferences.tr(c,"Widget · 续航 270 km","Widget · Range 270 km"));widgetSample.setTextSize(11);addView(widgetSample);refresh();
 }
 private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
 private void refresh(){boolean dark=AppPreferences.dark(getContext());caption.setTextColor(dark?Color.WHITE:Color.BLACK);widgetSample.setTextColor(dark?Color.LTGRAY:Color.DKGRAY);climate.setTextColor(PsaDesign.ORANGE);service.setTextColor(dark?Color.WHITE:Color.BLACK);climate.refreshAppearance();service.refreshAppearance();UiTypography.applyTree(this);invalidate();}
 void previewIcons(int percent){climate.previewAppearance(percent,-1);service.previewAppearance(percent,-1);}
 void previewLabels(int percent){climate.previewAppearance(-1,percent);service.previewAppearance(-1,percent);}
 void previewFont(int percent){caption.setTextSize(13*percent/100f);}
 void previewWidget(int percent){widgetSample.setTextSize(11*percent/100f);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();preferences.registerOnSharedPreferenceChangeListener(this);refresh();}
 @Override protected void onDetachedFromWindow(){preferences.unregisterOnSharedPreferenceChangeListener(this);super.onDetachedFromWindow();}
 @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){post(this::refresh);}
}
