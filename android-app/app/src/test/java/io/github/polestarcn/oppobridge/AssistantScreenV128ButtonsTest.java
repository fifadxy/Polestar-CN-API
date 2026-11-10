package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.os.Bundle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class AssistantScreenV128ButtonsTest {
  public static final class Menu {
    public final String id,label,uri;
    public final boolean toggled;
    public Menu(String id,String title,String label,int a,int b,boolean active,String uri){
      this.id=id;this.label=label;this.uri=uri;this.toggled=active;
    }
  }
  @Test public void fourButtonsAreExactlyTheConfirmedControls() throws Exception {
    Bundle state=new Bundle();
    state.putString("lock","已上锁");
    state.putString("climate","运行中");
    Constructor<Menu> ctor=Menu.class.getConstructor(
        String.class,String.class,String.class,int.class,int.class,boolean.class,String.class);
    Method method=AssistantScreenNativeVehicleHook.class.getDeclaredMethod(
        "menus",Constructor.class,Bundle.class,String.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<Menu> menus=(List<Menu>)method.invoke(null,ctor,state,"psassistant-companion");
    assertEquals(4,menus.size());
    assertEquals("车锁",menus.get(0).label);
    assertEquals("空调",menus.get(1).label);
    assertEquals("闪灯",menus.get(2).label);
    assertEquals("鸣笛",menus.get(3).label);
    assertTrue(menus.get(0).toggled);
    assertTrue(menus.get(1).toggled);
    assertFalse(menus.get(2).toggled);
    assertFalse(menus.get(3).toggled);
    assertTrue(menus.get(2).uri.endsWith("action=flash"));
    assertTrue(menus.get(3).uri.endsWith("action=honk"));
    for(Menu item:menus){
      assertFalse(item.uri.contains("air-toggle"));
      assertFalse(item.uri.contains("trunk-toggle"));
    }
  }
}
