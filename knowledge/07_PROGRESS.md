## 2026-10-27 / v0.8.2-beta1 — VoLike 参考布局收敛

- `REFERENCE_LAYOUT`：继续基于 v0.8.1-beta1 / Run #104 成功基线，不改 China API、远控、安全确认、停车 LBS 和 Widget 逻辑。VoLike 竖屏从“车辆大卡 + 下方四控制”改为用户参考图的“顶部能源条 + 左侧纵向圆形控制 + 中间 Polestar 2 顶视车辆”。
- `DETAIL_RAIL`：VoLike 详细 / 极星配色新增右侧信息轨，直接显示保养时间剩余%、里程剩余%、车内温度和停车位置入口；保养百分比仍来自 ServiceReading 当前真实/本地配置逻辑，不伪造车辆百分比。
- `SIMPLE_LABELS`：`volike_simple_labels` 继续只影响 VoLike 简洁主控的文字；关闭后图标、点击热区、contentDescription 和 BIOMETRIC_STRONG 车控链保持不变。
- `PREVIEW_SYNC`：`SkinPreviewView` 与实际新版式同步，VoLike 预览改成纵向控制、顶视车辆和详细信息轨；NIO 两套预览不改变。
- `LANDSCAPE_GUARD`：640×320 及大系统字号继续保留 v0.8.1 已通过的横屏结构，避免为了视觉一致重新引入裁切。
- `WATCH_SCOPE`：手表 Volvo/Psa 仍为手机内可切换布局预览；独立 Wear OS APK 不在本手机 APK 中冒充完成。
- `VERSION`：`versionName=0.8.2-beta1`, `versionCode=83`。

## 2026-10-27 / v0.8.1-beta1 — VoLike 极星配色、可切换 Widget、皮肤缩略预览

- `SOURCE_BASE`：从 `0.8.0-beta1` / commit `7f562261` 连续演进；其 CI Run #96 未通过，继续修复充电字串在 1.56× 字体下的真实行高问题，不通过删测试规避。
- `FIVE_UI_SKINS`：VoLike 简洁、VoLike 详细、VoLike 极星配色、NIOlike、NIOlike2；手机 UI 版本选择独立保存。极星配色复用详细布局但改用 Polestar 黑白灰与橙色点缀。
- `SKIN_PREVIEW`：使用实际车辆资源的 `SkinPreviewView` 在选择器内渲染每个皮肤的布局缩略图，点击卡片选择并应用；全部皮肤日/夜复用原车图资源。
- `SIMPLE_LABELS`：设置 `volike_simple_labels`，默认 ON；OFF 后 4 个 VoLike 简洁主控仅显示圆形图标，`contentDescription` 仍提供具体动作与状态。
- `WIDGET_DESIGN`：`widget_design` 与手机 `ui_skin` 独立：`classic`、`volike_simple`、`volike_detailed`、`volike_polestar`。使用 `VoLikeWidgetRenderer` + 纯 RemoteViews 布局，不依赖地图 SDK/WebView/运行时网络图片。门锁/空调仍走原生物识别安全链；充电为只读状态，定位打开现有安全的停车位置详情。
- `CHARGING_MOTION`：仅 `ChargingVisibility.shouldShow(snapshot)` 返回真时展示 `ChargingPulseView`；非充电隐藏并取消动画，显示大小独立适配系统大字号。
- `WATCH_SCOPE`：已有 `WatchPreviewView` 支持 Volvo/Psa 预览；不谎称独立 WearOS 程序已交付。
- `VERSION`：`versionName=0.8.1-beta1`, `versionCode=82`。

## 2026-10-26 / v0.7.5-beta1 — 车辆信息二级化、Health 聚合与动态密度 VI

- `PRIMARY_IDENTITY`：两套控制页主标题只显示车型；年份与 VIN 从主界面移除，点击车型进入二级“车辆详情”查看完整车型、年款和 VIN，再从该菜单切换车辆。
- `POLESTAR_2_ART`：控制页 Polestar 2 主视觉统一放大约 8%，继续沿用白天原色 / 夜间黑色车身处理。
- `HEALTH_CROSS_CHECK`：对上游 Hisingen commit `5301353` 的 `HealthService/GetHealth` 源码重新逐字段核对：washer fluid=field 13（>1 告警）、low-voltage/12V=field 38（2 告警）、tyre warning=fields 9–12；全球实现明确玻璃水是告警状态，不是液位值。
- `CHINA_BOUNDARY`：中国 Android 分支已有 `HealthService/GetHealth` 只读调用；本轮扩展为同一次 Health 返回同时解析 12V、玻璃水和胎压告警。官方 China App 的历史可访问证据只定位到 `com.polestar.carcontrol / CarMainFragment` 车辆状态 UI，尚未直接确认 HealthService 字段，因此 China 语义仍标记为待授权实车验证。
- `VEHICLE_STATUS`：主界面取消独立胎压按钮，改为统一“车辆状态”按钮；12V、玻璃水、胎压告警在二级状态页聚合。四轮 iTPMS 详情默认隐藏，设置内提供“实验性 iTPMS 四轮状态（待修复）”开关。
- `DYNAMIC_METRICS`：车辆控制页自选数据上限由 6 提升到 12；1–4 项采用 2 列、5–6 项 3 列、7–12 项 4 列，字号/行高随密度自动收缩，保持竖屏/横屏单页无滚动。
- `AMBIENT_VI`：原 3dp 细线升级为约 20dp 的整体橙色环境光带，3.6 秒低频位移+呼吸，仅重绘光带自身区域；日间降低透明度、夜间增强层次，按钮按压回弹继续保留。
- `VERSION`：Android 提升至 `0.7.5-beta1` / versionCode 75。

## 2026-10-26 / v0.7.4-beta1 — 两套皮肤、详细风格重排、动态 VI 与车辆空调剩余时间

- `UI_SKINS`：删除“最新模板”皮肤入口；现仅保留 **简洁风格** 与 **详细风格**。旧 `latest/ev_concept` 自动迁移到简洁风格，旧 `current` 自动迁移到详细风格。
- `DETAILED_LAYOUT`：详细风格完整显示车辆 `车型 · 年款 · VIN 后4位`；车窗、门窗、胎压、露营、鸣笛/闪灯全部移动到车辆图片下方；四个核心远控保持单屏显示。
- `CONTROL_DATA`：设置中的数据显示改为“车辆控制页数据显示 · 最多 6 项”，简洁/详细两种控制页使用同一用户选择，保证切换皮肤后数据项一致。
- `LANGUAGE`：移除各页面右上角语言切换按钮；语言仅在设置菜单中修改。
- `MOTION_VI`：所有通过统一 click helper 的交互控件加入轻量缩放/透明度回弹；两套车辆控制页加入独立小区域的 Polestar Orange 循环呼吸光带，避免全屏高频重绘。
- `CLIMATE_REFERENCE_VERIFIED`：继续拆解当前参考 APK DEX，确认 ParkingClimatization：field 2=状态、field 3=剩余运行分钟、field 7=车内温度、field 8=目标温度；参考 UI 仅在运行分钟 > 0 时显示“剩余”，否则显示“剩余时间暂无”。
- `CLIMATE_RUNTIME`：本项目现在优先显示车辆返回的 field 3；只有 field 3 缺失且本机刚确认启动空调时才显示带“估算/~”标记的本地 30 分钟回退，不再把本地计时当成车辆真实剩余时间。
- `VERSION`：Android 提升至 `0.7.4-beta1` / versionCode 74。

## 2026-10-26 / v0.7.3-beta1 — 三套日夜模式、单屏控制页、iTPMS 与 Polestar VI

- `DAY_NIGHT`：三套 UI 皮肤统一支持 System / Light / Dark；取消“此刻图片风格强制浅色”的旧限制。
- `FIXED_CONTROL_PAGE`：三套车辆控制页均改为单屏固定布局，不依赖上下滚动查看控制项；保留 v0.7.2 的顶部下拉刷新手势作为唯一页面滑动操作。
- `POLESTAR_2_ART`：此刻图片皮肤改回项目原始 Polestar 2 顶视车辆素材；所有 Polestar 车辆素材在夜间通过暗色矩阵呈现黑色车身，并保留轮廓高光。
- `ITPMS_RECHECK`：对当前参考模板 APK 再做静态核对，仅发现 Battery / Exterior / Odometer / ParkingClimatization / PreCleaning 五类 `services.vehiclestates.*` 服务；**未发现 HealthService/GetHealth、TPMS/iTPMS 或四轮 kPa 的第一方调用字符串**。因此模板 APK 不能作为“具体胎压数值”证据。
- `TYRE_UI`：胎压 UI 改为 iTPMS 告警状态优先，四轮只显示 正常 / 偏低 / 严重偏低 / 偏高 / 未知；不再在用户界面显示未验证的 kPa。ALL GOOD 只有四轮告警字段全部明确为 OK 时才出现。
- `VI_CONTROLS`：车辆电量进度条、露营模式、Widget 外观、触感强度等所有 SeekBar/ProgressBar 统一使用 Polestar 黑/白/灰 + 橙色强调的 VI 色彩。
- `VERSION`：Android 提升至 `0.7.3-beta1` / versionCode 73；CI test/lint/assemble 结果以本轮交付记录为准。

## 2026-10-26 / v0.7.2-beta1 — 控制首页模板式下拉刷新

- \`REFERENCE_APK_CONFIRMED\`：重新核对用户提供模板 APK，确认包含 \`androidx.swiperefreshlayout:swiperefreshlayout:1.1.0\`，DEX 中存在 \`MainActivity$HorizontalSwipeRefreshLayout\`、\`SwipeRefreshLayout$OnRefreshListener\`、\`setOnRefreshListener\` 与 \`setRefreshing\`。
- \`PULL_TO_REFRESH\`：三套控制首页全部加入顶部下拉刷新；只调用现有 \`VehicleCoordinator.refresh()\`，因此复用 China Session、车辆发现、Battery/Range/Odometer/Exterior/Health 等既有读取链路。
- \`GESTURE_GUARD\`：新增 \`HorizontalSwipeRefreshLayout\`，明显横向手势不被刷新容器拦截；纵向下拉保持模板式刷新体验。
- \`TOP_ONLY\`：对嵌套 ScrollView 递归检测 \`canScrollVertically(-1)\`，只有页面已经滚动到顶部时才允许触发下拉刷新。
- \`STATE\`：车辆读取刷新期间显示刷新指示器；远程控制命令 busy 不伪装成“正在刷新”；busy 状态下再次下拉不会重复叠加网络请求。
- \`VERSION\`：Android 版本提升至 \`0.7.2-beta1\` / versionCode 72；CI test/lint/assemble 结果以本轮交付记录为准。

## 2026-10-26 / v0.7.1-beta1 — 三套 UI 皮肤与模板 APK 兼容校验修复

- \`UI_SKINS\`：设置新增三套可持久化皮肤：① 最新模板（新版 Polestar 方向）；② 当前 v0.7 布局原样保留；③ 按用户提供 EV 截图重做的浅色大续航/车辆主视觉/圆形四车控/状态条/充电与露营卡片。
- \`FEATURE_PRESERVATION\`：新皮肤继续保留门锁、空调、净化、尾箱、车窗、门窗详情、胎压、露营以及鸣笛/闪灯入口，不改变 China Session、Biometric 强校验或远控协议。
- \`REFERENCE_IMPORT_FIX\`：定位到现用模板 APK 文件 SHA-256 为 \`1e817e29c97ea21b3ed1b78b81325f01c116446d126ee85a4b84a4b3fcb1a031\`，不同于历史样本，但 APK v2 签名完整、证书 SHA-256 仍为 \`a64bcf2456d3a2e920a7620e899e7da74df7bcc39d53f517f43461eaf1166bc7\`。导入校验改为包名 + 已验证签名证书身份，不再把“整包字节完全相同”误当成唯一合法条件。
- \`SECURITY_BOUNDARY\`：仍拒绝包名错误、签名证书不匹配、DEX 兼容材料定位异常的 APK；不会因为本次兼容修复接受第三方重签名包。
- \`BUILD\`：版本提升至 v0.7.1-beta1；本轮 GitHub Actions test/lint/assemble 结果以交付记录为准。

## 2026-10-26 / v0.7.0-beta1 — 胎压、保养与 Widget 自适应

- `HEALTH_SOURCE_VERIFIED`：对照 Mac China KB 与上游 Hisingen，确认 HealthService/GetHealth 的四轮报警字段 9–12、压力字段 39–42 kPa。
- `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`：Android 新增 TireSnapshot、Health 读取、四轮胎压位置图；正常显示 ALL GOOD，异常橙色 + !。
- `MAINTENANCE`：默认 365 天 / 20,000 km；按车辆保存上次保养日期/里程；提供剩余时间、剩余里程及两个剩余百分比。
- `WIDGET`：默认透明风格；容量扩展为 4×2=8、2×2=4、4×1=4、2×1=2；字体随数据量缩放；新增小字具体更新时间；移除“旧 / Old”。
- `REMOTE`：车窗与 HonkFlash 入口已接入；瞬时鸣笛/闪灯不伪造车辆持续状态确认。
- `BUILD`：版本提升至 v0.7.0-beta1；等待本轮 CI 完整 test/lint/assemble 结果后更新最终 APK 校验值。

# 07｜Android 分支增量研究与开发进度

格式：`日期 | 阶段 | 发现/实现 | 证据 | 状态 | 下一步`。每次分析追加，不覆盖过去状态。

## 2026-10-26 / v0.4.0-beta1 — 真实登录入口、指纹车控、双语设计与常驻通知

- `USER_CONFIRMED`：区域弹窗移除，固定中国区手机号+密码真实登录；登录后默认车控首页；中英文、日夜模式、英文 Widget；静默常驻通知显示电量、充电、空调/净化和 Token 最近签发/刷新时间。
- `IMPLEMENTED`：登录/车辆/控制统一 VehicleCoordinator，共享单队列和复用的 gRPC channel；模板可一次性 SAF 导入并本机 Keystore 加密，避免强制安装模板依赖；生产页面不再显示模拟数据。
- `REMOTE`：系统 BIOMETRIC_STRONG 门槛覆盖门锁、尾箱、空调和净化；一条写指令之后最多四次只读回读；支持 Cancel/Failure/Unknown，不自动重发写操作。
- `BACKGROUND`：默认关闭的 specialUse 前台通知，LOW 静默保留图标；JobScheduler 约三十分钟批量读取，未使用 WakeLock；开机/覆盖安装恢复已启用选项；锁屏不展示详细状态。
- `DESIGN`：按用户 PDF 提取官方公共符号，保留明暗比例；原模板 P2 图压缩 WebP；Lato 开源英文/数字与系统中文回退（未声称为 Polestar 专用字库）；小屏/横屏弹性控制布局。
- `LOCAL_VERIFIED`：JDK17 + Gradle8.7 + SDK35；testDebugUnitTest/assembleDebug/lintDebug 通过；10 个 Robolectric 回归测试零失败（Android26/28、首次启动、320×568/360×640/640×320、深色中文、指纹失败/取消不发送、缓存、通知、导入校验）；Android Lint 0 errors（兼容与样式建议 warning 保留）；纯 Java Proto/状态模型、知识库与源码检查均通过。
- `APK_VERIFIED`：5,017,338 字节，SHA256 `7ca6938ec456929cea4909237fcb85709a828a5bb529d4b993642c8d6f89a6e7`；ZIP 完整性、aapt 包信息及 APK v2 签名通过；签名证书 SHA256 与已交付 alpha2 完全一致，可覆盖同签名旧版。
- `LIMITS`：Robolectric 使用测试车况 fixture，仅验证本机 UI/逻辑；云环境没有 USB/KVM 与用户车云账号，不表示真实登录、尾箱/空调/净化车端执行或指纹硬件已实测。Global/Live/PCCS/露营自动化保持未实现。使用说明 docs/ANDROID_V04.md。

## 2026-10-26 / Phase 2 — v0.3.0-alpha2 协议纠偏与可安装 APK

- `PROTOCOL_FIX`：按参考 APK 方法级证据修正公共参数签名为 `salt + canonical + salt`、`systemVer=Build.DISPLAY`、嵌套 `globleToken` 登录/刷新解析、C3 discovery v1 Accept、状态 RPC VIN field 2 与里程米→公里换算。
- `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`：Battery/Range/Charging、Odometer、Exterior 门窗/尾门/锁状态映射；多车选择；China 实车快照接入首页和标准/紧凑 Widget。
- `REMOTE_STATE`：China 空调启动/停止与解锁进入手动确认通道；UI 区分 `SENDING`、`SERVER_ACCEPTED`、`FAILED`、`UNCONFIRMED`；写操作不自动重试。
- `SESSION`：继续使用 Android Keystore AES-GCM 保存 Token；显示缓存不保存完整 VIN 或凭证，恢复后标 `CACHED`。
- `LOCAL_BUILD_VERIFIED`：JDK 17 + Gradle 8.7 + Android SDK 35 执行 `assembleDebug` 成功；交付 APK 2.6 MiB，SHA256 `1398f074cf3ea7527c23134d20db12675762766600be9c32d7175cc6cf14710f`；ZIP 完整性及 APK v2 签名通过。
- `CI_VERIFIED`：功能提交 `9a84b829d6287ba2c5871891b579f560ee7a2ee6` 已推送至 `android-polestar-assistant-main`；artifact 工作流提交 `d5aa902c71981f18b3f1eee28e2478cfbda529d4` 的 Actions run [`40268035481`](https://github.com/fifadxy/Polestar-CN-API/actions/runs/40268035481) 已通过模型/安全门、SDK 35、Debug APK 构建、产物检查和上传，artifact 名为 `Polestar-Assistant-CN-v0.3.0-alpha2-debug`。
- `DEVICE_LIMITATION`：当前执行环境无 USB/KVM，无法完成 T01–T12 真机/模拟器验证；China 能力保持 `DEVICE_VERIFICATION`，Global/C3 Live/PCCS/后台 Worker 仍未实现。

## 2026-10-26 / Phase 0 — 参考 APK 入库

- `APK_CONFIRMED`：样本 SHA256 `8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735`；4,856,483 字节；526 ZIP 成员；两份 DEX；包名 `app.c3garage.companion`。
- `APK_CONFIRMED`：识别主 UI、PolestarApi、Widget、WidgetRefreshWorker、CampModeWorker；7 个核心 Java 源文件名。
- `APK_CONFIRMED`：确认中国区网关 / 密码登录 / Global Token 刷新路径及 5 类 C3 状态服务。
- `PROJECT_DOC`：对照现有 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`，China C3 主路径与 Global 双 Token 逻辑不得混用。
- `DECISION`：R8/混淆/安全加固后期评估，本阶段 DEFERRED；真实远程命令全部禁用。

## 2026-10-26 / Phase 1 — 新 Android 工程

- `IMPLEMENTED_DEMO`：原生 Java Android 项目结构，选择 China / Global，区域独立持久化。
- `IMPLEMENTED_DEMO`：车辆看板、模拟 SOC/Range/Odometer、车身状态草图、车辆列表入口。
- `IMPLEMENTED_DEMO`：两种原生 Widget、模拟数据刷新、操作反馈与可复制日志。
- `IMPLEMENTED_DEMO`：真实登录、网络读取、C3 stream、PCCS 与所有远控均标注未验证/未开放。
- `PREPARED`：Gradle 8.7 / AGP 8.5.2 工程和 GitHub Actions Debug 编译配置。
- `BUILD_LIMITATION`：本地当前环境缺 Android SDK/Gradle/ADB 与可用 DNS 下载；本地无法编译 APK；CI 状态需独立检查。

## 下次更新需附

- Git 分支名称与 commit SHA；CI run 及 APK checksum（若构建成功）。
- 真机上各 UI/Widget 页面截图与交互问题清单；不能仅使用静态方法名推断。
- 每个新 C3 RPC 的来源、方法、请求/响应字段、测试车型与验证级别（脱敏）。

## 2026-10-26 / Phase 1.1 — 双报告交叉融合与 CI 维护

- `APK_CONFIRMED`：原始 APK v2 RSA 签名与文件内容摘要独立通过，证书 SHA256 已核对；仅证实完整性。
- `APK_CONFIRMED`：`PolestarApi$Vehicle` 共 **33 个字段**；生成 `evidence/apk-vehicle-schema.json` 与字典，纠正报告 B 的 30 个字段。
- `SOURCE_REVIEW`：分析 A/B 已逐项归纳到 `10_TWO_REPORTS_RECONCILED.md`；未见 Gemini 链接正文不计为证据；流式 API 与后台定时仍区分静态/动态。
- `NEW_PROJECT_CODE`：新增 `TelemetryStatus`, `OperationState`、`ModelStateTest`；演示数据采集时间为 0，按钮反馈无真实车辆命令。
- `BUILD_FIX`：针对两次 GitHub CI SDK 初始化失败改成内置 cmdline-tools 定位；**新 CI 状态待运行确认**。
- `DECISION`：R8、混淆、安全加固、真实远控全部延期；继续独立 Android 分支，不覆盖 Hisingen Mac。

本轮具体 commit SHA、build run、APK 产物以 GitHub Actions 页面及最终交付说明为准。

## 2026-10-26 / Phase 1.2 — Android Debug 编译完成

- `CI_VERIFIED`：Actions Run [`40261229408`](https://github.com/fifadxy/Polestar-CN-API/actions/runs/40261229408) 成功，构建 commit `5f3afc415acfe4ffc2d33fd8613511e51019b7fb`。
- `BUILD_PASS`：成功生成 `app-debug.apk`（28,951 字节，SHA256 `0e8d417cd6354ecf55ac414492a466ce10d02778e40eb59aa850bdbac0767a40`）；人工解包、zip 完整性检查、v2 签名内容摘要检查均通过。
- `NOT_YET_TESTED`：Android 模拟器/真机 UI、Widget、账户登录、中国区车辆 API、真实控制；不可从编译成功推断行为可用。
- 新增文档仅补成功记录和签名读取的准确算法位数标注，**不改变参与上述 CI 的 Android 应用源代码**。R8 与后期安全工作仍 `DEFERRED`。

## 2026-10-26 / Android Assistant Parity V3.0：主线创建与计划/知识库同步

- `USER_EVIDENCE`：用户明确报告参考 `app-release.apk.1.1(1).1` 的中国区登录、SOC、累计里程、空调控制与车门解锁实际可用，且长期使用可能弹 Token 错误但车控仍有效；标签 **`REFERENCE_USER_VERIFIED`**，非本工程设备实测。
- `PLAN_UPDATED`：将远程控制从原有后期研究调整为 V3 核心 P3，先复现参考 APK 可用的空调/解锁；Token 误报列 P1 核心诊断；P4/P5 原版 UI/Widget；P6 Global/Live/PCCS。
- `BRANCH_DECISION`：新建 `android-polestar-assistant-main` 作为 Android 子项目长期主线；保留历史 `feature/android-companion-v0.1` 和仓库原默认 `main`，Mac/Hisingen 不改。
- `KNOWLEDGE_SYNC`：新建 `13_REFERENCE_APK_FIELD_VALIDATION.md`、`14_ANDROID_PARITY_V3_MASTER_PLAN.md`、`15_TOKEN_AND_REMOTE_VALIDATION.md`、`16_ANDROID_BRANCH_GOVERNANCE.md`，并更新 INDEX/API/FEATURE/ROADMAP/VALIDATION/README。
- `CODE_STATUS_UNCHANGED`：`v0.1.0-dev2` 仍为 DEMO/OFFLINE，未加入真实认证/API/远控；既有成功构建 run `40261229408` 是旧版记录。V3 为计划版本，非新 APK 发布。
- `SCOPE`：R8、混淆、非必要安全加固仍延期；真实远控后续在授权、确认与逐项验证下实施。


## 2026-10-26 / Phase 2 — v0.2.0-alpha1 China 实验实车链路

- `P0_METHOD_LEVEL`：直接复核参考 APK DEX 方法体，确认 China 登录请求体、signed URL 参数/排序/MD5 流程、C3 discovery、车辆发现、Battery/Odometer/Exterior RPC，以及 `invocation.InvocationService` 的空调和解锁请求构造。
- `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`：新增 `ChinaPolestarApi`、原始 Protobuf codec、China 登录/refresh、C3 host + vehicle discovery、SOC/Range/Odometer 基础读取、Exterior 锁状态尝试读取。
- `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`：新增空调启动/停止、车门解锁；全部需要用户主动确认，网络结果只标“服务器接受/失败/未确认”，不自动重复高影响写请求。
- `SESSION_SECURITY`：China Session 使用 Android Keystore AES-GCM 加密持久化；密码不持久化；诊断日志不写 Token、手机号、完整 VIN。
- `COMPATIBILITY_BRIDGE`：参考 APK 的固定兼容签名材料不提交仓库。alpha1 仅在设备端发现包 `app.c3garage.companion` 且 SHA-256 与已验证样本完全一致时，从其 `classes.dex` 运行时定位材料；仅保存在内存。
- `CI_VERIFIED`：GitHub Actions run [40265773414](https://github.com/fifadxy/Polestar-CN-API/actions/runs/40265773414) 全部通过；head `bcbfaa24e4d81dada749bc3a02d470e9992dcfb0`；artifact `polestar-android-companion-v0.2.0-alpha1-debug`。
- `BUILD_PASS`：Actions artifact ZIP digest `sha256:81bfd7f3ed628c2711eca906e16e2a8eaf42ee96bd7a36174629b626518d1a08`；解包后的 APK SHA-256 `eac3d5118ed715ab7d5e739fe55b6675472cee4783cda37254cb23e218673a6e`。
- `NOT_PROJECT_DEVICE_VERIFIED`：本次只证明源码门禁和 APK 编译成功；China 登录、真实车单、SOC/里程及空调/解锁仍需在用户本人授权车辆上逐项实测，不能从参考 APK 的成功外推为本 APK 已成功。
- `REMAINS`：P4 原版 UI、真实 Widget、完整 Exterior/车窗/尾门/鸣笛闪灯/净化、命令车辆侧回读确认、Token 误报长时测试、C3 Live、PCCS、Global 仍未完成。


## 2026-10-26 v0.7.6-beta1

v0.7.5 Actions 40348312346 完成并上传 artifact 12661514910 后，以 fa81ac5 为基线开始修复。完成一页自适应布局、主控制状态/配色、空调展示倒计时、最小 Widget 更新时间和字体、局部更新、Health 首条流式读取和缓存、车辆列表缓存及动画生命周期。同步 Psa_v076_Update.md 与 knowledge/23。保留远控验证/延迟回读、不重发与露营阈值。Release 使用保留签名；旧 CI APK 与该签名不同，覆盖升级需原签名。


## 2026-10-27 · v0.7.7-beta1

新增设置可切换 NIOlike 日夜皮肤；移除动态光效；车图下方 Health 汇总与可隐藏胎压；充电仅在启动/进行时独立分组；SOC 横条与旁侧续航、系统字体和 Widget 同步。Debug/Preview 各 102 项项目回归、Lint 与 Release/Preview 编译通过，APK v2 签名验证通过；账号/实车/手表与耗电未实测。详见 [本版知识库](24_V077_NIOLIKE_VI_CHARGING.md) 和 [更新说明](../docs/Psa_v077_Update.md)。


## 2026-10-27 v0.7.8 当前增量

从 v0.7.7/e28f857 迭代；交叉核对最新 China 参考和 Mac f50820b，新增完整车况/保养、四套皮肤字体与位置二级预览。TA 不再使用可能为容量的 field12 独立读数；可印证则显示，无证据仍 --。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。


## 2026-10-27 v0.7.9 免 Key 地图增量

当前位置方案改为用户明确选择的内嵌高德官方 URI 网页，移除 staticmap Key 依赖并清理旧配置。参考 APK 仍证明 field1=经度、field2=纬度，未采用外部 AI 文本中的反序定义。c3Lbs 4秒截止、失败隔离及切车/退出结果丢弃已补强；网页需网络和系统 WebView，手机底图/坐标精度仍待实车验收。见 [v0.7.9 知识库](26_V079_KEYLESS_PARKING_WEB.md)。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。


## v0.8.4 当前增量 · 2026-10-27

[KB29](29_V084_ICONS_SERVICE_TARGETSOC_READBACK.md)与[更新说明](../docs/Psa_v084_Update.md)记录新母版/昼夜通知、30000km与合并保养周期、C3 TargetSoC只读字段、首次协议、5/10/15/20秒四次回读和非阻塞等待。历史20000km、单次15秒回读与黑武士启动器约定被本轮授权替代。实车TargetSoC/平台桌面昼夜刷新仍待验收。
