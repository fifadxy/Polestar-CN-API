package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;

public final class MaintenanceScheduleTest {
  @Test public void defaultsAre365DaysAnd20000Km() {
    MaintenanceSchedule.Status s = MaintenanceSchedule.evaluate(
        LocalDate.of(2026, 1, 1), 10_000,
        LocalDate.of(2026, 4, 11), 16_000,
        MaintenanceSchedule.DEFAULT_INTERVAL_DAYS, MaintenanceSchedule.DEFAULT_INTERVAL_KM);
    assertTrue(s.configured);
    assertFalse(s.due);
    assertEquals(100, s.elapsedDays);
    assertEquals(Integer.valueOf(6_000), s.elapsedKm);
    assertTrue(s.summary(false).contains("距上次保养 100天"));
  }

  @Test public void timeOverdueAndMileageOverdueAreIndependent() {
    MaintenanceSchedule.Status time = MaintenanceSchedule.evaluate(
        LocalDate.of(2025, 10, 1), 20_000,
        LocalDate.of(2026, 10, 11), 25_000, 365, 20_000);
    assertTrue(time.due);
    assertEquals(10, time.overdueDays);
    assertEquals(Integer.valueOf(0), time.overdueKm);

    MaintenanceSchedule.Status mileage = MaintenanceSchedule.evaluate(
        LocalDate.of(2026, 9, 1), 20_000,
        LocalDate.of(2026, 10, 1), 41_250, 365, 20_000);
    assertTrue(mileage.due);
    assertEquals(0, mileage.overdueDays);
    assertEquals(Integer.valueOf(1_250), mileage.overdueKm);
    assertTrue(mileage.summary(false).contains("里程超 1,250km"));
  }

  @Test public void odometerRollbackDoesNotCreateNegativeServiceDistance() {
    MaintenanceSchedule.Status s = MaintenanceSchedule.evaluate(
        LocalDate.of(2026, 1, 1), 30_000,
        LocalDate.of(2026, 2, 1), 29_000, 365, 20_000);
    assertNull(s.elapsedKm);
    assertFalse(s.due);
  }
}
