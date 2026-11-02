# beta27 FULL交付验证（2026-11-02）

源码：`9030e9aa3c2288319d2a986541a3da6a26412ee5`；主APK`0.9.7-beta27-standalone` /141，集成Bridge1.13.0 /25。

## 安装包

- `PsAssistant-v0.9.7-beta27-Standalone-FULL.apk`：**9,565,811字节（9.57MB）**，低于10,000,000字节。
- SHA256：`1b3680140005ed31954441a97de1ca431a2869d85a1b0eb5d86f68a5e2fbe083`。
- 签名证书SHA256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`，与beta26完整包相同。APK v2签名及内容摘要验证通过。
- 已包含批准的独立China登录配置，无需导入其他APK；DEX确认真实OPPO CarLink实现109个已定义类与内置LSPosed入口。公开CI的VALIDATION包没有这些私有完整构建输入，不能替代本FULL文件。
- 无损重压缩、配置封装和重新签名逐项比较，DEX、资源、图片与模块元数据解压后保持不变。

## 检查结果

- 真实SDK全量回归：466项，0失败、0跳过；最后改动另跑45项定向回归，0失败、0跳过。
- 最终源码真实SDK编译与Lint通过：**0错误、99警告**；保留Android8/API26兼容性，修复反射Type.getTypeName的API28要求，使用兼容方法。
- 最终公开CI [41478847907](https://github.com/fifadxy/Polestar-CN-API/actions/runs/41478847907)成功，包含主程序、独立模块、单元测试、Lint、整合包及10MB检查。
- 源码结构、知识库、模型与地图检查，以及5项云端签名测试通过。新Provider回归覆盖宿主先鉴权、内部转发身份、失败后的身份恢复、selection按列匹配、空卡原因、旧主程序写入拒绝、验证任务取消/换车/设置变化、空目录构造与不可变列表。
- 更新后的手机/车辆/手表端到端验收尚未进行，单测模拟不等同于实机。

## 修复范围

完整证据与逻辑见[Provider研究记录](../knowledge/OPPO_PROVIDER_BETA27_20261102.md)。本轮同时处理设备卡ContentProvider和系统车辆Provider目录，保留原有卡片数据、其他品牌和主程序外观功能；主程序每个操作的直接/确认/生物识别设置统一生效，移除宿主重复弹窗。

原始轻量包没有Provider诊断，不能直接宣称某个实机异常已经定位为唯一原因。本轮新增schema5和provider-diagnostics.json，区分本地注册、发现、自检查询、空卡原因与目录观测来源。自检查询只证明本机Provider能返回缓存，不证明OEM已经渲染或官方CarLink授权。

## 更新后操作

1. 覆盖安装本FULL包；LSPosed只启用内置或独立模块其中一份，勾选建议作用域，重启手机，确保宿主加载Bridge1.13.0。
2. 主程序登录并手动刷新一次车况，进入**OPPO互联**，开启车辆桥接，点击**修复Provider注册并恢复设备卡**，再重新进入设备空间添加卡片。
3. 根据需要在主程序为各操作选择直接发送、二次确认或生物识别。Provider沿用同一设置。系统阻止验证界面时打开主程序验证，不会因此跳过验证或自动重发车辆写入。
4. 如仍异常，直接导出≤2MB轻量包。新包会明确显示componentEnabled、discoveredByAction、querySucceeded、rowCount、queryReason及目录观测来源，且不包含原始车辆值、VIN、令牌或完整系统APK。

独立Provider复用PsAssistant的账号及车控会话。OPPO官方身份/车型技能、原生页面实际渲染与Watch X2长续航模式仍按各自链路验收，没有标为全部打通。
