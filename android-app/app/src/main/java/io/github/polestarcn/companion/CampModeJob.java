package io.github.polestarcn.companion;

import android.app.job.JobService;
import android.app.job.JobParameters;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android may delay these checks in Doze; every write is preceded by a fresh state decision. */
public final class CampModeJob extends JobService {
  private final java.util.Map<Integer, AtomicBoolean> tasks = new java.util.HashMap<>();
  @Override public boolean onStartJob(JobParameters parameters) {
    if(!FirstUseConsent.accepted(this))return false;
    AtomicBoolean token = new AtomicBoolean(false);
    tasks.put(parameters.getJobId(), token);
    VehicleCoordinator.get(this).checkCamp(token, () -> {
      if (tasks.get(parameters.getJobId()) == token) tasks.remove(parameters.getJobId());
      if (!token.get()) jobFinished(parameters, false);
    });
    return true;
  }
  @Override public boolean onStopJob(JobParameters parameters) {
    if(!FirstUseConsent.accepted(this))return false;
    AtomicBoolean token = tasks.remove(parameters.getJobId());
    if (token != null) token.set(true);
    // Ask JobScheduler to retry after connectivity/process interruption, not to issue commands blindly.
    return CampModeManager.needsRecovery(this);
  }
}
