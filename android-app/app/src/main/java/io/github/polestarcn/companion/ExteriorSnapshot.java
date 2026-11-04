package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Map;

/** Reference order: front left, front right, rear left, rear right. */
public final class ExteriorSnapshot {
  private final int[] doors, windows;
  public ExteriorSnapshot(int[] doors, int[] windows) {
    this.doors = copy(doors);
    this.windows = copy(windows);
  }
  private static int[] copy(int[] input) {
    int[] out = {-1, -1, -1, -1};
    if (input != null) System.arraycopy(input, 0, out, 0, Math.min(4, input.length));
    return out;
  }
  public static ExteriorSnapshot unknown() { return new ExteriorSnapshot(null, null); }
  public static ExteriorSnapshot fromData(Map<Integer, Object> data) {
    int[] d = new int[4], w = new int[4];
    for (int i = 0; i < 4; i++) {
      Integer door = ProtoWire.integer(data, i + 3), window = ProtoWire.integer(data, i + 7);
      d[i] = door == null ? -1 : door;
      w[i] = window == null ? -1 : window;
    }
    return new ExteriorSnapshot(d, w);
  }
  public int state(boolean window, int position) {
    return position < 0 || position >= 4 ? -1 : (window ? windows : doors)[position];
  }
  public int[] states(boolean window) { return (window ? windows : doors).clone(); }
  public String summary(boolean window) {
    int open = 0;
    boolean unknown = false;
    for (int value : window ? windows : doors) {
      if (value == 1 || value == 3) open++;
      else if (value != 2) unknown = true;
    }
    if (open > 0) return open + " 个开启" + (unknown ? " · 部分未知" : "");
    return unknown ? "状态不完整" : "全部关闭";
  }
  public static String label(int value) {
    return value == 1 ? "已打开" : value == 2 ? "已关闭" : value == 3 ? "未关严" : "未知";
  }
}
