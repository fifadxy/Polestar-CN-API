package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Compatibility signer for the exact user-verified reference APK.
 * The fixed signing material is never present in this source tree, logs, preferences or artifacts.
 */
public final class ReferenceSignatureProvider {
    public static final String REFERENCE_PACKAGE = "app.c3garage.companion";
    public static final String REFERENCE_SHA256 =
            "8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735";

    private final Context context;
    private volatile String inMemorySecret;

    public ReferenceSignatureProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isReferenceInstalled() {
        try {
            context.getPackageManager().getApplicationInfo(REFERENCE_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String resolve() throws Exception {
        String cached = inMemorySecret;
        if (cached != null) return cached;

        ApplicationInfo info;
        try {
            info = context.getPackageManager().getApplicationInfo(REFERENCE_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("未检测到已验证模板 APK；请先安装 app.c3garage.companion v0.1.0");
        }
        File apk = new File(info.sourceDir);
        String digest = sha256(apk);
        if (!REFERENCE_SHA256.equalsIgnoreCase(digest)) {
            throw new IllegalStateException("检测到模板 APK，但 SHA-256 与已验证样本不一致，已拒绝提取兼容签名材料");
        }

        byte[] dex;
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry("classes.dex");
            if (entry == null) throw new IllegalStateException("模板 APK 缺少 classes.dex");
            try (InputStream in = zip.getInputStream(entry)) {
                dex = readAll(in);
            }
        }

        Set<String> candidates = new LinkedHashSet<>();
        for (int i = 0; i + 32 <= dex.length; i++) {
            if (!isUpperHex(dex[i])) continue;
            boolean ok = true;
            for (int j = 1; j < 32; j++) {
                if (!isUpperHex(dex[i + j])) { ok = false; break; }
            }
            if (!ok) continue;
            boolean left = i > 0 && isUpperHex(dex[i - 1]);
            boolean right = i + 32 < dex.length && isUpperHex(dex[i + 32]);
            if (!left && !right) {
                candidates.add(new String(dex, i, 32, StandardCharsets.US_ASCII));
                i += 31;
            }
        }
        if (candidates.size() != 1) {
            throw new IllegalStateException("模板兼容签名材料定位失败：候选数=" + candidates.size());
        }
        inMemorySecret = candidates.iterator().next();
        return inMemorySecret;
    }

    private static boolean isUpperHex(byte b) {
        int c = b & 0xff;
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
    }

    private static String sha256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[64 * 1024];
        try (InputStream in = new FileInputStream(file)) {
            int n;
            while ((n = in.read(buf)) >= 0) if (n > 0) md.update(buf, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : md.digest()) out.append(String.format(Locale.US, "%02x", b & 0xff));
        return out.toString();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[32 * 1024];
        int n;
        while ((n = in.read(buf)) >= 0) if (n > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }
}
