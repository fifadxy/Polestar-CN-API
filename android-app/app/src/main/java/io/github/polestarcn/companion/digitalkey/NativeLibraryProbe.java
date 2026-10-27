package io.github.polestarcn.companion.digitalkey;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Research-only native loader.
 *
 * It only calls System.loadLibrary() for explicitly bundled libraries and
 * reports whether the Java/JNI bridge classes are present. It never invokes
 * vehicle pairing, crypto provisioning, account APIs, or native key methods.
 */
public final class NativeLibraryProbe {
  private NativeLibraryProbe() {}

  public static final class Item {
    public final String library;
    public final boolean packaged;
    public final boolean loaded;
    public final String error;

    Item(String library, boolean packaged, boolean loaded, String error) {
      this.library = library;
      this.packaged = packaged;
      this.loaded = loaded;
      this.error = error == null ? "" : error;
    }
  }

  public static final class Result {
    public final List<Item> items;
    public final boolean vocmoJavaBridgePresent;

    Result(List<Item> items, boolean vocmoJavaBridgePresent) {
      this.items = Collections.unmodifiableList(new ArrayList<>(items));
      this.vocmoJavaBridgePresent = vocmoJavaBridgePresent;
    }
  }

  public static Result inspect(Context context, boolean load) {
    List<Item> items = new ArrayList<>();
    items.add(check(context, "vocmo-lib", load));
    items.add(check(context, "glble", load));

    boolean bridge = false;
    try {
      Class.forName("com.volvocars.vocmo.djinni.CertificateApi$CppProxy");
      bridge = true;
    } catch (Throwable ignored) {
      bridge = false;
    }
    return new Result(items, bridge);
  }

  private static Item check(Context context, String library, boolean load) {
    File dir = new File(context.getApplicationInfo().nativeLibraryDir);
    File so = new File(dir, "lib" + library + ".so");
    boolean packaged = so.isFile();
    if (!load) return new Item(library, packaged, false, "");

    try {
      System.loadLibrary(library);
      return new Item(library, packaged, true, "");
    } catch (Throwable error) {
      String message = error.getClass().getSimpleName();
      if (error.getMessage() != null && !error.getMessage().trim().isEmpty()) {
        String clean = error.getMessage().replace('\n', ' ').replace('\r', ' ');
        if (clean.length() > 220) clean = clean.substring(0, 220);
        message += ": " + clean;
      }
      return new Item(library, packaged, false, message);
    }
  }
}
