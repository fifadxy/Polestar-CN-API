# 23｜v0.7.8-alpha1 静态 VI 与 NIOlike 第三皮肤

## 1. 分支策略

本改造从 Android 主线 v0.7.6 的 commit `45281847a437127300de20f9c5457b60f20ec190` 分出：

`feature/niolike-ui-v078`

原因是另一位开发者仍在制作 v0.7.7。该分支先完成 UI/回归验证，v0.7.7 上传后再做主线重放，避免互相覆盖。

## 2. 所有控制页共通规则

- 不再实例化任何循环环境光 View；
- 电量百分比固定展示，正下方是 Polestar Orange 横向进度条；
- 预估续航与电量并列；
- Battery / Range / Charging 字段从“常规自选数据”中移除，避免重复；
- 自选常规数据仍最多 12 项；
- 车辆图片下方固定放置静态车辆状态条。

### 车辆状态条

- 当前可见 Health 项全部正常：线性 check + `ALL GOOD`；
- 任一当前可见项告警：橙色 warning + CHECK/需检查；
- 无数据：灰色状态；
- 主色为黑/白/灰，橙色只用于状态强调。

## 3. 胎压显示

设置：

`显示胎压 / iTPMS（实验性）`

默认关闭。关闭时：

- 主车况不纳入胎压进行可见状态判断；
- 车辆状态弹窗不显示“胎压告警”行；
- Widget 选择中已保存的 Tire Status 会被运行时隐藏；
- 底层 TireSnapshot / Health 字段仍保留，不删除数据。

## 4. 条件充电栏

只有以下状态显示独立充电栏：

- charging = 充电中；
- charging = 智能充电；
- powerStatus = 初始化中；
- powerStatus = 正在供电。

空闲/完成/计划中不显示。

充电栏集中展示：

- AC/DC/连接类型；
- 充电功率；
- 电流；
- 预计充满时间。

详情页也只有充电会话有效时才展示充电卡片。

## 5. NIOlike

设计只借鉴用户提供参考图的模块化信息架构，不复制品牌图标或车辆素材。

### 浅色

- 背景：暖灰粉；
- 卡片：高亮米白；
- 圆角：22dp；
- 文字：近黑 + 暖灰；
- 状态强调：Polestar Orange。

### 深色

- 背景：近黑；
- 卡片：深灰；
- 文字：白/灰；
- 状态强调保持 Polestar Orange。

### 四张卡

1. 能源：Battery / Range / 进度条 / 原 Polestar 2 车辆图 / ALL GOOD；
2. 快捷控制：门锁、尾箱、车窗、鸣笛/闪灯；
3. 空调：驻车空调开关、目标温度、车内温度、空气净化；
4. 车辆：总里程、门窗、车况、露营、详情。

横屏时四卡横排，竖屏时 2×2。

## 6. 图标

新增 vector：

- `ic_vi_check`
- `ic_vi_warning`
- `ic_windows`
- `ic_signal`
- `ic_camp`
- `ic_charge`
- `ic_power`

统一 24dp 画布、圆角 lineCap / lineJoin、1.5–1.7dp 线宽。

## 7. 当前版本

- branch: `feature/niolike-ui-v078`
- versionName: `0.7.8-alpha1`
- versionCode: `78`
