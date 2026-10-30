package io.github.polestarcn.oppobridge;
import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
public class HostMethodResolverTest {
  interface Observer {}
  static class Named {
    public void a(String p,Observer o){} public void b(String p,Observer o){}
    public void addDeviceObserver(String p,Observer o){}
    public void c(String p){} public void d(String p){} public void removeDeviceObserver(String p){}
    public List<Object> getSupportDeviceApplications(){return null;}
  }
  static class Ambiguous {public void a(String p,Observer o){} public void b(String p,Observer o){}}
  static class Legacy {public void a(String p,Observer o){} public final void f(String p){} public void d(String p){}}
  @Test public void namedMethodsTakePriorityOverOtherMatchingShapes() {
    assertEquals("addDeviceObserver",HostMethodResolver.register(Named.class).getName());
    assertEquals("removeDeviceObserver",HostMethodResolver.unregister(Named.class).getName());
    assertEquals("getSupportDeviceApplications",HostMethodResolver.supportList(Named.class).getName());
  }
  @Test(expected=IllegalStateException.class) public void ambiguousLegacyShapeIsRejected(){HostMethodResolver.register(Ambiguous.class);}
  @Test public void legacyUnregisterIgnoresFinalLookalike(){assertEquals("d",HostMethodResolver.unregister(Legacy.class).getName());}
}
