package io.github.polestarcn.oppobridge;

import android.app.BroadcastOptions;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fallback IPC for an already-installed PsAssistant APK that predates PsAssistantBridgeProvider.
 * The LSPosed module injects the matching receiver into the PsAssistant process.
 */
final class RuntimeBridgeClient {
  static final String ACTION = "io.github.polestarcn.oppobridge.RUNTIME_CALL";
  static final String PSASSISTANT = "io.github.polestarcn.companion";
  private static final long TIMEOUT_MS = 1400L;

  private RuntimeBridgeClient() {}

  static Bundle call(Context context, String method, Bundle payload) {
    CountDownLatch done = new CountDownLatch(1);
    AtomicReference<Bundle> result = new AtomicReference<>();
    ResultReceiver reply = new ResultReceiver(null) {
      @Override protected void onReceiveResult(int resultCode, Bundle resultData) {
        result.set(resultData == null ? Bundle.EMPTY : resultData);
        done.countDown();
      }
    };
    Intent intent = new Intent(ACTION)
        .setPackage(PSASSISTANT)
        .putExtra("method", method)
        .putExtra("payload", payload == null ? Bundle.EMPTY : payload)
        .putExtra("reply", reply);
    try {
      if (Build.VERSION.SDK_INT < 34) {
        Bundle unsupported = new Bundle();
        unsupported.putBoolean("available", false);
        unsupported.putBoolean("accepted", false);
        unsupported.putString("reason", "runtime-bridge-requires-api-34");
        return unsupported;
      }
      // The injected receiver authenticates getSentFromUid(). Identity sharing is
      // opt-in on API 34+, otherwise legitimate calls are rejected as anonymous.
      Bundle options = BroadcastOptions.makeBasic()
          .setShareIdentityEnabled(true).toBundle();
      context.sendBroadcast(intent, null, options);
      if (!done.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
        Bundle unavailable = new Bundle();
        unavailable.putBoolean("available", false);
        unavailable.putBoolean("accepted", false);
        unavailable.putString("reason", "psassistant-runtime-bridge-unavailable");
        return unavailable;
      }
      return result.get() == null ? Bundle.EMPTY : result.get();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      Bundle out = new Bundle();
      out.putBoolean("accepted", false);
      out.putString("reason", "interrupted");
      return out;
    } catch (RuntimeException failure) {
      Bundle out = new Bundle();
      out.putBoolean("accepted", false);
      out.putString("reason", failure.getClass().getSimpleName());
      return out;
    }
  }
}
