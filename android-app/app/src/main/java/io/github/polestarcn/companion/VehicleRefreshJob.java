package io.github.polestarcn.companion;

import android.app.job.*;

/** Android batches this optional thirty-minute job under Doze and battery restrictions. */
public final class VehicleRefreshJob extends JobService {
  private boolean stopped;

  @Override
  public boolean onStartJob(JobParameters p) {
    if(!FirstUseConsent.accepted(this))return false;
    stopped = false;
    if ((RefreshPolicy.background(this,AppPreferences.enabled(this),WidgetRenderer.hasWidgets(this))==0)
        || !VehicleCoordinator.get(this).hasSession()) return false;
    VehicleCoordinator.get(this)
        .refresh(
            () -> {
              if (!stopped) jobFinished(p, false);
            });
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters p) {
    stopped = true;
    return false;
  }
}
