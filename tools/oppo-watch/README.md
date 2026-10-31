# OPPO Watch X2 轻量采集

先在电脑安装 Python 3.9+、Android Platform Tools，并让 `adb` 可以在终端使用。需要手表开启并授权 ADB；手机 PsAssistant 不能代替这一步读取手表固件。

Mac：下载本目录的文件，双击 `开始采集.command`，或在终端运行 `python3 capture_watch.py`。Windows/Linux 可直接使用后一个命令。若有多个已授权设备，按型号选择手表。

按提示分别在全智能、长续航模式采集。脚本不会自动切换模式，不执行车辆命令，不读取日志、账号或私有目录。长续航下 ADB 断开会被记录为不可用，不需要 Root。生成 `OPPO-Watch-X2-light.zip`，上限 1 MB，通常只有数 KB。脚本仅验证 Android 可见环境，不能读取 RTOS 固件，也不能证明 RTOS 有原生控车能力。ADB 调试可能影响功耗，报告不用于测量续航；采集后可关闭调试。

先上传手机 APK 生成的轻量包；若继续研究 Watch X2，再补充此脚本生成的手表包。小包不包含完整 DEX 方法体，必要时再按具体目标补取代码，避免一次上传完整系统 APK。
