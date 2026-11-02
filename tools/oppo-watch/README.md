# OPPO Watch X2 轻量采集

先在电脑安装 Python 3.9+、Android Platform Tools，并让 `adb` 可以在终端使用。需要手表开启并授权 ADB；手机 PsAssistant 不能代替这一步读取手表固件。

Mac：下载本目录的文件，双击 `开始采集.command`，或在终端运行 `python3 capture_watch.py`。Windows/Linux 可直接使用后一个命令。若有多个已授权设备，按型号选择手表。

按提示分别在全智能、长续航模式采集。脚本不会自动切换模式，不执行车辆命令，不读取日志、账号或私有目录。长续航下 ADB 断开会被记录为不可用，不需要 Root。生成 `OPPO-Watch-X2-light.zip`，上限 1 MB，通常只有数 KB。脚本仅验证 Android 可见环境，不能读取 RTOS 固件，也不能证明 RTOS 有原生控车能力。ADB 调试可能影响功耗，报告不用于测量续航；采集后可关闭调试。

先上传手机 APK 生成的轻量包；若继续研究 Watch X2，再补充此脚本生成的手表包。小包不包含完整 DEX 方法体，必要时再按具体目标补取代码，避免一次上传完整系统 APK。


## 2026-11-02 数字车钥匙研究补充

采集器现在额外识别 `CarLink / CarKey / DigitalKey / Wallet / Health Wallet` 相关包，仅用于判断手表系统是否存在官方车辆钥匙组件。

研究分成两条：
- **全智能模式**：可安装手表 App，优先做“手表 -> 手机 PsAssistant -> Polestar 远程控制”的安全中转；手表不保存 Polestar 账号密码或复制手机钥匙密钥。
- **长续航模式**：Android App 可能不运行。只有当 OPPO 自带的车钥匙/车辆服务被 RTOS 或系统级服务保留时，才可能继续实现无感钥匙或车控。手机侧 LSPosed 不能凭空让 RTOS 执行一个不存在的第三方 App。

注意：OPPO CarLink 的 `BIND_BT_KEY` / Wallet 卡片属于车企集成框架。真实钥匙必须先由车企/Polestar 的授权配对流程创建，不能用 Hook 把“未绑定”改成“已绑定”来代替。
