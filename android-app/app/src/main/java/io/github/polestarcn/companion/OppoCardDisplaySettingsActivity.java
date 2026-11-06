package io.github.polestarcn.companion;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import io.github.polestarcn.oppobridge.OppoCardSummary;
import java.util.Locale;

/** Fully editable preview for MyDevices / AssistantScreen; reads the local cache only. */
public final class OppoCardDisplaySettingsActivity extends Activity {
  private static final int ORANGE=0xffff6b00;
  private static final String[][] PRESETS={
      {"电量 + 续航","Battery + range","{soc}{sep}{range}"},
      {"电量 + 更新时间","Battery + update","{soc}{sep}{updated}"},
      {"续航 + 更新时间","Range + update","{range}{sep}{updated}"},
      {"仅更新时间","Update time only","{updated}"},
      {"电量 + 锁车状态","Battery + lock","{soc}{sep}{lock}"},
      {"自定义内容","Custom wording","Polestar 2{sep}{soc}"}
  };
  private EditText templateInput,separatorInput;
  private TextView previewPrimary,previewShelf,previewMessage,previewSource;
  private Bundle data;
  private boolean previewUsesDemo;
  private LinearLayout root;
  private int ink,secondary,surface;
  private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
  private String tr(String zh,String en){return AppPreferences.tr(this,zh,en);}
  @Override public void onCreate(Bundle saved){
    super.onCreate(saved);
    boolean dark=AppPreferences.dark(this);
    ink=dark?Color.WHITE:0xff1f2326;
    secondary=dark?0xffbac0c4:0xff70767c;
    surface=dark?0xff252a2e:0xfff3f4f5;
    int page=dark?0xff111416:Color.WHITE;
    data=fromCache();
    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(18),dp(16),dp(18),dp(26));
    root.setBackgroundColor(page);scroll.addView(root);
    addText(tr("OPPO 卡片显示","OPPO card display"),22,ink,true);
    addText(tr("设备空间 · 负一屏 · 本地卡片，共用一套显示规则",
        "MyDevices · Shelf · local card share one display template"),12,secondary,false);

    header(tr("实时预览","Live preview"));
    LinearLayout previewPanel=new LinearLayout(this);previewPanel.setOrientation(LinearLayout.VERTICAL);
    previewPanel.setPadding(dp(14),dp(10),dp(14),dp(12));previewPanel.setBackground(background(surface,14));
    previewPanel.addView(small(tr("设备空间 · 4×2","MyDevices · 4×2")));
    previewPrimary=strongPreview(14);previewPrimary.setTag("oppo_card_preview_device");
    previewPanel.addView(previewPrimary,new LinearLayout.LayoutParams(-1,dp(34)));
    previewPanel.addView(small(tr("负一屏 · 摘要栏","Shelf · status")));
    previewShelf=strongPreview(14);previewShelf.setTag("oppo_card_preview_shelf");
    previewPanel.addView(previewShelf,new LinearLayout.LayoutParams(-1,dp(34)));
    previewSource=small("");previewSource.setTag("oppo_card_preview_origin");
    previewPanel.addView(previewSource);
    previewMessage=small("");previewMessage.setTag("oppo_card_preview_status");
    previewMessage.setTextColor(ORANGE);previewPanel.addView(previewMessage);
    root.addView(previewPanel,new LinearLayout.LayoutParams(-1,-2));

    header(tr("快速组合","Presets"));
    for(int row=0;row<PRESETS.length;row+=2){
      LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);
      for(int i=row;i<Math.min(row+2,PRESETS.length);i++){
        final String text=PRESETS[i][2];Button b=tile(tr(PRESETS[i][0],PRESETS[i][1]),()->templateInput.setText(text));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);
        if(i>row)p.leftMargin=dp(8);buttons.addView(b,p);
      }
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(6);root.addView(buttons,p);
    }

    header(tr("完整内容模板","Custom text template"));
    addText(tr("按显示顺序编辑。可加入自定义文字及下方字段；{sep} 使用自定分隔符。缺失数据不会伪造成 0。",
        "Edit text and field order. {sep} uses your separator; unavailable values never become zero."),
        12,secondary,false);
    templateInput=new EditText(this);templateInput.setTag("oppo_card_template_editor");
    templateInput.setSingleLine(true);templateInput.setTextSize(14);templateInput.setTextColor(ink);
    templateInput.setHint("{soc}{sep}{range}");templateInput.setSelectAllOnFocus(false);
    templateInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(OppoCardSummary.MAX_TEMPLATE_LENGTH)});
    templateInput.setText(OppoCardSummarySettings.template(this));
    root.addView(templateInput,new LinearLayout.LayoutParams(-1,dp(50)));

    header(tr("点击插入字段","Insert field at cursor"));
    final String[][] fields={
        {"电量","SOC","{soc}"},{"续航","Range","{range}"},{"更新时间","Updated","{updated}"},
        {"总里程","Odometer","{odometer}"},{"门锁","Lock","{lock}"},{"空调","Climate","{climate}"},
        {"净化","Air","{air}"},{"充电状态","Charging","{charging}"},
        {"缓存来源","Cache source","{source}"},{"分隔符","Separator","{sep}"}
    };
    for(int row=0;row<fields.length;row+=3){
      LinearLayout choices=new LinearLayout(this);
      for(int i=row;i<Math.min(row+3,fields.length);i++){
        String token=fields[i][2];Button b=tile(tr(fields[i][0],fields[i][1]),()->insertToken(token));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(40),1);
        if(i>row)p.leftMargin=dp(6);choices.addView(b,p);
      }
      LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);
      rowParams.topMargin=dp(6);root.addView(choices,rowParams);
    }

    header(tr("中间分隔符","Separator"));
    addText(tr("可使用 ·、/、|、空格或完全自定义的符号（最多 8 个字符）。",
        "Use ·, /, |, spaces, or your own separator (up to 8 characters)."),12,secondary,false);
    separatorInput=new EditText(this);separatorInput.setTag("oppo_card_separator_editor");
    separatorInput.setSingleLine(true);separatorInput.setTextSize(14);separatorInput.setTextColor(ink);
    separatorInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(OppoCardSummary.MAX_SEPARATOR_LENGTH)});
    separatorInput.setText(OppoCardSummarySettings.separator(this));
    root.addView(separatorInput,new LinearLayout.LayoutParams(-1,dp(50)));
    LinearLayout separators=new LinearLayout(this);
    String[] choices={" · "," | "," / "," • ","  ",""};
    String[] labels={tr("中点","Dot"),"|","/",tr("圆点","Bullet"),tr("空格","Space"),tr("无","None")};
    for(int i=0;i<choices.length;i++){
      String chosen=choices[i];
      Button b=tile(labels[i],()->separatorInput.setText(chosen));
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(40),1);
      if(i>0)p.leftMargin=dp(4);separators.addView(b,p);
    }
    LinearLayout.LayoutParams sepParams=new LinearLayout.LayoutParams(-1,-2);sepParams.topMargin=dp(6);
    root.addView(separators,sepParams);

    header(tr("保存显示样式","Apply to OPPO cards"));
    LinearLayout controls=new LinearLayout(this);
    Button save=tile(tr("保存并同步","Save & refresh"),this::save);
    save.setTag("oppo_card_save");
    Button reset=tile(tr("恢复默认","Restore default"),()->{
      templateInput.setText(OppoCardSummary.DEFAULT_TEMPLATE);
      separatorInput.setText(OppoCardSummary.DEFAULT_SEPARATOR);
    });
    controls.addView(save,new LinearLayout.LayoutParams(0,dp(48),2));
    LinearLayout.LayoutParams resetParams=new LinearLayout.LayoutParams(0,dp(48),1);resetParams.leftMargin=dp(8);
    controls.addView(reset,resetParams);root.addView(controls);
    addText(tr("只改变缓存数据的显示文字，不会申请新车况、发送远控或改变 OPPO 绑定状态。"
        +"官方卡片的最终截断还受 ColorOS 版本及宿主模板宽度限制；较长内容会在本机预览中提示缩短。",
        "Display-only. No telemetry fetch, remote command, or binding change. Actual truncation depends on the OEM host width."),
        11,secondary,false);

    TextWatcher watcher=new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){updatePreview();}
      public void afterTextChanged(Editable s){}
    };
    templateInput.addTextChangedListener(watcher);separatorInput.addTextChangedListener(watcher);
    setContentView(scroll);updatePreview();
  }

  private Bundle fromCache(){
    VehicleSnapshot state=VehicleCoordinator.get(this).snapshot;
    if(state==null)state=VehicleSnapshotStore.loadChina(this);
    Bundle b=new Bundle();
    if(state==null||state.isDemo){
      previewUsesDemo=true;
      b.putBoolean("available",true);
      b.putBoolean("batteryPercentKnown",true);b.putInt("batteryPercent",88);
      b.putBoolean("rangeKmKnown",true);b.putInt("rangeKm",370);
      b.putBoolean("odometerKmKnown",true);b.putInt("odometerKm",25100);
      b.putString("lock","已锁");b.putString("climate","未运行");
      b.putString("air","关闭");b.putString("charging","未充电");
      b.putLong("capturedAt",System.currentTimeMillis());
      return b;
    }
    b.putBoolean("available",true);
    if(state.batteryPercent!=null){b.putBoolean("batteryPercentKnown",true);b.putInt("batteryPercent",state.batteryPercent);}
    if(state.rangeKm!=null){b.putBoolean("rangeKmKnown",true);b.putInt("rangeKm",state.rangeKm);}
    if(state.odometerKm!=null){b.putBoolean("odometerKmKnown",true);b.putInt("odometerKm",state.odometerKm);}
    b.putLong("capturedAt",state.timestampMillis);
    b.putString("lock",state.lock);b.putString("climate",state.climate);
    b.putString("air",state.air);b.putString("charging",state.charging);
    return b;
  }
  private void insertToken(String token){
    int start=Math.max(0,templateInput.getSelectionStart());
    templateInput.getText().insert(start,token);
  }
  private void updatePreview(){
    if(templateInput==null||separatorInput==null||previewPrimary==null)return;
    String tpl=templateInput.getText().toString(),sep=separatorInput.getText().toString();
    try{
      OppoCardSummary.checkTemplate(tpl);OppoCardSummary.checkSeparator(sep);
      String shown=OppoCardSummary.format(data,tpl,sep);
      previewPrimary.setText(shown);previewShelf.setText(shown);
      previewSource.setText(previewUsesDemo?tr("演示数据，不代表真实车辆","Sample data, not the real car"):
          tr("来自本机车辆缓存，不申请远程刷新","From local car cache, no network refresh"));
      int weight=OppoCardSummary.units(shown);
      previewMessage.setText(weight>25?
          tr("内容较长，窄版 OPPO 卡片可能截断；可减少字段或缩短文字。","Long text may be truncated by narrow OEM cards."):
          tr("已实时预览，保存后同步 OPPO 卡片。","Live preview; save to apply to OPPO cards."));
    }catch(IllegalArgumentException e){
      previewPrimary.setText("—");previewShelf.setText("—");
      previewMessage.setText(tr("模板或分隔符格式不正确，请修改后保存。","Invalid template/separator; please correct it."));
    }
  }
  private void save(){
    try{
      OppoCardSummarySettings.save(this,templateInput.getText().toString(),separatorInput.getText().toString());
      Toast.makeText(this,tr("已保存并通知 OPPO 卡片；如未即时变化，请重新打开负一屏或设备空间。",
          "Saved; reopen the OPPO host if it does not update immediately."),Toast.LENGTH_LONG).show();
      updatePreview();
    }catch(IllegalArgumentException e){
      Toast.makeText(this,tr("格式不正确：只允许已列出的 {字段}、单行内容和最多 8 字符分隔符。",
          "Use known single-line field tokens and a separator of up to 8 characters."),Toast.LENGTH_LONG).show();
      updatePreview();
    }
  }
  private int color(){return ink;}
  private void header(String title){
    TextView t=new TextView(this);t.setText(title);t.setTextColor(ORANGE);t.setTextSize(15);
    t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(15);p.bottomMargin=dp(5);
    root.addView(t,p);
  }
  private void addText(String label,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);
    if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(6);root.addView(t,p);
  }
  private GradientDrawable background(int fill,int radius){
    GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));return d;
  }
  private TextView small(String text){
    TextView t=new TextView(this);t.setText(text);t.setTextSize(11);t.setTextColor(secondary);
    t.setGravity(Gravity.CENTER_VERTICAL);t.setMinHeight(dp(22));return t;
  }
  private TextView strongPreview(int size){
    TextView t=new TextView(this);t.setTextColor(ink);t.setTextSize(size);
    t.setGravity(Gravity.CENTER_VERTICAL);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    t.setSingleLine(true);t.setHorizontallyScrolling(false);t.setEllipsize(null);
    t.setAutoSizeTextTypeUniformWithConfiguration(9,size,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    return t;
  }
  private Button tile(String label,Runnable action){
    Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(11);
    b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);
    b.setPadding(dp(4),dp(2),dp(4),dp(2));b.setTextColor(ink);
    b.setBackground(background(surface,8));b.setOnClickListener(v->action.run());return b;
  }
}
