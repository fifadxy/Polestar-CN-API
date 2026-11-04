# 19｜v0.7.2 控制首页模板式下拉刷新

## 模板证据

用户提供的参考 APK 静态核对确认：

- 自带 \`androidx.swiperefreshlayout_swiperefreshlayout.version\`，版本为 **1.1.0**；
- DEX 存在 \`SwipeRefreshLayout\`、\`OnRefreshListener\`、\`setOnRefreshListener\`、\`setRefreshing\`；
- DEX 还存在模板自定义类 \`MainActivity$HorizontalSwipeRefreshLayout\`。

因此本轮不是仅添加一个刷新按钮，而是复刻其“下拉刷新 + 横向手势保护”的交互结构。

## Android 实现

三套 UI 皮肤的控制首页统一套入 \`HorizontalSwipeRefreshLayout\`：

1. 最新模板；
2. 当前版本；
3. 此刻图片风格。

下拉刷新直接调用现有 \`VehicleCoordinator.refresh()\`。该方法继续负责车辆发现、选择车辆、读取完整 VehicleSnapshot、保存显示缓存并刷新 Widget，不新增第二套网络逻辑。

### 触发规则

- 仅控制首页启用；
- 页面滚动到顶部后才能下拉；
- 正在执行车辆读取时再次下拉不会重复请求；
- 远程控制命令执行中的 busy 状态不会显示为“数据刷新”；
- 右上角原刷新按钮继续保留，按钮刷新与下拉刷新使用同一数据链路。

### 手势规则

\`HorizontalSwipeRefreshLayout\` 记录 ACTION_DOWN 与 ACTION_MOVE：

- 明显横向移动超过 touch slop 且横向距离大于纵向距离时，不拦截；
- 纵向下拉继续交由 SwipeRefreshLayout；
- ACTION_UP / CANCEL 重置状态。

## 版本

- branch: \`android-polestar-assistant-main\`
- app: \`0.7.2-beta1\`
- versionCode: \`72\`
