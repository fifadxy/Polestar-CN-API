# PsAssistant v0.9.1-beta1 · 高清 PNG 车辆图库 / 确认回读
发布日期：2026-10-28 · versionCode 92 · 基于 v0.9.0 主线

## 1. 车辆图库
- 默认皮肤自动模式，日间 `vehicle_top.png`（用户原版高清白车），夜间 `psa_vehicle_top_dark.png`（用户原版黑车）。用户原有 `vehicle_art_day/night` 设置不清除。
- 原有 WebP 车图删除，改为用户上传高清 PNG / 极星官方 PNG。PNG 内容不使用 AI 重新生成，不拉伸车辆比例；原图本来横向放置的顶视资源使用 `FIT_CENTER`，停止二次旋转。
- 选择逻辑为“白天/夜间 → 极星 2/极星 5 → 视角 → 颜色”，Widget 的车辆图取当前白天/夜间选中图片。
- 极星 2：原版/黑/白/蓝灰/银色顶视，45°，正面，侧面；官方部分提供雪白/深空黑侧视。极星 5：当前只有核实的官方侧面 PNG 四色（雪白、深空黑、磨砂风暴灰、磨砂镁银）；不添加未核实的整车顶视或 45°素材。

## 2. 官方 PNG 来源
- 极星 2 雪白侧视：https://www.polestar.cn/dato-assets/94392/1773909330-10-1-polestar-2-27-overview-config-snow-d.png
- 极星 2 深空黑侧视：https://www.polestar.cn/dato-assets/94392/1773909450-10-5-polestar-2-27-overview-config-space-d.png
- 极星 5 雪白：https://www.polestar.cn/dato-assets/94392/1756806036-04-3-polestar-5-26-overview-nano-configurator-snow-d-1.png
- 极星 5 深空黑：https://www.polestar.cn/dato-assets/94392/1756806096-04-4-polestar-5-26-overview-nano-configurator-space-d-1.png
- 极星 5 磨砂风暴灰：https://www.polestar.cn/dato-assets/94392/1756805834-04-1-polestar-5-26-overview-nano-configurator-storm-matte-d-1.png
- 极星 5 磨砂镁银：https://www.polestar.cn/dato-assets/94392/1756805953-04-2-polestar-5-26-overview-nano-configurator-magnesium-matte-d-1.png
网站图片的原始编码与交付 PNG 不必相同；团队已验证 CDN 返回图片，再使用 PNG 保存，保持原构图与比例。

## 3. 远控命令确认
- 单次发送（不自动重复写入），服务器返回仅表示收到请求，不等同车辆真正执行。
- 回读检查点（相对写请求返回后）：5、10、15、18、26 秒。不中途判 Failed；26 秒最终确认需要连续三次有效 LIVE 回读，错误/空回读会中断连续计数。
- UI 信息栏目每秒更新 0% 到最高 98%，完成 26 秒后的检查再显示 100%；每次返回显示 x/5 次回读。因为网络延迟，实测可能晚于时间点。
- 不支持从普通车况读取的鸣笛/闪灯结果，不宣称实车确认成功；如通信超时，仅写入一次后读取验证。欠缺证据时标注“未确认”，不把未知等同失败或成功。
- 露营指令校验遵循相同五次时间节点和三次确认条件。电量未知、未登录、明确参数错误仍可即时阻断发送。

## 4. 构建和验证
公共 Debug 不保证可中国区登录。完整可登录包需先前有效的私有客户端配置以及稳定签名，不能靠公开仓库凭空获得。
源代码单元测试覆盖五节点与三次确认、车辆图库图片类别/车型隔离/日夜默认/无旋转；运行中的 API、手机权限和实车仍需独立验证。
