package io.github.polestarcn.companion.uploadedcarlink;

import android.os.Bundle;
import java.util.Locale;

/**
 * 统一映射路径 A (platform_sdk 字符串/数值指令) 与路径 B (4 个 CAR 控制事件)
 */
public final class OppoCarLinkCommandMap {

    private OppoCarLinkCommandMap() {}

    public enum MappedAction {
        LOCK(CarLinkDexContract.CAR_EVENT_LOCK_UNLOCK, true, "LOCK"),
        UNLOCK(CarLinkDexContract.CAR_EVENT_LOCK_UNLOCK, false, "UNLOCK"),
        CLIMATE_START(CarLinkDexContract.CAR_EVENT_CLIMATE, true, "CLIMATE_START"),
        CLIMATE_STOP(CarLinkDexContract.CAR_EVENT_CLIMATE, false, "CLIMATE_STOP"),
        WINDOW_CLOSE(CarLinkDexContract.CAR_EVENT_WINDOW, true, "WINDOW_CLOSE"),
        WINDOW_VENT(CarLinkDexContract.CAR_EVENT_WINDOW, false, "WINDOW_VENT"),
        FIND_CAR_FLASH_HORN(CarLinkDexContract.CAR_EVENT_FIND_CAR, true, "FIND_CAR"),
        UNKNOWN(-1, false, "UNKNOWN");

        public final int eventId;
        public final boolean targetState;
        public final String actionCode;

        MappedAction(int eventId, boolean targetState, String actionCode) {
            this.eventId = eventId;
            this.targetState = targetState;
            this.actionCode = actionCode;
        }
    }

    /**
     * 解析来自 LSPosed 或 platform_sdk 的控制事件
     */
    public static MappedAction resolve(int carEventId, Bundle extras) {
        if (extras != null) {
            String sdkCmd = extras.getString(CarLinkDexContract.KEY_SDK_CMD_NAME, "");
            if (sdkCmd != null && !sdkCmd.trim().isEmpty()) {
                MappedAction fromName = resolveFromSdkCommandName(sdkCmd, extras);
                if (fromName != MappedAction.UNKNOWN) {
                    return fromName;
                }
            }
        }

        boolean targetState = extras == null || extras.getBoolean(CarLinkDexContract.KEY_ACTION_TARGET, true);

        switch (carEventId) {
            case 1:
            case CarLinkDexContract.CAR_EVENT_LOCK_UNLOCK:
                return targetState ? MappedAction.LOCK : MappedAction.UNLOCK;
            case 2:
            case CarLinkDexContract.CAR_EVENT_CLIMATE:
                return targetState ? MappedAction.CLIMATE_START : MappedAction.CLIMATE_STOP;
            case 3:
            case CarLinkDexContract.CAR_EVENT_WINDOW:
                return targetState ? MappedAction.WINDOW_CLOSE : MappedAction.WINDOW_VENT;
            case 4:
            case CarLinkDexContract.CAR_EVENT_FIND_CAR:
                return MappedAction.FIND_CAR_FLASH_HORN;
            default:
                return MappedAction.UNKNOWN;
        }
    }

    /**
     * 解析路径 A (CarAppDemo / platform_sdk) 下发的字符串动作名
     */
    public static MappedAction resolveFromSdkCommandName(String cmdName, Bundle extras) {
        if (cmdName == null) return MappedAction.UNKNOWN;
        String normalized = cmdName.trim().toLowerCase(Locale.ROOT);
        boolean targetState = extras == null || extras.getBoolean(CarLinkDexContract.KEY_ACTION_TARGET, true);

        if (normalized.contains("unlock")) {
            return MappedAction.UNLOCK;
        } else if (normalized.contains("lock")) {
            return targetState ? MappedAction.LOCK : MappedAction.UNLOCK;
        } else if (normalized.contains("climate") || normalized.contains("air") || normalized.contains("ac") || normalized.contains("hvac")) {
            if (normalized.contains("off") || normalized.contains("stop") || normalized.contains("close")) {
                return MappedAction.CLIMATE_STOP;
            }
            return targetState ? MappedAction.CLIMATE_START : MappedAction.CLIMATE_STOP;
        } else if (normalized.contains("window") || normalized.contains("sunroof") || normalized.contains("vent")) {
            if (normalized.contains("vent") || normalized.contains("open") || normalized.contains("breathe")) {
                return MappedAction.WINDOW_VENT;
            }
            return targetState ? MappedAction.WINDOW_CLOSE : MappedAction.WINDOW_VENT;
        } else if (normalized.contains("find") || normalized.contains("horn") || normalized.contains("flash") || normalized.contains("whistle")) {
            return MappedAction.FIND_CAR_FLASH_HORN;
        }
        return MappedAction.UNKNOWN;
    }
}

