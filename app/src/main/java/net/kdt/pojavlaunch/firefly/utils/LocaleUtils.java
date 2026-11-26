package net.kdt.pojavlaunch.firefly.utils;

import static net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences.DEFAULT_PREF;
import static net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences.PREF_FORCE_ENGLISH;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.preference.PreferenceManager;

import java.util.Locale;

public class LocaleUtils extends ContextWrapper {

    public LocaleUtils(Context base) {
        super(base);
    }

    private static Locale parseLocale(String tag) {
        String[] parts = tag.split("-");
        if (parts.length == 1) return new Locale(parts[0]);
        if (parts.length == 2) return new Locale(parts[0], parts[1]);
        return new Locale(parts[0], parts[1], parts[2]);
    }

    public static ContextWrapper setLocale(Context context) {
        if (DEFAULT_PREF == null) {
            DEFAULT_PREF = PreferenceManager.getDefaultSharedPreferences(context);
            PREF_FORCE_ENGLISH = DEFAULT_PREF.getBoolean("force_english", false);
        }

        String uiLang = DEFAULT_PREF.getString("ui_language", "");
        if (uiLang != null && !uiLang.isEmpty() && !uiLang.equals("system")) {
            // Bahasa UI dipilih manual di Settings — abaikan force_english
            Resources resources = context.getResources();
            Configuration configuration = resources.getConfiguration();
            Locale locale = parseLocale(uiLang);
            configuration.setLocale(locale);
            Locale.setDefault(locale);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList localeList = new LocaleList(locale);
                LocaleList.setDefault(localeList);
                configuration.setLocales(localeList);
            }
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                context = context.createConfigurationContext(configuration);
            }
            return new LocaleUtils(context);
        }

        if (DEFAULT_PREF.getBoolean("force_english", false)) {
            Resources resources = context.getResources();
            Configuration configuration = resources.getConfiguration();

            /**
             * en-XA is a pseudo-language (usually used for UI testing)
             * that has no corresponding resource directory
             * Therefore, when the application is set to this language
             * the system falls back to the default resource in the values directory
             */
            Locale defaultLocale = new Locale("en", "XA");
            configuration.setLocale(defaultLocale);
            Locale.setDefault(defaultLocale);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList localeList = new LocaleList(defaultLocale);
                LocaleList.setDefault(localeList);
                configuration.setLocales(localeList);
            }

            resources.updateConfiguration(configuration, resources.getDisplayMetrics());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                context = context.createConfigurationContext(configuration);
            }
        }

        return new LocaleUtils(context);
    }
}
