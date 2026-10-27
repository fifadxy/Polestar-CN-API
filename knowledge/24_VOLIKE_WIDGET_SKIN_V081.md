# 24｜v0.8.1 VoLike 多配色与 Widget 独立皮肤

## 设计原则
参考用户提供的沃尔沃 App / Widget / 手表布局，全部使用项目已留存的 Polestar 2 车辆资源、通用原生控件与现有认证、读车、远控链路。设计参考不等同于内置 Volvo 官方素材或标志。

## 手机皮肤
五种：VoLike 简洁、VoLike 详细、VoLike 极星 VI、NIOlike、NIOlike2。极星 VI 使用近黑、白、灰与 Polestar Orange，既保留 VoLike 版式又保留品牌区分。旧 `simple/detailed` 偏好不变；新 `volike_polestar` 为独立选择值。

`SkinPreviewView` 为轻量本地 Canvas 控件，读取 `vehicle_hero` 静态项目资源，显示车辆、电量条和布局概貌；选择器内逐项展示，点击后立即切换。所有图片仅作为 UI 预览，实车数据继续由模型注入。没有上传远程服务或第三方追踪。

`volike_simple_labels=off` 只控制 **VoLike 简洁版主控** 4 个按钮下的文字。按键仍保留图标、明确点击热区、无障碍文字与生物验证；其余皮肤默认不受影响。

## Widget
`widget_design` 独立于 `ui_skin`，四种值：
- `classic`：原项目可配置 4×2 / 2×2 / 4×1 / 2×1 / 4×4 指标网格；
- `volike_simple`：车辆 + SOC / Range + 少量状态；
- `volike_detailed`：增加车辆细节与更丰富静态卡片；
- `volike_polestar`：上面卡片的极星黑白灰/橙色调色。

新 Widget 是 **RemoteViews + 本地 Canvas Bitmap 背景 + 四个原生 PendingIntent 入口**，不引入高德地图 SDK 或持续 Widget 动画。门锁与空调入口调用已有 `lock-choice / climate-choice` 认证链，不绕过强认证；充电入口为只读查看，不触发充电远控；停车位置使用当前会话已有 LBS 只读详情。点击卡片主体进入 App。

Widget 刷新遵循现有时间/后台约束；仅在车辆确认正在充电时将充电文案及配色写入静态图片，Widget 不模拟实时流动。

## 动态与性能
手机页 `ChargingPulseView` 仅在充电卡可见且窗口附着时运行，动画仅重绘小型能量条。停充立即隐藏；背景环境光动画不恢复。使用系统字体比例适配充电时间文本及固定紧凑高度。

## 风险与证据边界
本轮 CI 可证明源码及 UI 渲染回归；不证明中国区实车远程指令执行。Polestar 2 车辆图继承项目静态素材，最终车型年款视觉准确度需用户用实车图像核对。手表仍只有可切换预览，独立 Wear OS APK 尚未构建。
