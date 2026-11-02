package io.github.polestarcn.companion;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.*;
/** Internal one-use task approval. External apps cannot supply an action or verification result. */
public final class OppoCarLinkAuthorizeActivity extends FragmentActivity {
  private String task;private boolean resolved;
  @Override public void onCreate(Bundle saved){super.onCreate(saved);task=getIntent().getStringExtra("task");
    OppoCarLinkRuntime runtime=OppoCarLinkRuntime.get(this);String action=runtime.action(task);if(action==null||saved!=null){finish();return;}
    String label=ControlAuthorization.actionLabel(this,action),mode=ControlAuthorization.mode(this,action);
    if("confirm".equals(mode)){new android.app.AlertDialog.Builder(this).setTitle(label).setMessage("确认执行来自CarLink的车辆指令？")
      .setPositiveButton("确认",(d,w)->approve()).setNegativeButton("取消",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
    if("direct".equals(mode)){approve();return;}
    if(BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)!=BiometricManager.BIOMETRIC_SUCCESS){finish();return;}
    BiometricPrompt prompt=new BiometricPrompt(this,androidx.core.content.ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){
      @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){approve();}
      @Override public void onAuthenticationError(int code,CharSequence reason){finish();}
    });
    prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle(label).setSubtitle("CarLink · PsAssistant车辆控制").setNegativeButtonText("取消").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).build());
  }
  private void approve(){resolved=true;OppoCarLinkRuntime.get(this).dispatch(task);finish();}
  @Override public void finish(){if(!resolved&&task!=null){resolved=true;OppoCarLinkRuntime.get(this).cancel(task);}super.finish();}
}
