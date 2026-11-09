package io.github.polestarcn.companion;

import android.os.Build;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import io.github.polestarcn.companion.auth.ClientSignatureProvider;
import io.github.polestarcn.companion.auth.SecureCredentialStore;
import java.util.Arrays;

/** Foreground-only system-authentication gate for the encrypted local credential vault. */
final class AccountLoginController {
  private final FragmentActivity activity;
  private final VehicleCoordinator vehicle;
  private final java.util.function.Consumer<String> notice;
  private BiometricPrompt prompt;
  private boolean authenticating;
  private int generation;
  private char[] pendingSecret;
  AccountLoginController(FragmentActivity activity,VehicleCoordinator coordinator,java.util.function.Consumer<String> notice){
    this.activity=activity;vehicle=coordinator;this.notice=notice;
  }
  boolean hasSaved(){return SecureCredentialStore.hasSaved(activity);}
  boolean authenticationInProgress(){return authenticating;}
  void clear(){cancel();SecureCredentialStore.clear(activity);}
  void cancel(){
    generation++;
    authenticating=false;
    if(prompt!=null){prompt.cancelAuthentication();prompt=null;}
    if(pendingSecret!=null){Arrays.fill(pendingSecret,'\0');pendingSecret=null;}
  }
  void storeAfterVerifiedLogin(String phone,char[] password){
    if(password==null||password.length==0)return;
    if(pendingSecret!=null)Arrays.fill(pendingSecret,'\0');
    pendingSecret=password;
    authenticate("加密保存本机账号","Secure local credential storage",()->{
      try{
        SecureCredentialStore.save(activity,phone,password);
        notice.accept(AppPreferences.tr(activity,"账号已加密保存，仅可在系统身份验证后使用","Credentials securely saved; device authentication required"));
      }catch(Exception error){
        notice.accept(AppPreferences.tr(activity,"保存失败：请先设置系统锁屏或录入强生物识别","Secure storage unavailable: configure a screen lock or strong biometrics"));
      }finally{Arrays.fill(password,'\0');if(pendingSecret==password)pendingSecret=null;}
    },()->{Arrays.fill(password,'\0');if(pendingSecret==password)pendingSecret=null;});
  }
  void oneClickRelogin(){
    if(!hasSaved()){
      notice.accept(AppPreferences.tr(activity,"没有保存的账号；请先使用手机号和密码登录并选择保存","No saved account; sign in and opt in to saving credentials"));return;
    }
    if(!new ClientSignatureProvider(activity).isReady()){
      notice.accept(AppPreferences.tr(activity,"当前安装包缺少独立登录资源，无法重新登录","Standalone login client configuration is missing"));return;
    }
    if(!vehicle.canReauthenticate()){
      notice.accept(AppPreferences.tr(activity,"正在刷新、控制车辆或托管中，请操作完成后再重新登录","Wait for active reads, commands or camping before reauthenticating"));return;
    }
    authenticate("一键重新登录","Reauthenticate saved account",()->{
      try(SecureCredentialStore.Credentials value=SecureCredentialStore.load(activity)){
        if(value==null){notice.accept(AppPreferences.tr(activity,"已保存账号不存在，请重新输入","Saved account not found"));return;}
        String pass=value.password();
        boolean started=vehicle.login(value.phone,pass,null);
        pass=null;
        if(!started)notice.accept(AppPreferences.tr(activity,"当前有车辆操作或登录任务，未发起新登录","An operation is active; sign-in was not started"));
      }catch(Exception error){
        notice.accept(AppPreferences.tr(activity,"本机凭据不可读取，请重新输入账号密码并重新保存","Saved credentials unavailable; sign in manually to replace them"));
      }
    },()->{});
  }
  private void authenticate(String zh,String en,Runnable success,Runnable cancelled){
    int allowed=BiometricManager.Authenticators.BIOMETRIC_STRONG;
    if(Build.VERSION.SDK_INT>=30)allowed|=BiometricManager.Authenticators.DEVICE_CREDENTIAL;
    if(BiometricManager.from(activity).canAuthenticate(allowed)!=BiometricManager.BIOMETRIC_SUCCESS){
      cancelled.run();
      notice.accept(AppPreferences.tr(activity,"请先设置系统锁屏，并录入指纹或安全的设备身份验证","Set up a device lock screen and strong biometric or device credentials first"));
      return;
    }
    int current=++generation;
    authenticating=true;
    prompt=new BiometricPrompt(activity,command->activity.runOnUiThread(command),new BiometricPrompt.AuthenticationCallback(){
      @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
        if(current!=generation||activity.isFinishing()||activity.isDestroyed())return;
        generation++;authenticating=false;prompt=null;success.run();
      }
      @Override public void onAuthenticationError(int code,CharSequence text){
        if(current!=generation)return;
        generation++;authenticating=false;prompt=null;cancelled.run();
        if(code!=BiometricPrompt.ERROR_USER_CANCELED&&code!=BiometricPrompt.ERROR_NEGATIVE_BUTTON&&code!=BiometricPrompt.ERROR_CANCELED)
          notice.accept(AppPreferences.tr(activity,"身份验证没有完成","Device authentication was not completed"));
      }
    });
    BiometricPrompt.PromptInfo.Builder info=new BiometricPrompt.PromptInfo.Builder()
        .setTitle(AppPreferences.tr(activity,zh,en))
        .setSubtitle(AppPreferences.tr(activity,"密码仅解密于当前进程，不会提供给插件","Credentials are never exposed to plugins"))
        .setAllowedAuthenticators(allowed);
    if(Build.VERSION.SDK_INT<30)info.setNegativeButtonText(AppPreferences.tr(activity,"取消","Cancel"));
    try{prompt.authenticate(info.build());}
    catch(RuntimeException unavailable){
      if(current==generation){
        authenticating=false;prompt=null;generation++;cancelled.run();
        notice.accept(AppPreferences.tr(activity,"系统身份验证暂时不可用","Device authentication unavailable"));
      }
    }
  }
}
