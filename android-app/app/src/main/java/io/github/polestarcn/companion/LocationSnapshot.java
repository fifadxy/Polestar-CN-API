package io.github.polestarcn.companion;
import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Map;
/** Last parked location, held in memory only. Coordinates never enter display cache or logs. */
public final class LocationSnapshot {
  public final double latitude,longitude;public final long reportedAt;public final boolean stale;
  private LocationSnapshot(double lat,double lon,long at,boolean stale){latitude=lat;longitude=lon;reportedAt=at;this.stale=stale;}
  public boolean valid(){return Double.isFinite(latitude)&&Double.isFinite(longitude)&&Math.abs(latitude)<=90&&Math.abs(longitude)<=180&&(latitude!=0||longitude!=0);}
  public static LocationSnapshot fromReply(Map<Integer,Object> reply){
    Map<Integer,Object> point=ProtoWire.nested(reply,2);Double lon=ProtoWire.decimal(point,1),lat=ProtoWire.decimal(point,2);
    if(lat==null||lon==null||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||(lat==0&&lon==0))return null;
    Integer stale=ProtoWire.integer(reply,3);
    return new LocationSnapshot(lat,lon,ProtoWire.timestampMillis(point,3),stale!=null&&stale!=0);
  }
}
