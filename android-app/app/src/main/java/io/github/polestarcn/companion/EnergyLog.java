package io.github.polestarcn.companion;
import android.content.*;import android.database.Cursor;import android.database.sqlite.*;import java.util.*;import java.util.concurrent.*;
/** Bounded, sampled observations, never a vehicle energy meter. All I/O uses WORK. */
public final class EnergyLog extends SQLiteOpenHelper {
 public static final ExecutorService WORK=Executors.newSingleThreadExecutor();public static final double KWH_PER_PERCENT=.71;@android.annotation.SuppressLint("StaticFieldLeak") private static EnergyLog instance; // Holds app context, never activity context.
 public static synchronized EnergyLog get(Context c){if(instance==null)instance=new EnergyLog(c.getApplicationContext());return instance;}
 private long lastPrune;private int inserts;private final android.app.Application c;private EnergyLog(Context c){super(c,"energy_history.db",null,1);this.c=(android.app.Application)c.getApplicationContext();setWriteAheadLoggingEnabled(true);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE samples(vehicle TEXT NOT NULL,t INTEGER NOT NULL,soc REAL,tm REAL,cons REAL,speed REAL,odo REAL,climate INTEGER,charge INTEGER,ac REAL NOT NULL,drive REAL NOT NULL,acvalid INTEGER NOT NULL,PRIMARY KEY(vehicle,t))");db.execSQL("CREATE INDEX sample_time ON samples(t)");}
 public void onUpgrade(SQLiteDatabase db,int old,int next){throw new IllegalStateException("Unsupported energy schema");}
 public static boolean validVehicle(String id){return id!=null&&id.matches("[a-fA-F0-9]{8,64}");}
 public synchronized List<String> vehicles(){List<String> ids=new ArrayList<>();try(Cursor r=getReadableDatabase().rawQuery("SELECT vehicle FROM samples GROUP BY vehicle ORDER BY MAX(t) DESC",null)){while(r.moveToNext()){String id=r.getString(0);if(validVehicle(id))ids.add(id);}}return ids;}
 public static int months(Context c){try{return Math.max(1,Math.min(24,Integer.parseInt(AppPreferences.get(c,"energy_months","24"))));}catch(Exception e){return 24;}}
 public static double capacity(Context c){try{double v=Double.parseDouble(AppPreferences.get(c,"battery_capacity_kwh","71"));return Double.isFinite(v)&&v>=10&&v<=200?v:71;}catch(Exception e){return 71;}}
 public static double perPercent(Context c){return capacity(c)/100.;}
 public static double price(Context c){try{double v=Double.parseDouble(AppPreferences.get(c,"energy_price","0"));return Double.isFinite(v)&&v>=0&&v<=100?v:0;}catch(Exception e){return 0;}}
 public static void record(Context c,String vehicle,VehicleSnapshot s){if(s==null||s.isDemo||!"LIVE".equals(s.source)||vehicle==null||vehicle.isEmpty())return;AppPreferences.set(c,"energy_last_vehicle",vehicle);Context app=c.getApplicationContext();WORK.execute(()->{try{get(app).append(vehicle,s);}catch(Exception e){DiagnosticLog.add("Energy observation not saved");}});}
 static Double number(Cursor r,int index){return r.isNull(index)?null:r.getDouble(index);}static void put(ContentValues v,String key,Double x){if(x!=null&&Double.isFinite(x))v.put(key,x);}
 public synchronized void append(String vehicle,VehicleSnapshot s){long now=System.currentTimeMillis(),t=s.timestampMillis;if(s.isDemo||!"LIVE".equals(s.source)||t<=0||t>now+300000||now-t>1200000)return;
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
   double ac=0,drive=0;boolean valid=false;int climate="开启".equals(s.climate)?1:0,charge=ChargingVisibility.shouldShow(s)?1:0;Double odo=s.mileage.totalMeters==null?null:s.mileage.totalMeters/1000.;
   try(Cursor p=db.rawQuery("SELECT t,soc,tm,cons,odo,climate,charge FROM samples WHERE vehicle=? ORDER BY t DESC LIMIT 1",new String[]{vehicle})){
    if(p.moveToFirst()){long age=t-p.getLong(0);if(age<=0)return;if(age<300000&&climate==p.getInt(5)&&charge==p.getInt(6))return;
     Double soc=number(p,1),tm=number(p,2),cons=number(p,3),oldOdo=number(p,4);
     boolean still=odo!=null&&oldOdo!=null?Math.abs(odo-oldOdo)<=.05:s.mileage.manualKm!=null&&tm!=null&&Math.abs(s.mileage.manualKm-tm)<=.05;
     boolean moving=odo!=null&&oldOdo!=null?odo-oldOdo>.05:s.mileage.manualKm!=null&&tm!=null&&s.mileage.manualKm-tm>.05;
     valid=age<=1200000&&still&&climate==1&&p.getInt(5)==1&&charge==0&&p.getInt(6)==0&&soc!=null&&s.batteryPercent!=null;
     if(valid){double drop=soc-s.batteryPercent;if(drop>=0&&drop<=10)ac=drop;else valid=false;}
     if(age<=1800000&&moving&&charge==0&&p.getInt(6)==0&&tm!=null&&cons!=null&&s.mileage.manualKm!=null&&s.mileage.manualConsumption!=null&&s.mileage.manualKm>=tm){double diff=s.mileage.manualKm*s.mileage.manualConsumption/100-tm*cons/100;if(diff>=0&&diff<=200*age/3600000.)drive=diff;}
    }
   }
   ContentValues v=new ContentValues();v.put("vehicle",vehicle);v.put("t",t);if(s.batteryPercent!=null&&s.batteryPercent>=0&&s.batteryPercent<=100)v.put("soc",s.batteryPercent);put(v,"tm",s.mileage.manualKm);put(v,"cons",s.mileage.manualConsumption);put(v,"speed",s.mileage.manualAverageSpeed);put(v,"odo",odo);v.put("climate",climate);v.put("charge",charge);v.put("ac",ac);v.put("drive",drive);v.put("acvalid",valid?1:0);db.insertOrThrow("samples",null,v);if(++inserts>=288||System.currentTimeMillis()-lastPrune>=86400000L){prune(db);inserts=0;lastPrune=System.currentTimeMillis();}db.setTransactionSuccessful();
  }finally{db.endTransaction();}}
 private void prune(SQLiteDatabase db){Calendar cal=Calendar.getInstance();cal.add(Calendar.MONTH,-months(c));db.delete("samples","t<?",new String[]{""+cal.getTimeInMillis()});db.execSQL("DELETE FROM samples WHERE rowid IN (SELECT rowid FROM samples ORDER BY t DESC LIMIT -1 OFFSET 249712)");}
 public synchronized void prune(){prune(getWritableDatabase());}public synchronized void clear(){getWritableDatabase().delete("samples",null,null);getWritableDatabase().execSQL("VACUUM");try(Cursor checkpoint=getWritableDatabase().rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){checkpoint.moveToFirst();}}
 public static final class Point{public long t;public Double soc,cons,speed;public double ac,drive,distanceKm;public int count;}
 public static final class Report{public final List<Point> points=new ArrayList<>();public double ac,drive,distanceKm;public int count;}
 /** The full retained interval is the default chart range. Returns at most 24 monthly
  * points while streaming raw rows on the single background worker; no UI row list. */
 public synchronized Report readRetention(String vehicle){
  long now=System.currentTimeMillis();Calendar first=Calendar.getInstance();
  first.setTimeInMillis(now);first.add(Calendar.MONTH,-months(c));
  return readWindow(vehicle,2,first.getTimeInMillis(),now,2678400000L);
 }
 public synchronized Report read(String vehicle,int range){return read(vehicle,range,System.currentTimeMillis());}
 public synchronized Report read(String vehicle,int range,long until){long now=Math.min(until,System.currentTimeMillis());Calendar cal=Calendar.getInstance();cal.setTimeInMillis(now);long bucket;
  if(range==0){cal.add(Calendar.DAY_OF_YEAR,-7);bucket=43200000L;}else if(range==1){cal.add(Calendar.DAY_OF_YEAR,-31);bucket=86400000L;}else if(range==2){cal.add(Calendar.MONTH,-12);bucket=2678400000L;}else{cal.add(Calendar.MONTH,-24);bucket=31622400000L;}
  return readWindow(vehicle,range,cal.getTimeInMillis(),now,bucket);
 }
 /** Rolling 3/7/31-day periods; never replaces the existing EnergyActivity range numbering. */
 public synchronized Report readRecentDays(String vehicle,int days){
  if(days!=3&&days!=7&&days!=31)throw new IllegalArgumentException("Supported periods: 3, 7, 31");
  long now=System.currentTimeMillis();Calendar start=Calendar.getInstance();start.setTimeInMillis(now);start.add(Calendar.DAY_OF_YEAR,-days);
  return readWindow(vehicle,-1,start.getTimeInMillis(),now,days<=3?43200000L:86400000L);
 }
 /** Custom inclusive interval, limited to local data retention and current time. */
 public synchronized Report readBetween(String vehicle,long start,long until){
  long now=Math.min(until,System.currentTimeMillis());Calendar retained=Calendar.getInstance();retained.add(Calendar.MONTH,-months(c));
  start=Math.max(start,retained.getTimeInMillis());
  if(start<=0||start>now)return new Report();
  long span=now-start;
  // Long custom ranges are summarized by month, rather than materializing
  // hundreds of daily buckets before the chart's own grouping pass.
  return span>210L*86400000L
      ?readWindow(vehicle,2,start,now,2678400000L)
      :readWindow(vehicle,-1,start,now,span<=7L*86400000L?43200000L:86400000L);
 }

 /** TM odometer is the primary source for daily distance. Never count trip resets
  * as negative mileage or backfill an unknown gap. Fallback to total odometer
  * only if either TM reading is absent. */
 static double distanceIncrement(Double lastTm,Double tm,Double lastOdo,Double odo,long intervalMillis){
  if(intervalMillis<=0||intervalMillis>3600000L)return 0;
  Double delta=null;
  if(lastTm!=null&&tm!=null&&Double.isFinite(lastTm)&&Double.isFinite(tm))
    delta=tm-lastTm;
  else if(lastOdo!=null&&odo!=null&&Double.isFinite(lastOdo)&&Double.isFinite(odo))
    delta=odo-lastOdo;
  if(delta==null||!Double.isFinite(delta)||delta<=0)return 0;
  double plausible=200.0*intervalMillis/3600000.0+0.1;
  return delta<=plausible?delta:0;
 }

 private Report readWindow(String vehicle,int range,long start,long now,long bucket){
  Report out=new Report();
  if(vehicle==null||vehicle.isEmpty()||start>now)return out;
  // Reads never prune the database: daily/288-insertion maintenance already does
  // that in append(). Avoid a blocking write-lock/VACUUM on every graph tap.
  SQLiteDatabase db=getReadableDatabase();
  Map<Long,Point> grouped=new LinkedHashMap<>();
  Double lastOdo=null,lastTm=null;long lastSampleAt=0;
  // Seed the first boundary from a preceding sample, but only a <=1h gap
  // can contribute distance. This avoids losing the first leg of a day.
  try(Cursor prev=db.rawQuery("SELECT t,odo,tm FROM samples WHERE vehicle=? AND t<? ORDER BY t DESC LIMIT 1",
      new String[]{vehicle,Long.toString(start)})){
    if(prev.moveToFirst()){lastSampleAt=prev.getLong(0);lastOdo=number(prev,1);lastTm=number(prev,2);}
  }
  Calendar month=Calendar.getInstance();TimeZone local=TimeZone.getDefault();
  try(Cursor rows=db.rawQuery(
      "SELECT t,soc,cons,speed,ac,drive,acvalid,odo,tm FROM samples "
      +"WHERE vehicle=? AND t>=? AND t<=? ORDER BY t",
      new String[]{vehicle,Long.toString(start),Long.toString(now)})){
    while(rows.moveToNext()){
      long time=rows.getLong(0),key;
      if(range>=2){
        month.setTimeInMillis(time);
        key=range==2?month.get(Calendar.YEAR)*12L+month.get(Calendar.MONTH):month.get(Calendar.YEAR);
      }else key=(time+local.getOffset(time))/bucket;
      Double odo=number(rows,7),tm=number(rows,8);
      double km=distanceIncrement(lastTm,tm,lastOdo,odo,time-lastSampleAt);
      lastOdo=odo;lastTm=tm;lastSampleAt=time;
      Point point=grouped.get(key);
      if(point==null){point=new Point();grouped.put(key,point);}
      point.t=time;
      point.distanceKm+=km;out.distanceKm+=km;
      if(rows.getInt(6)==1)point.soc=number(rows,1);
      point.cons=number(rows,2);point.speed=number(rows,3);
      double air=rows.getDouble(4),drive=rows.getDouble(5);
      point.ac+=air;point.drive+=drive;point.count++;
      out.ac+=air;out.drive+=drive;out.count++;
    }
  }
  out.points.addAll(grouped.values());
  return out;
 }
}
