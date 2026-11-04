package io.github.polestarcn.companion;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
/** Local component preview. Never subscribes to vehicle data or sends commands. */
final class SettingsPreviewView extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {
 private final TextView caption,widgetSample;private final ImageView widgetImage;
 private final Runnable widgetRedraw;private final UtilityButton climate;private final VehicleConditionButton service;
 private final SharedPreferences preferences;private int fontPreview=-1,widgetPreview=-1;
 SettingsPreviewView(Context c){super(c);setOrientation(VERTICAL);int p=dp(6);setPadding(p,p,p,p);setTag("settings_live_preview");preferences=c.getSharedPreferences("preferences",0);
  caption=new TextView(c);caption.setText(AppPreferences.tr(c,"模拟数据 · 实时样式预览","Demo data · live style preview"));caption.setTextSize(13);addView(caption);
  LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER);climate=new UtilityButton(c);climate.setText(AppPreferences.tr(c,"空调","Climate"));climate.setUtilityIcon(R.drawable.ic_climate);climate.feedback("12m");service=new VehicleConditionButton(c);service.setText(AppPreferences.tr(c,"保养","Service"));service.setConditionIcon(R.drawable.ic_service_reference);
  row.addView(climate,new LayoutParams(0,dp(58),1));row.addView(service,new LayoutParams(0,dp(58),1));addView(row);
  widgetSample=new TextView(c);widgetSample.setText(AppPreferences.tr(c,"Widget · 续航 270 km","Widget · Range 270 km"));widgetSample.setTextSize(11);addView(widgetSample);widgetImage=new ImageView(c);widgetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);addView(widgetImage,new LayoutParams(-1,dp(112)));widgetRedraw=()->{try{widgetImage.setImageBitmap(VoLikeWidgetRenderer.preview(getContext(),new DemoRepository().snapshot(Region.CHINA),VoLikeWidgetRenderer.style(getContext()),300,155));}catch(RuntimeException ignored){widgetImage.setImageDrawable(null);}};refresh();
 }
 private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
 private void refresh(){caption.setTypeface(PsaDesign.regular());widgetSample.setTypeface(PsaDesign.regular());boolean dark=AppPreferences.dark(getContext());caption.setTextColor(dark?Color.WHITE:Color.BLACK);widgetSample.setTextColor(WidgetAppearance.text(getContext()));climate.setTextColor(WidgetAppearance.button(getContext()));service.setTextColor(dark?Color.WHITE:Color.BLACK);climate.refreshAppearance();service.refreshAppearance();int surface=dark?0xFF292D31:Color.WHITE;android.graphics.drawable.GradientDrawable card=new android.graphics.drawable.GradientDrawable();card.setColor(surface);card.setCornerRadius(dp(10));card.setStroke(Math.max(1,WidgetAppearance.width(getContext(),"widget_border_width",2)),WidgetAppearance.border(getContext()));setBackground(card);float opacity=EffectSettings.number(getContext(),"button_background_percent",100,0,100)/100f;for(TextView button:new TextView[]{climate,service}){android.graphics.drawable.GradientDrawable b=new android.graphics.drawable.GradientDrawable();b.setColor(((Math.round(opacity*255))<<24)|(surface&0xFFFFFF));b.setCornerRadius(dp(8));button.setBackground(b);}if(fontPreview>=0)previewFont(fontPreview);if(widgetPreview>=0)previewWidget(widgetPreview);UiTypography.applyTree(this);removeCallbacks(widgetRedraw);postDelayed(widgetRedraw,140);invalidate();}
 void previewIcons(int percent){climate.previewAppearance(percent,-1);service.previewAppearance(percent,-1);}
 void previewLabels(int percent){climate.previewAppearance(-1,percent);service.previewAppearance(-1,percent);}
 void previewFont(int percent){fontPreview=percent;caption.setTextSize(13*percent/(float)UiTypography.percent(getContext(),UiTypography.key(getContext())));}
 void previewWidget(int percent){widgetPreview=percent;widgetSample.setTextSize(11*percent/(float)UiTypography.percent(getContext(),UiTypography.key(getContext())));}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();preferences.registerOnSharedPreferenceChangeListener(this);refresh();}
 @Override protected void onDetachedFromWindow(){removeCallbacks(widgetRedraw);preferences.unregisterOnSharedPreferenceChangeListener(this);super.onDetachedFromWindow();}
 @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){post(this::refresh);}
}
