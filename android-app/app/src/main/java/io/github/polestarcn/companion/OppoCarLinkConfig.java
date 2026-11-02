package io.github.polestarcn.companion;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import java.util.*;
/** Official developer identity is build-only; vehicle model mapping is user-editable, never guessed. */
final class OppoCarLinkConfig {
  final int companyId;final String authCode,typeCode,typeName,image;
  OppoCarLinkConfig(int company,String auth,String type,String name,String image){companyId=company;authCode=safe(auth);typeCode=safe(type);typeName=safe(name);this.image=safe(image);}
  static OppoCarLinkConfig load(Context c){
    Bundle meta=null;try{meta=c.getPackageManager().getApplicationInfo(c.getPackageName(),PackageManager.GET_META_DATA).metaData;}catch(Exception ignored){}
    if(meta==null)meta=new Bundle();
    return new OppoCarLinkConfig(meta.getInt("oplus.app.carlink.sdk.companyid",0),meta.getString("com.coloros.ocs.car.AUTH_CODE",""),
      AppPreferences.get(c,"carlink_type_code",""),AppPreferences.get(c,"carlink_type_name","Polestar 2"),AppPreferences.get(c,"carlink_image_url",""));
  }
  List<String> missing(){List<String> out=new ArrayList<>();if(companyId<=0)out.add("OPPO分配的companyId");if(authCode.isEmpty()||authCode.startsWith("PSASSISTANT_"))out.add("本应用包名/签名对应的AUTH_CODE");if(typeCode.isEmpty())out.add("后台已配置的typeCode");if(typeName.isEmpty())out.add("typeName");if(!httpsImage(image))out.add("可访问的HTTPS车辆图片URL");return out;}
  static boolean httpsImage(String value){try{java.net.URI u=java.net.URI.create(value);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}}
  private static String safe(String s){return s==null?"":s.trim();}
}
