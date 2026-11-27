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

        // Cliorius v8: preset performa sekali klik (menulis pref nyata yang dibaca JREUtils)
        bindPreset("cli_preset_pvp", net.kdt.pojavlaunch.firefly.utils.PerfPresets.INSTANCE.getPVP());
        bindPreset("cli_preset_survival", net.kdt.pojavlaunch.firefly.utils.PerfPresets.INSTANCE.getSURVIVAL());
        bindPreset("cli_preset_balanced", net.kdt.pojavlaunch.firefly.utils.PerfPresets.INSTANCE.getBALANCED());

        // Cliorius v8: Safe Mode — nonaktifkan/aktifkan semua mod sekali klik
        Preference safeDisable = findPreference("cli_safe_mode_disable");
        if (safeDisable != null) safeDisable.setOnPreferenceClickListener(p -> {
            int n = net.kdt.pojavlaunch.firefly.utils.SafeMode.INSTANCE.disableAll();
            android.widget.Toast.makeText(requireContext(),
                    getString(R.string.cli_safe_mode_toast) + " (" + n + ")",
                    android.widget.Toast.LENGTH_LONG).show();
            return true;
        });
        Preference safeEnable = findPreference("cli_safe_mode_enable");
        if (safeEnable != null) safeEnable.setOnPreferenceClickListener(p -> {
            int n = net.kdt.pojavlaunch.firefly.utils.SafeMode.INSTANCE.enableAll();
            android.widget.Toast.makeText(requireContext(),
                    getString(R.string.cli_safe_mode_restored) + " (" + n + ")",
                    android.widget.Toast.LENGTH_LONG).show();
            return true;
        });

        Preference ramInfo = findPreference("cli_perf_ram_info");
        if (ramInfo != null) ramInfo.setOnPreferenceClickListener(p -> {
            Tools.swapFragment(requireActivity(),
                    LauncherPreferenceJavaFragment.class, "JAVA_TWEAKS", null);
            return true;
        });
    }

    private void bindPreset(String key, net.kdt.pojavlaunch.firefly.utils.PerfPresets.Preset preset) {
        Preference p = findPreference(key);
        if (p == null) return;
        p.setOnPreferenceClickListener(pref -> {
            net.kdt.pojavlaunch.firefly.utils.PerfPresets.INSTANCE.apply(requireContext(), preset);
            String fps = preset.getMaxFps() > 0 ? String.valueOf(preset.getMaxFps())
                    : getString(R.string.cli_preset_fps_unlimited);
            String detail = getString(R.string.cli_preset_detail,
                    preset.getRamMb(), preset.getRenderer(), fps);
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.cli_preset_applied, getString(preset.getLabelRes())))
                    .setMessage(detail)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        });
    }
}
