package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

final class OppoVehicleProfileSettings {
  static final String PREF="oppo-vehicle-profile-v1";
  static final String PROVIDER_CODE="POLESTAR",DEVICE_TYPE="CAR";
  static final String LOCK_SHORTCUT="chesuo",CLIMATE_SHORTCUT="kongtiao",FLASH_SHORTCUT="shanguang",HORN_SHORTCUT="mingdi";
  static final String DISPLAY_NAME="displayName",BRAND="brand",MANUFACTURER="manufacturer",MODEL_ID="modelId",MODEL_NAME="modelName",
      CONNECT_STATE="connectState",VIN_PREFIX="vinPrefix",CAPABILITIES="capabilitiesCsv",CAR_CONTROL_CLASS="carControlClassName",
      SESSION_MANAGER_CLASS="sessionManagerClassName",SESSION_CLASS="sessionClassName",CATALOG_AUTHORITIES="catalogAuthoritiesCsv";
  private static final String DEFAULT_DISPLAY_NAME="Polestar 极星",DEFAULT_BRAND="Polestar",DEFAULT_MANUFACTURER="Polestar",
      DEFAULT_MODEL_ID="POLESTAR_2",DEFAULT_MODEL_NAME="Polestar 2",DEFAULT_CONNECT_STATE="COORDINATED",DEFAULT_VIN_PREFIX="TST0",
      DEFAULT_CAPABILITIES="LOCK,UNLOCK,CLIMATE_START,CLIMATE_STOP,AIR_START,AIR_STOP,TRUNK_OPEN,FLASH,HORN,SOC,RANGE,ODOMETER,LOCK_STATE,CLIMATE_STATE",
      DEFAULT_CAR_CONTROL="com.oplus.ocs.carlink.control.CarControl",DEFAULT_SESSION_MANAGER="com.oplus.carlink.session.SessionManager",
      DEFAULT_SESSION_CLASS="com.oplus.carlink.session.CarDeviceSession",
      DEFAULT_AUTHORITIES="com.heytap.opluscarlink.CarAppProvider,com.oplus.sceneservice.AccountSDKInitProvider";
  private OppoVehicleProfileSettings(){}

  static Bundle snapshot(Context context){
    SharedPreferences p=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);Bundle out=defaults();
    for(String key:editableKeys())out.putString(key,bounded(p.getString(key,out.getString(key,"")),512));
    out.putString("providerCode",PROVIDER_CODE);out.putString("deviceType",DEVICE_TYPE);
    out.putString("lockShortcut",LOCK_SHORTCUT);out.putString("climateShortcut",CLIMATE_SHORTCUT);
    out.putString("flashShortcut",FLASH_SHORTCUT);out.putString("hornShortcut",HORN_SHORTCUT);
    out.putStringArray("capabilities",parseCsv(out.getString(CAPABILITIES,"")));
    out.putStringArray("catalogAuthorities",parseCsv(out.getString(CATALOG_AUTHORITIES,"")));out.putInt("profileVersion",1);return out;
  }
  static void save(Context context,Bundle values){
    if(values==null)throw new IllegalArgumentException("Missing vehicle profile");
    SharedPreferences.Editor e=context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit();
    for(String key:editableKeys())if(values.containsKey(key))e.putString(key,sanitize(key,values.getString(key,"")));e.apply();
  }
  static Bundle restoreDefaults(Context context){
    Bundle d=defaults();SharedPreferences.Editor e=context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear();
    for(String key:editableKeys())e.putString(key,d.getString(key,""));e.apply();return snapshot(context);
  }
  static Bundle defaults(){
    Bundle out=new Bundle();out.putString(DISPLAY_NAME,DEFAULT_DISPLAY_NAME);out.putString(BRAND,DEFAULT_BRAND);
    out.putString(MANUFACTURER,DEFAULT_MANUFACTURER);out.putString(MODEL_ID,DEFAULT_MODEL_ID);out.putString(MODEL_NAME,DEFAULT_MODEL_NAME);
    out.putString(CONNECT_STATE,DEFAULT_CONNECT_STATE);out.putString(VIN_PREFIX,DEFAULT_VIN_PREFIX);out.putString(CAPABILITIES,DEFAULT_CAPABILITIES);
    out.putString(CAR_CONTROL_CLASS,DEFAULT_CAR_CONTROL);out.putString(SESSION_MANAGER_CLASS,DEFAULT_SESSION_MANAGER);
    out.putString(SESSION_CLASS,DEFAULT_SESSION_CLASS);out.putString(CATALOG_AUTHORITIES,DEFAULT_AUTHORITIES);return out;
  }
  private static String[] editableKeys(){return new String[]{DISPLAY_NAME,BRAND,MANUFACTURER,MODEL_ID,MODEL_NAME,CONNECT_STATE,VIN_PREFIX,CAPABILITIES,CAR_CONTROL_CLASS,SESSION_MANAGER_CLASS,SESSION_CLASS,CATALOG_AUTHORITIES};}
  private static String sanitize(String key,String raw){
    String value=bounded(raw==null?"":raw.trim(),512);
    if(DISPLAY_NAME.equals(key)||MODEL_NAME.equals(key))return value.isEmpty()?defaults().getString(key):value;
    if(BRAND.equals(key)||MANUFACTURER.equals(key))return value.replaceAll("[\r\n\t]"," ");
    if(MODEL_ID.equals(key))return token(value,DEFAULT_MODEL_ID);
    if(CONNECT_STATE.equals(key))return token(value.toUpperCase(Locale.ROOT),DEFAULT_CONNECT_STATE);
    if(VIN_PREFIX.equals(key)){String v=value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");return v.isEmpty()?DEFAULT_VIN_PREFIX:v.substring(0,Math.min(6,v.length()));}
    if(CAPABILITIES.equals(key)||CATALOG_AUTHORITIES.equals(key))return String.join(",",parseCsv(value));
    if(CAR_CONTROL_CLASS.equals(key)||SESSION_MANAGER_CLASS.equals(key)||SESSION_CLASS.equals(key)){String v=value.replaceAll("[^A-Za-z0-9_.$]","");return v.isEmpty()?defaults().getString(key):v;}
    return value;
  }
  private static String token(String value,String fallback){String v=value.replaceAll("[^A-Za-z0-9_.-]","");return v.isEmpty()?fallback:v;}
  private static String bounded(String value,int max){return value==null?"":value.substring(0,Math.min(value.length(),max));}
  static String[] parseCsv(String raw){
    LinkedHashSet<String> values=new LinkedHashSet<>();if(raw!=null)for(String part:raw.split(",")){String v=part.trim();if(!v.isEmpty()&&v.length()<=160)values.add(v);if(values.size()>=48)break;}
    return new ArrayList<>(values).toArray(new String[0]);
  }
}
