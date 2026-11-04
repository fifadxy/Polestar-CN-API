package io.github.polestarcn.companion;
/** Deterministic smooth paths: no per-frame random numbers, paths, or allocations. */
final class ParticleMotion {
 private static final double PI=Math.PI;
 static boolean curved(int index,EffectSettings p){return p.turbulence&&(index*37+13)%100<p.turbulencePercent&&p.arcPercent>0;}
 static float offset(float t,int index){double u=PI*t,seed=(index*2.399963)%(2*PI);return (float)(Math.sin(u)*(.62*Math.sin(2*u+seed)+.38*Math.sin(4*u+seed*.73f)));}
 static float slope(float t,int index){double u=PI*t,seed=(index*2.399963)%(2*PI);return (float)(PI*Math.cos(u)*(.62*Math.sin(2*u+seed)+.38*Math.sin(4*u+seed*.73f))+Math.sin(u)*(1.24*PI*Math.cos(2*u+seed)+1.52*PI*Math.cos(4*u+seed*.73f)));}
 private ParticleMotion(){}
}
