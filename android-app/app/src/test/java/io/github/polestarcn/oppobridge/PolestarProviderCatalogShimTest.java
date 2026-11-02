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

  @Test public void appendsPolestarToProviderShapedListWithoutDuplicates() {
    List<Provider> providers=new ArrayList<>();providers.add(new Provider("EXAMPLE","Example"));
    assertEquals(1,PolestarProviderCatalogShim.injectList(providers));
    assertTrue(PolestarProviderCatalogShim.hasCode(providers,"POLESTAR"));
    assertEquals(0,PolestarProviderCatalogShim.injectList(providers));
    assertEquals(2,providers.size());
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