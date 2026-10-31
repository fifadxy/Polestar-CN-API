package io.github.polestarcn.oppobridge;

/** Only IDs emitted by our own ShortcutMenu serializer are executable. */
final class CardShortcutCommand {
  private CardShortcutCommand() {}
  static String fromId(String id) {
    if (id == null) return null;
    switch (id) {
      case "ps_lock": return "lock";
      case "ps_unlock": return "unlock";
      case "ps_climate-start": return "climate-start";
      case "ps_climate-stop": return "climate-stop";
      case "ps_trunk-open": return "trunk-open";
      case "ps_trunk-close": return "trunk-close";
      case "ps_air-start": return "air-start";
      case "ps_air-stop": return "air-stop";
      default: return null;
    }
  }
}
