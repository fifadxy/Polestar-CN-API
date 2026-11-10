package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
public class TmCostTest {
 @Test public void instantaneousTmUsesDistanceAndHundredKmConsumption(){assertEquals(10.0,TmCost.kwh(50.0,20.0),.00001);assertEquals(8.0,TmCost.cost(10.0,.8),.00001);}
 @Test public void missingOrInvalidTmNeverInventsConsumption(){assertNull(TmCost.kwh(null,20.0));assertNull(TmCost.kwh(40.0,null));assertNull(TmCost.kwh(-1.0,19.0));assertNull(TmCost.kwh(40.0,Double.NaN));}
 @Test public void costRatesAndTaEstimateUseTmConsumptionWithoutInventingTaAverage(){
   assertEquals(24.0,TmCost.yuanPer100Km(20.0,1.2),.00001);
   assertEquals(.24,TmCost.yuanPerKm(20.0,1.2),.00001);
   assertEquals(3.6,TmCost.kwh(18.0,20.0),.00001);
   assertEquals(4.32,TmCost.cost(3.6,1.2),.00001);
   assertNull(TmCost.yuanPer100Km(20.0,0));
   assertNull(TmCost.yuanPerKm(null,1.2));
 }
 @Test public void demoAndCachedSamplesAreNotPresentedAsLive(){long now=System.currentTimeMillis();VehicleSnapshot demo=new VehicleSnapshot("demo","id",70,300,1000,"充电中","全部关闭","全部关闭","已关闭","已上锁",now,"DEMO",true);assertNull(TmCost.kwh(demo,now));}
}
