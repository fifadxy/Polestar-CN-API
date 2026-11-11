package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.res.XmlResourceParser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class ServiceIconAndAmapV129Test {
  private String script(int id)throws Exception {
    Context c=RuntimeEnvironment.getApplication();
    try(InputStream input=c.getResources().openRawResource(id);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
      byte[] buffer=new byte[2048];int n;
      while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);
      return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
    }
  }
  @Test public void onlyServiceGlyphUsesSixPercentScaleAndEightPercentWeight()throws Exception {
    Context c=RuntimeEnvironment.getApplication();
    try(XmlResourceParser xml=c.getResources().getXml(R.drawable.ic_service_reference)){
      boolean group=false;int strokes=0;boolean gauge=false;
      while(xml.next()!=XmlPullParser.END_DOCUMENT){
        if(xml.getEventType()!=XmlPullParser.START_TAG)continue;
        String tag=xml.getName();
        if("group".equals(tag)){
          group=true;
          assertEquals(1.06f,xml.getAttributeFloatValue("http://schemas.android.com/apk/res/android","scaleX",0),.0001f);
          assertEquals(1.06f,xml.getAttributeFloatValue("http://schemas.android.com/apk/res/android","scaleY",0),.0001f);
        }
        if("path".equals(tag)){
          String stroke=xml.getAttributeValue("http://schemas.android.com/apk/res/android","strokeWidth");
          if(stroke!=null){strokes++;float weight=Float.parseFloat(stroke);
            assertTrue(weight==8.86415f||weight==7.38679f);
            if(weight==7.38679f)gauge=true;
          }
        }
      }
      assertTrue(group);assertEquals(3,strokes);assertTrue(gauge);
    }
  }
  @Test public void amapDoesNotShowPhoneLocationOnBanner()throws Exception {
    String distance=script(R.raw.parking_map_distance);
    assertFalse(distance.contains("手机定位未开启"));
    assertFalse(distance.contains("正在定位手机"));
    assertTrue(distance.contains("psa-phone-distance"));
    String compact=script(R.raw.parking_map_compact);
    assertTrue(compact.contains(".common_bottom"));
    assertTrue(compact.contains("pointer-events:none"));
  }
}
