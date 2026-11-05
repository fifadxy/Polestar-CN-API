package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
public final class V100Beta42PolicyTest {
 @Test public void adaptiveParkingNeverFloods(){assertEquals(15,ParkingRefreshStrategy.intervalMinutes("smart",15,0,0));assertEquals(30,ParkingRefreshStrategy.intervalMinutes("smart",15,1,0));assertEquals(60,ParkingRefreshStrategy.intervalMinutes("smart",15,3,0));assertEquals(120,ParkingRefreshStrategy.intervalMinutes("smart",15,3,4));}
 @Test public void fixedAndManual(){assertEquals(5,ParkingRefreshStrategy.intervalMinutes("fixed",5,4,0));assertEquals(0,ParkingRefreshStrategy.intervalMinutes("manual",30,4,0));}
 @Test public void locationNullHandling(){assertFalse(ParkingRefreshStrategy.same(null,null));}
}