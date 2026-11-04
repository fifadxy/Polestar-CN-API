package io.github.polestarcn.companion;

public enum Region {
    CHINA("China", "中国区"), GLOBAL("Global", "全球区");
    public final String english;
    public final String chinese;
    Region(String english, String chinese) { this.english = english; this.chinese = chinese; }
    public static Region fromStored(String value) {
        try { return Region.valueOf(value); } catch (Exception ignored) { return null; }
    }
}
