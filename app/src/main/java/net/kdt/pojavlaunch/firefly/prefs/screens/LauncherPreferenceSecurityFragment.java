package net.kdt.pojavlaunch.firefly.prefs.screens;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import net.kdt.pojavlaunch.firefly.R;
import net.kdt.pojavlaunch.firefly.Tools;
import net.kdt.pojavlaunch.firefly.utils.AppLock;
import net.kdt.pojavlaunch.firefly.utils.SessionManager;

/** Cliorius: keamanan & akun — kunci app (PIN) + logout semua sesi. */
public class LauncherPreferenceSecurityFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_security);

        SwitchPreference lock = findPreference("app_lock_enabled");
        if (lock != null) {
            lock.setChecked(AppLock.isEnabled(requireContext()));
            lock.setOnPreferenceChangeListener((p, newVal) -> {
                boolean enabled = (Boolean) newVal;
                if (enabled && !AppLock.hasPin(requireContext())) {
                    promptSetPin(true);
                    return false;
                }
                AppLock.setEnabled(requireContext(), enabled);
                return true;
            });
        }

        Preference setPin = findPreference("app_lock_set_pin");
        if (setPin != null) setPin.setOnPreferenceClickListener(p -> { promptSetPin(false); return true; });

        Preference sessions = findPreference("cli_sessions_info");
        if (sessions != null) sessions.setOnPreferenceClickListener(p -> {
            int count = SessionManager.countAccounts();
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.cli_sec_sessions_title)
                    .setMessage(getString(R.string.cli_sec_sessions_msg, count))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        });

        Preference logout = findPreference("cli_logout_all");
        if (logout != null) logout.setOnPreferenceClickListener(p -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.cli_sec_logout_confirm_title)
                    .setMessage(R.string.cli_sec_logout_confirm_msg)
                    .setPositiveButton(R.string.cli_sec_logout, (d, w) -> {
                        SessionManager.LogoutResult r = SessionManager.logoutAll();
                        toast(getString(R.string.cli_sec_logout_done, r.removed, r.microsoftRevoked));
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
    }

    private void promptSetPin(boolean enabling) {
        android.view.View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_pin_input, null, false);
        EditText input = view.findViewById(R.id.pin_input);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cli_sec_pin_set_title)
                .setMessage(R.string.cli_sec_pin_set_msg)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String pin = input.getText().toString().trim();
                    if (pin.length() < 4) {
                        toast(getString(R.string.cli_sec_pin_too_short));
                        return;
                    }
                    AppLock.setPin(requireContext(), pin);
                    AppLock.setEnabled(requireContext(), true);
                    toast(getString(R.string.cli_sec_pin_saved));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
    }
}
