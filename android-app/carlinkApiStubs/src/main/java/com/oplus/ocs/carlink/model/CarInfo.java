package com.oplus.ocs.carlink.model;
/** Minimal documented data contract, no SDK transport. */
public final class CarInfo {
 private String openId;
 private String carId;
 private String name;
 private String companyId;
 private String typeCode;
 private String typeName;
 private String image;
 private String powerType;
 private String vin;
 private boolean current;
 public CarInfo(String openId,String carId,String name,boolean current){this.openId=openId;this.carId=carId;this.name=name;this.current=current;}
 public boolean isCurrentCar(){return current;}
 public String getOpenId(){return openId;} public void setOpenId(String value){openId=value;}
 public String getCarId(){return carId;} public void setCarId(String value){carId=value;}
 public String getName(){return name;} public void setName(String value){name=value;}
 public String getCompanyId(){return companyId;} public void setCompanyId(String value){companyId=value;}
 public String getTypeCode(){return typeCode;} public void setTypeCode(String value){typeCode=value;}
 public String getTypeName(){return typeName;} public void setTypeName(String value){typeName=value;}
 public String getImage(){return image;} public void setImage(String value){image=value;}
 public String getPowerType(){return powerType;} public void setPowerType(String value){powerType=value;}
 public String getVin(){return vin;} public void setVin(String value){vin=value;}
}
