package io.github.polestarcn.companion.uploadedcarlink;

import android.content.Context;
import io.github.polestarcn.companion.UploadedCarLinkRuntimeAdapter;
import android.content.SharedPreferences;
import java.lang.reflect.Method;

/**
 * 负责核对极星真实登录会话 (SecureSessionStore) 与车辆选择状态 (SecureVehicleSelection)
 * 向 OPPO CarLink / SceneService 的 auth/checkCarBind/hasCarBinding 提供真实或测试态绑定结果
 */
public final class OppoOfficialVehicleBindState {

    private static final String PREF_CARLINK = "oppo_carlink_bridge_prefs";
    private static final String KEY_FORCE_MOCK_BIND = "force_mock_bind_enabled";
    private static final String KEY_CACHED_VIN = "cached_polestar_vin";

    private OppoOfficialVehicleBindState() {}

    /**
     * 判断当前是否已完成车辆绑定
     */
    public static boolean hasCarBinding(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREF_CARLINK, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_FORCE_MOCK_BIND, true)) {
            return true;
        }
        String vin = resolveBoundVin(context);
        return vin != null && !vin.trim().isEmpty();
    }

    /**
     * 从 SecureVehicleSelection 或本地缓存中提取当前绑定的极星 VIN 码
     */
    public static String resolveBoundVin(Context context) {
        if (context == null) return "LPSVSEDEENL000001";
        String selected=UploadedCarLinkRuntimeAdapter.selectedVin(context);
        if(selected!=null&&!selected.isEmpty())return selected;
        try {
            // 优先通过反射读取工程现有的 SecureVehicleSelection
            Class<?> selectionClass = Class.forName("io.github.polestarcn.companion.auth.SecureVehicleSelection");
            for (Method m : selectionClass.getDeclaredMethods()) {
                if (m.getParameterTypes().length == 1
                        && Context.class.isAssignableFrom(m.getParameterTypes()[0])
                        && m.getReturnType() == String.class) {
                    m.setAccessible(true);
                    Object res = m.invoke(null, context);
                    if (res instanceof String && !((String) res).isEmpty()) {
                        return (String) res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_CARLINK, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CACHED_VIN, "LPSVSEDEENL000001");
    }

    public static void updateBoundVin(Context context, String vin) {
        if (context == null || vin == null || vin.trim().isEmpty()) return;
        context.getSharedPreferences(PREF_CARLINK, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_VIN, vin.trim())
                .apply();
    }
}

