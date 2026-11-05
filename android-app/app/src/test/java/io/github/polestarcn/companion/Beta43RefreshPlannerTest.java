package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
/** Clock boundaries must not turn a recent successful read into a new request. */
public final class Beta43RefreshPlannerTest {
 @Test public void respectsTtlAndClockSkew(){
  assertTrue(RefreshPlanner.fresh(1000,1500,1000));
  assertFalse(RefreshPlanner.fresh(1000,2000,1000));
  assertFalse(RefreshPlanner.fresh(1000,900,1000));
  assertFalse(RefreshPlanner.fresh(0,5000,1000));
  assertTrue(RefreshPlanner.shouldRead(1000,2000,1000));
 }
 @Test public void twoSecondMergeLeavesP0Independent(){
  assertEquals(2,RefreshPlanner.shouldRead(1000,1500,1000)?0:2);
 }
}
