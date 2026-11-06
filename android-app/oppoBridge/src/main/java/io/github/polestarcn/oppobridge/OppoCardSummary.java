package io.github.polestarcn.oppobridge;

import android.os.Bundle;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cache-only display contract shared by all OPPO card hosts. No vehicle commands or secrets. */
public final class OppoCardSummary {
  public static final String CONFIG_TEMPLATE="oppoCardTemplate";
  public static final String CONFIG_SEPARATOR="oppoCardSeparator";
  public static final String DEFAULT_TEMPLATE="{soc}{sep}{range}";
  public static final String DEFAULT_SEPARATOR=" · ";
  public static final int MAX_TEMPLATE_LENGTH=160,MAX_SEPARATOR_LENGTH=8,MAX_UNITS=40;
  private static final Pattern TOKEN=Pattern.compile("\\{([a-zA-Z]+)\\}");
  public static final String[] FIELDS={"soc","range","odometer","updated","lock","climate","air","charging","source"};
  private OppoCardSummary(){}

  public static String checkTemplate(String value) {
    if(value==null||value.length()==0||value.length()>MAX_TEMPLATE_LENGTH)
      throw new IllegalArgumentException("Template length must be 1-160");
    if(value.indexOf('\n')>=0||value.indexOf('\r')>=0||value.indexOf('\t')>=0)
      throw new IllegalArgumentException("Card title must be on one line");
    Matcher m=TOKEN.matcher(value);int last=0;
    while(m.find()){
      String gap=value.substring(last,m.start());
      if(gap.indexOf('{')>=0||gap.indexOf('}')>=0)throw new IllegalArgumentException("Malformed field token");
      String key=m.group(1);
      if(!"sep".equals(key)&&!field(key))throw new IllegalArgumentException("Unknown field "+key);
      last=m.end();
    }
    String tail=value.substring(last);
    if(tail.indexOf('{')>=0||tail.indexOf('}')>=0)throw new IllegalArgumentException("Malformed field token");
    for(int i=0;i<value.length();i++)
      if(Character.isISOControl(value.charAt(i)))throw new IllegalArgumentException("Control character");
    return value;
  }
  public static String checkSeparator(String value){
    if(value==null||value.length()>MAX_SEPARATOR_LENGTH)
      throw new IllegalArgumentException("Separator may be 0-8 characters");
    for(int i=0;i<value.length();i++)
      if(Character.isISOControl(value.charAt(i))||value.charAt(i)=='{'||value.charAt(i)=='}')
        throw new IllegalArgumentException("Invalid separator");
    return value;
  }
  public static String safeTemplate(String value){
    try{return checkTemplate(value);}catch(IllegalArgumentException e){return DEFAULT_TEMPLATE;}
  }
  public static String safeSeparator(String value){
    try{return checkSeparator(value);}catch(IllegalArgumentException e){return DEFAULT_SEPARATOR;}
  }
  private static boolean field(String name){for(String f:FIELDS)if(f.equals(name))return true;return false;}
  public static String format(Bundle state){
    if(state==null)state=Bundle.EMPTY;
    return format(state,state.getString(CONFIG_TEMPLATE,DEFAULT_TEMPLATE),
        state.getString(CONFIG_SEPARATOR,DEFAULT_SEPARATOR));
  }
  public static String format(Bundle state,String template,String separator){
    if(state==null)state=Bundle.EMPTY;
    template=safeTemplate(template);separator=safeSeparator(separator);
    String[] pieces=template.split(Pattern.quote("{sep}"),-1);
    ArrayList<String> segments=new ArrayList<>();
    for(String piece:pieces){
      Matcher m=TOKEN.matcher(piece);StringBuffer output=new StringBuffer();
      boolean fieldFound=false,fieldPresent=false;
      while(m.find()){
        fieldFound=true;String value=value(state,m.group(1));
        if(!value.isEmpty())fieldPresent=true;
        m.appendReplacement(output,Matcher.quoteReplacement(value));
      }
      m.appendTail(output);
      String part=output.toString().trim();
      if(!part.isEmpty()&&(!fieldFound||fieldPresent))segments.add(part);
    }
    if(segments.isEmpty())return "车况未更新";
    StringBuilder result=new StringBuilder();
    for(String part:segments){
      String prefix=result.length()>0?separator:"";
      if(units(result.toString())+units(prefix)+units(part)>MAX_UNITS){
        // Drop overflowing trailing fields as a whole, never append "..." to the card.
        if(result.length()==0)result.append(fit(part,MAX_UNITS));
        break;
      }
      result.append(prefix).append(part);
    }
    return result.length()==0?"车况未更新":result.toString();
  }
  public static int units(String text){
    if(text==null)return 0;int units=0;
    for(int i=0;i<text.length();){
      int cp=text.codePointAt(i);i+=Character.charCount(cp);
      units+=(cp>0x2e80||Character.isSupplementaryCodePoint(cp))?2:1;
    }
    return units;
  }
  private static String fit(String text,int capacity){
    StringBuilder out=new StringBuilder();int width=0;
    for(int i=0;i<text.length();){
      int cp=text.codePointAt(i),cost=cp>0x2e80||Character.isSupplementaryCodePoint(cp)?2:1;
      if(width+cost>capacity)break;
      out.appendCodePoint(cp);i+=Character.charCount(cp);width+=cost;
    }
    return out.toString();
  }
  private static String value(Bundle data,String key){
    switch(key){
      case "soc":{
        int n=data.getInt("batteryPercent",-1);
        return data.getBoolean("batteryPercentKnown")&&n>=0&&n<=100?n+"%":"";
      }
      case "range":{
        int n=data.getInt("rangeKm",-1);
        return data.getBoolean("rangeKmKnown")&&n>=0?n+"KM":"";
      }
      case "odometer":{
        int n=data.getInt("odometerKm",-1);
        return data.getBoolean("odometerKmKnown")&&n>=0?n+"KM":"";
      }
      case "updated":{
        long ms=data.getLong("capturedAt",0);
        return ms>0?"更新 "+new SimpleDateFormat("MM-dd HH:mm",Locale.getDefault()).format(new Date(ms)):"";
      }
      case "source":return data.getBoolean("available",false)?"缓存":"";
      case "lock":case "climate":case "air":case "charging":{
        String raw=data.getString(key,"");
        if(raw==null)return "";
        String v=raw.trim();
        return v.length()==0||"未知".equals(v)||"UNKNOWN".equalsIgnoreCase(v)?"":fit(v,16);
      }
      default:return "";
    }
  }
}
