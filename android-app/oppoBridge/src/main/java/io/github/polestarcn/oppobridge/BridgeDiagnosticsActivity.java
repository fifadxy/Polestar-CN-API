package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** LSPosed settings entry. Integrated and standalone modules share the main app's settings. */
public final class BridgeDiagnosticsActivity extends Activity {
  @Override protected void onCreate(Bundle state){
    super.onCreate(state);
    Intent settings=new Intent().setClassName(BridgeIdentity.INTEGRATED_PACKAGE,"io.github.polestarcn.companion.OppoSystemCompatActivity");
    // Same-UID integrated settings are private. A separate module opens the exported legacy entry.
    if(!BridgeIdentity.INTEGRATED_PACKAGE.equals(getPackageName()))settings.setClassName(BridgeIdentity.INTEGRATED_PACKAGE,"io.github.polestarcn.companion.MainActivity");
    if(BridgeIdentity.INTEGRATED_PACKAGE.equals(getPackageName())){startActivity(settings);finish();return;}
    LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
    TextView note=new TextView(this);note.setText("OPPO Bridge "+BridgeBuild.VERSION_NAME+"\n模块诊断/脱敏导出/相关进程重启位于 PsAssistant → OPPO系统兼容 → LSPosed。只启用内置或独立模块中的一种。\n\n"+BridgeSettings.describe(this));page.addView(note);
    Button open=new Button(this);open.setText("打开主程序接入设置");open.setOnClickListener(v->{try{startActivity(settings);}catch(RuntimeException missing){note.setText("请安装支持系统接入设置的 PsAssistant 主程序");}});page.addView(open);setContentView(page);
  }
}
