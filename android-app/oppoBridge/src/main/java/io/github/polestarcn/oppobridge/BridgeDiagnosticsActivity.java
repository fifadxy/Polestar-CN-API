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
    official.setText("打开 Polestar「我的车辆」系统入口");
    official.setOnClickListener(v -> startActivity(new Intent(this, PolestarOfficialBindActivity.class)));
    column.addView(official);
    Button officialCarLink = new Button(this);
    officialCarLink.setText("打开 OPPO CarLink 官方「我的车辆」绑定");
    officialCarLink.setOnClickListener(v -> {
      try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("card://carlink.heytap.com/control?operation=bind"))); }
      catch (RuntimeException unavailable) { status.setText("OPPO CarLink 绑定入口不可用：" + unavailable.getClass().getSimpleName()); }
    });
    column.addView(officialCarLink);
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
      StringBuilder out = new StringBuilder("PsAssistant OPPO Bridge 1.9.0\n\n");
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
            if(now-seen>10*60*1000L||!"1.9.0".equals(module))
              out.append("  ⚠ Hook 回报已过期或版本不一致；当前 Bridge 1.9.0，请重启设备空间后再判断新代码是否生效。\n");
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
          if(now-assistantSeen>10*60*1000L||!"1.9.0".equals(assistantModule))
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
        Bundle carLink=BridgeClient.call(this,"oppo-carlink-status",null);
        out.append("OPPO CarLink 官方通道: ")
            .append(carLink.getBoolean("installed",false)?"已安装":"未检测到")
            .append(" · ").append(carLink.getString("versionName",""))
            .append(" · OCS service=").append(carLink.getBoolean("serviceAvailable",false))
            .append(" · CarAppProvider=").append(carLink.getBoolean("providerAvailable",false))
            .append(" · 绑定入口=").append(carLink.getBoolean("bindRouteAvailable",false)).append('\n');
        out.append("  官方路线需要 PsAssistant 自己的 OPPO 开发者授权；Hook/OEM CAR 仅作为兼容研究路线。\n\n");
        Bundle carLinkPreview=BridgeClient.call(this,"oppo-carlink-preview",null);
        out.append("CarStatus 映射预览: ");
        if(!carLinkPreview.getBoolean("available",false))out.append("暂无车辆快照\n");
        else {
          if(carLinkPreview.getBoolean("batteryKnown",false))out.append("SOC=").append(Math.round(carLinkPreview.getFloat("batteryLeftRatio",0f)*100f)).append("% / ratio=").append(carLinkPreview.getFloat("batteryLeftRatio",0f));
          else out.append("SOC=未知");
          if(carLinkPreview.getBoolean("batteryRangeKnown",false))out.append(" · 续航=").append(carLinkPreview.getFloat("batteryEnduranceMileage",0f)).append("km");
          if(carLinkPreview.getBoolean("odometerKnown",false))out.append(" · 里程=").append(carLinkPreview.getFloat("totalMileage",0f)).append("km");
          out.append(" · charging=").append(carLinkPreview.getBoolean("charging",false)).append('\n');
        }

        Bundle systemVehicle=BridgeClient.call(this,"system-vehicle-status",null);
        out.append("Polestar 品牌入口: ")
            .append(systemVehicle.getBoolean("providerEnabled",false)?"已启用":"未启用")
            .append(" · Provider目录=").append(systemVehicle.getBoolean("providerCatalogContainsPolestar",false)?"已出现":"未出现")
            .append(" · AssistantHook=").append(systemVehicle.getBoolean("assistantHookSeen",false))
            .append(" · CarLinkHook=").append(systemVehicle.getBoolean("carLinkHostHookSeen",false)).append('\n');
        out.append("  真实状态: 登录=").append(systemVehicle.getBoolean("signedIn",false))
            .append(" · 车辆=").append(systemVehicle.getBoolean("vehicleAvailable",false))
            .append(" · 远程控车=").append(systemVehicle.getBoolean("remoteControlAvailable",false))
            .append(" · PAK状态已知=").append(systemVehicle.getBoolean("pakStateKnown",false)).append('\n');

        Bundle keySurface=BridgeClient.call(this,"oppo-car-key-status",null);
        out.append("OPPO 蓝牙车钥匙框架: bindHandler=").append(keySurface.getInt("bindHandlerCount",0))
            .append(" · Wallet入口=").append(keySurface.getBoolean("walletDetailSurfaceAvailable",false))
            .append(" · BLE=").append(keySurface.getBoolean("systemBluetoothLe",false)).append('\n');
        out.append("  数字钥匙不会由 Hook 伪造；只有真实 Polestar PAK 配对后才可注册 OPPO Key。\n");

        Bundle carLinkHost=BridgeClient.call(this,"carlink-host-status",null);
        long carLinkSeen=carLinkHost.getLong("seenAt",0L);
        out.append("CarLink LSPosed 宿主: ");
        if(carLinkSeen<=0)out.append("未观察到 com.heytap.opluscarlink 进程加载模块\n");
        else out.append(carLinkHost.getBoolean("ready",false)?"已加载":"未就绪")
            .append(" · host=").append(carLinkHost.getString("hostVersion",""))
            .append(" · module=").append(carLinkHost.getString("moduleVersion",""))
            .append(" · process=").append(carLinkHost.getString("process","")).append('\n');
        String carLinkClasses=carLinkHost.getString("classes","");
        if(carLinkClasses!=null&&!carLinkClasses.isEmpty())out.append("  OCS classes: ").append(carLinkClasses).append('\n');

        Bundle surfaces=BridgeClient.call(this,"vehicle-surface-status",null);
        for(String prefix:new String[]{"carlink","wallet","healthWallet","digitalKey"}) {
          long seen=surfaces.getLong(prefix+"_seenAt",0L);
          out.append("  ").append(prefix).append(": ");
          if(seen==0L)out.append("未观察到 Hook 宿主\n");
          else out.append(surfaces.getString(prefix+"_versionName",""))
              .append(" · ready=").append(surfaces.getBoolean(prefix+"_ready",false))
              .append(" · components=").append(surfaces.getString(prefix+"_components","")).append('\n');
        }
        out.append('\n');

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
        Bundle shelfCard=BridgeClient.call(this,"oppo-shelf-card-status",null);
        Bundle shelfPreview=BridgeClient.call(this,"oppo-shelf-card-preview",null);
        Bundle shelfHost=BridgeClient.call(this,"shelf-host-status",null);
        out.append("OPPO 负一屏插件卡（CardWidget/Seedling）: host=")
            .append(shelfCard.getBoolean("cardHostVisible",false))
            .append(" · CardWidget=").append(shelfCard.getBoolean("cardWidgetRuntimeSupported",false))
            .append(" SDK=").append(shelfCard.getString("cardWidgetSdkVersionCode",""))
            .append(" · UIEngine=").append(shelfCard.getString("uiEngineVersion",""))
            .append(" · CardService=").append(shelfCard.getBoolean("cardServiceInstalled",false))
            .append(" · metadata=").append(shelfCard.getBoolean("shelfMetadataReady",false))
            .append(" · PantanalHook=").append(shelfCard.getBoolean("pantanalHookSeen",false))
            .append(" · Pantanal=").append(shelfCard.getString("pantanalVersion","")).append('\n');
        long pantanalSeen=shelfHost.getLong("pantanal_seenAt",0L);
        if(pantanalSeen>0)out.append("  Pantanal components: ").append(shelfHost.getString("pantanal_components","")).append('\n');
        long assistantShelfSeen=shelfHost.getLong("assistant_seenAt",0L);
        if(assistantShelfSeen>0)out.append("  AssistantScreen card components: ").append(shelfHost.getString("assistant_components","")).append('\n');
        out.append("  Card data: ");
        if(!shelfPreview.getBoolean("available",false))out.append("暂无车辆快照\n");
        else {
          if(shelfPreview.getBoolean("socKnown",false))out.append("SOC=").append(shelfPreview.getInt("socPercent",0)).append("% ");
          if(shelfPreview.getBoolean("rangeKnown",false))out.append("range=").append(shelfPreview.getInt("rangeKm",0)).append("km ");
          out.append("charging=").append(shelfPreview.getBoolean("charging",false)).append(" · lock=").append(shelfPreview.getString("lockLabel","")).append('\n');
        }
        out.append("  已按官方 CardWidget 1.2.3 合同识别 AssistantScreen support flag、SmartEngine/UIEngine 与 CardService；正式卡片发布仍需 OPPO 模板/事件配置与可能的白名单。\n\n");

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
      out.append("\n1.9.0 已实现: Polestar Provider 多入口注入、绑定入口转发、AssistantScreen/CarLink/Wallet 宿主探测、真实 PsAssistant 车辆状态映射，以及 MyDevices 原生卡片兼容。\n数字钥匙仍保持真实 PAK/OPPO Key 状态分离，不修改 isBluetoothKeyExist 或车辆验证结果。\n若品牌仍未显示，先确认 Provider 目录是否出现 POLESTAR，再核对 AssistantScreen 宿主版本和 Hook 回报。\n\n文字可长按选择复制。");
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
        .setMessage("将使用 root 强制停止“设备空间”、“负一屏”和 CarLink进程，使 LSPosed/Vector 在下次启动时加载当前 Bridge。不会修改系统 APK 或车辆数据。")
        .setNegativeButton("取消",null)
        .setPositiveButton("重载",(dialog,which)->reloadHosts())
        .show();
  }

  private void reloadHosts() {
    status.setText("正在请求 root 重载设备空间、负一屏和 CarLink…");
    worker.execute(() -> {
      String message;
      try {
        Process process=new ProcessBuilder("su","-c",
            "am force-stop com.heytap.mydevices; am force-stop com.coloros.assistantscreen; am force-stop com.heytap.opluscarlink").start();
        boolean done=process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS);
        int exit=done?process.exitValue():-1;
        if(!done)process.destroyForcibly();
        if(done&&exit==0) message="目标进程已停止。现在重新打开“设备空间”，再打开一次负一屏；Hook 回报应更新为 Bridge 1.9.0。";
        else message="root 重载未成功（exit="+exit+"）。请在 LSPosed/Vector 确认作用域后手动强停“设备空间”、“负一屏”和 CarLink。";
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