package io.github.polestarcn.companion;
import android.content.Context;
import android.os.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/** Typed API adapters only. Uploaded default/fallback/optimistic result semantics stay in its namespace. */
public final class UploadedCarLinkRuntimeAdapter {
 private UploadedCarLinkRuntimeAdapter(){}
 public static final class Snapshot {
  public Boolean locked,climateActive,windowClosed;public Integer soc,range;
 }
 public static Object snapshot(Context c){VehicleSnapshot v=VehicleCoordinator.get(c).snapshot;if(v==null)v=VehicleSnapshotStore.loadChina(c);if(v==null)return null;Snapshot out=new Snapshot();out.soc=v.batteryPercent;out.range=v.rangeKm;
  if("已锁".equals(v.lock)||"已上锁".equals(v.lock))out.locked=true;else if("未锁".equals(v.lock))out.locked=false;
  if("开启".equals(v.climate))out.climateActive=true;else if("关闭".equals(v.climate))out.climateActive=false;
  String windows=ControlAction.next("windows",v);if(windows!=null)out.windowClosed="windows-open".equals(windows);return out;
 }
 public static String selectedVin(Context c){VehicleCoordinator m=VehicleCoordinator.get(c);if(m.selected!=null&&m.selected.vin!=null)return m.selected.vin;
  io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo saved=io.github.polestarcn.companion.auth.SecureVehicleSelection.load(c,AppPreferences.get(c,"vehicle",""));return saved==null?"":saved.vin;}
 public static boolean enqueue(Context c,String uploadedAction){final String action;switch(uploadedAction){case "LOCK":action="lock";break;case "UNLOCK":action="unlock";break;case "CLIMATE_START":action="climate-start";break;case "CLIMATE_STOP":action="climate-stop";break;case "WINDOW_CLOSE":action="windows-close";break;case "WINDOW_VENT":action="windows-open";break;case "FIND_CAR":action="honk-flash";break;default:return false;}
  boolean[] submitted={false};CountDownLatch done=new CountDownLatch(1);Runnable submit=()->{try{VehicleCoordinator m=VehicleCoordinator.get(c);if(FirstUseConsent.accepted(c)&&m.canCommand(action)){m.command(action,state->{/* Existing coordinator independently records the real result. */});submitted[0]=true;}}finally{done.countDown();}};
  if(Looper.myLooper()==Looper.getMainLooper())submit.run();else{new Handler(Looper.getMainLooper()).post(submit);try{if(!done.await(3,TimeUnit.SECONDS))return true;}catch(InterruptedException interrupted){Thread.currentThread().interrupt();return false;}}
  return submitted[0];
 }
}
