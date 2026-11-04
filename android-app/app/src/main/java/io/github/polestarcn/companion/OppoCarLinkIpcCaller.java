package io.github.polestarcn.companion;
import android.content.Context;
final class OppoCarLinkIpcCaller {
 private OppoCarLinkIpcCaller(){}
 static boolean allowed(Context c,int uid){if(uid==android.os.Process.myUid())return true;String[] packages=c.getPackageManager().getPackagesForUid(uid);if(packages==null)return false;for(String p:packages)if("com.heytap.opluscarlink".equals(p)||"io.github.polestarcn.oppobridge".equals(p))return true;return false;}
}
