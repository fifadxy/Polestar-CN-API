package io.github.polestarcn.companion;
/** No unsupported HVAC mode inference. Preview states never enter vehicle telemetry. */
public final class EffectStateController {
 public enum Mode { OFF, CHARGING, NEUTRAL, COOLING, HEATING, VENTILATION, PURIFICATION }
 private EffectStateController(){}
 public static boolean fresh(VehicleSnapshot s,long now){return s!=null&&!s.isDemo&&"LIVE".equals(s.source)&&s.timestampMillis>0&&s.timestampMillis<=now+300000&&now-s.timestampMillis<900000;}
 public static Mode resolve(VehicleSnapshot s,EffectSettings prefs,long now){if(!prefs.master||!fresh(s,now))return Mode.OFF;
  String charging=s.charging==null?"":s.charging.toLowerCase(java.util.Locale.ROOT);
  if(prefs.charge&&(charging.equals("充电中")||charging.equals("智能充电")||charging.equals("charging")||charging.equals("smart charging")))return Mode.CHARGING;
  if(prefs.air&&"开启".equals(s.climate)&&s.environment.climateReportedAt>0&&now-s.environment.climateReportedAt<900000&&s.environment.climateReportedAt<=now+300000)return "heating".equals(prefs.airTint)?Mode.HEATING:"cooling".equals(prefs.airTint)?Mode.COOLING:Mode.NEUTRAL;
  if(prefs.air&&"开启".equals(s.air)&&s.environment.airReportedAt>0&&now-s.environment.airReportedAt<900000&&s.environment.airReportedAt<=now+300000)return Mode.PURIFICATION;
  return Mode.OFF;
 }
}
