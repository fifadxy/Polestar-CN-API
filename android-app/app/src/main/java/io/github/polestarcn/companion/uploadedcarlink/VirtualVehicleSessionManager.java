package io.github.polestarcn.companion.uploadedcarlink;

import android.content.Context;
import io.github.polestarcn.companion.UploadedCarLinkRuntimeAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 维护 OPPO CarLink 所需的虚拟车辆会话 (Virtual Vehicle Session) 与实时车况快照
 * 自动与主程序的 VehicleSnapshotStore 同步，并在车况更新时主动通知系统刷新卡片
 */
public class VirtualVehicleSessionManager {

    private static final String PREF_SESSION = "psa_virtual_vehicle_session";
    private static volatile VirtualVehicleSessionManager instance;

    private final Context appContext;
    private volatile boolean isLocked = true;
    private volatile boolean isClimateActive = false;
    private volatile boolean isWindowClosed = true;
    private volatile int socPercent = 82;
    private volatile int rangeKm = 415;
    private volatile float cabinTemp = 22.0f;
    private volatile long updatedAt = System.currentTimeMillis();

    private VirtualVehicleSessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        loadPersistedState();
        syncFromVehicleSnapshotStore();
    }

    public static VirtualVehicleSessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (VirtualVehicleSessionManager.class) {
                if (instance == null) {
                    instance = new VirtualVehicleSessionManager(context);
                }
            }
        }
        return instance;
    }

    public boolean hasActiveBinding() {
        return OppoOfficialVehicleBindState.hasCarBinding(appContext);
    }

    public String getCurrentVin() {
        return OppoOfficialVehicleBindState.resolveBoundVin(appContext);
    }

    /**
     * 尝试从主程序现有的 VehicleSnapshotStore 读取真实车况缓存
     */
    public void syncFromVehicleSnapshotStore() {
        Object snapshot=UploadedCarLinkRuntimeAdapter.snapshot(appContext);
        if(snapshot!=null)extractSnapshotFields(snapshot);
    }

    private void extractSnapshotFields(Object snapshotObj) {
        for (Field f : snapshotObj.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                String name = f.getName().toLowerCase();
                Object val = f.get(snapshotObj);
                if (val == null) continue;
                if (name.contains("lock") && val instanceof Boolean) {
                    this.isLocked = (Boolean) val;
                } else if ((name.contains("climate") || name.contains("hvac")) && val instanceof Boolean) {
                    this.isClimateActive = (Boolean) val;
                } else if (name.contains("window") && val instanceof Boolean) {
                    this.isWindowClosed = (Boolean) val;
                } else if ((name.contains("soc") || name.contains("battery")) && val instanceof Number) {
                    this.socPercent = ((Number) val).intValue();
                } else if (name.contains("range") && val instanceof Number) {
                    this.rangeKm = ((Number) val).intValue();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public Bundle buildSessionBundle() {
        syncFromVehicleSnapshotStore();
        Bundle bundle = new Bundle();
        boolean bound = hasActiveBinding();
        bundle.putBoolean(CarLinkDexContract.KEY_HAS_BINDING, bound);
        bundle.putBoolean(CarLinkDexContract.KEY_HAS_BINDING_SNAKE, bound);
        bundle.putString(CarLinkDexContract.KEY_BRAND_NAME, "Polestar");
        bundle.putString(CarLinkDexContract.KEY_VEHICLE_NAME, "Polestar");
        bundle.putString(CarLinkDexContract.KEY_VIN, getCurrentVin());
        bundle.putInt(CarLinkDexContract.KEY_CONNECT_STATE,
                bound ? CarLinkDexContract.SESSION_STATE_CONNECTED_READY
                      : CarLinkDexContract.SESSION_STATE_DISCONNECTED);
        bundle.putInt(CarLinkDexContract.KEY_CAPABILITY_MASK,
                CarLinkDexContract.DEFAULT_POLESTAR_CAPABILITY_MASK);
        bundle.putAll(buildSnapshotBundle());
        return bundle;
    }

    public Bundle buildSnapshotBundle() {
        Bundle snapshot = new Bundle();
        snapshot.putBoolean(CarLinkDexContract.KEY_LOCKED, isLocked);
        snapshot.putBoolean(CarLinkDexContract.KEY_CLIMATE_ACTIVE, isClimateActive);
        snapshot.putBoolean(CarLinkDexContract.KEY_WINDOW_CLOSED, isWindowClosed);
        snapshot.putInt(CarLinkDexContract.KEY_SOC_PERCENT, socPercent);
        snapshot.putInt(CarLinkDexContract.KEY_RANGE_KM, rangeKm);
        snapshot.putFloat(CarLinkDexContract.KEY_CABIN_TEMP, cabinTemp);
        snapshot.putLong(CarLinkDexContract.KEY_UPDATED_AT, updatedAt);
        return snapshot;
    }

    public void applyOptimisticUpdate(OppoCarLinkCommandMap.MappedAction action) {
        switch (action) {
            case LOCK:
                isLocked = true;
                break;
            case UNLOCK:
                isLocked = false;
                break;
            case CLIMATE_START:
                isClimateActive = true;
                break;
            case CLIMATE_STOP:
                isClimateActive = false;
                break;
            case WINDOW_CLOSE:
                isWindowClosed = true;
                break;
            case WINDOW_VENT:
                isWindowClosed = false;
                break;
            default:
                break;
        }
        updatedAt = System.currentTimeMillis();
        persistState();
        notifySystemSurfaces();
    }

    private void loadPersistedState() {
        SharedPreferences sp = appContext.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
        isLocked = sp.getBoolean(CarLinkDexContract.KEY_LOCKED, true);
        isClimateActive = sp.getBoolean(CarLinkDexContract.KEY_CLIMATE_ACTIVE, false);
        isWindowClosed = sp.getBoolean(CarLinkDexContract.KEY_WINDOW_CLOSED, true);
        socPercent = sp.getInt(CarLinkDexContract.KEY_SOC_PERCENT, 82);
        rangeKm = sp.getInt(CarLinkDexContract.KEY_RANGE_KM, 415);
    }

    private void persistState() {
        appContext.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(CarLinkDexContract.KEY_LOCKED, isLocked)
                .putBoolean(CarLinkDexContract.KEY_CLIMATE_ACTIVE, isClimateActive)
                .putBoolean(CarLinkDexContract.KEY_WINDOW_CLOSED, isWindowClosed)
                .putInt(CarLinkDexContract.KEY_SOC_PERCENT, socPercent)
                .putInt(CarLinkDexContract.KEY_RANGE_KM, rangeKm)
                .putLong(CarLinkDexContract.KEY_UPDATED_AT, updatedAt)
                .apply();
    }

    public void notifySystemSurfaces() {
        try {
            appContext.getContentResolver().notifyChange(CarLinkDexContract.BRIDGE_URI, null);
            appContext.getContentResolver().notifyChange(CarLinkDexContract.CHECK_CAR_BIND_URI, null);
            Intent broadcast = new Intent(CarLinkDexContract.ACTION_PSA_STATE_CHANGED);
            broadcast.putExtras(buildSessionBundle());
            appContext.sendBroadcast(broadcast);
        } catch (Throwable ignored) {
        }
    }
}

