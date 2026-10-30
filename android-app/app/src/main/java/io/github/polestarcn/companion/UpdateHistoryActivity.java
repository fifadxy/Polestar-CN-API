package io.github.polestarcn.companion;
import android.app.Activity;import android.os.Bundle;import android.widget.*;
/** Curated feature history only. Does not display source documents or service credentials. */
public final class UpdateHistoryActivity extends Activity {
 static final String[][] HISTORY={
 {"v0.9.6","设置搜索、沉浸显示、车况文字自定义、配置备份恢复；统一二级车况菜单，优化长按、门锁确认与同向补发；通知图标独立可选，新增三种充电流光。","Searchable settings, edge-to-edge display, condition text customization, settings backup/restore, secondary condition menus, improved holds, lock verification with one same-direction resend, independent notification icons and three new charging sweeps."},
 {"v0.9","新增能耗历史曲线、车辆图片选择、可调动态效果、分层粒子、轨迹尾迹和自定义背景；持续修复布局和稳定性。","Energy history, vehicle image selection, adjustable effects, depth-layered particles, motion trails and custom backgrounds; layout and stability fixes."},
 {"v0.8","扩展桌面组件尺寸、透明风格、数据与按钮选择；优化地图预览、车况展开方式及刷新规则。","More widget sizes, transparency, selectable data and controls; improved map previews, expandable condition details and refresh rules."},
 {"v0.7","增加行程、空气质量、胎压和保养数据显示；增加可切换布局、字号、反馈与桌面组件选项。","Trip, air quality, tyre and service data; selectable layouts, type sizes, feedback and widget options."},
 {"v0.6","统一日夜配色、图标与组件规范，增加圆屏和方屏布局预览，优化主页排版。","Unified day/night colors, icons and components; round/square display previews and improved home layout."},
 {"v0.5","优化车辆控制首页、常用按钮与信息卡片，完善不同尺寸屏幕上的显示。","Improved control home, common actions and information cards across display sizes."},
 {"v0.4","加入桌面组件、常驻状态通知、中英文与日夜显示选项。","Introduced widgets, persistent status notifications, bilingual display and day/night options."},
 {"v0.3及更早 / and earlier","建立初始页面、基础数据显示和设置框架。","Initial screens, basic data presentation and settings foundation."}};
 @Override public void onCreate(Bundle state){super.onCreate(state);boolean dark=AppPreferences.dark(this);ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);int p=Math.round(16*getResources().getDisplayMetrics().density);root.setPadding(p,p,p,p);root.setBackgroundColor(PsaDesign.canvas(dark));scroll.addView(root);Button back=new Button(this);back.setText(AppPreferences.tr(this,"返回设置","Back to settings"));back.setOnClickListener(v->{Haptics.tap(this);finish();});root.addView(back);for(String[] entry:HISTORY){TextView heading=new TextView(this);heading.setText(entry[0]);heading.setTextSize(22);heading.setTextColor(PsaDesign.ink(dark));heading.setPadding(0,p,0,p/2);root.addView(heading);TextView text=new TextView(this);text.setText(entry[AppPreferences.english(this)?2:1]);text.setTextSize(15);text.setTextColor(PsaDesign.muted(dark));text.setLineSpacing(p/4f,1);root.addView(text);}setContentView(scroll);AppWindow.apply(this,null);}
}
