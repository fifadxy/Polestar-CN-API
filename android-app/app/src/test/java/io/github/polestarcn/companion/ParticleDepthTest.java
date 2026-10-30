package io.github.polestarcn.companion;
import static org.junit.Assert.*;import org.junit.Test;
public class ParticleDepthTest {
 @Test public void largestParticlesGoBehindAtExactSelectedRatio(){ParticleDepth d=new ParticleDepth(new float[]{3,1,4,2});assertTrue(d.behind(0,4,50));assertTrue(d.behind(2,4,50));assertFalse(d.behind(1,4,50));assertFalse(d.behind(3,4,50));}
 @Test public void budgetChangesAndTiesStayStable(){ParticleDepth d=new ParticleDepth(new float[]{2,2,1,4});assertFalse(d.behind(0,2,50));assertTrue(d.behind(1,2,50));assertTrue(d.behind(3,4,25));assertFalse(d.behind(1,4,25));for(int i=0;i<4;i++){assertFalse(d.behind(i,4,0));assertTrue(d.behind(i,4,100));}}
}
