package net.kdt.pojavlaunch.firefly.prefs.screens;

import android.os.Bundle;

import androidx.preference.Preference;
import net.kdt.pojavlaunch.firefly.R;
import net.kdt.pojavlaunch.firefly.Tools;

/** Cliorius: layar Performa & FPS — semua opsi di sini dipakai langsung oleh JREUtils saat launch. */
public class LauncherPreferencePerformanceFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_performance);
        // Info RAM: buka dialog kustomisasi RAM yang sama dengan Java Tweaks (nilai nyata, bukan gimmick)
        Preference autoTune = findPreference("cli_auto_tune");
        if (autoTune != null) autoTune.setOnPreferenceClickListener(p -> {
            net.kdt.pojavlaunch.firefly.utils.AutoTune.Result r =
                    net.kdt.pojavlaunch.firefly.utils.AutoTune.INSTANCE.run(requireContext());
            StringBuilder msg = new StringBuilder();
            for (String note : r.getNotes()) { msg.append("• ").append(note).append("\n"); }
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.cli_perf_autotune_done_title)
                    .setMessage(msg.toString().trim())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        });

        Preference ramInfo = findPreference("cli_perf_ram_info");
        if (ramInfo != null) ramInfo.setOnPreferenceClickListener(p -> {
            Tools.swapFragment(requireActivity(),
                    LauncherPreferenceJavaFragment.class, "JAVA_TWEAKS", null);
            return true;
        });
    }
}
