package io.github.polestarcn.companion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Compatibility route for shortcuts created before v0.4. */
public final class ChinaLiveActivity extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    finish();
  }
}
