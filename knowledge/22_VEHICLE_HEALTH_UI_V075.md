# 22｜v0.7.5 车辆 Health 聚合与控制页 VI

## 1. 主界面车辆身份

两套皮肤的车辆控制首页只显示车型名称，例如 `Polestar 2`。主页面不再显示：

- 年款（例如 2021）；
- VIN 后4位；
- 完整 VIN。

点击车型进入二级“车辆详情”菜单，显示车型、年款、完整 VIN，并提供切换车辆入口。完整 VIN 不写入展示缓存。

## 2. Polestar 2 主视觉

控制首页的 Polestar 2 车辆图整体放大约 8%，不改变原始素材比例。白天使用原车素材；夜间继续采用保留轮廓高光的黑色/深灰车身矩阵。

## 3. 统一车辆状态按钮

原独立“胎压”主按钮取消，主界面统一为 **车辆状态**：

- 12V 电瓶低电压告警；
- 玻璃水告警；
- 胎压 / iTPMS 告警。

状态聚合规则：

- 任一告警：橙色 + `!`；
- 三项均明确正常：`ALL GOOD`；
- 部分上报：部分已上报；
- 无数据：未上报。

### Global 源码映射

Hisingen commit `5301353`：

- field 13 = washer fluid warning，>1 告警；
- field 38 = low-voltage battery，2 告警；
- fields 9–12 = 四轮 tyre warning；
- fields 39–42 = 四轮压力研究值；
- field 1 = Health timestamp。

玻璃水只表示告警，不表示剩余百分比；12V 只表示低电压告警，不表示实时电压。

### China 证据边界

当前 Android China 代码已能请求同名 `HealthService/GetHealth`，v0.7.5 将一个 Health payload 同时解析为 `TireSnapshot` + `VehicleHealthSnapshot`。

中国官方 App 历史材料只确认车辆状态 UI 位于 `com.polestar.carcontrol`，尚未直接验证上述字段；因此 China 仍需授权实车验证。

## 4. iTPMS 默认隐藏

设置新增：

`实验性 iTPMS 四轮状态（待修复）`

默认关闭。关闭时：

- 控制首页没有独立胎压按钮；
- 自定义数据列表隐藏单独“胎压状态”指标；
- 车辆状态详情仍保留“胎压告警”聚合结果。

开启后仅允许进入四轮实验告警视图，不宣称具体 kPa 已在中国 Polestar 2 验证。

## 5. 数据密度

控制页数据选择上限提升到 12 项：

- 1–4：2 列；
- 5–6：3 列；
- 7–12：4 列。

字号、内边距和行高随密度动态缩放；竖屏与横屏继续保持固定单页控制页，不引入 ScrollView。

## 6. 动态 VI

原 3dp 呼吸线升级为约 20dp 的大面积环境光：

- Polestar Orange 中心高光；
- 横向缓慢位移；
- 3.6 秒呼吸周期；
- 夜间透明度更强，白天更克制；
- 只重绘独立光带 View，避免整个控制页持续重绘。

## 7. 版本

- branch: `android-polestar-assistant-main`
- app: `0.7.5-beta1`
- versionCode: `75`
