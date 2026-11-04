package io.github.polestarcn.companion.uploadedcarlink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * 负责接收：
 * 1. OPPO CarLink / SceneService 的初始化唤醒广播 (路径 A)
 * 2. LSPosed 模块的显式 Intent 控车广播路由 (路径 B 兜底通道)
 */
public class OppoCarLinkInitReceiver extends BroadcastReceiver {

    private static final String TAG = "OppoCarLinkInitReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        Log.i(TAG, "Received CarLink broadcast action=" + action);

        // 1. 收到控车指令广播时，直接转交 VehicleProtocolGateway 执行真实极星控车
        if (CarLinkDexContract.ACTION_PSA_DIRECT_COMMAND.equals(action)
                || CarLinkDexContract.ACTION_OPPO_CAR_CONTROL_SERVICE.equals(action)
                || CarLinkDexContract.ACTION_OPLUS_CAR_CONTROL_SERVICE.equals(action)) {
            Bundle extras = intent.getExtras() != null ? new Bundle(intent.getExtras()) : new Bundle();
            int eventId = extras.getInt(CarLinkDexContract.KEY_EVENT_ID, -1);
            String sdkCmd = extras.getString(CarLinkDexContract.KEY_SDK_CMD_NAME, "");
            if (eventId != -1 || (sdkCmd != null && !sdkCmd.isEmpty())) {
                VehicleProtocolGateway.getInstance(context).executeCarEventAsync(eventId, extras);
                return;
            }
        }

        // 2. 收到系统开机或 CarLink 扫描广播时，预热虚拟车辆 Session 并通知系统刷新卡片
        VirtualVehicleSessionManager.getInstance(context).notifySystemSurfaces();
    }
}

