package net.kdt.pojavlaunch.firefly.prefs.screens;

import android.os.Bundle;

import androidx.preference.Preference;

import com.firefly.feature.UpdateLauncher;

import net.kdt.pojavlaunch.firefly.R;

public class LauncherPreferenceLauncherFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_launcher);

        androidx.preference.ListPreference uiLang = findPreference("ui_language");
        if (uiLang != null) {
            uiLang.setOnPreferenceChangeListener((p, newVal) -> {
                String tag = (String) newVal;
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit().putString("ui_language", tag).apply();
                // Restart activity agar locale baru langsung aktif di seluruh UI
                android.content.Intent intent = requireActivity().getPackageManager()
                        .getLaunchIntentForPackage(requireContext().getPackageName());
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finishAffinity();
                return true;
            });
        }

        Preference updatePreference = requirePreference("update_launcher");
        updatePreference.setOnPreferenceClickListener(preference -> {
            UpdateLauncher updateLauncher = new UpdateLauncher(getContext());
            updateLauncher.checkForUpdates(false);
            return true;
        });

    }

}
