package io.github.polestarcn.companion;

import android.content.Context;

public final class UiSkin {
  public static final String SIMPLE="simple";
  public static final String DETAILED="detailed";
  public static final String NIO_LIKE="niolike";
  public static final String NIO_LIKE_2="niolike2";
  public static final String VOLIKE_POLESTAR="volike_polestar";
  public static final String PS_DEFAULT="ps_default";
  private UiSkin(){}
  public static final String[] CHOICES={
      SIMPLE,DETAILED,VOLIKE_POLESTAR,NIO_LIKE,NIO_LIKE_2,PS_DEFAULT};
  public static String get(Context c){
    String value=AppPreferences.get(c,"ui_skin",PS_DEFAULT);
    for(String choice:CHOICES)if(choice.equals(value))return value;
    String migrated="latest".equals(value)||"ev_concept".equals(value)?SIMPLE:PS_DEFAULT;
    AppPreferences.set(c,"ui_skin",migrated);return migrated;
  }
  public static void set(Context c,String value){
    for(String choice:CHOICES)if(choice.equals(value)){AppPreferences.set(c,"ui_skin",value);return;}
    AppPreferences.set(c,"ui_skin",DETAILED);
  }
  public static int index(Context c){
    String value=get(c);
    for(int i=0;i<CHOICES.length;i++)if(CHOICES[i].equals(value))return i;
    return 1;
  }
  public static boolean nioLayout(Context c){return NIO_LIKE.equals(get(c))||NIO_LIKE_2.equals(get(c));}
  public static boolean nioColors(Context c){return NIO_LIKE.equals(get(c));}
  public static boolean polestarVo(Context c){return VOLIKE_POLESTAR.equals(get(c));}
  public static String label(Context c,String v){
    boolean en=AppPreferences.english(c);
    if(PS_DEFAULT.equals(v))return en?"PS Default":"PS默认皮肤";
    if(SIMPLE.equals(v))return en?"VoLike Simple":"VoLike 简洁";
    if(DETAILED.equals(v))return en?"VoLike Detailed":"VoLike 详细";
    if(VOLIKE_POLESTAR.equals(v))return en?"VoLike Polestar VI":"VoLike 极星配色";
    if(NIO_LIKE.equals(v))return "NIOlike";
    return "NIOlike2 · Psa VI";
  }
}
