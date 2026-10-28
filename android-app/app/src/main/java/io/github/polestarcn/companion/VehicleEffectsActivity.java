package io.github.polestarcn.companion;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.Locale;
/** Preview-only controls: changes local rendering preferences, never sends vehicle commands. */
public final class VehicleEffectsActivity extends Activity {
  private LinearLayout page;
  private VehicleHeroView hero;
  private VehicleEffectSettings.Mode preview=VehicleEffectSettings.Mode.CHARGING;
  private int ink,muted,surface;
  private String tr(String zh,String en){return AppPreferences.tr(this,zh,en);}
  private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
  private TextView text(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(ink);v.setTextSize(sp);v.setPadding(dp(6),dp(6),dp(6),dp(4));return v;}
  private void choose(String title,String[] labels,String[] keys,String pref,String fallback){
    page.addView(text(title,14));
    Spinner spin=new Spinner(this);
    ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels){
      @Override public View getView(int position,View view,ViewGroup parent){View result=super.getView(position,view,parent);if(result instanceof TextView)((TextView)result).setTextColor(ink);return result;}
    };
    spin.setAdapter(adapter);
    String selected=AppPreferences.get(this,pref,fallback);int ix=0;
    for(int i=0;i<keys.length;i++)if(selected.equals(keys[i]))ix=i;
    spin.setSelection(ix);
    spin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      public void onNothingSelected(android.widget.AdapterView<?> p){}
      public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){
        AppPreferences.set(VehicleEffectsActivity.this,pref,keys[position]);
        if(hero!=null){hero.reloadEffectSettings();hero.preview(preview);}
      }
    });
    page.addView(spin);
  }
  private void seek(String title,String pref,int min,int max,int initial,String unit){
    TextView label=text("",14);page.addView(label);
    SeekBar bar=new SeekBar(this);bar.setMax(max-min);
    bar.setProgress(VehicleEffectSettings.intValue(this,pref,initial,min,max)-min);
    Runnable update=()->{
      int value=min+bar.getProgress();
      label.setText(title+" · "+(pref.equals("effect_speed")?String.format(Locale.US,"%.1f",(value/100.0)):Integer.toString(value))+unit);
      AppPreferences.set(this,pref,Integer.toString(value));
      if(hero!=null)hero.reloadEffectSettings();
    };
    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar bar,int p,boolean fromUser){if(fromUser)update.run();}
      public void onStartTrackingTouch(SeekBar bar){}
      public void onStopTrackingTouch(SeekBar bar){}
    });
    update.run();page.addView(bar);
  }
  private void toggle(String title,String pref,boolean fallback){
    Switch control=new Switch(this);control.setText(title);control.setTextColor(ink);
    control.setPadding(dp(6),dp(9),dp(6),dp(9));
    control.setChecked(AppPreferences.visible(this,pref,fallback));
    control.setOnCheckedChangeListener((button,checked)->{
      AppPreferences.set(this,pref,checked?"on":"off");if(hero!=null)hero.reloadEffectSettings();
    });
    page.addView(control);
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);boolean dark=AppPreferences.dark(this);
    ink=PsaDesign.ink(dark);muted=PsaDesign.muted(dark);surface=PsaDesign.surface(dark);
    ScrollView scroll=new ScrollView(this);page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(16),dp(12),dp(16),dp(30));page.setBackgroundColor(PsaDesign.canvas(dark));
    scroll.addView(page);setContentView(scroll);
    Button back=new Button(this);back.setText(tr("返回设置","Back to settings"));back.setOnClickListener(v->finish());page.addView(back);
    page.addView(text(tr("车辆动态效果 · 原生实时动画","Vehicle effects · native real-time"),22));
    page.addView(text(tr("仅影响视觉展示。原始高清 PNG 保持不变；不使用 GIF，也不触发远程控制。","Presentation only. The HD PNG is unchanged; no GIF and no remote commands."),12));
    hero=new VehicleHeroView(this);page.addView(hero,new LinearLayout.LayoutParams(-1,dp(220)));
    page.addView(text(tr("即时预览场景","Preview scenario"),15));
    String[] labels={tr("充电 · VI橙","Charging · VI orange"),tr("制冷 · 蓝色A1","Cooling · blue A1"),
      tr("制热 · 橙色A1","Heating · orange A1"),tr("通风 · 绿色A2","Ventilation · green A2"),tr("无动画","No effect")};
    VehicleEffectSettings.Mode[] modes=VehicleEffectSettings.Mode.values();
    LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.VERTICAL);
    for(int i=0;i<labels.length;i++){final VehicleEffectSettings.Mode mode=modes[i==4?0:i+1];
      Button button=new Button(this);button.setText(labels[i]);button.setTextSize(12);
      button.setOnClickListener(v->{preview=mode;hero.preview(mode);});buttons.addView(button);}
    page.addView(buttons);hero.preview(preview);
    toggle(tr("车辆动效总开关","Vehicle effects enabled"),"effect_enabled",true);
    choose(tr("充电粒子风格 · V5","Charge style · V5"),
      new String[]{"C1a · 自然星尘","C1d · 羽化柔光","C1+a · 自然吸收","C1+d · 柔光吸收","C1+c · 弧线引导"},
      new String[]{"C1a","C1d","C1+a","C1+d","C1+c"},"effect_charge_style","C1a");
    choose(tr("充电粒子运动方向","Particle direction"),new String[]{tr("上升","Rise"),tr("吸入","Absorb")},
      new String[]{"rise","absorb"},"effect_charge_direction","rise");
    choose(tr("空调冷热显示 · 车辆接口无可靠冷热模式，仅作视觉选择","Climate color · visual choice only, not vehicle-reported"),
      new String[]{tr("制冷蓝色A1","Cooling blue A1"),tr("制热橙色A1","Heating orange A1")},
      new String[]{"cool","heat"},"effect_climate_visual","cool");
    seek(tr("动画速度","Animation speed"),"effect_speed",50,200,100,"×");
    seek(tr("实心粒子数量","Solid particle count"),"effect_particles",20,150,60,"");
    seek(tr("气流线数量","Airflow segments"),"effect_wind_count",3,30,12,"");
    seek(tr("气流强度","Airflow intensity"),"effect_wind_strength",20,100,65,"%");
    Button reset=new Button(this);reset.setText(tr("恢复动效默认参数","Restore animation defaults"));
    reset.setOnClickListener(v->{
      for(String key:new String[]{"effect_charge_style","effect_charge_direction","effect_speed",
          "effect_particles","effect_wind_count","effect_wind_strength","effect_climate_visual"})
        getSharedPreferences("preferences",Context.MODE_PRIVATE).edit().remove(key).apply();
      recreate();
    });page.addView(reset);
  }
}
