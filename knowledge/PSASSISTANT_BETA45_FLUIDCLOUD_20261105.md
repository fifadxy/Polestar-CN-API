# Beta45 OPPO 系统兼容：流体云 / Indicator / LSPosed

- 开发目标：OPPO 系统兼容聚合设置，全项目模拟测试，通知场景可选，级联展开含车控动作、Android 16 推广型通知实验。
- 官方权限边界：OPPO Pantanal `IntelligentIntent` 需开发者服务 ID / 官方业务审核；当前没有此 ID，不能声称已经原生集成。
- 实现：`OppoFluidCloud` 用 Android 原生 Notifications，满足条件时在 API36 运行时反射请求 `setRequestPromotedOngoing`。不引入新系统签名 Hook，也不伪造厂商状态。
- 默认全部关闭；通知事件、流体云请求、二级操作各自可选；11 项场景 × 2 通路 × 2 样式 × 2 层级 = 88 项构造测试，一键测试 11 项发布。
- 安全：锁车/解锁/车窗/尾门「成功」只从真正 `VEHICLE_CONFIRMED` 发出；提醒由真实缓存驱动，故障逐项描述并去重；后台任务不加新的云端请求；所有控制必须回主程序再次确认，并遵守原授权。
- 开发/测试的详细列表、局限及真实验证步骤参见 `docs/PSASSISTANT_BETA45_OPPO_FLUIDCLOUD_TEST_20261105.md`。
