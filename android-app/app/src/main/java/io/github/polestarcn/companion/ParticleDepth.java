package io.github.polestarcn.companion;
/** Stable size-ranked depth, recalculated only when the visible pool count changes. */
final class ParticleDepth {
 private final float[] sizes;private final int[] ranks;private int cachedCount=-1;
 ParticleDepth(float[] sizes){this.sizes=sizes;this.ranks=new int[sizes.length];}
 boolean behind(int index,int count,int percent){
  count=Math.max(0,Math.min(count,sizes.length));
  if(cachedCount!=count){for(int i=0;i<count;i++){int rank=0;for(int j=0;j<count;j++)if(sizes[j]<sizes[i]||(sizes[j]==sizes[i]&&j<i))rank++;ranks[i]=rank;}cachedCount=count;}
  return index>=0&&index<count&&ranks[index]>=count-Math.round(count*Math.max(0,Math.min(100,percent))/100f);
 }
}
