# Psa Android 车辆主图素材记录

- 车型：Polestar 2，车头三分之四视角，灰色。
- 原始来源：官方 Polestar 2 Overview 页面，素材 URL：<https://www.polestar.com/dato-assets/94392/1742461054-01-polestar-2-26-overview-hero-d.png>，2026-10-27 获取。
- 处理：使用 ImageGen 从该车辆摄影素材移除纯色背景，保留车型、朝向和比例；为 Android 分发编码为带透明度 WebP，图像尺寸 1681×936，quality 90。衍生素材不是官方 UI 资源；来源记录不表示获取商标或图片授权。
- 应用资源：`android-app/app/src/main/res/drawable-nodpi/vehicle_hero.webp`。只在 NIOlike 主控制页使用，静态 FIT_CENTER 绘制，不拉伸，不定时重采样。
- 门窗和四轮状态图继续使用原模板的 `vehicle_top`；桌面启动图标继续使用用户指定的黑武士图标。此车辆图片不替换桌面图标。
