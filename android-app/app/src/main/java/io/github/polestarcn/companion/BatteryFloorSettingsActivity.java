package io.github.polestarcn.companion;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.widget.SwitchCompat;

/** Experimental P2 editor with live local mock preview and independent four-corner controls. */
public final class BatteryFloorSettingsActivity extends Activity {
  private LinearLayout root;private VehicleHeroView preview;
  private boolean chargingPreview=true;private int ink,muted;
  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private String tr(String a,String b){return AppPreferences.tr(this,a,b);}
  protected void attachBaseContext(Context c){super.attachBaseContext(UiTypography.wrap(c));}
  @Override public void onCreate(Bundle b){super.onCreate(b);
    if(b!=null)chargingPreview=b.getBoolean("floor_preview_charging",true);
    ink=PsaDesign.ink(AppPreferences.dark(this));muted=PsaDesign.muted(AppPreferences.dark(this));build();}
  private TextView text(String label,int sp){
    TextView t=new TextView(this);t.setText(label);t.setTextSize(sp);t.setTextColor(ink);
    t.setPadding(dp(8),dp(7),dp(8),dp(7));return t;
  }
  private Button button(String label,Runnable action){
    Button b=new Button(this);b.setText(label);b.setTextColor(ink);
    b.setBackgroundTintList(ColorStateList.valueOf(PsaDesign.surface(AppPreferences.dark(this))));
    b.setOnClickListener(v->{Haptics.tap(this);action.run();});return b;
  }
  private void save(String key,String value){
    AppPreferences.set(this,key,value);
    // Force immediate preview refresh, independent of asynchronous preference notifications.
    if(preview!=null)preview.refreshFloorSettings();
  }
  private void section(String label){TextView t=text(label,16);t.setTextColor(PsaDesign.ORANGE);
    t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);root.addView(t);}
  private void toggle(String key,String title,boolean defaultValue){
    SwitchCompat sw=new SwitchCompat(this);sw.setTag(key);sw.setText(title);sw.setTextColor(ink);
    sw.setMinHeight(dp(46));sw.setChecked(AppPreferences.visible(this,key,defaultValue));
    sw.setOnCheckedChangeListener((v,on)->save(key,on?"on":"off"));root.addView(sw);
  }
  private void slider(String key,String title,int initial,int min,int max){
    int value=EffectSettings.number(this,key,initial,min,max);
    TextView caption=text(title+" · "+value,13);root.addView(caption);
    SeekBar seek=new SeekBar(this);seek.setTag(key);seek.setMax(max-min);seek.setProgress(value-min);
    seek.setProgressTintList(ColorStateList.valueOf(PsaDesign.ORANGE));
    seek.setThumbTintList(ColorStateList.valueOf(PsaDesign.ORANGE));
    seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar bar,int v,boolean fromUser){
        int number=v+min;caption.setText(title+" · "+number);
        if(fromUser)save(key,String.valueOf(number));
      }
      public void onStartTrackingTouch(SeekBar bar){}
      public void onStopTrackingTouch(SeekBar bar){}
    });root.addView(seek);
  }
  private void options(String label,String key,String[] names,String[] values){
    root.addView(text(label,13));LinearLayout line=new LinearLayout(this);
    line.setOrientation(LinearLayout.HORIZONTAL);
    for(int i=0;i<names.length;i++){final int index=i;
      Button b=button(names[i],()->save(key,values[index]));
      b.setTag(key+"_"+i);line.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));
    }root.addView(line);
  }
  private void customColor(){
    EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(ink);
    input.setText(AppPreferences.get(this,BatteryFloor.PREFIX+"custom_color",BatteryFloor.VI));
    AlertDialog dialog=new HapticDialogBuilder(this)
        .setTitle(tr("自定义地板颜色 #RRGGBB","Custom floor color #RRGGBB"))
        .setView(input).setPositiveButton(tr("保存","Save"),null)
        .setNegativeButton(tr("取消","Cancel"),null).create();
    dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      String value=input.getText().toString().trim();
      if(!value.matches("#[0-9a-fA-F]{6}")){input.setError("#RRGGBB");return;}
      save(BatteryFloor.PREFIX+"custom_color",value);
      save(BatteryFloor.PREFIX+"color_preset","custom");dialog.dismiss();
    });
  }
  private void build(){
    // Fixed upper preview + independently scrolling controls: no nested whole-page scroll.
    LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);
    screen.setTag("battery_floor_screen");screen.setBackgroundColor(PsaDesign.canvas(AppPreferences.dark(this)));
    LinearLayout pinned=new LinearLayout(this);pinned.setOrientation(LinearLayout.VERTICAL);
    pinned.setTag("battery_floor_pinned_preview");
    pinned.setPadding(dp(12),dp(4),dp(12),dp(6));
    pinned.setBackgroundColor(PsaDesign.surface(AppPreferences.dark(this)));
    pinned.addView(button(tr("返回设置","Back to settings"),this::finish),
        new LinearLayout.LayoutParams(-1,dp(38)));
    TextView title=text(tr("试验功能 · 车辆底部电量地板条 P2","Experimental · under-car battery floor P2"),17);
    pinned.addView(title);
    preview=new VehicleHeroView(this,true);preview.setTag("battery_floor_preview");
    preview.setFloorPreview(true,chargingPreview);
    int screenDp=getResources().getConfiguration().screenHeightDp;
    int previewDp=Math.max(132,Math.min(190,Math.round(screenDp*.25f)));
    pinned.addView(preview,new LinearLayout.LayoutParams(-1,dp(previewDp)));
    pinned.addView(button(tr("预览：切换充电 / 未充电","Preview: charging / idle"),()->{
      chargingPreview=!chargingPreview;preview.setFloorPreview(true,chargingPreview);
    }),new LinearLayout.LayoutParams(-1,dp(39)));
    TextView hint=text(tr("固定预览 · 模拟 81% 电量；实际车辆仅启用后显示。","Pinned live preview · mock 81% SOC; enable to show on the vehicle."),11);
    hint.setTextColor(muted);pinned.addView(hint);
    screen.addView(pinned,new LinearLayout.LayoutParams(-1,-2));

    ScrollView scroll=new ScrollView(this);scroll.setTag("battery_floor_settings_scroll");
    scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
    scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setTag("battery_floor_settings_body");
    root.setPadding(dp(12),dp(8),dp(12),dp(22));
    root.setBackgroundColor(PsaDesign.canvas(AppPreferences.dark(this)));
    scroll.addView(root);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
    setContentView(screen);AppWindow.apply(this,null);

    section(tr("显示模式","Display"));
    toggle(BatteryFloor.PREFIX+"enabled",tr("启用地板条（默认关闭）","Enable battery floor (default off)"),false);
    toggle(BatteryFloor.PREFIX+"idle",tr("未充电时也显示静态电量条","Display static floor when not charging"),false);
    toggle(BatteryFloor.PREFIX+"status",tr("充电状态紧贴地板条下方","Charging text directly below floor"),true);
    section(tr("颜色与效果","Color and style"));
    options(tr("快捷颜色","Presets"),BatteryFloor.PREFIX+"color_preset",
        new String[]{tr("VI 橙色","VI orange"),tr("绿色","Green")},new String[]{"vi","green"});
    root.addView(button(tr("自定义 HEX 颜色","Custom HEX color"),this::customColor));
    slider(BatteryFloor.PREFIX+"opacity",tr("电量填充透明度 %","Fill opacity %"),80,0,100);
    toggle(BatteryFloor.PREFIX+"grid",tr("分割线","Floor grid"),true);
    options(tr("分割线密度","Grid density"),BatteryFloor.PREFIX+"density",
        new String[]{tr("低","Low"),tr("中","Medium"),tr("高","High")},new String[]{"0","1","2"});
    slider(BatteryFloor.PREFIX+"grid_opacity",tr("分割线透明度 %","Grid line opacity %"),24,0,100);
    options(tr("边缘发光","Edge glow"),BatteryFloor.PREFIX+"glow",
        new String[]{tr("关闭","Off"),tr("弱","Soft"),tr("中","Medium")},new String[]{"0","1","2"});
    section(tr("尺寸与四角位置","Size and four-corner placement"));
    slider(BatteryFloor.PREFIX+"length",tr("长度 %","Length %"),100,80,120);
    slider(BatteryFloor.PREFIX+"height",tr("高度 %","Height %"),100,70,130);

    Button expand=button(tr("展开四角独立调整 ▾","Edit four corners ▾"),()->{});root.addView(expand);
    LinearLayout advanced=new LinearLayout(this);advanced.setOrientation(LinearLayout.VERTICAL);
    advanced.setVisibility(View.GONE);root.addView(advanced);
    expand.setOnClickListener(v->{Haptics.tap(this);boolean show=advanced.getVisibility()!=View.VISIBLE;
      advanced.setVisibility(show?View.VISIBLE:View.GONE);
      expand.setText(show?tr("收起四角调整 ▴","Collapse corners ▴"):tr("展开四角独立调整 ▾","Edit four corners ▾"));
    });
    LinearLayout regular=root;root=advanced;
    String[] names={tr("左后","Back left"),tr("右后","Back right"),tr("左前","Front left"),tr("右前","Front right")};
    for(int i=0;i<4;i++){section(names[i]);slider(BatteryFloor.corner(i,"x"),tr("水平 X","Horizontal X"),0,-25,25);
      slider(BatteryFloor.corner(i,"y"),tr("垂直 Y","Vertical Y"),0,-25,25);}
    root=regular;
    TextView tip=text(tr("独立调整四个角的 X/Y 坐标；不会更改车图比例。","Independent X/Y adjustment; vehicle image proportions stay intact."),11);
    tip.setTextColor(muted);root.addView(tip);
    root.addView(button(tr("恢复地板条默认设置","Reset floor defaults"),()->new HapticDialogBuilder(this)
        .setTitle(tr("恢复默认？","Reset defaults?"))
        .setMessage(tr("只重置地板条，不影响账号、车辆或其他设置。","Only resets this effect; account and vehicle settings stay unchanged."))
        .setPositiveButton(tr("恢复","Reset"),(d,w)->{BatteryFloor.reset(this);build();})
        .setNegativeButton(tr("取消","Cancel"),null).show()));
  }
  @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);
    out.putBoolean("floor_preview_charging",chargingPreview);}
}
