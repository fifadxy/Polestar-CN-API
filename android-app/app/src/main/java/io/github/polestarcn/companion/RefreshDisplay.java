package io.github.polestarcn.companion;
/** A UI fingerprint that excludes Java object identity and background scheduling bookkeeping. */
final class RefreshDisplay {
 private RefreshDisplay(){}
 static boolean changed(VehicleSnapshot a,VehicleSnapshot b){
   if(a==b)return false;if(a==null||b==null)return true;
   return !java.util.Objects.equals(a.batteryPercent,b.batteryPercent)
       ||!java.util.Objects.equals(a.rangeKm,b.rangeKm)
       ||!java.util.Objects.equals(a.charging,b.charging)
       ||!java.util.Objects.equals(a.lock,b.lock)
       ||!java.util.Objects.equals(a.doors,b.doors)
       ||!java.util.Objects.equals(a.windows,b.windows)
       ||!java.util.Objects.equals(a.tailgate,b.tailgate)
       ||!java.util.Objects.equals(a.climate,b.climate)
       ||!java.util.Objects.equals(a.air,b.air)
       ||!java.util.Objects.equals(a.odometerKm,b.odometerKm)
       ||!java.util.Objects.equals(a.environment.aqi,b.environment.aqi)
       ||!java.util.Objects.equals(a.environment.pm25UgM3,b.environment.pm25UgM3)
       ||!java.util.Objects.equals(a.health.reportedAt,b.health.reportedAt)
       ||a.timestampMillis!=b.timestampMillis;
 }
}
