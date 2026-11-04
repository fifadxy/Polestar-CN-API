package io.github.polestarcn.oppobridge;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
/** Resolve stable names first; ambiguous obfuscated shapes are never guessed. */
final class HostMethodResolver {
  static Method cardClick(Class<?> type) {
    Method named=null,legacy=null;
    for(Method method:type.getDeclaredMethods()) {
      if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=void.class)continue;
      Class<?>[] p=method.getParameterTypes();
      String lower=method.getName().toLowerCase(java.util.Locale.ROOT);
      boolean semantic=(lower.contains("click")||lower.contains("shortcut")||lower.contains("action")
          ||lower.contains("control")||lower.contains("execute")||lower.contains("handle"));
      boolean hasString=false;
      for(Class<?> value:p)if(value==String.class)hasString=true;
      if(semantic&&hasString&&p.length>=2&&p.length<=4) {
        if(named!=null)throw new IllegalStateException("Card click callback ambiguous");
        named=method;
      }
      if(p.length==3&&p[0].isInterface()&&p[1]==String.class&&p[2]==Object.class) {
        if(legacy!=null)throw new IllegalStateException("Card click callback ambiguous");
        legacy=method;
      }
    }
    if(named!=null)return named;
    if(legacy!=null)return legacy;
    throw new IllegalStateException("Card click callback missing");
  }
  static Method register(Class<?> type) { return resolve(type,"addDeviceObserver",true); }
  static Method unregister(Class<?> type) { return resolve(type,"removeDeviceObserver",false); }
  private static Method resolve(Class<?> type,String name,boolean add) {
    Method named=null,shape=null;int count=0;
    for(Method method:type.getDeclaredMethods()) {
      Class<?>[] p=method.getParameterTypes();
      if(Modifier.isStatic(method.getModifiers())||method.getReturnType()!=void.class)continue;
      boolean matches=add?p.length==2&&p[0]==String.class&&p[1].isInterface():p.length==1&&p[0]==String.class;
      if(!matches)continue;
      if(name.equals(method.getName())) {if(named!=null)throw new IllegalStateException(name+" ambiguous");named=method;}
      if(add||!Modifier.isFinal(method.getModifiers())) {shape=method;count++;}
    }
    if(named!=null)return named;
    if(count!=1)throw new IllegalStateException(name+(count==0?" missing":" ambiguous"));
    return shape;
  }
  static Method supportList(Class<?> type) {
    for(Method m:type.getDeclaredMethods())if(m.getName().equals("getSupportDeviceApplications")
        &&!Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==0&&List.class.isAssignableFrom(m.getReturnType()))return m;
    return null;
  }
}
