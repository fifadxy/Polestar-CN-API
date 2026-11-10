package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import java.util.Locale;
import org.junit.Test;

/** Chart annotations and axes use observed values, not invented missing-period data. */
public final class EnergyChartDisplayV126Test {
  @Test public void latestMeansLastActualObservationNotTheLastEmptyBin(){
    double[] values={1.25,Double.NaN,0.0,Double.NaN};
    assertArrayEquals(new int[]{2},EnergyChartDisplay.annotated(values,EnergyChartDisplay.LATEST,-1));
    assertArrayEquals(new int[]{0,2},EnergyChartDisplay.annotated(values,EnergyChartDisplay.ALL,-1));
    assertArrayEquals(new int[]{},EnergyChartDisplay.annotated(values,EnergyChartDisplay.HIDDEN,-1));
    assertArrayEquals(new int[]{0},EnergyChartDisplay.annotated(values,EnergyChartDisplay.LATEST,0));
    assertArrayEquals(new int[]{2},EnergyChartDisplay.annotated(values,EnergyChartDisplay.LATEST,1));
  }
  @Test public void invalidReadingsCannotAcquireCallouts(){
    double[] values={Double.NaN,Double.POSITIVE_INFINITY,-1,Double.NaN};
    assertEquals(0,EnergyChartDisplay.annotated(values,EnergyChartDisplay.LATEST,-1).length);
    assertEquals(0,EnergyChartDisplay.annotated(values,EnergyChartDisplay.ALL,-1).length);
    assertFalse(EnergyChartDisplay.valid(-1));
  }
  @Test public void labelsCanCycleAndPersist(){
    assertEquals(EnergyChartDisplay.LATEST,EnergyChartDisplay.mode(null));
    assertEquals(EnergyChartDisplay.ALL,EnergyChartDisplay.next(EnergyChartDisplay.LATEST));
    assertEquals(EnergyChartDisplay.HIDDEN,EnergyChartDisplay.next(EnergyChartDisplay.ALL));
    assertEquals(EnergyChartDisplay.LATEST,EnergyChartDisplay.next(EnergyChartDisplay.HIDDEN));
    assertEquals(EnergyChartDisplay.ALL,EnergyChartDisplay.mode(EnergyChartDisplay.preference(EnergyChartDisplay.ALL)));
    assertEquals(EnergyChartDisplay.HIDDEN,EnergyChartDisplay.mode(EnergyChartDisplay.preference(EnergyChartDisplay.HIDDEN)));
  }
  @Test public void fiveYAxisTickMarksEncloseEachMetricRange(){
    for(double max:new double[]{.009,.15,.9,1.8,4.2,37,850,12000}){
      double step=EnergyChartDisplay.tickStep(max);
      assertTrue(step>0);
      assertTrue(4*step+1e-9>=max);
      assertTrue(EnergyChartDisplay.tick(0,step,Locale.US).length()>0);
      assertTrue(EnergyChartDisplay.tick(4*step,step,Locale.US).length()>0);
    }
    assertEquals(1d,EnergyChartDisplay.tickStep(3.8),.00001);
    assertEquals(10d,EnergyChartDisplay.tickStep(37),.00001);
    assertEquals("2.5",EnergyChartDisplay.tick(2.5,2.5,Locale.US));
    assertEquals("0.25",EnergyChartDisplay.tick(.25,.25,Locale.US));
  }
  @Test public void calloutValuesAlwaysMatchMetricUnits(){
    assertEquals("1.25 kWh",EnergyChartDisplay.value(0,1.25,Locale.US));
    assertEquals("0.50 kWh",EnergyChartDisplay.value(1,.5,Locale.US));
    assertEquals("12.00 km",EnergyChartDisplay.value(2,12,Locale.US));
    assertEquals("\u00a53.45",EnergyChartDisplay.value(3,3.45,Locale.US));
    assertEquals("--",EnergyChartDisplay.value(0,Double.NaN,Locale.US));
    assertEquals("km",EnergyChartDisplay.unit(2));
  }
}
