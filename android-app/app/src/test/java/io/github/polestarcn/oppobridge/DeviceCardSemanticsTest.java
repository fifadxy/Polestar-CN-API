package io.github.polestarcn.oppobridge;
import static org.junit.Assert.*;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import io.github.polestarcn.companion.OppoDeviceIntegration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class DeviceCardSemanticsTest {
 @Test public void onlyPositiveChargeStatesAreCharging(){
   for(String s:new String[]{"", "空闲","计划中","放电中","错误","已完成","智能充电暂停","未知","Not charging","Unknown"})assertFalse(s,VehicleDeviceCardProvider.isCharging(s));
   for(String s:new String[]{"充电中","正在充电","智能充电","Charging"})assertTrue(s,VehicleDeviceCardProvider.isCharging(s));
 }
 @Test public void unknownHostCallsFailAndCardShowIsCacheOnly(){
   Context c=RuntimeEnvironment.getApplication();OppoDeviceIntegration.setEnabled(c,true);
   VehicleDeviceCardProvider p=new VehicleDeviceCardProvider();ProviderInfo info=new ProviderInfo();info.authority=VehicleDeviceCardProvider.AUTHORITY;p.attachInfo(c,info);
   Bundle result=p.call("future_unknown_method",null,null);
   assertFalse(result.getBoolean("result"));assertFalse(result.getBoolean("accepted"));assertEquals("unsupported-host-method",result.getString("reason"));
   Bundle shown=p.call("card_show",null,null);assertEquals("cached-card-only",shown.getString("reason"));
 }
}
