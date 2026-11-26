package net.kdt.pojavlaunch.firefly.prefs.screens;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.kdt.pojavlaunch.firefly.CustomControlsActivity;
import net.kdt.pojavlaunch.firefly.LauncherActivity;
import net.kdt.pojavlaunch.firefly.R;
import net.kdt.pojavlaunch.firefly.Tools;
import net.kdt.pojavlaunch.firefly.benchmark.BenchmarkFragment;
import net.kdt.pojavlaunch.firefly.fragments.AboutFragment;
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences;
import com.qz.terminal2.ConsoleActivity;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(getResources().getColor(R.color.background_app));
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();
        setupToolsPreferences();
    }

    private void setupToolsPreferences() {
        Preference customControls = findPreference("cli_custom_controls");
        if (customControls != null) customControls.setOnPreferenceClickListener(p -> {
            startActivity(new Intent(requireContext(), CustomControlsActivity.class));
            return true;
        });

        Preference executeJar = findPreference("cli_execute_jar");
        if (executeJar != null) executeJar.setOnPreferenceClickListener(p -> {
            Tools.installMod(requireActivity(), false);
            return true;
        });

        Preference terminal = findPreference("cli_terminal");
        if (terminal != null) terminal.setOnPreferenceClickListener(p -> {
            startActivity(new Intent(requireContext(), ConsoleActivity.class));
            return true;
        });

        Preference shareLog = findPreference("cli_share_log");
        if (shareLog != null) shareLog.setOnPreferenceClickListener(p -> {
            Tools.shareLog(requireContext());
            return true;
        });

        Preference benchmark = findPreference("cli_benchmark");
        if (benchmark != null) benchmark.setOnPreferenceClickListener(p -> {
            Tools.swapFragment(requireActivity(), BenchmarkFragment.class, BenchmarkFragment.TAG, null);
            return true;
        });

        Preference about = findPreference("cli_about");
        if (about != null) about.setOnPreferenceClickListener(p -> {
            Tools.swapFragment(requireActivity(), AboutFragment.class, AboutFragment.TAG, null);
            return true;
        });
    }

    private void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = requirePreference("notification_permission_request");
        Activity activity = getActivity();
        if (activity instanceof LauncherActivity) {
            LauncherActivity launcherActivity = (LauncherActivity) activity;
            mRequestNotificationPermissionPreference.setVisible(!launcherActivity.checkForNotificationPermission());
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener(preference -> {
                launcherActivity.askForNotificationPermission(() -> mRequestNotificationPermissionPreference.setVisible(false));
                return true;
            });
        } else {
            mRequestNotificationPermissionPreference.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        LauncherPreferences.loadPreferences(getContext());
    }

    protected Preference requirePreference(CharSequence key) {
        Preference preference = findPreference(key);
        if (preference != null) return preference;
        throw new IllegalStateException("Preference " + key + " is null");
    }

    @SuppressWarnings("unchecked")
    protected <T extends Preference> T requirePreference(CharSequence key, Class<T> preferenceClass) {
        Preference preference = requirePreference(key);
        if (preferenceClass.isInstance(preference)) return (T) preference;
        throw new IllegalStateException("Preference " + key + " is not an instance of " + preferenceClass.getSimpleName());
    }
}
