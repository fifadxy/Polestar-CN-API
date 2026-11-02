package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class PolestarProviderCatalogShimTest {
  public static final class Provider {
    private String spCode; private String name; private String iconUrl="sample-icon";
    public Provider() {}
    Provider(String code,String name){this.spCode=code;this.name=name;}
    public String getSpCode(){return spCode;} public void setSpCode(String value){spCode=value;}
    public String getName(){return name;} public void setName(String value){name=value;}
    public String getIconUrl(){return iconUrl;} public void setIconUrl(String value){iconUrl=value;}
  }


  public static final class SmartApp {
    private String appName; private String packageName;
    public SmartApp() {}
    SmartApp(String name,String pkg){appName=name;packageName=pkg;}
    public String getAppName(){return appName;} public void setAppName(String value){appName=value;}
    public String getPackageName(){return packageName;} public void setPackageName(String value){packageName=value;}
  }
  public static final class OemProvider {
    private final String code,name,icon;
    public OemProvider(String code,String name,String icon){this.code=code;this.name=name;this.icon=icon;}
    public String getSpCode(){return code;}public String getName(){return name;}public String getIconUrl(){return icon;}
  }
  @Test public void emptyTypedOemCatalogWorksWithoutBorrowingAnotherBrandsTemplate(){
    List<?> empty=java.util.Collections.emptyList();List<?> result=PolestarProviderCatalogShim.appendCopy(empty,OemProvider.class);
    assertEquals(0,empty.size());assertEquals(1,result.size());OemProvider row=(OemProvider)result.get(0);
    assertEquals("POLESTAR",row.getSpCode());assertTrue(row.getName().contains("极星"));
    assertTrue(row.getIconUrl().startsWith("android.resource://"));
    assertSame(empty,PolestarProviderCatalogShim.appendCopy(empty,null));
  }
  @Test public void immutableCatalogIsCopiedBeforeAddingAndNeverCorruptsTheOriginal(){
    List<Provider> original=java.util.Collections.singletonList(new Provider("EXAMPLE","Example"));
    List<?> result=PolestarProviderCatalogShim.appendCopy(original,null);
    assertEquals(1,original.size());assertEquals(2,result.size());assertTrue(PolestarProviderCatalogShim.hasCode(result,"POLESTAR"));
    assertSame(result,PolestarProviderCatalogShim.appendCopy(result,null));
  }

  @Test public void appendsPolestarToProviderShapedListWithoutDuplicates() {
    List<Provider> providers=new ArrayList<>();providers.add(new Provider("EXAMPLE","Example"));
    assertEquals(1,PolestarProviderCatalogShim.injectList(providers));
    assertTrue(PolestarProviderCatalogShim.hasCode(providers,"POLESTAR"));
    assertEquals(0,PolestarProviderCatalogShim.injectList(providers));
    assertEquals(2,providers.size());
  }

  @Test public void appendsPolestarToSmartControlsAppListUsingMainPackage() {
    List<SmartApp> apps=new ArrayList<>();
    apps.add(new SmartApp("比亚迪","com.byd.app"));
    apps.add(new SmartApp("Tesla","com.tesla.app"));
    assertEquals(1,PolestarProviderCatalogShim.injectList(apps));
    assertEquals(3,apps.size());
    SmartApp added=apps.get(2);
    assertTrue(added.getAppName().contains("Polestar")||added.getAppName().contains("极星"));
    assertEquals("io.github.polestarcn.companion",added.getPackageName());
    assertEquals(0,PolestarProviderCatalogShim.injectList(apps));
  }

  @Test public void bindRedirectRequiresBothPolestarMarkerAndVehicleBindShape() {
    Intent bind=new Intent("oppo.intent.action.opluscarlink.BIND_BT_KEY").putExtra("brandCode","POLESTAR");
    assertTrue(PolestarBindIntentShim.isPolestarBind(bind));
    Intent scene=new Intent("oplus.intent.action.sceneservice.ACCOUNT_BIND").putExtra("spCode","POLESTAR");
    assertTrue(PolestarBindIntentShim.isPolestarBind(scene));
    assertFalse(PolestarBindIntentShim.isPolestarBind(new Intent("oppo.intent.action.opluscarlink.BIND_BT_KEY")));
    assertFalse(PolestarBindIntentShim.isPolestarBind(new Intent("android.intent.action.VIEW").putExtra("spCode","POLESTAR")));
  }
}
