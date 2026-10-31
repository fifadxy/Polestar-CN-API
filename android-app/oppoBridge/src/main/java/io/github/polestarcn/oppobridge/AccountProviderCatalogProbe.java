package io.github.polestarcn.oppobridge;

import android.app.Application;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Observes the exact AssistantScreen 17.14.60 ProviderInfo catalog after the OEM has loaded it.
 * Only spCode and display name are reported; icon URLs are intentionally ignored.
 */
final class AccountProviderCatalogProbe {
  private static final String COROUTINE =
      "com.oplus.advice.settings.ui.viewmodel.AdviceSettingCompatViewModel$queryProviderInfoListFromDB$1";

  private AccountProviderCatalogProbe() {}

  static void install(OppoBridgeModule module, ClassLoader loader) throws Exception {
    Class<?> coroutine = loader.loadClass(COROUTINE);
    Method invokeSuspend = coroutine.getDeclaredMethod("invokeSuspend", Object.class);
    invokeSuspend.setAccessible(true);
    module.hook(invokeSuspend)
        .setId("psassistant_account_provider_catalog")
        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        .intercept(chain -> {
          Object result = chain.proceed();
          try {
            Object continuation = chain.getThisObject();
            Field outer = uniqueFieldBySuffix(continuation.getClass(), "this$0", null);
            Object viewModel = outer.get(continuation);
            Field catalogField = uniqueArrayListField(viewModel.getClass());
            Object value = catalogField.get(viewModel);
            if (value instanceof List<?>) report(module, (List<?>) value);
          } catch (Throwable failure) {
            module.warn("account_provider_catalog_probe failed="
                + failure.getClass().getSimpleName());
          }
          return result;
        });
    module.info("account_provider_catalog_probe ready=true");
  }

  private static void report(OppoBridgeModule module, List<?> items) throws Exception {
    Application app = currentApplication();
    if (app == null) return;
    List<String> out = new ArrayList<>();
    for (Object item : items) {
      if (item == null || out.size() >= 48) continue;
      Method getCode = item.getClass().getMethod("getSpCode");
      Method getName = item.getClass().getMethod("getName");
      String code = safe(String.valueOf(getCode.invoke(item)), 32);
      String name = safe(String.valueOf(getName.invoke(item)), 48);
      if (!code.isEmpty()) out.add(code + "=" + name);
    }
    String catalog = String.join("|", out);
    SceneAccountBindProbe.reportCatalog(app, catalog);
    module.info("account_provider_catalog observed=" + out.size());
  }

  private static Field uniqueArrayListField(Class<?> type) {
    Field found = null;
    for (Field f : type.getDeclaredFields()) {
      if (!ArrayList.class.isAssignableFrom(f.getType())) continue;
      if (found != null) throw new IllegalStateException("Provider catalog field ambiguous");
      found = f;
    }
    if (found == null) throw new IllegalStateException("Provider catalog field missing");
    found.setAccessible(true);
    return found;
  }

  private static Field uniqueFieldBySuffix(Class<?> type, String name, Class<?> expected) {
    try {
      Field f = type.getDeclaredField(name);
      if (expected == null || expected.isAssignableFrom(f.getType())) {
        f.setAccessible(true);
        return f;
      }
    } catch (NoSuchFieldException ignored) {}
    throw new IllegalStateException("Outer view-model field missing");
  }

  private static String safe(String value, int max) {
    if (value == null || "null".equals(value)) return "";
    String clean = value.replace('|', ' ').replace('=', ' ')
        .replace('\n', ' ').replace('\r', ' ').trim();
    return clean.substring(0, Math.min(clean.length(), max));
  }

  private static Application currentApplication() {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch (Throwable ignored) {
      return null;
    }
  }
}
