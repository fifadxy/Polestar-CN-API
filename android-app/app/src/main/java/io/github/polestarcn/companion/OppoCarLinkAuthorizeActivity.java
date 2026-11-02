package io.github.polestarcn.companion;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.*;
/** Internal one-use task approval. External apps cannot supply an action or verification result. */
public final class OppoCarLinkAuthorizeActivity extends FragmentActivity {
  private String task,verificationMode;private boolean resolved,providerRequest;
  @Override public void onCreate(Bundle saved){super.onCreate(saved);task=getIntent().getStringExtra("task");
    providerRequest=getIntent().getBooleanExtra("providerRequest",false);
    OppoCarLinkRuntime runtime=OppoCarLinkRuntime.get(this);String action=providerRequest?BridgeCommandRequests.action(task):runtime.action(task);if(action==null||saved!=null){finish();return;}
    String label=ControlAuthorization.actionLabel(this,action),mode=ControlAuthorization.mode(this,action);
    verificationMode=mode;
    if("confirm".equals(mode)){new android.app.AlertDialog.Builder(this).setTitle(label).setMessage("确认执行来自"+(providerRequest?"设备卡":"CarLink")+"的车辆指令？")
      .setPositiveButton("确认",(d,w)->approve()).setNegativeButton("取消",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
    if("direct".equals(mode)){approve();return;}
    if(BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)!=BiometricManager.BIOMETRIC_SUCCESS){finish();return;}
    BiometricPrompt prompt=new BiometricPrompt(this,androidx.core.content.ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){
      @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){approve();}
      @Override public void onAuthenticationError(int code,CharSequence reason){finish();}
    });
    prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle(label).setSubtitle((providerRequest?"设备卡":"CarLink")+" · PsAssistant车辆控制").setNegativeButtonText("取消").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).build());
  }
  private void approve(){resolved=true;if(providerRequest)BridgeCommandRequests.approve(this,task,verificationMode);else OppoCarLinkRuntime.get(this).dispatch(task);finish();}
  @Override public void finish(){if(!resolved&&task!=null){resolved=true;if(providerRequest)BridgeCommandRequests.cancel(task);else OppoCarLinkRuntime.get(this).cancel(task);}super.finish();}
}
