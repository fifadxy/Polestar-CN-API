package io.github.polestarcn.companion;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Official live Polestar specifications in-app + a compact offline index. */
public final class VehicleSpecificationsActivity extends Activity {
  public static final String OFFICIAL="https://www.polestar.com/global/polestar-2/specifications/";
  private LinearLayout root;
  private WebView web;
  private boolean dark;
  private boolean en;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    dark=AppPreferences.dark(this);en=AppPreferences.english(this);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(dark?0xFF151617:0xFFF3F3F1);
    setContentView(root);
    LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(12),dp(8),dp(12),dp(8));bar.setGravity(Gravity.CENTER_VERTICAL);
    TextView back=item("‹  "+(en?"Vehicle":"返回"),15,()->finish());bar.addView(back,new LinearLayout.LayoutParams(0,dp(46),1));
    TextView title=item(en?"Polestar 2 Specs":"Polestar 2 车辆规格",18,null);title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    bar.addView(title,new LinearLayout.LayoutParams(0,dp(46),2));root.addView(bar);
    LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(12),0,dp(12),dp(8));
    TextView local=item(en?"Specifications index":"规格目录",14,this::showIndex);
    TextView official=item(en?"Official full page":"官网完整规格",14,this::showOfficial);
    tabs.addView(local,new LinearLayout.LayoutParams(0,dp(42),1));
    tabs.addView(official,new LinearLayout.LayoutParams(0,dp(42),1));root.addView(tabs);
    showIndex();
  }
  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private TextView item(String value,int size,Runnable action){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
    t.setTextColor(dark?Color.WHITE:0xFF151617);t.setGravity(Gravity.CENTER_VERTICAL);
    t.setPadding(dp(10),dp(4),dp(10),dp(4));
    if(action!=null)t.setOnClickListener(v->action.run());return t;
  }
  private void clearBody(){if(web!=null){root.removeView(web);web.destroy();web=null;}while(root.getChildCount()>2)root.removeViewAt(2);}
  private void section(LinearLayout body,String name,String data){
    TextView h=item(name,17,null);h.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
    h.setPadding(dp(14),dp(16),dp(12),0);body.addView(h);
    TextView d=item(data,14,null);d.setTextColor(dark?0xFFCFD1D0:0xFF555A5A);d.setLineSpacing(dp(3),1.05f);
    d.setPadding(dp(14),dp(6),dp(14),dp(7));body.addView(d);
  }
  private void showIndex(){
    clearBody();ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    TextView note=item(en?"Official global-market specifications; equipment depends on market, model year and trim. This is not a live readout from your vehicle."
      :"以下为极星全球官网公开规格摘要，不等于当前中国区车辆的实车配置；年款、销售地区及选装包可能不同。",12,null);
    note.setTextColor(dark?0xFFFFAB70:0xFFB34700);body.addView(note);
    section(body,en?"Powertrains (four variants)":"动力系统（四种版本）",
      "Standard range Single motor: 200 kW / 272 hp · 490 Nm · 0–100 6.4 s · WLTP 554 km\n"
      +"Long range Single motor: 220 kW / 299 hp · 490 Nm · 6.2 s · WLTP 659 km\n"
      +"Long range Dual motor: 310 kW / 421 hp · 740 Nm · 4.5 s · WLTP 596 km\n"
      +"Performance: 350 kW / 476 hp · 740 Nm · 4.2 s · WLTP up to 568 km");
    section(body,en?"Battery & charging":"电池与补能",
      "Standard 70 kWh · DC up to 180 kW · 10–80% 26 min · AC 11 kW / 7 h\n"
      +"Long range 82 kWh · DC up to 205 kW · 10–80% 28 min · AC 11 kW / 8 h");
    section(body,en?"Dimensions & chassis":"尺寸与底盘",
      en?"Length 4606 mm · Width 1859 mm (mirrors out 1985 mm) · Height 1479 mm\nWheelbase 2735 mm · Turning circle 11.5 m · Front MacPherson / rear multi-link"
      :"长 4606 mm · 宽 1859 mm（含展开后视镜 1985 mm）· 高 1479 mm\n轴距 2735 mm · 转弯直径 11.5 m · 前麦弗逊 / 后多连杆");
    section(body,en?"Storage":"储物空间",
      en?"Rear 407 L (seats up), 1097 L (seats down); rear underfloor 43 L; front compartment 41 L"
      :"后备箱 407 L（座椅直立）/ 1097 L（后排放倒）；后备箱地板下 43 L；前备箱 41 L");
    section(body,en?"Wheels & brakes":"轮胎与制动",
      "19 in Aero: 245/45R19 · 20 in Pro / Performance: 245/40R20\n"
      +"Standard front brake discs 345 × 30 mm; Performance front Brembo 375 × 35 mm");
    section(body,en?"Other official sections":"其他官方规格目录",
      en?"Exterior · Interior · Infotainment · Safety & assistance · Prime, Pilot, Plus, Climate, Pro, Performance and Nappa packages · Single options · Sustainability"
      :"外观 · 内饰 · 信息娱乐 · 安全与辅助 · Prime / Pilot / Plus / Climate / Pro / Performance / Nappa 选装包 · 单项配置 · 可持续材料");
    TextView full=item(en?"View complete official specifications online ›":"在应用内查看极星官网完整规格 ›",15,this::showOfficial);
    full.setTextColor(dark?0xFFFFB16C:0xFFB34700);full.setPadding(dp(14),dp(18),dp(14),dp(25));body.addView(full);
  }
  private void showOfficial(){
    clearBody();web=new WebView(this);
    web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
    web.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest request){
        Uri uri=request.getUrl();String host=uri.getHost();
        if("https".equalsIgnoreCase(uri.getScheme())&&host!=null&&(host.equals("polestar.com")||host.endsWith(".polestar.com")))return false;
        if("https".equalsIgnoreCase(uri.getScheme())||"mailto".equalsIgnoreCase(uri.getScheme())){
          try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}return true;
        }
        return true;
      }
    });
    root.addView(web,new LinearLayout.LayoutParams(-1,0,1));web.loadUrl(OFFICIAL);
  }
  @Override public void onBackPressed(){
    if(web!=null&&web.canGoBack())web.goBack();else if(web!=null)showIndex();else super.onBackPressed();
  }
  @Override protected void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}
