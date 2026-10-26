# 09｜另一位 AI 分析建议交叉审核

本文件不是完整安全审计，目的是将外部建议变成具备证据级别、优先级的开发知识，避免原文某些推算被视为事实。

| 原分析主张 | 本次交叉结论 | 当前处理 |
|---|---|---|
| APK 4.63MB、526 成员、AGP 8.5.2 | ZIP / 元数据独立复核一致 | `APK_CONFIRMED` |
| minSdk 26, targetSdk 34, compileSdk 35 | AXML 复核一致 | 新工程暂对齐 |
| app 自身 96 类、总类 9117 | DEX 类定义统计一致；含合成类/资源类 | 记录统计口径 |
| “仅 6 个核心 Java 源文件” | 原文实列出了 **7 个**：MainActivity / PolestarApi / PolestarWidgetProvider / WidgetConfigureActivity / WidgetControlActivity / CampModeWorker / WidgetRefreshWorker | 更正为 7 |
| `MainActivity` Java 动态 UI | 方法和内部类证据支持 | 工程拆分职责 |
| HTTP/JSON + gRPC 混合 | URL、gRPC 包、方法/服务名支持 | 待运行验证 |
| APK 构建时 `isMinifyEnabled=false` | 可读名很多说明**混淆可能未开启**，但仅靠 APK 不足以证明原 build.gradle 的具体属性 | 记录为静态推断，不冒充原 Gradle |
| “预计缩小 70% / 到 1.2–1.5MB” | 没有经过依赖裁剪与编译测量，属于推测 | 不当作可靠节省指标 |
| Guava >2K 类且 Multidex | DEX 类数量/依赖存在可核；不能仅由主 DEX 方法数直接断言具体分包成因 | 不做 R8 优化 |
| WebP 单图降到 30–50KB | 有可能但未做格式试验；需权衡图片保真度 | 不执行 |
| `Room` 可删除 | WorkManager 会间接用到 Room；不能直接断言全部多余 | 先不引入重依赖 |
| 需要拆分 MainActivity God Class | 一致且可维护性直接受益 | 首版分拆 Repository/Widget/State |
| 需要启动 R8、做安全扫描/组件加固 | 用户明确延期 | `DEFERRED` |
| “存在安全漏洞” | 部分为静态风险假设，未对可利用性做动态验证 | 不给确定性漏洞判定 |

### 可直接采纳且不属于安全后期事项的工程改善

- 状态模型抽象 + 地区隔离 + Widget 主页面数据源统一；
- 调试期间每次按钮操作返回明确反馈；
- 各个字段显式注明 DEMO / LIVE / CACHE、数据时效；
- 确定性编译、版本记录、可复制日志与回归测试清单。
