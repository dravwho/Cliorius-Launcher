package net.kdt.pojavlaunch.firefly.prefs.screens;

import android.os.Bundle;

import androidx.preference.Preference;

import net.kdt.pojavlaunch.firefly.R;
import net.kdt.pojavlaunch.firefly.Tools;
import net.kdt.pojavlaunch.firefly.fragments.CrashViewerFragment;

public class LauncherPreferenceMiscellaneousFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_misc);

        Preference crashViewer = findPreference("cli_crash_viewer");
        if (crashViewer != null) {
            crashViewer.setOnPreferenceClickListener(preference -> {
                Tools.swapFragment(requireActivity(), CrashViewerFragment.class,
                        CrashViewerFragment.TAG, null);
                return true;
            });
        }
    }
}
