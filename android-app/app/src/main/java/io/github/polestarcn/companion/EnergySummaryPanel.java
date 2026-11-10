package io.github.polestarcn.companion;

import android.content.Context;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

/** Reusable energy summary from real vehicle snapshots and private local history only. */
public final class EnergySummaryPanel extends LinearLayout {
  private final Context context;
  private final String vehicle;
  private final int ink, muted;
  private final TextView historyTitle, historyValues, distanceSummary, note, chartToggle, trendToggle;
  private final HorizontalScrollView chartScroller;
  private final TextView[] groupTabs=new TextView[3], styleTabs=new TextView[3];
  private TextView valueLabelsToggle;
  private int groupStep, chartStyle, compareDays, valueLabelsMode;
  private static final String LABELS_KEY="energy_chart_value_labels";
  private int trendRequest;
  private static final String GROUP_KEY="energy_chart_step",STYLE_KEY="energy_chart_style",TREND_KEY="energy_chart_compare_days";
  private TextView rateToggle;
  private final TextView[] metricTabs=new TextView[4], rangeTabs=new TextView[4];
  private boolean showPer100Km=true;
  private final Trend chart;
  private int range = 1, metric = 0; // default: 7 days
  private EnergyLog.Report currentHistory, history7;
  private long displayedSpanMillis=7L*86400000L;
  private long customStart, customEnd;
  private static final String CUSTOM_END_KEY="energy_custom_end";
  private static final String CUSTOM_DATE_KEY="energy_custom_start";
  private Double currentTmEnergy,tmAverage;
  private boolean chartExpanded = true;
  private int generation;

  public EnergySummaryPanel(Context context, VehicleSnapshot snapshot, String vehicleId) {
    super(context);
    this.context = context;
    vehicle = EnergyLog.validVehicle(vehicleId) ? vehicleId : null;
    customStart=readCustomStart();
    customEnd=readCustomEnd();
    groupStep=EnergyChartSeries.granularity(AppPreferences.get(context,GROUP_KEY,"day"));
    chartStyle=EnergyChartSeries.style(AppPreferences.get(context,STYLE_KEY,"bars"));
    compareDays=readCompareDays();
    valueLabelsMode=EnergyChartDisplay.mode(AppPreferences.get(context,LABELS_KEY,"latest"));
    boolean dark = AppPreferences.dark(context);
    ink = PsaDesign.ink(dark);
    muted = PsaDesign.muted(dark);
    setOrientation(VERTICAL);
    setTag("energy_summary_panel");
    setPadding(dp(4), dp(2), dp(4), dp(4));
    addView(title(tr("TM 行程与费用估算", "TM trip & cost estimates"), 16));
    currentTrip(snapshot);
    LinearLayout heading=rowLayout();heading.setTag("energy_chart_heading");
    heading.addView(title(tr("行驶与能耗费用统计", "Driving & Energy Cost Statistics"),15),new LayoutParams(0,-2,1));
    trendToggle=modeTab("",this::cycleComparison);trendToggle.setTag("energy_compare_toggle");
    trendToggle.setTextColor(PsaDesign.ORANGE);trendToggle.setTextSize(11);
    heading.addView(trendToggle,new LayoutParams(-2,dp(33)));
    addView(heading);
    chart = new Trend(context);
    chartScroller=new HorizontalScrollView(context);chartScroller.setTag("energy_history_chart");
    chartScroller.setHorizontalScrollBarEnabled(false);chartScroller.setFillViewport(true);
    chartScroller.addView(chart,new android.widget.FrameLayout.LayoutParams(-2,dp(190)));
    addView(chartScroller, new LayoutParams(-1, dp(190)));
    LinearLayout tabs = new LinearLayout(context);
    tabs.setOrientation(HORIZONTAL);
    String[] zh = {"行驶能耗", "驻车空调", "行驶里程", "能耗费"};
    String[] en = {"Drive kWh", "Parked AC", "Distance", "Energy ¥"};
    for (int i = 0; i < 4; i++) {
      final int selected = i;
      metricTabs[i]=modeTab(tr(zh[i],en[i]),()->changeMetric(selected));
      metricTabs[i].setTextSize(12);
      metricTabs[i].setSingleLine(true);
      LayoutParams cell=new LayoutParams(0,dp(38),1);cell.setMargins(dp(2),0,dp(2),0);
      tabs.addView(metricTabs[i],cell);
    }
    addView(tabs);
    chartToggle = action(tr("收起图表  ⌃", "Collapse chart  ⌃"), this::toggleChart);
    chartToggle.setGravity(Gravity.CENTER_VERTICAL);
    addView(chartToggle);
    LinearLayout byTime=rowLayout();byTime.setTag("energy_bucket_controls");
    String[] bucketZh={"按天","按周","按月"},bucketEn={"Day","Week","Month"};
    for(int i=0;i<3;i++){final int index=i;
      groupTabs[i]=modeTab(tr(bucketZh[i],bucketEn[i]),()->setGroupStep(index));
      groupTabs[i].setTag("energy_bucket_"+i);
      LayoutParams item=new LayoutParams(0,dp(34),1);item.setMargins(dp(2),0,dp(2),0);byTime.addView(groupTabs[i],item);
    }
    addView(byTime);
    LinearLayout byStyle=rowLayout();byStyle.setTag("energy_style_controls");
    String[] styleZh={"柱状","波浪","叠加"},styleEn={"Bars","Curve","Both"};
    for(int i=0;i<3;i++){final int index=i;
      styleTabs[i]=modeTab(tr(styleZh[i],styleEn[i]),()->setChartStyle(index));
      styleTabs[i].setTag("energy_style_"+i);
      LayoutParams item=new LayoutParams(0,dp(34),1);item.setMargins(dp(2),0,dp(2),0);byStyle.addView(styleTabs[i],item);
    }
    valueLabelsToggle=modeTab("",this::cycleValueLabels);
    valueLabelsToggle.setTextSize(11);
    valueLabelsToggle.setTag("energy_value_labels_toggle");
    LayoutParams labelCell=new LayoutParams(0,dp(34),1.18f);
    labelCell.setMargins(dp(2),0,dp(2),0);
    byStyle.addView(valueLabelsToggle,labelCell);
    addView(byStyle);
    tripDetails(snapshot);
    row(tr("本机历史采样", "Local recorded history"), "");
    LinearLayout periods = new LinearLayout(context);
    periods.setOrientation(HORIZONTAL);
    rangeTabs[0]=modeTab(tr("3天", "3d"), () -> changeRange(0));
    rangeTabs[1]=modeTab(tr("7天", "7d"), () -> changeRange(1));
    rangeTabs[2]=modeTab(tr("31天", "31d"), () -> changeRange(2));
    rangeTabs[3]=modeTab(tr("自定义", "Custom"), this::customRangeDialog);
    for(int i=0;i<rangeTabs.length;i++){LayoutParams cell=new LayoutParams(0,dp(38),1);cell.setMargins(dp(2),0,dp(2),0);periods.addView(rangeTabs[i],cell);}
    periods.setTag("energy_range_periods");
    addView(periods);
    historyTitle = title(tr("读取本机采样…", "Loading local observations…"), 13);
    addView(historyTitle);
    historyValues = title("", 14);
    addView(historyValues);
    distanceSummary=title("",12);distanceSummary.setTag("energy_mileage_statistics");
    distanceSummary.setTextColor(muted);distanceSummary.setVisibility(GONE);addView(distanceSummary);
    note = title(tr("历史为本机有效采样的估算值，并非充电账单。", "History estimates use local samples, not charging invoices."), 12);
    note.setTextColor(muted);addView(note);
    addView(action(tr("设置电价 / 电池容量 / 保存期限", "Tariff / capacity / retention"), () -> {
      android.content.Intent intent = new android.content.Intent(context, EnergyActivity.class);
      if(vehicle != null)intent.putExtra("vehicle", vehicle);
      intent.putExtra("open_energy_settings",true);
      context.startActivity(intent);
    }), new LayoutParams(-1, dp(44)));
    refreshTabStyles();
    refreshTrendLabel(null);
    load();
  }

  private LinearLayout rowLayout(){LinearLayout line=new LinearLayout(context);line.setOrientation(HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);return line;}
  private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
  private String tr(String zh, String en) { return AppPreferences.tr(context, zh, en); }
  private TextView title(String value, int sp) {
    TextView t = new TextView(context);
    t.setText(value);t.setTextSize(sp);t.setTextColor(ink);
    t.setPadding(dp(4),dp(2),dp(4),dp(2));
    return t;
  }
  private TextView action(String name, Runnable click) {
    TextView b = title(name, 12);
    b.setGravity(Gravity.CENTER);
    b.setBackgroundColor(PsaDesign.surface(AppPreferences.dark(context)));
    b.setOnClickListener(v -> {Haptics.tap(context);click.run();});
    return b;
  }
  private TextView modeTab(String name,Runnable callback){
    TextView tab=action(name,callback);
    tab.setTag("energy_switch_"+name);
    tab.setTextSize(12);
    tab.setSingleLine(true);tab.setEllipsize(TextUtils.TruncateAt.END);
    tab.setIncludeFontPadding(false);
    tab.setPadding(dp(4),0,dp(4),0);
    tab.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
    return tab;
  }
  private void highlight(TextView tab,boolean selected){
    if(tab==null)return;
    GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(dp(7));
    shape.setColor(selected?PsaDesign.ORANGE:PsaDesign.surface(AppPreferences.dark(context)));
    shape.setStroke(dp(1),selected?PsaDesign.ORANGE:PsaDesign.line(AppPreferences.dark(context)));
    // Inset each colored surface INSIDE its cell. Selected backgrounds never touch
    // neighboring buttons, including at larger Android font and display scales.
    tab.setBackground(new InsetDrawable(shape,dp(3),dp(2),dp(3),dp(2)));
    tab.setTextColor(selected?android.graphics.Color.BLACK:ink);
    tab.setSelected(selected);
  }
  private void refreshTabStyles(){
    for(int i=0;i<metricTabs.length;i++)highlight(metricTabs[i],metric==i);
    for(int i=0;i<rangeTabs.length;i++)highlight(rangeTabs[i],range==i);
    for(int i=0;i<groupTabs.length;i++)highlight(groupTabs[i],groupStep==i);
    for(int i=0;i<styleTabs.length;i++)highlight(styleTabs[i],chartStyle==i);
    if(valueLabelsToggle!=null){
      valueLabelsToggle.setText(valueLabelsMode==EnergyChartDisplay.ALL?tr("数值·全部","Values: all")
          :valueLabelsMode==EnergyChartDisplay.HIDDEN?tr("数值·隐藏","Values: off")
          :tr("数值·最新","Values: latest"));
      valueLabelsToggle.setContentDescription(tr("数值标签显示方式，点击切换：","Value labels, tap to switch: ")
          +valueLabelsToggle.getText());
      highlight(valueLabelsToggle,valueLabelsMode!=EnergyChartDisplay.HIDDEN);
    }
  }
  private void row(String label, String value) {
    LinearLayout r = new LinearLayout(context);r.setOrientation(HORIZONTAL);
    TextView a=title(label,13);a.setTextColor(muted);
    TextView b=title(value,14);b.setGravity(Gravity.END);
    r.addView(a,new LayoutParams(0,-2,1));r.addView(b,new LayoutParams(0,-2,1));
    addView(r);
  }
  private String fmt(Double number, String unit) {
    return number == null ? "--" : String.format(Locale.getDefault(),"%.1f %s",number,unit);
  }
  private void kpi(String key, String value, String tag) {
    LinearLayout item=new LinearLayout(context);item.setOrientation(HORIZONTAL);
    item.setTag("energy_kpi_"+tag);item.setMinimumHeight(dp(32));
    TextView name=title(key,14);TextView number=title(value,16);
    number.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
    number.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
    item.addView(name,new LayoutParams(0,-2,1));
    item.addView(number,new LayoutParams(0,-2,1));
    addView(item);
  }
  private void currentTrip(VehicleSnapshot s) {
    MileageSnapshot m=s==null||s.isDemo?null:s.mileage;
    Double distance=m==null?null:m.manualKm;
    Double average=m==null?null:m.manualConsumption;
    tmAverage=average;
    currentTmEnergy=TmCost.kwh(distance,average);
    double tariff=EnergyLog.price(context);
    kpi(tr("TM 里程","TM distance"),fmt(distance,"km"),"tm_distance");
    kpi(tr("TM 总能耗（估算）","TM energy (estimate)"),fmt(currentTmEnergy,"kWh"),"tm_total_energy");
    kpi(tr("估算费用","Estimated cost"),currentTmEnergy==null?"--"
        :tariff<=0?tr("未设电价","No tariff")
        :String.format(Locale.getDefault(),"%.2f %s",currentTmEnergy*tariff,tr("元","CNY")),"tm_cost");
    rateToggle=title("",13);
    rateToggle.setTag("energy_cost_rate_toggle");rateToggle.setGravity(Gravity.CENTER_VERTICAL);
    rateToggle.setPadding(dp(10),dp(4),dp(8),dp(4));rateToggle.setMinHeight(dp(34));
    GradientDrawable rateOutline=new GradientDrawable();rateOutline.setCornerRadius(dp(7));
    rateOutline.setStroke(dp(1),PsaDesign.ORANGE);rateOutline.setColor(PsaDesign.surface(AppPreferences.dark(context)));
    rateToggle.setBackground(rateOutline);rateToggle.setOnClickListener(v->{Haptics.tap(context);showPer100Km=!showPer100Km;updateRate();});
    addView(rateToggle,new LayoutParams(-1,-2));updateRate();
    Double taKm=m==null?null:m.automaticKm;
    Double taKwh=TmCost.kwh(taKm,tmAverage);
    String taCost=taKwh==null?"--":tariff<=0?tr("未设电价","No tariff")
        :String.format(Locale.getDefault(),"%.2f %s",taKwh*tariff,tr("元","CNY"));
    // Preserve the existing TM-based estimation rule; distinguish kWh from money.
    kpi(tr("TA能耗估算","TA energy estimate"),fmt(taKwh,"kWh"),"ta_energy_estimate");
    kpi(tr("TA费用估算","TA estimated cost"),taCost,"ta_cost");
    TextView taDetail=title(tr("TA 里程：","TA distance: ")+fmt(taKm,"km"),12);
    taDetail.setTag("energy_ta_estimation_details");taDetail.setTextColor(muted);addView(taDetail);
  }
  private void updateRate(){
    if(rateToggle==null)return;
    double tariff=EnergyLog.price(context);
    Double value=showPer100Km?TmCost.yuanPer100Km(tmAverage,tariff):TmCost.yuanPerKm(tmAverage,tariff);
    String unit=showPer100Km?tr("元/百公里","CNY/100 km"):tr("元/公里","CNY/km");
    String number=value==null?(tariff<=0?tr("请设置电价","Set tariff"):"--")
        :String.format(Locale.getDefault(),"%.2f %s",value,unit);
    rateToggle.setText(tr("行驶单价：","Driving rate: ")+number
        +tr("    点击切换 ↔","    Tap to switch ↔"));
    rateToggle.setContentDescription(rateToggle.getText());
  }
  private void tripDetails(VehicleSnapshot s) {
    MileageSnapshot m=s==null||s.isDemo?null:s.mileage;
    Double average=m==null?null:m.manualConsumption;
    TextView averageLabel=title(tr("TM 平均电耗（参考）","TM average consumption (reference)")+" · "+fmt(average,"kWh/100km"),12);
    averageLabel.setTextColor(muted);averageLabel.setTag("tm_average_consumption_secondary");addView(averageLabel);
    if(s!=null&&m!=null&&s.timestampMillis>0) {
      String when=android.text.format.DateFormat.format("MM-dd HH:mm",s.timestampMillis).toString();
      TextView stamp=title(tr("最近车辆数据：","Vehicle report: ")+when+tr(" · 非实时电表"," · not a live energy meter"),11);
      stamp.setTextColor(muted);addView(stamp);
    }
    if(currentTmEnergy==null) {
      TextView message=title(tr("里程或均耗未同时上报，不计算总能耗及费用。","TM estimate needs both trip distance and consumption."),12);
      message.setTextColor(muted);addView(message);
    } else if(EnergyLog.price(context)<=0) {
      TextView message=title(tr("设置电价后可显示估算费用。","Set a tariff to show estimated cost."),12);
      message.setTextColor(muted);addView(message);
    }
  }
  private void changeRange(int value) { range = value; refreshTabStyles(); load(); }
  static long startOfDate(int year,int month,int day) {
    Calendar cal=Calendar.getInstance();cal.setLenient(false);cal.clear();cal.set(year,month-1,day,0,0,0);
    if(year<2000||year>2100)throw new IllegalArgumentException("year");
    return cal.getTimeInMillis();
  }
  private long readCustomStart(){
    String value=AppPreferences.get(context,CUSTOM_DATE_KEY,"");
    try {String[] fields=value.split("-");if(fields.length==3)return startOfDate(Integer.parseInt(fields[0]),Integer.parseInt(fields[1]),Integer.parseInt(fields[2]));}
    catch(Exception ignored){}
    Calendar cal=Calendar.getInstance();cal.add(Calendar.MONTH,-1);
    return startOfDate(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DAY_OF_MONTH));
  }
  static long endOfDate(int year,int month,int day){
    Calendar end=Calendar.getInstance();end.setTimeInMillis(startOfDate(year,month,day));
    end.add(Calendar.DAY_OF_YEAR,1);return end.getTimeInMillis()-1;
  }
  private long readCustomEnd(){
    String saved=AppPreferences.get(context,CUSTOM_END_KEY,"");
    try{String[] parts=saved.split("-");if(parts.length==3)
      return endOfDate(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));
    }catch(Exception ignored){}
    Calendar today=Calendar.getInstance();return endOfDate(today.get(Calendar.YEAR),today.get(Calendar.MONTH)+1,today.get(Calendar.DAY_OF_MONTH));
  }
  private String formatDay(long time){return new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date(time));}
  private TextView monthShortcut(String title,int delta,EditText y,EditText m,EditText d){
    return action(title,()->{
      try {
        Calendar selected=Calendar.getInstance();selected.setTimeInMillis(startOfDate(Integer.parseInt(y.getText().toString()),Integer.parseInt(m.getText().toString()),Integer.parseInt(d.getText().toString())));
        selected.add(Calendar.MONTH,delta);
        fillDate(selected,y,m,d);
      } catch(Exception e){y.setError(tr("请输入有效日期","Enter a valid date"));}
    });
  }
  private void fillDate(Calendar cal,EditText year,EditText month,EditText day){
    year.setText(String.valueOf(cal.get(Calendar.YEAR)));month.setText(String.valueOf(cal.get(Calendar.MONTH)+1));day.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
  }
  private void customRangeDialog(){
    Calendar selected=Calendar.getInstance();selected.setTimeInMillis(customStart);
    LinearLayout form=new LinearLayout(context);form.setOrientation(VERTICAL);form.setPadding(dp(10),dp(4),dp(10),dp(4));
    form.addView(title(tr("开始日期 · 年／月／日","Start date · year / month / day"),13));
    LinearLayout fields=new LinearLayout(context);fields.setOrientation(HORIZONTAL);
    EditText y=new EditText(context),m=new EditText(context),d=new EditText(context);
    EditText[] inputs={y,m,d};String[] hints={tr("年","Year"),tr("月","Month"),tr("日","Day")};
    for(int i=0;i<inputs.length;i++){
      inputs[i].setSingleLine(true);inputs[i].setInputType(InputType.TYPE_CLASS_NUMBER);inputs[i].setHint(hints[i]);inputs[i].setTextColor(ink);inputs[i].setTextSize(16);
      fields.addView(inputs[i],new LayoutParams(0,dp(50),i==0?1.45f:1f));
    }
    fillDate(selected,y,m,d);form.addView(fields);
    TextView picker=action(tr("选择日期  ▾","Pick a date  ▾"),()->{
      Calendar current=Calendar.getInstance();try{current.setTimeInMillis(startOfDate(Integer.parseInt(y.getText().toString()),Integer.parseInt(m.getText().toString()),Integer.parseInt(d.getText().toString())));}catch(Exception ignored){}
      DatePickerDialog dialog=new DatePickerDialog(context,(view,year,month,day)->{
        Calendar chosen=Calendar.getInstance();chosen.set(year,month,day);fillDate(chosen,y,m,d);
      },current.get(Calendar.YEAR),current.get(Calendar.MONTH),current.get(Calendar.DAY_OF_MONTH));
      Calendar earliest=Calendar.getInstance();earliest.add(Calendar.MONTH,-EnergyLog.months(context));
      dialog.getDatePicker().setMinDate(earliest.getTimeInMillis());dialog.getDatePicker().setMaxDate(System.currentTimeMillis());dialog.show();
    });form.addView(picker,new LayoutParams(-1,dp(40)));
    String[][] months={{"−1月","+1月","−3月"},{"+3月","−6月","+6月"}};
    int[][] delta={{-1,1,-3},{3,-6,6}};
    for(int i=0;i<months.length;i++){
      LinearLayout row=new LinearLayout(context);row.setOrientation(HORIZONTAL);
      for(int j=0;j<months[i].length;j++){
        String text=tr(months[i][j],months[i][j].replace("月"," mo"));
        row.addView(monthShortcut(text,delta[i][j],y,m,d),new LayoutParams(0,dp(38),1));
      }
      form.addView(row);
    }
    form.addView(title(tr("结束日期 · 年／月／日（默认今天）","End date · year / month / day (defaults to today)"),13));
    LinearLayout untilFields=new LinearLayout(context);untilFields.setOrientation(HORIZONTAL);
    EditText y2=new EditText(context),m2=new EditText(context),d2=new EditText(context);
    EditText[] ends={y2,m2,d2};
    for(int i=0;i<ends.length;i++){
      ends[i].setSingleLine(true);ends[i].setInputType(InputType.TYPE_CLASS_NUMBER);
      ends[i].setHint(hints[i]);ends[i].setTextColor(ink);ends[i].setTextSize(16);
      untilFields.addView(ends[i],new LayoutParams(0,dp(48),i==0?1.45f:1f));
    }
    Calendar until=Calendar.getInstance();until.setTimeInMillis(Math.min(System.currentTimeMillis(),customEnd));
    fillDate(until,y2,m2,d2);form.addView(untilFields);
    TextView endPicker=action(tr("选择结束日期 ▾","Pick end date ▾"),()->{
      Calendar current=Calendar.getInstance();
      try{current.setTimeInMillis(startOfDate(Integer.parseInt(y2.getText().toString()),Integer.parseInt(m2.getText().toString()),Integer.parseInt(d2.getText().toString())));}catch(Exception ignored){}
      DatePickerDialog dateDialog=new DatePickerDialog(context,(view,year,month,day)->{
        Calendar chosen=Calendar.getInstance();chosen.set(year,month,day);fillDate(chosen,y2,m2,d2);
      },current.get(Calendar.YEAR),current.get(Calendar.MONTH),current.get(Calendar.DAY_OF_MONTH));
      Calendar earliest=Calendar.getInstance();earliest.add(Calendar.MONTH,-EnergyLog.months(context));
      dateDialog.getDatePicker().setMinDate(earliest.getTimeInMillis());
      dateDialog.getDatePicker().setMaxDate(System.currentTimeMillis());dateDialog.show();
    });form.addView(endPicker,new LayoutParams(-1,dp(38)));
    LinearLayout endShortcuts=rowLayout();
    endShortcuts.addView(monthShortcut(tr("结束−1月","End −1 mo"),-1,y2,m2,d2),new LayoutParams(0,dp(35),1));
    endShortcuts.addView(monthShortcut(tr("结束+1月","End +1 mo"),1,y2,m2,d2),new LayoutParams(0,dp(35),1));
    form.addView(endShortcuts);
    form.addView(title(tr("开始与结束日期均包含在查询内，仅统计本机有效采样。","Both dates are inclusive. Only real retained samples are used."),11));
    android.widget.ScrollView scroller=new android.widget.ScrollView(context);
    scroller.addView(form);
    AlertDialog pickerDialog=new HapticDialogBuilder(context).setTitle(tr("自定义日期区间","Custom date range"))
        .setView(scroller).setPositiveButton(tr("查看","Apply"),null).setNegativeButton(tr("取消","Cancel"),null).create();
    pickerDialog.show();pickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      try{
        long start=startOfDate(Integer.parseInt(y.getText().toString()),Integer.parseInt(m.getText().toString()),Integer.parseInt(d.getText().toString()));
        long end=endOfDate(Integer.parseInt(y2.getText().toString()),Integer.parseInt(m2.getText().toString()),Integer.parseInt(d2.getText().toString()));
        Calendar earliest=Calendar.getInstance();earliest.add(Calendar.MONTH,-EnergyLog.months(context));
        if(start>end||start>System.currentTimeMillis()||end>System.currentTimeMillis()+86400000L
            ||start<earliest.getTimeInMillis()-86400000L){
          d2.setError(tr("检查日期顺序与有效记录期限","Check date order and retention period"));return;
        }
        customStart=start;customEnd=end;
        AppPreferences.set(context,CUSTOM_DATE_KEY,formatDay(start));
        AppPreferences.set(context,CUSTOM_END_KEY,formatDay(end));
        pickerDialog.dismiss();changeRange(3);
      }catch(Exception e){d.setError(tr("请输入有效的年月日","Enter a valid calendar date"));}
    });
  }
  private void changeMetric(int value) { metric = value; refreshTabStyles(); chart.setMetric(metric);
    updateDistanceSummary();loadTrend(); }
  private void cycleValueLabels(){
    valueLabelsMode=EnergyChartDisplay.next(valueLabelsMode);
    AppPreferences.set(context,LABELS_KEY,EnergyChartDisplay.preference(valueLabelsMode));
    chart.setLabels(valueLabelsMode,-1);
    refreshTabStyles();
    chartScroller.post(()->chartScroller.fullScroll(View.FOCUS_RIGHT));
  }
  private void selectChartBar(int index){
    valueLabelsMode=EnergyChartDisplay.LATEST;
    AppPreferences.set(context,LABELS_KEY,EnergyChartDisplay.preference(valueLabelsMode));
    chart.setLabels(valueLabelsMode,index);
    refreshTabStyles();
  }
  private void hideChartLabels(){
    valueLabelsMode=EnergyChartDisplay.HIDDEN;
    AppPreferences.set(context,LABELS_KEY,EnergyChartDisplay.preference(valueLabelsMode));
    chart.setLabels(valueLabelsMode,-1);
    refreshTabStyles();
  }
  private void setGroupStep(int value) {
    groupStep=value;
    AppPreferences.set(context,GROUP_KEY,value==0?"day":value==1?"week":"month");
    refreshTabStyles();chart.setGrouping(value);
  }
  private void setChartStyle(int value) {
    chartStyle=value;
    AppPreferences.set(context,STYLE_KEY,value==0?"bars":value==1?"curve":"both");
    refreshTabStyles();chart.setStyle(value);
  }
  private int readCompareDays(){String days=AppPreferences.get(context,TREND_KEY,"7");
    if("31".equals(days))return 31;if("3".equals(days))return 3;
    if("off".equals(days))return 0;return 7;
  }
  private void cycleComparison(){
    compareDays=compareDays==7?31:compareDays==31?3:compareDays==3?0:7;
    AppPreferences.set(context,TREND_KEY,compareDays==0?"off":""+compareDays);
    refreshTrendLabel(null);loadTrend();
  }
  private void refreshTrendLabel(Double percent){
    if(compareDays==0){trendToggle.setText(tr("趋势：不显示 ↻","Trend: off ↻"));return;}
    String prefix=tr("对比前"+compareDays+"天", "vs prior "+compareDays+"d");
    String value=percent==null?tr("采样不足","not enough data")
        :String.format(Locale.getDefault(),"%+.1f%%",percent);
    trendToggle.setText(prefix+"  "+value+" ↻");
    trendToggle.setTextColor(PsaDesign.ORANGE);
    trendToggle.setContentDescription(tr("与前"+compareDays+"天相同长度区间比较，点击切换；","Compared to preceding "+compareDays+"-day window, tap to switch; ")+value);
  }
  private void loadTrend(){
    final int run=++trendRequest,days=compareDays,chosenMetric=metric;
    if(days==0||vehicle==null){refreshTrendLabel(null);return;}
    refreshTrendLabel(null);
    Context app=context.getApplicationContext();
    EnergyLog.WORK.execute(()->{
      Double change=null;
      try{
        Calendar time=Calendar.getInstance();long end=time.getTimeInMillis();
        time.add(Calendar.DAY_OF_YEAR,-days);long begin=time.getTimeInMillis();
        time.add(Calendar.DAY_OF_YEAR,-days);long previous=time.getTimeInMillis();
        EnergyLog.Report recent=EnergyLog.get(app).readBetween(vehicle,begin,end);
        EnergyLog.Report before=EnergyLog.get(app).readBetween(vehicle,previous,begin-1);
        change=EnergyChartSeries.changeMetric(recent,before,chosenMetric,EnergyLog.perPercent(app),EnergyLog.price(app));
      }catch(Exception ignored){}
      final Double value=change;
      post(()->{if(run==trendRequest&&isAttachedToWindow()&&compareDays==days&&metric==chosenMetric)refreshTrendLabel(value);});
    });
  }
  private void toggleChart() {
    chartExpanded = !chartExpanded;
    chartScroller.setVisibility(chartExpanded ? VISIBLE : GONE);
    chartToggle.setText(chartExpanded ? tr("收起图表  ⌃", "Collapse chart  ⌃")
        : tr("展开图表  ⌄", "Expand chart  ⌄"));
  }
  private void load() {
    int request = ++generation;
    loadTrend();
    historyTitle.setText(tr("读取本机采样…", "Loading local observations…"));
    if(vehicle == null) {
      historyTitle.setText(tr("未选择车辆，暂无本机历史。", "Select a vehicle to view local history."));
      historyValues.setText("");chart.update(null);distanceSummary.setVisibility(GONE);return;
    }
    final int chosen=range;final long from=customStart,until=customEnd;
    final long now=System.currentTimeMillis();
    final long first=chosen==0?now-3L*86400000L:chosen==1?now-7L*86400000L:chosen==2?now-31L*86400000L:from;
    displayedSpanMillis=Math.max(86400000L,(chosen==3?Math.min(now,until):now)-first);
    final String periodLabel=chosen==0?tr("最近3天","Last 3 days"):chosen==1?tr("最近7天","Last 7 days"):chosen==2?tr("最近31天","Last 31 days")
        :tr("自定义 ","Custom ")+formatDay(from)+" — "+formatDay(Math.min(now,until));
    EnergyLog.WORK.execute(() -> {
      try {
        EnergyLog.Report result=chosen==3?EnergyLog.get(context).readBetween(vehicle,from,Math.min(until,System.currentTimeMillis()))
            :EnergyLog.get(context).readRecentDays(vehicle,chosen==0?3:chosen==1?7:31);
        EnergyLog.Report latest7=chosen==1?result:EnergyLog.get(context).readRecentDays(vehicle,7);
        post(() -> {
          if(request != generation || !isAttachedToWindow())return;
          currentHistory=result;history7=latest7;
          double driving=result.drive;
          double climate=result.ac*EnergyLog.perPercent(context);
          double price=EnergyLog.price(context);
          historyTitle.setText(periodLabel+tr(" · ", " · ")+result.count+tr(" 次采样", " observations"));
          if(result.count == 0){
            historyValues.setText(tr("曲线随成功同步逐步生成；当前 TM 读数仍可单独查看。", "The chart builds as live refreshes are recorded. Current TM is shown above."));
          }else{
            String cost=price<=0 ? tr("电价未设", "tariff not set")
                : String.format(Locale.getDefault(),"%.2f %s",(driving+climate)*price,tr("元","CNY"));
            historyValues.setText(String.format(Locale.getDefault(),"%s %.2f kWh  ·  %s %.2f kWh\n%s %.1f km  ·  %s %s",
                tr("行驶", "Drive"),driving,tr("空调", "Climate"),climate,
                tr("采样里程", "Sampled km"),result.distanceKm,tr("费用", "Cost"),cost));
          }
          chart.update(result);
          updateDistanceSummary();
          chartScroller.post(()->chartScroller.fullScroll(View.FOCUS_RIGHT));
          note.setText(tr("区间里程仅统计连续、合理的里程表增量；行驶与驻车空调电耗为估算值，费用按设置电价计算。", "Sampled distance counts plausible consecutive odometer changes. Driving and parked climate energy are estimates; cost uses your tariff."));
        });
      } catch(Exception e) {
        post(() -> {if(request!=generation||!isAttachedToWindow())return;
          historyTitle.setText(tr("本机历史读取失败，可刷新后重试", "Local history could not load; retry after refresh"));
          historyValues.setText(""); chart.update(null);distanceSummary.setVisibility(GONE);
        });
      }
    });
  }
  private void updateDistanceSummary(){
    if(distanceSummary==null)return;
    if(metric!=2){distanceSummary.setVisibility(GONE);return;}
    distanceSummary.setVisibility(VISIBLE);
    if(currentHistory==null||currentHistory.count<2){
      distanceSummary.setText(tr("本区间缺少连续里程采样，暂不能估算行驶趋势。","Insufficient consecutive mileage samples for this interval."));return;
    }
    Double weekly=history7==null||history7.count<2?null:history7.distanceKm;
    double dayCount=Math.max(1d,displayedSpanMillis/86400000d);
    double averageDay=currentHistory.distanceKm/dayCount;
    double averageMonth=averageDay*30.4375;
    distanceSummary.setText(tr("近7日采样里程 ","Last 7d sampled: ")
        +(weekly==null?"--":String.format(Locale.getDefault(),"%.1f km",weekly))
        +tr("  ·  日均 ","  ·  daily avg ")+String.format(Locale.getDefault(),"%.1f km",averageDay)
        +tr("  ·  月均 ","  ·  monthly avg ")+String.format(Locale.getDefault(),"%.1f km",averageMonth)
        +tr("（按区间天数折算）"," (normalized by period days)"));
  }
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(historyTitle!=null)load();}
  @Override protected void onDetachedFromWindow(){generation++;trendRequest++;super.onDetachedFromWindow();}

  private final class Trend extends View {
    private EnergyLog.Report report;
    private int selection,step=groupStep,style=chartStyle;
    private int labels=valueLabelsMode,focusedBar=-1;
    private float downX,downY;
    Trend(Context c){super(c);setTag("energy_chart_canvas");setMinimumWidth(dp(270));setClickable(true);}
    void update(EnergyLog.Report value){report=value;focusedBar=-1;resize();invalidate();}
    void setMetric(int index){selection=index;focusedBar=-1;invalidate();}
    void setGrouping(int index){step=index;focusedBar=-1;resize();invalidate();}
    void setStyle(int index){style=index;invalidate();}
    void setLabels(int value,int focus){labels=value;focusedBar=focus;resize();invalidate();}
    private void resize(){
      int count=report==null?0:EnergyChartSeries.aggregate(report.points,step).size();
      // When every bar has a numeric callout, give the labels enough real width
      // instead of allowing them to overlap. Horizontal scrolling preserves all bins.
      int perBar=labels==EnergyChartDisplay.ALL?72:23;
      int minWidth=Math.max(dp(290),dp(Math.min(16000,Math.max(1,count)*perBar+72)));
      setMinimumWidth(minWidth);android.view.ViewGroup.LayoutParams lp=getLayoutParams();
      if(lp!=null&&lp.width!=minWidth){lp.width=minWidth;setLayoutParams(lp);}
    }
    @Override protected void onDraw(Canvas canvas){
      super.onDraw(canvas);
      List<EnergyChartSeries.Bin> bins=report==null?null:EnergyChartSeries.aggregate(report.points,step);
      double tariff=EnergyLog.price(context),unit=EnergyLog.perPercent(context);
      if((bins==null||bins.isEmpty())&&selection==0&&currentTmEnergy!=null){
        Paint paint=new Paint(3);float density=getResources().getDisplayMetrics().density;
        float h=getHeight()*.57f,cx=getWidth()*.5f,bottom=getHeight()*.78f;
        paint.setColor(PsaDesign.ORANGE);paint.setAlpha(220);
        canvas.drawRoundRect(cx-dp(25),bottom-h,cx+dp(25),bottom,dp(4),dp(4),paint);
        paint.setColor(muted);paint.setAlpha(255);paint.setTextSize(11*density);
        canvas.drawText(tr("当前TM（非历史）","Current TM (not history)"),dp(8),dp(22),paint);
        if(labels!=EnergyChartDisplay.HIDDEN)
          canvas.drawText(String.format(Locale.getDefault(),"%.1f kWh",currentTmEnergy),cx-dp(26),bottom-h-dp(7),paint);
        setContentDescription(tr("TM估算参考柱，非历史采样","TM estimate reference, not history"));return;
      }
      EnergyChartPainter.draw(canvas,bins,selection,style,unit,tariff,ink,muted,step,false,
          getResources().getDisplayMetrics().density,labels,focusedBar);
      setContentDescription(tr("历史统计图，","Historical statistics, ")
          +(step==0?tr("按天","daily"):step==1?tr("按周","weekly"):tr("按月","monthly"))
          +tr("；点击柱子显示/隐藏数值；仅有效历史采样","; tap a bar to show or hide its value; actual observations only"));
    }
    @Override public boolean onTouchEvent(MotionEvent event){
      int action=event.getActionMasked();
      if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();return true;}
      if(action==MotionEvent.ACTION_MOVE){
        int slop=ViewConfiguration.get(context).getScaledTouchSlop();
        if(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop){
          if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
          return false; // Leave horizontal panning to the chart's scroll container.
        }
        return true;
      }
      if(action==MotionEvent.ACTION_UP){
        int slop=ViewConfiguration.get(context).getScaledTouchSlop();
        if(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop)return false;
        performClick();return true;
      }
      return action==MotionEvent.ACTION_CANCEL||super.onTouchEvent(event);
    }
    @Override public boolean performClick(){
      super.performClick();
      List<EnergyChartSeries.Bin> bins=report==null?null:EnergyChartSeries.aggregate(report.points,step);
      if(bins==null||bins.isEmpty()){
        if(labels==EnergyChartDisplay.HIDDEN)selectChartBar(-1);else hideChartLabels();
        return true;
      }
      float density=getResources().getDisplayMetrics().density;
      float left=EnergyChartPainter.plotLeft(density);
      float width=Math.max(1,getWidth()-left-EnergyChartPainter.plotRight(density));
      int hit=(int)Math.floor((downX-left)/width*bins.size());
      if(hit<0||hit>=bins.size())return true;
      if(!EnergyChartDisplay.valid(bins.get(hit).amount(selection,EnergyLog.perPercent(context),EnergyLog.price(context))))return true;
      double[] amounts=new double[bins.size()];
      for(int i=0;i<amounts.length;i++)amounts[i]=bins.get(i).amount(selection,EnergyLog.perPercent(context),EnergyLog.price(context));
      int[] currently=EnergyChartDisplay.annotated(amounts,labels,focusedBar);
      if(labels==EnergyChartDisplay.LATEST&&currently.length==1&&currently[0]==hit)hideChartLabels();
      else selectChartBar(hit);
      return true;
    }
  }
}
