package io.github.polestarcn.companion;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.content.Context;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Local native simulation. Has no coordinator, session, location or command calls. */
public final class VehicleEffectsActivity extends Activity {
  private LinearLayout controls,settingsRoot;
  private VehicleHeroView preview;
  private TextView caption;private Spinner airflowDirection;
  private Button cycle;
  private AllGoodTextView goodPreview;private ChargingPulseView pulse;private FrameLayout pulseCard,stage;
  private int ink,muted,scene;
  private boolean cycling;
  private int step;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private static final EffectStateController.Mode[] MODES={EffectStateController.Mode.CHARGING,EffectStateController.Mode.NEUTRAL,EffectStateController.Mode.COOLING,EffectStateController.Mode.HEATING,EffectStateController.Mode.VENTILATION,EffectStateController.Mode.PURIFICATION,EffectStateController.Mode.OFF,EffectStateController.Mode.OFF,EffectStateController.Mode.OFF,EffectStateController.Mode.OFF};
  private final Runnable next=new Runnable(){public void run(){if(!cycling)return;if(step<10){int style=step/2;boolean absorb=step%2==1;showVisual(0);preview.setPreviewCharge(style,absorb);caption.setText(getString(R.string.ui_text_parts_3, String.valueOf(tr("模拟测试 · ","Simulation · ")), String.valueOf(EffectSettings.STYLES[style]), String.valueOf((absorb?tr(" · 吸入"," · absorption"):tr(" · 上升"," · rising")))));}else if(step<20){int index=1+(step-10)%5;preview.setPreviewCharge(-1,false);preview.setPreviewAir(step>=15?1:0);preview.setPreviewMode(MODES[index]);showVisual(index);caption.setText(getString(R.string.ui_text_parts_3, String.valueOf(tr("模拟测试 · ","Simulation · ")), String.valueOf(scenes()[index]), String.valueOf((step>=15?tr(" · 吸入"," · absorption"):tr(" · 穿透"," · through")))));}else{int index=step-13;preview.setPreviewAir(-1);preview.setPreviewMode(MODES[index]);showVisual(index);caption.setText(getString(R.string.ui_text_parts_2, String.valueOf(tr("模拟测试 · ","Simulation · ")), String.valueOf(scenes()[index])));}step=(step+1)%23;handler.postDelayed(this,4000);}};
  protected void attachBaseContext(Context c){super.attachBaseContext(UiTypography.wrap(c));}
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private String tr(String a,String b){return AppPreferences.tr(this,a,b);}
  private String[] scenes(){return new String[]{tr("充电粒子","Charging particles"),tr("空调 · 中性气流","Climate · neutral"),tr("制冷 · A1","Cooling · A1"),tr("制热 · A1","Heating · A1"),tr("通风 · A2","Ventilation · A2"),tr("空气净化 · A2","Air purification · A2"),tr("停止动效","Effects off"),tr("All Good！慢速光效","All Good! sweep"),tr("充电文字框流光","Charging-card sweep"),tr("页面切换光效","Page transition")};}
  @Override public void onCreate(Bundle state){super.onCreate(state);if(state!=null)scene=Math.max(0,Math.min(9,state.getInt("scene")));ink=PsaDesign.ink(AppPreferences.dark(this));muted=PsaDesign.muted(AppPreferences.dark(this));build();}
  private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(ink);t.setTextSize(size);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
  private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(ink);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PsaDesign.surface(AppPreferences.dark(this))));b.setHapticFeedbackEnabled(false);b.setOnClickListener(v->{Haptics.tap(this);action.run();});return b;}
  private void build(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(PsaDesign.canvas(AppPreferences.dark(this)));root.setPadding(dp(12),dp(8),dp(12),dp(8));
    root.addView(button(tr("返回设置","Back to settings"),this::finish));root.addView(text(tr("车辆动态效果 · 测试","Vehicle effects · test"),21));
    TextView warning=text(tr("仅本机模拟，不发送车辆指令。省电、系统关闭动画或光效开关关闭时，预览静止。","Local simulation only; no vehicle commands. Battery saver, disabled system animations or effects switches pause the preview."),12);HelpDisclosure.add(root,this,tr("测试说明","Test notes"),warning.getText().toString());
    preview=new VehicleHeroView(this,true);preview.setTag("effect_test_preview");preview.setContentDescription(tr("模拟车辆动态效果","Simulated vehicle effects"));int h=getResources().getConfiguration().screenHeightDp<500?100:180;stage=new FrameLayout(this);stage.addView(preview,new FrameLayout.LayoutParams(-1,-1));
    goodPreview=new AllGoodTextView(this);goodPreview.setText(getString(R.string.all_good_label));goodPreview.setTag("effect_test_good");goodPreview.setTextSize(30);goodPreview.setTextColor(PsaDesign.accentText(AppPreferences.dark(this)));goodPreview.setGravity(Gravity.CENTER);stage.addView(goodPreview,new FrameLayout.LayoutParams(-1,-1));
    pulseCard=new FrameLayout(this);pulse=new ChargingPulseView(this);pulseCard.addView(pulse,new FrameLayout.LayoutParams(-1,-1));TextView sample=text(tr("模拟充电 · 68% · 12.8kW","Simulated charging · 68% · 12.8kW"),17);sample.setGravity(Gravity.CENTER);pulseCard.addView(sample,new FrameLayout.LayoutParams(-1,-1));FrameLayout.LayoutParams card=new FrameLayout.LayoutParams(-1,dp(72),Gravity.CENTER);card.leftMargin=dp(8);card.rightMargin=dp(8);stage.addView(pulseCard,card);root.addView(stage,new LinearLayout.LayoutParams(-1,dp(h)));
    caption=text(tr("模拟测试","Simulation"),13);caption.setTag("effect_test_caption");caption.setGravity(Gravity.CENTER);root.addView(caption);
    ScrollView scroll=new ScrollView(this);controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);scroll.addView(controls);if(getResources().getConfiguration().screenHeightDp<500){root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));ScrollView whole=new ScrollView(this);whole.setFillViewport(true);whole.addView(root);setContentView(whole);}else{root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    cycle=button(tr("依次测试全部 · 每项4s","Test all · 4s per effect"),()->{if(cycling){stopCycle();showScene();}else{cycling=true;step=0;cycle.setText(tr("停止自动测试","Stop automatic test"));next.run();}});cycle.setTag("effect_test_all");controls.addView(cycle);
    choice(tr("测试场景","Test scenario"),"effect_test_scene",scenes(),scene,index->{stopCycle();scene=index;showScene();});
    settingsRoot=controls;section(tr("总开关与独立效果","Master and effect switches"),true);
    toggle("light_effects",tr("界面光效总开关","UI effects master"),true);
    toggle("fx_air",tr("空调（制冷 / 通风）","Climate (cooling / ventilation)"),true);toggle("fx_heating",tr("制热动态效果","Heating effects"),true);toggle("fx_purification",tr("空气净化动态效果","Air purification effects"),true);
    section(tr("充电粒子","Charging particles"),false);toggle("fx_charge",tr("充电粒子","Charging particles"),true);
    choice(tr("粒子风格","Particle style"),"fx_style",new String[]{tr("C1a · 自然星尘","C1a · natural stardust"),tr("C1d · 羽化柔光","C1d · feather glow"),tr("C1+a · 自然聚拢","C1+a · natural convergence"),tr("C1+d · 柔光聚拢","C1+d · soft convergence"),tr("C1+c · 弧线引导","C1+c · guided arcs")},new EffectSettings(this).style,index->{stopCycle();AppPreferences.set(this,"fx_style",EffectSettings.STYLES[index]);showScene();});
    choice(tr("运动方向","Motion direction"),"fx_direction",new String[]{tr("上升","Rising"),tr("吸入充电口","Absorb into charge port")},new EffectSettings(this).absorb?1:0,index->{stopCycle();AppPreferences.set(this,"fx_direction",index==1?"absorb":"up");showScene();});
    slider("fx_charge_speed",tr("粒子速度（%）","Particle speed (%)"),100,50,200);slider("fx_particles",tr("粒子数量","Particle count"),60,20,150);
    section(tr("空调与净化 · 形状、吸入与配色","Climate and purification · shape, absorption, color"),true);
    choice(tr("粒子形状","Particle shape"),"fx_air_shape",new String[]{tr("轨迹气流（默认）","Trajectory streaks (default)"),tr("球型粒子 · 吸入","Spheres · absorption")},new EffectSettings(this).airSpheres?1:0,index->{AppPreferences.set(this,"fx_air_shape",index==1?"spheres":"streaks");if(index==1){AppPreferences.set(this,"fx_air_direction","absorb");if(airflowDirection!=null)airflowDirection.setSelection(1);}});
    airflowDirection=choice(tr("气流方向","Airflow direction"),"fx_air_direction",new String[]{tr("穿过整车","Through vehicle"),tr("吸入车内","Absorb into cabin")},new EffectSettings(this).airAbsorb?1:0,index->{AppPreferences.set(this,"fx_air_direction",index==1?"absorb":"through");});
    String[] tintKeys={"neutral","heating","cooling"};choice(tr("空调运行时的视觉配色（不是车端模式）","Running climate visual tint (not vehicle HVAC mode)"),"fx_air_tint",new String[]{tr("中性（默认）","Neutral (default)"),tr("制热 · VI橙","Heating · VI orange"),tr("制冷 · 蓝色","Cooling · blue")},Math.max(0,java.util.Arrays.asList(tintKeys).indexOf(new EffectSettings(this).airTint)),index->AppPreferences.set(this,"fx_air_tint",tintKeys[index]));
    section(tr("吸入范围与四方向密度","Absorption range and directional density"),false);
    slider("fx_absorb_range",tr("吸入范围（%）","Absorption range (%)"),100,50,250);
    slider("fx_density_top",tr("上方密度（%）","Top density (%)"),100,0,200);
    slider("fx_density_bottom",tr("下方密度（%）","Bottom density (%)"),100,0,200);
    slider("fx_density_left",tr("左侧密度（%）","Left density (%)"),100,0,200);
    slider("fx_density_right",tr("右侧密度（%）","Right density (%)"),100,0,200);
    HelpDisclosure.add(controls,this,tr("密度说明","Density notes"),tr("0关闭该方向；100为基准。前后两层共享数量预算；顶视/旋转车图按车辆图局部方向计算。","0 disables a side; 100 is baseline. Both depth layers share a particle budget. Directions follow local vehicle-image axes, including rotated views."));
    section(tr("车窗光效","Window glow"),false);
    toggle("fx_window_glow",tr("车窗内渐变循环光效","Pulsing gradient inside windows"),true);
    slider("fx_window_strength",tr("车窗光效强度（%）","Window glow intensity (%)"),60,0,100);
    slider("fx_window_speed",tr("车窗光效速度（%）","Window glow speed (%)"),100,25,300);
    section(tr("轨迹、拖影与尺寸","Paths, trails and size"),false);
    toggle("fx_turbulence",tr("不规则弧线扰动（关闭为直线）","Irregular arcs (off: straight paths)"),true);


    slider("fx_turbulence_percent",tr("弧线粒子比例（%）","Curved particles (%)"),20,0,100);
    slider("fx_arc_percent",tr("弧线范围（%）","Arc range (%)"),15,0,100);
    slider("fx_particle_size",tr("颗粒大小（%）","Particle size (%)"),100,25,300);
    HelpDisclosure.add(controls,this,tr("轨迹与配色说明","Paths and colors"),tr("扰动作用于气流和充电吸入；弧线范围按车辆显示尺寸计算。颗粒大小也调整气流粗细。制冷蓝、制热VI橙、净化绿；未知空调模式仍为中性配色。","Perturbation affects airflow and charging absorption. Arc range is relative to the displayed vehicle; size also changes airflow thickness. Cooling: blue; heating: VI orange; purification: green. Unreported climate mode stays neutral."));
    slider("fx_air_speed",tr("气流速度（%）","Airflow speed (%)"),100,50,200);slider("fx_lines",tr("气流密度","Airflow density"),36,12,120);slider("fx_strength",tr("气流强度（%）","Airflow strength (%)"),60,20,100);toggle("fx_fade",tr("车尾气流渐隐","Fade airflow beyond rear"),true);
    TextView note=text(tr("实时空调无可靠制冷/制热模式字段时使用中性气流，不按设定温度猜测。充电优先于气流；低内存机自动限制粒子数量。自动测试不会修改已保存风格。","Live climate uses neutral airflow without a verified heat/cool mode. Charging takes priority; low-memory devices cap particles. Automatic testing does not overwrite your saved style."),12);HelpDisclosure.add(controls,this,tr("动效说明","Effect notes"),note.getText().toString());
    section(tr("界面流光与切换","UI sweeps and transitions"),false);
    toggle("page_transition",tr("页面切换光效","Page transition"),true);toggle("all_good_shimmer",tr("All Good！慢速光效","All Good! sweep"),true);toggle("charging_animation",tr("充电文字框流光","Charging-card sweep"),true);
    controls=settingsRoot;controls.addView(button(tr("恢复动效默认参数","Reset effect parameters"),()->{stopCycle();EffectSettings.defaults(this);build();}));showScene();
  }
  private void section(String title,boolean open){LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setVisibility(open?View.VISIBLE:View.GONE);Button heading=button(getString(R.string.ui_text_parts_2,open?"⌄  ":"›  ",title),()->{});heading.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);heading.setOnClickListener(v->{Haptics.tap(this);boolean show=group.getVisibility()!=View.VISIBLE;group.setVisibility(show?View.VISIBLE:View.GONE);heading.setText(getString(R.string.ui_text_parts_2,show?"⌄  ":"›  ",title));});settingsRoot.addView(heading);settingsRoot.addView(group);controls=group;}
  private void showVisual(int index){preview.setVisibility(index<7||index==9?View.VISIBLE:View.INVISIBLE);goodPreview.setVisibility(index==7?View.VISIBLE:View.GONE);pulseCard.setVisibility(index==8?View.VISIBLE:View.GONE);pulse.setActive(index==8);if(index==9)PageTransition.play(stage);else PageTransition.stop(stage);}
  private void showScene(){preview.setPreviewAir(-1);preview.setPreviewCharge(-1,false);preview.setPreviewMode(MODES[scene]);showVisual(scene);caption.setText(getString(R.string.ui_text_parts_2, String.valueOf(tr("模拟测试 · ","Simulation · ")), String.valueOf(scenes()[scene])));}
  private void stopCycle(){cycling=false;handler.removeCallbacks(next);if(cycle!=null)cycle.setText(tr("依次测试全部 · 每项4s","Test all · 4s per effect"));}
  private void toggle(String key,String label,boolean fallback){SwitchCompat s=new SwitchCompat(this);s.setTag(key);s.setText(label);s.setTextColor(ink);s.setMinHeight(dp(48));s.setChecked(AppPreferences.visible(this,key,fallback));s.setHapticFeedbackEnabled(false);s.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{PsaDesign.ORANGE,muted}));s.setTrackTintList(android.content.res.ColorStateList.valueOf(PsaDesign.line(AppPreferences.dark(this))));s.setOnCheckedChangeListener((v,on)->{Haptics.tap(this);AppPreferences.set(this,key,on?"on":"off");AllGoodTextView.host(findViewById(android.R.id.content),true);});controls.addView(s);}
  private Spinner choice(String title,String tag,String[] items,int initial,java.util.function.IntConsumer action){controls.addView(text(title,14));Spinner spinner=new Spinner(this);spinner.setTag(tag);android.graphics.drawable.GradientDrawable border=new android.graphics.drawable.GradientDrawable();border.setColor(PsaDesign.surface(AppPreferences.dark(this)));border.setCornerRadius(dp(12));border.setStroke(dp(1),PsaDesign.line(AppPreferences.dark(this)));spinner.setBackground(border);spinner.setPadding(dp(12),dp(8),dp(12),dp(8));spinner.setMinimumHeight(dp(48));spinner.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(PsaDesign.surface(AppPreferences.dark(this))));spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,items){@Override public View getView(int p,View view,ViewGroup parent){View v=super.getView(p,view,parent);((TextView)v).setTextColor(ink);return v;}});spinner.setSelection(initial);spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){int last=initial;public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){if(n!=last){last=n;Haptics.tap(VehicleEffectsActivity.this);action.accept(n);}}});controls.addView(spinner);return spinner;}
  private void slider(String key,String title,int fallback,int low,int high){int value=EffectSettings.number(this,key,fallback,low,high);TextView label=text(title+" · "+value,14);controls.addView(label);SeekBar seek=new SeekBar(this);seek.setTag(key);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(PsaDesign.ORANGE));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(PsaDesign.ORANGE));seek.setMax(high-low);seek.setProgress(value-low);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){label.setText(getString(R.string.ui_text_parts_3, String.valueOf(title), String.valueOf(" · "), String.valueOf((n+low))));if(user)AppPreferences.set(VehicleEffectsActivity.this,key,""+(n+low));}public void onStartTrackingTouch(SeekBar s){Haptics.tap(VehicleEffectsActivity.this);}public void onStopTrackingTouch(SeekBar s){}});controls.addView(seek);}
  @Override protected void onResume(){super.onResume();AllGoodTextView.host(findViewById(android.R.id.content),true);}
  @Override protected void onPause(){stopCycle();AllGoodTextView.host(findViewById(android.R.id.content),false);super.onPause();}
  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
  @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("scene",scene);}
}
