package io.github.polestarcn.companion;
import android.app.*;
import android.content.Context;
import android.widget.*;
/** Versioned acknowledgement; no implicit acceptance on dismiss or process restart. */
public final class FirstUseConsent {
 private FirstUseConsent(){}
 public static boolean accepted(Context c){return c.getSharedPreferences("legal",0).getBoolean("accepted_disclaimer_v1",false);}
 public static String text(Context c){return AppPreferences.tr(c,
 "本应用为个人学习研究作品，仅供非商业用途，不是 Polestar / Volvo 官方应用，也未获其认可或授权。\n\n本应用通过服务接口读取车辆数据并发送车辆控制指令。数据可能延迟、错误或与车辆实际状态不一致；控制指令也可能失败、延迟或重复。\n\n作者不承认对使用本应用造成的任何后果负有责任，并在法律允许的最大范围内不承担任何责任，包括但不限于人身伤害、财产损失、车辆损坏、数据丢失、服务中断或控制异常造成的损失。\n\n使用者应遵守适用法律、服务条款及车辆安全规定。请勿在驾驶时操作；执行车辆控制前请确认环境安全，不要仅依赖本应用信息作出安全判断。如有疑问，以车辆实际状态及官方服务为准。",
 "This is a personal learning and research project for non-commercial use. It is not an official Polestar / Volvo app and is not endorsed or authorized by them.\n\nThe app reads vehicle data and sends vehicle commands through service interfaces. Data may be delayed, incorrect or inconsistent with the vehicle. Commands may fail, be delayed or repeat.\n\nTo the maximum extent permitted by law, the author disclaims liability for consequences including injury, property or vehicle damage, lost data, service interruption or control errors.\n\nFollow applicable laws, service terms and vehicle safety requirements. Do not operate while driving. Confirm the environment is safe before controlling the vehicle; do not rely solely on this app for safety decisions. Refer to the actual vehicle and official services when in doubt.");}
 public static void show(Activity a,Runnable done){
  LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);int p=(int)(16*a.getResources().getDisplayMetrics().density);panel.setPadding(p,p,p,p);
  TextView body=new TextView(a);body.setText(text(a));body.setTextSize(14);ScrollView scroll=new ScrollView(a);scroll.addView(body);panel.addView(scroll,new LinearLayout.LayoutParams(-1,Math.max(p*6,Math.min(p*22,a.getResources().getDisplayMetrics().heightPixels/3))));
  CheckBox check=new CheckBox(a);check.setTag("legal_accept");check.setText(AppPreferences.tr(a,"我已阅读并同意上述协议","I have read and agree to this disclaimer"));panel.addView(check);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle(AppPreferences.tr(a,"首次使用 · 免责声明","First use · Disclaimer")).setView(panel).setCancelable(false).setNegativeButton(AppPreferences.tr(a,"退出","Exit"),(d,w)->a.finish()).setPositiveButton(AppPreferences.tr(a,"同意并继续","Agree and continue"),null).create();
  dialog.show();dialog.getButton(-1).setEnabled(false);check.setOnCheckedChangeListener((b,on)->dialog.getButton(-1).setEnabled(on));dialog.getButton(-1).setOnClickListener(v->{if(!check.isChecked())return;if(a.getSharedPreferences("legal",0).edit().putBoolean("accepted_disclaimer_v1",true).putLong("accepted_at",System.currentTimeMillis()).commit()){dialog.dismiss();done.run();}});
  if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-2);
 }
}
