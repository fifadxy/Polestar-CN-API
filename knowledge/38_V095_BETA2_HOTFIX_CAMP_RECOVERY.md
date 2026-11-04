# KB38 · v0.9.5-beta2 Hotfix 核心设计和已提交修改

**基线** 29d36b45；**分支** hotfix/android-v095-beta2-20261029；versionCode 97。

- VehicleCoordinator：不同指令类别独立回读；待确认点击弹一次性二级确认，至多排队一个同类重复；连续三次 LIVE 提前确认。Camping 本地停止不等待云端响应。
- ControlApproval：各类指令可选 biometric / confirm / direct；默认 biometric。退出本地托管允许明确的二次安全确认，不受离线指纹流程阻断。
- CampModeManager：STOPPED 本地同步持久化，remote_stop_pending 独立保存；重新联网先核实新鲜 LIVE，再最多一次尝试每类远程 stop；不自动重新发送结果未知的写请求。清楚区分本地停止、远程未确认、远程确认。
- CampModeJob / CampRecovery：网络任务约束、状态分类（断网 vs 明确认证拒绝），后台受 Doze 限制。
- ChinaPolestarApi：SESSION_STATE_LOCK / sessionEpoch；退出无需等待 HTTP 刷新，旧请求完成不能复活已退出会话。加密本地会话，按服务端授权的 refresh token 规则使用，拒绝后重新输入账号信息。
- EnergyLog：保存用户已选车辆的不可逆哈希标识；离线查看本地能耗曲线，不能伪造历史测量。
- EffectSettings / ChargingParticleRenderer / AirflowRenderer：不规则路径 15%、直线比例 20%、粒子 300% 上限、范围渐显、顶视四边/非顶视前脸车底吸入；图像保持原始车辆比例。
- WindowGlowRenderer：蓝色冷气、VI 橙色暖气、绿色净化，玻璃光强/速度独立调节；HVAC 无核实的冷热字段时必须表达为视觉偏好。
- MapBackdropOptions / ParkingWebPreview / ParkingBackdrop：地图类型、缩放、有限抓图、Amap 网页失败尝试 OSM；网络或地区不可用时回退普通背景。
- UI：橙色 ? 帮助、全皮肤电量条开关、车况/露营图标文字状态、夜间状态高对比、一行车况连接·指令状态。

**明确边界**：本地停止≠远程 HVAC 停止；云端回读≠车辆持续连接；公共 Debug 缺私有中国区登录配置；未经中国区实车/真机验证不得冒称已经测试成功。
