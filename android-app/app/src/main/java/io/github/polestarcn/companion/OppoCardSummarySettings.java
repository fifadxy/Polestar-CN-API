package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;
import io.github.polestarcn.oppobridge.OppoCardSummary;

/** Two presentation-only preferences, mirrored through the existing read-only snapshot IPC. */
public final class OppoCardSummarySettings {
  public static final String TEMPLATE="oppo_card_summary_template";
  public static final String SEPARATOR="oppo_card_summary_separator";
  private OppoCardSummarySettings(){}
  public static String template(Context c){
    return OppoCardSummary.safeTemplate(AppPreferences.get(c,TEMPLATE,OppoCardSummary.DEFAULT_TEMPLATE));
  }
  public static String separator(Context c){
    return OppoCardSummary.safeSeparator(AppPreferences.get(c,SEPARATOR,OppoCardSummary.DEFAULT_SEPARATOR));
  }
  public static void write(Bundle out,Context c){
    out.putString(OppoCardSummary.CONFIG_TEMPLATE,template(c));
    out.putString(OppoCardSummary.CONFIG_SEPARATOR,separator(c));
  }
  public static void save(Context c,String tpl,String sep){
    tpl=OppoCardSummary.checkTemplate(tpl);
    sep=OppoCardSummary.checkSeparator(sep);
    c.getSharedPreferences("preferences",0).edit()
        .putString(TEMPLATE,tpl).putString(SEPARATOR,sep).apply();
    changed(c);
  }
  public static void reset(Context c){
    c.getSharedPreferences("preferences",0).edit().remove(TEMPLATE).remove(SEPARATOR).apply();
    changed(c);
  }
  public static void changed(Context c){
    // Existing card observers receive a refresh signal without a vehicle request.
    c.getContentResolver().notifyChange(PsAssistantBridgeProvider.URI,null);
  }
}
