package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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
  private final TextView historyTitle, historyValues, note, chartToggle;
  private final Trend chart;
  private int range = 0, metric = 0;
  private Double currentTmEnergy;
  private boolean chartExpanded = true;
  private int generation;

  public EnergySummaryPanel(Context context, VehicleSnapshot snapshot, String vehicleId) {
    super(context);
    this.context = context;
    vehicle = EnergyLog.validVehicle(vehicleId) ? vehicleId : null;
    boolean dark = AppPreferences.dark(context);
    ink = PsaDesign.ink(dark);
    muted = PsaDesign.muted(dark);
    setOrientation(VERTICAL);
    setTag("energy_summary_panel");
    setPadding(dp(6), dp(4), dp(6), dp(8));
    addView(title(tr("本次 TM 行程（上次清零至今）", "Current TM trip (since reset)"), 17));
    currentTrip(snapshot);
    TextView heading=title(tr("能耗图表 · 本机历史", "Energy chart · local history"), 15);
    heading.setTag("energy_chart_heading");addView(heading);
    chart = new Trend(context);
    addView(chart, new LayoutParams(-1, dp(165)));
    LinearLayout tabs = new LinearLayout(context);
    tabs.setOrientation(HORIZONTAL);
    String[] zh = {"行驶", "空调", "里程", "费用"};
    String[] en = {"Driving", "Climate", "Distance", "Cost"};
    for (int i = 0; i < 4; i++) {
      final int selected = i;
      tabs.addView(action(tr(zh[i], en[i]), () -> changeMetric(selected)), new LayoutParams(0, dp(42), 1));
    }
    addView(tabs);
    chartToggle = action(tr("收起图表  ⌃", "Collapse chart  ⌃"), this::toggleChart);
    chartToggle.setGravity(Gravity.CENTER_VERTICAL);
    addView(chartToggle);
    tripDetails(snapshot);
    row(tr("本机历史采样", "Local recorded history"), "");
    LinearLayout periods = new LinearLayout(context);
    periods.setOrientation(HORIZONTAL);
    periods.addView(action(tr("最近 7 天", "Last 7 days"), () -> changeRange(0)), new LayoutParams(0, dp(42), 1));
    periods.addView(action(tr("最近 31 天", "Last 31 days"), () -> changeRange(1)), new LayoutParams(0, dp(42), 1));
    addView(periods);
    historyTitle = title(tr("读取本机采样…", "Loading local observations…"), 13);
    addView(historyTitle);
    historyValues = title("", 14);
    addView(historyValues);
    note = title(tr("历史为本机有效采样的估算值，并非充电账单。", "History estimates use local samples, not charging invoices."), 12);
    note.setTextColor(muted);addView(note);
    addView(action(tr("电价 / 电池容量 / 保存期限设置", "Tariff / capacity / retention settings"), () -> {
      android.content.Intent intent = new android.content.Intent(context, EnergyActivity.class);
      if(vehicle != null)intent.putExtra("vehicle", vehicle);
      context.startActivity(intent);
    }), new LayoutParams(-1, dp(44)));
    load();
  }

  private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
  private String tr(String zh, String en) { return AppPreferences.tr(context, zh, en); }
  private TextView title(String value, int sp) {
    TextView t = new TextView(context);
    t.setText(value);t.setTextSize(sp);t.setTextColor(ink);
    t.setPadding(dp(5),dp(5),dp(5),dp(5));
    return t;
  }
  private TextView action(String name, Runnable click) {
    TextView b = title(name, 12);
    b.setGravity(Gravity.CENTER);
    b.setBackgroundColor(PsaDesign.surface(AppPreferences.dark(context)));
    b.setOnClickListener(v -> {Haptics.tap(context);click.run();});
    return b;
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
    item.setTag("energy_kpi_"+tag);item.setMinimumHeight(dp(45));
    TextView name=title(key,15);TextView number=title(value,17);
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
    currentTmEnergy=TmCost.kwh(distance,average);
    double tariff=EnergyLog.price(context);
    kpi(tr("TM 里程","TM distance"),fmt(distance,"km"),"tm_distance");
    kpi(tr("TM 总能耗（估算）","TM energy (estimate)"),fmt(currentTmEnergy,"kWh"),"tm_total_energy");
    kpi(tr("估算费用","Estimated cost"),currentTmEnergy==null?"--"
        :tariff<=0?tr("未设电价","No tariff")
        :String.format(Locale.getDefault(),"%.2f %s",currentTmEnergy*tariff,tr("元","CNY")),"tm_cost");
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
  private void changeRange(int value) { range = value; load(); }
  private void changeMetric(int value) { metric = value; chart.setMetric(metric); }
  private void toggleChart() {
    chartExpanded = !chartExpanded;
    chart.setVisibility(chartExpanded ? VISIBLE : GONE);
    chartToggle.setText(chartExpanded ? tr("收起图表  ⌃", "Collapse chart  ⌃")
        : tr("展开图表  ⌄", "Expand chart  ⌄"));
  }
  private void load() {
    int request = ++generation;
    historyTitle.setText(tr("读取本机采样…", "Loading local observations…"));
    if(vehicle == null) {
      historyTitle.setText(tr("未选择车辆，暂无本机历史。", "Select a vehicle to view local history."));
      historyValues.setText(""); chart.update(null);return;
    }
    EnergyLog.WORK.execute(() -> {
      try {
        EnergyLog.Report result=EnergyLog.get(context).read(vehicle,range);
        post(() -> {
          if(request != generation || !isAttachedToWindow())return;
          double driving=result.drive;
          double climate=result.ac*EnergyLog.perPercent(context);
          double price=EnergyLog.price(context);
          historyTitle.setText((range == 0 ? tr("最近 7 天", "Last 7 days") : tr("最近 31 天", "Last 31 days"))
              +tr(" · ", " · ")+result.count+tr(" 次采样", " observations"));
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
          note.setText(tr("区间里程仅统计连续、合理的里程表增量；行驶与驻车空调电耗为估算值，费用按设置电价计算。", "Sampled distance counts plausible consecutive odometer changes. Driving and parked climate energy are estimates; cost uses your tariff."));
        });
      } catch(Exception e) {
        post(() -> {if(request!=generation||!isAttachedToWindow())return;
          historyTitle.setText(tr("本机历史读取失败，可刷新后重试", "Local history could not load; retry after refresh"));
          historyValues.setText(""); chart.update(null);
        });
      }
    });
  }
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(historyTitle!=null)load();}
  @Override protected void onDetachedFromWindow(){generation++;super.onDetachedFromWindow();}

  private final class Trend extends View {
    private final Paint p=new Paint(3);
    private EnergyLog.Report report;
    private int selection;
    Trend(Context c) {super(c);setTag("energy_history_chart");}
    void update(EnergyLog.Report value) {report=value;invalidate();}
    void setMetric(int value) {selection=value;invalidate();}
    @Override protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      float d=getResources().getDisplayMetrics().density;
      float left=dp(43),top=dp(19),width=Math.max(1,getWidth()-left-dp(12)),height=Math.max(1,getHeight()-top-dp(38));
      p.setColor(muted);p.setStrokeWidth(d);p.setStyle(Paint.Style.STROKE);
      canvas.drawLine(left,top,left,top+height,p);canvas.drawLine(left,top+height,left+width,top+height,p);p.setStyle(Paint.Style.FILL);
      List<EnergyLog.Point> points=report==null?null:report.points;
      if(points==null||points.isEmpty()){
        if(selection==0&&currentTmEnergy!=null){
          // A single current TM estimate is a reference bar, not fabricated history.
          double value=Math.max(0,currentTmEnergy);
          float barHeight=value<=0?0:height*.73f;
          float middle=left+width*.5f,half=Math.min(dp(45),width*.16f);
          p.setColor(PsaDesign.ORANGE);
          canvas.drawRoundRect(middle-half,top+height-barHeight,middle+half,top+height,dp(5),dp(5),p);
          p.setColor(muted);p.setTextSize(11*d);
          canvas.drawText(tr("当前 TM · 估算", "Current TM · estimate"),left+dp(6),top+dp(13),p);
          canvas.drawText(String.format(Locale.getDefault(),"%.1f kWh",value),middle-dp(26),top+height-barHeight-dp(6),p);
          setContentDescription(tr("当前 TM 总能耗参考柱，非历史趋势","Current TM reference bar, not historical trend"));
          return;
        }
        p.setTextSize(12*d);canvas.drawText(tr("等待历史采样", "Awaiting samples"),left+dp(12),top+height/2,p);return;
      }
      double tariff=EnergyLog.price(context), perPercent=EnergyLog.perPercent(context);
      if(selection==3&&tariff<=0){p.setTextSize(12*d);canvas.drawText(tr("请先设置电价", "Set electricity tariff"),left+dp(12),top+height/2,p);return;}
      double max=0;
      double[] values=new double[points.size()];
      for(int i=0;i<values.length;i++){
        EnergyLog.Point point=points.get(i);
        double energy=point.drive+point.ac*perPercent;
        values[i]=selection==0?point.drive:selection==1?point.ac*perPercent:selection==2?point.distanceKm:energy*tariff;
        max=Math.max(max,values[i]);
      }
      if(max<=0){p.setTextSize(12*d);canvas.drawText(tr("已有采样，尚无可计算区间", "Samples exist; no estimated interval yet"),left+dp(6),top+height/2,p);return;}
      double scale=max*1.08;
      p.setColor(muted);p.setTextSize(10*d);
      canvas.drawText(String.format(Locale.getDefault(),"%.1f",max),dp(2),top+dp(6),p);
      canvas.drawText("0",dp(22),top+height,p);
      float step=width/values.length;
      p.setColor(PsaDesign.ORANGE);
      for(int i=0;i<values.length;i++){
        float bar=Math.max(0,(float)(values[i]/scale*height));
        float x=left+i*step+step*.2f;
        canvas.drawRoundRect(x,top+height-bar,x+step*.6f,top+height,Math.min(dp(3),step*.15f),Math.min(dp(3),step*.15f),p);
      }
      p.setColor(muted);p.setTextSize(10*d);
      int[] labels={0,points.size()/2,points.size()-1};
      for(int index:labels){
        if(index<0||index>=points.size())continue;
        String name=android.text.format.DateFormat.format("MM-dd",points.get(index).t).toString();
        float x=left+(index+.5f)*step-p.measureText(name)/2;
        canvas.drawText(name,Math.max(left,Math.min(left+width-p.measureText(name),x)),top+height+dp(19),p);
      }
      setContentDescription(tr("本机能耗图表，", "Local energy chart, ")+String.format(Locale.getDefault(),"%.2f",max));
    }
  }
}
