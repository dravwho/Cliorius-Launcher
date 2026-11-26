package net.kdt.pojavlaunch.firefly.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.security.MessageDigest;

/** Cliorius: penguncian aplikasi (PIN disimpan sebagai SHA-256 hash). */
public final class AppLock {
    private static final String KEY_ENABLED = "app_lock_enabled";
    private static final String KEY_PIN_HASH = "app_lock_pin_hash";
    private static final String KEY_UNLOCKED = "app_lock_unlocked_session";

    private AppLock() {}

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String hash(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(pin.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return pin;
        }
    }

    public static boolean isEnabled(Context context) { return prefs(context).getBoolean(KEY_ENABLED, false); }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (!enabled) prefs(context).edit().remove(KEY_PIN_HASH).apply();
    }

    public static boolean hasPin(Context context) { return prefs(context).contains(KEY_PIN_HASH); }

    public static void setPin(Context context, String pin) {
        prefs(context).edit().putString(KEY_PIN_HASH, hash(pin)).apply();
        prefs(context).edit().putBoolean(KEY_ENABLED, true).apply();
    }

    public static boolean verify(Context context, String pin) {
        String saved = prefs(context).getString(KEY_PIN_HASH, null);
        return saved != null && saved.equals(hash(pin));
    }

    public static boolean isUnlockedThisSession(Context context) { return prefs(context).getBoolean(KEY_UNLOCKED, false); }

    public static void markUnlocked(Context context) { prefs(context).edit().putBoolean(KEY_UNLOCKED, true).apply(); }

    public static void resetSession(Context context) { prefs(context).edit().putBoolean(KEY_UNLOCKED, false).apply(); }
}
