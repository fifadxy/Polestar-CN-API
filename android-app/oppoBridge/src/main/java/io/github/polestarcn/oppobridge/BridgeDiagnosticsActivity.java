package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Direct diagnostic/control entry; usable before MyDevices discovers a card. */
public final class BridgeDiagnosticsActivity extends Activity {
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private TextView status;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    int p = (int) (18 * getResources().getDisplayMetrics().density);
    column.setPadding(p, p, p, p);
    Button panel = new Button(this);
    panel.setText("打开车辆控制面板");
    panel.setOnClickListener(v -> startActivity(new Intent(this, VehicleCardDetailActivity.class)));
    column.addView(panel);
    Button official = new Button(this);
    official.setText("打开 Polestar“我的车辆”实验绑定");
    official.setOnClickListener(v -> startActivity(new Intent(this, PolestarOfficialBindActivity.class)));
    column.addView(official);
    Button oemEnable = new Button(this);
    oemEnable.setText("启用 OEM 4×2 CAR 实验路由");
    oemEnable.setOnClickListener(v -> setOemCarExperiment(true));
    column.addView(oemEnable);
    Button oemDisable = new Button(this);
    oemDisable.setText("关闭 OEM 4×2 CAR 实验路由");
    oemDisable.setOnClickListener(v -> setOemCarExperiment(false));
    column.addView(oemDisable);
    Button refresh = new Button(this);
    refresh.setText("重新检测桥接");
    refresh.setOnClickListener(v -> diagnose());
    column.addView(refresh);
    Button baseline = new Button(this);
    baseline.setText("记录当前 Shelf / 账号基线");
    baseline.setOnClickListener(v -> runDiscoveryAction("discovery-baseline-save"));
    column.addView(baseline);
    Button compare = new Button(this);
    compare.setText("比较“我的车辆”变化");
    compare.setOnClickListener(v -> runDiscoveryAction("discovery-baseline-diff"));
    column.addView(compare);
    Button export = new Button(this);
    export.setText("一键导出 OPPO 轻量研究包（<10MB）");
    export.setOnClickListener(v -> exportResearchBundle());
    column.addView(export);
    Button reload = new Button(this);
    reload.setText("Root 重载 OPPO Hook 目标进程");
    reload.setOnClickListener(v -> confirmReloadHosts());
    column.addView(reload);
    status = new TextView(this);
    status.setTextSize(15);
    status.setTextIsSelectable(true);
    column.addView(status);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(column);
    setContentView(scroll);
    diagnose();
  }

  private void diagnose() {
    status.setText("正在检测 PsAssistant 桥接…");
    worker.execute(() -> {
      StringBuilder out = new StringBuilder("PsAssistant OPPO Bridge 1.4.0\n\n");
      try {
        Bundle host=BridgeClient.call(this,"host-status",null);
        long now=System.currentTimeMillis();
        for(String prefix:new String[]{"main_","cards_"}) {
          long seen=host.getLong(prefix+"seenAt",0);
          String module=host.getString(prefix+"moduleVersion","");
          out.append(prefix.equals("main_")?"设备空间主进程: ":"设备卡进程: ");
          if(seen==0)out.append("未收到 Hook 回报；请确认 LSPosed/Vector 作用域并重启目标进程\n");
          else {
            out.append("上次回报 ").append(new java.util.Date(seen)).append(" · ").append(host.getString(prefix+"phase",""))
                .append(" · ").append(host.getString(prefix+"reason","")).append(" · 设备空间 ")
                .append(host.getString(prefix+"hostVersion","")).append(" · 模块 ").append(module).append('\n');
            if(now-seen>10*60*1000L||!"1.4.0".equals(module))
              out.append("  ⚠ Hook 回报已过期或版本不一致；当前 Bridge 1.4.0，请重启设备空间后再判断新代码是否生效。\n");
          }
        }
        Bundle assistantHost=BridgeClient.call(this,"assistant-host-status",null);
        long assistantSeen=assistantHost.getLong("seenAt",0L);
        String assistantModule=assistantHost.getString("moduleVersion","");
        out.append("负一屏 Hook 进程: ");
        if(assistantSeen==0L) out.append("未收到当前代 Hook 回报；Shelf/SceneService 探针不会运行\n");
        else {
          out.append(new java.util.Date(assistantSeen)).append(" · ")
              .append(assistantHost.getString("phase","")).append(" · AssistantScreen ")
              .append(assistantHost.getString("hostVersion","")).append(" · 模块 ").append(assistantModule).append('\n');
          if(now-assistantSeen>10*60*1000L||!"1.4.0".equals(assistantModule))
            out.append("  ⚠ 负一屏 Hook 回报过期/版本不一致，请重启负一屏进程。\n");
        }
        out.append("回报是历史观测，不代表当前仍已启用或车辆已响应。\n\n");

        Bundle quick=BridgeClient.call(this,"quickapp-runtime-status",null);
        long quickSeen=quick.getLong("seenAt",0L);
        out.append("MyDevices QuickApp 路由: ");
        if(quickSeen==0L) out.append("尚无 widget 路由回报；打开一次 4×2/2×2 设备卡后再检测\n");
        else {
          out.append(new java.util.Date(quickSeen)).append(" · ")
              .append(quick.getString("method","")).append(" · ")
              .append(quick.getString("cardPath","")).append('\n');
          String replaced=quick.getString("replacedWidgetCode","");
          String oldCode=quick.getString("oldWidgetCode","");
          out.append("  replacedWidgetCode=").append(replaced==null||replaced.isEmpty()?"<空>":replaced).append('\n');
          out.append("  oldWidgetCode=").append(oldCode==null||oldCode.isEmpty()?"<空>":oldCode).append('\n');
          out.append("  4×2 CAR registry=").append(quick.getBoolean("single4x2CarRegistry",false))
              .append(" · eventSummary允许CAR=").append(quick.getBoolean("single4x2EventSummaryCar",false)).append('\n');
          out.append("  2×2 CAR registry=").append(quick.getBoolean("single2x2CarRegistry",false))
              .append(" · eventSummary允许CAR=").append(quick.getBoolean("single2x2EventSummaryCar",false)).append('\n');
        }
        Bundle oemState=BridgeClient.call(this,"oppo-oem-car-status",null);
        QuickAppCarCapability.Result capability=QuickAppCarCapability.detect(this);
        out.append("OEM 4×2 CAR 实验: ")
            .append(oemState.getBoolean("enabled",false)?"已启用":"未启用")
            .append(" · RPK路由安全=").append(capability.routeSafe())
            .append(" · 模式=").append(capability.mode()).append('\n');
        out.append("  registry=").append(capability.carRegistry)
            .append(" · smartHomeDefault=").append(capability.smartHomeDefaultCar)
            .append(" · eventSummary=").append(capability.eventSummaryCar)
            .append(" · ").append(capability.reason).append('\n');
        if(quick.getBoolean("oemShimApplied",false))
          out.append("  上次 QuickApp 运行时已应用 OEM CAR shim · ")
              .append(quick.getString("oemShimMode","")).append('\n');
        else if(quickSeen>0)
          out.append("  上次 QuickApp 运行时未应用 shim；NativeHomeCard 继续作为兜底。\n");
        out.append("该实验只修改 PsAssistant 自己的最终 deviceList；不会修改 MyDevices APK/RPK 文件。\n\n");

        Bundle shelf=BridgeClient.call(this,"shelf-status",null);
        long shelfSeen=shelf.getLong("seenAt",0);
        out.append("负一屏→Pantanal Shelf 探针: ");
        if(shelfSeen==0) out.append("尚无回报；打开/重启负一屏后再检测\n");
        else {
          out.append(shelf.getBoolean("ready",false)?"可读取":"未就绪")
              .append(" · ").append(new java.util.Date(shelfSeen))
              .append(" · rows=").append(shelf.getInt("rowCount",0))
              .append(" · ").append(shelf.getString("reason","")).append('\n');
          String services=shelf.getString("services","");
          if(services!=null&&!services.isEmpty())
            out.append("Shelf serviceId@score: ").append(services).append('\n');
          String candidates=shelf.getString("candidates","");
          if(candidates!=null&&!candidates.isEmpty())
            out.append("车辆候选服务: ").append(candidates).append('\n');
          String columns=shelf.getString("columns","");
          if(columns!=null&&!columns.isEmpty())
            out.append("Shelf 元数据列: ").append(columns).append('\n');
        }
        out.append("该探针只读 serviceId/包名/组件/名称元数据，不读取账号、Token 或卡片正文。\n\n");

        Bundle account=BridgeClient.call(this,"account-bind-status",null);
        long accountSeen=account.getLong("seenAt",0);
        out.append("负一屏→SceneService 账号绑定探针: ");
        if(accountSeen==0) out.append("尚无回报；进入负一屏/小布相关服务设置后再检测\n");
        else {
          out.append(account.getBoolean("ready",false)?"可读取":"未就绪")
              .append(" · ").append(new java.util.Date(accountSeen))
              .append(" · bindCount=").append(account.getInt("bindCount",0))
              .append(" · ").append(account.getString("reason","")).append('\n');
          String owner=account.getString("providerOwner","");
          if(owner!=null&&!owner.isEmpty())
            out.append("AccountSDK Provider owner: ").append(owner)
                .append(" · exported=").append(account.getBoolean("providerExported",false))
                .append(" · permission=").append(account.getString("providerPermission","")).append('\n');
          String bindActivity=account.getString("bindActivity","");
          if(bindActivity!=null&&!bindActivity.isEmpty())
            out.append("ACCOUNT_BIND target: ").append(bindActivity).append('\n');
          String dialogActivity=account.getString("dialogActivity","");
          if(dialogActivity!=null&&!dialogActivity.isEmpty())
            out.append("BINDING_DIALOG target: ").append(dialogActivity).append('\n');
          String codes=account.getString("boundCodes","");
          if(codes!=null&&!codes.isEmpty())
            out.append("已绑定 provider code: ").append(codes).append('\n');
          String catalog=account.getString("providerCatalog","");
          if(catalog!=null&&!catalog.isEmpty())
            out.append("Provider 目录(code=名称): ").append(catalog).append('\n');
        }
        out.append("账号绑定探针只保存短 provider code 和品牌显示名，不保存账号、Token、URL 或任意原始 payload。\n");
        Bundle officialState=BridgeClient.call(this,"oppo-official-bind-status",null);
        out.append("Polestar 官方入口实验: ")
            .append(officialState.getBoolean("enabled",false)?"已启用":"未启用")
            .append(" · PsAssistant登录=").append(officialState.getBoolean("signedIn",false))
            .append(" · 车辆=").append(officialState.getBoolean("vehicleAvailable",false)).append("\n\n");

        Bundle caps = BridgeClient.capabilities(this);
        int protocol = caps.getInt("protocolVersion", -1);
        boolean ready = protocol == 1 && caps.getBoolean("state", false);
        out.append("卡片开关: ").append(caps.getBoolean("cardEnabled", true) ? "开启" : "关闭，请在主程序设置启用").append('\n');
        out.append("桥接连接: ").append(ready ? "可用" : "不可用").append('\n');
        out.append("协议: ").append(protocol).append('\n');
        if (!ready) out.append("原因: ").append(caps.getString("reason", "invalid-capabilities")).append('\n');
        out.append("命令: ").append(Arrays.toString(caps.getStringArray("commands"))).append("\n\n");
        if (ready) {
          Bundle state = BridgeClient.snapshot(this);
          out.append("车辆数据: ").append(state.getBoolean("available", false) ? "可用" : "不可用").append('\n');
          if (state.getBoolean("available", false)) {
            out.append("车辆: ").append(state.getString("name", "--")).append('\n');
            out.append("SOC: ").append(state.getBoolean("batteryPercentKnown") ? state.getInt("batteryPercent") + "%" : "--").append('\n');
            out.append("门锁: ").append(state.getString("lock", "--")).append('\n');
            out.append("空调: ").append(state.getString("climate", "--")).append('\n');
          } else out.append("原因: ").append(state.getString("reason", "请先登录并刷新车辆")).append('\n');
        }
      } catch (Throwable failure) {
        out.append("检测失败: ").append(failure.getClass().getSimpleName()).append(": ").append(failure.getMessage()).append('\n');
      }
      out.append("\n安装路径\n新版 PsAssistant: 使用内置桥接接口。\n旧版 PsAssistant: 启用 API 102 模块，勾选 PsAssistant 与我的设备，重启目标应用。运行时桥接要求 Android 14 或更高版本。\n");
      out.append("\n已实现: 桥接、直接车控面板、MyDevices 17.4.15 原生首页车辆控件。\nQuickApp: 保留 OEM 数据，不修改系统 RPK；1.4.0 会自动检测并报告 converter/RPK 双重过滤与 OEM QuickApp Provider 实际调用状态。\n待真机验证: CAR=COORDINATED 后原生首页控件实际渲染、OEM QuickApp CAR 路由及车辆读回。\n负一屏静态 car setting 已确认属于车载蓝牙场景；0.9.0 现已增加 Pantanal serviceId 与 SceneService provider/绑定状态只读探针。\n小布、Watch 长续航、官方车企绑定尚未宣称完成。\n\n文字可长按选择复制。");
      runOnUiThread(() -> {
        if (!isFinishing() && !isDestroyed()) status.setText(out.toString());
      });
    });
  }

  private void setOemCarExperiment(boolean enabled) {
    status.setText(enabled?"正在启用 OEM CAR 实验路由…":"正在关闭 OEM CAR 实验路由…");
    worker.execute(() -> {
      String message;
      try {
        Bundle in=new Bundle();
        in.putBoolean("enabled",enabled);
        Bundle result=BridgeClient.call(this,"oppo-oem-car-set",in);
        QuickAppCarCapability.clearForReload();
        QuickAppCarCapability.Result capability=QuickAppCarCapability.detect(this);
        message=(result.getBoolean("accepted",false)?"设置成功":"设置失败")
            +" · routeSafe="+capability.routeSafe()
            +" · mode="+capability.mode()
            +"\n建议随后执行“Root 重载 OPPO Hook 目标进程”，再打开一次 4×2 设备卡。";
      } catch(Throwable failure) {
        message="设置失败："+failure.getClass().getSimpleName()+": "+failure.getMessage();
      }
      final String result=message;
      runOnUiThread(() -> {
        if(!isFinishing()&&!isDestroyed()) {
          status.setText(result);
          status.postDelayed(this::diagnose,1200L);
        }
      });
    });
  }

  private void confirmReloadHosts() {
    new android.app.AlertDialog.Builder(this)
        .setTitle("重新加载 OPPO Hook")
        .setMessage("将使用 root 强制停止“设备空间”和“负一屏”进程，使 LSPosed/Vector 在下次启动时加载当前 Bridge。不会修改系统 APK 或车辆数据。")
        .setNegativeButton("取消",null)
        .setPositiveButton("重载",(dialog,which)->reloadHosts())
        .show();
  }

  private void reloadHosts() {
    status.setText("正在请求 root 重载设备空间和负一屏…");
    worker.execute(() -> {
      String message;
      try {
        Process process=new ProcessBuilder("su","-c",
            "am force-stop com.heytap.mydevices; am force-stop com.coloros.assistantscreen").start();
        boolean done=process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS);
        int exit=done?process.exitValue():-1;
        if(!done)process.destroyForcibly();
        if(done&&exit==0) message="目标进程已停止。现在重新打开“设备空间”，再打开一次负一屏；Hook 回报应更新为 Bridge 1.4.0。";
        else message="root 重载未成功（exit="+exit+"）。请在 LSPosed/Vector 确认作用域后手动强停“设备空间”和“负一屏”。";
      } catch(Throwable failure) {
        message="root 重载不可用："+failure.getClass().getSimpleName()+"。请手动强停目标应用。";
      }
      final String result=message;
      runOnUiThread(() -> { if(!isFinishing()&&!isDestroyed())status.setText(result); });
    });
  }

  private void exportResearchBundle() {
    status.setText("正在生成轻量研究包…不会导出账号、Token、VIN 或原始账户数据。");
    worker.execute(() -> {
      try {
        java.io.File file=OppoResearchExport.create(this);
        Uri uri=FileProvider.getUriForFile(this,getPackageName()+".oppoexports",file);
        Intent share=new Intent(Intent.ACTION_SEND);
        share.setType("application/zip");
        share.putExtra(Intent.EXTRA_STREAM,uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        runOnUiThread(() -> {
          if(isFinishing()||isDestroyed())return;
          status.setText("研究包已生成：" + file.getName() + "\n大小 "
              + (file.length()/1024) + " KB\n可直接分享给 ChatGPT 分析。");
          startActivity(Intent.createChooser(share,"分享 OPPO 轻量研究包"));
        });
      } catch(Throwable failure) {
        runOnUiThread(() -> {
          if(!isFinishing()&&!isDestroyed())
            status.setText("研究包生成失败："+failure.getClass().getSimpleName()+": "+failure.getMessage());
        });
      }
    });
  }

  private void runDiscoveryAction(String method) {
    status.setText("正在比较 OPPO 服务元数据…");
    worker.execute(() -> {
      StringBuilder out=new StringBuilder();
      try {
        Bundle result=BridgeClient.call(this,method,null);
        long saved=result.getLong("savedAt",0L);
        if(saved==0L) {
          out.append("尚未记录基线。先打开一次负一屏后点击“记录当前 Shelf / 账号基线”。");
        } else if("discovery-baseline-save".equals(method)) {
          out.append("基线已记录: ").append(new java.util.Date(saved))
              .append("\n接下来进入负一屏“我的车辆”的添加/绑定流程，再回来点“比较变化”。");
        } else {
          out.append("基线: ").append(new java.util.Date(saved)).append('\n');
          appendDelta(out,"Shelf serviceId",
              result.getString("servicesAdded",""),result.getString("servicesRemoved",""));
          appendDelta(out,"已绑定 provider code",
              result.getString("boundCodesAdded",""),result.getString("boundCodesRemoved",""));
          appendDelta(out,"Provider 目录 code",
              result.getString("providerCatalogAdded",""),result.getString("providerCatalogRemoved",""));
          out.append("\n只比较服务/provider 标识，不比较账号、Token、URL 或卡片正文。");
        }
      } catch(Throwable failure) {
        out.append("比较失败: ").append(failure.getClass().getSimpleName())
            .append(": ").append(failure.getMessage());
      }
      runOnUiThread(() -> {
        if(!isFinishing()&&!isDestroyed())status.setText(out.toString());
      });
    });
  }

  private static void appendDelta(StringBuilder out,String title,String added,String removed) {
    out.append(title).append(": ");
    boolean changed=false;
    if(added!=null&&!added.isEmpty()) {out.append("+[").append(added).append("] ");changed=true;}
    if(removed!=null&&!removed.isEmpty()) {out.append("-[").append(removed).append("]");changed=true;}
    if(!changed)out.append("无变化");
    out.append('\n');
  }

  @Override protected void onDestroy() {
    worker.shutdownNow();
    super.onDestroy();
  }
}
