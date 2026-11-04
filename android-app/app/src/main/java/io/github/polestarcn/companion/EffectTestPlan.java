package io.github.polestarcn.companion;
import android.content.Context;import java.util.*;
/** Persistent selection of offline test projects, expanded into a bounded cycle only when starting. */
final class EffectTestPlan {
 static final String KEY="effect_auto_scenes",DEFAULT="0,1,2,3,4,6,7,8";
 static final class Case {final int scene,style,direction;Case(int s,int st,int dir){scene=s;style=st;direction=dir;}}
 static List<Integer> selected(Context c){List<Integer> out=new ArrayList<>();for(String raw:AppPreferences.get(c,KEY,DEFAULT).split(",")){try{int n=Integer.parseInt(raw);if(n>=0&&n<=8&&!out.contains(n))out.add(n);}catch(NumberFormatException ignored){}}return out;}
 static List<Case> cases(Context c){List<Case> out=new ArrayList<>();for(int n:selected(c)){if(n==0){for(int style=0;style<5;style++)for(int dir=0;dir<2;dir++)out.add(new Case(0,style,dir));}else if(n>=1&&n<=4){for(int dir=0;dir<2;dir++)out.add(new Case(n,-1,dir));}else out.add(new Case(n,-1,-1));}return out;}
 static void save(Context c,List<Integer> values){List<String> valid=new ArrayList<>();for(int n:values)if(n>=0&&n<=8&&!valid.contains(String.valueOf(n)))valid.add(String.valueOf(n));AppPreferences.set(c,KEY,String.join(",",valid));}
 private EffectTestPlan(){}
}
