package net.kdt.pojavlaunch.firefly.fragments;

import static com.firefly.utils.ToastUtils.Toast;

import static net.kdt.pojavlaunch.firefly.Tools.runOnUiThread;
import static net.kdt.pojavlaunch.firefly.Tools.shareLog;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;
import com.movtery.ui.fragment.ProfilePathManagerFragment;
import com.qz.terminal2.ConsoleActivity;

import net.kdt.pojavlaunch.firefly.CustomControlsActivity;
import net.kdt.pojavlaunch.firefly.LauncherActivity;
import net.kdt.pojavlaunch.firefly.R;
import net.kdt.pojavlaunch.firefly.Tools;
import net.kdt.pojavlaunch.firefly.extra.ExtraConstants;
import net.kdt.pojavlaunch.firefly.extra.ExtraCore;
import net.kdt.pojavlaunch.firefly.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.firefly.progresskeeper.TaskCountListener;

public class MainMenuFragment extends Fragment implements TaskCountListener {
    public static final String TAG = "MainMenuFragment";
    private static final int REQUEST_CODE_PERMISSIONS = 0;
    private mcVersionSpinner mVersionSpinner;
    private boolean mTasksRunning;

    public MainMenuFragment() {
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageButton mPathManagerButton = view.findViewById(R.id.path_manager_button);
        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        bindQuickStats(view);

        mPathManagerButton.setOnClickListener(v -> {
            if (!mTasksRunning) {
                checkPermissions(() -> Tools.swapFragment(requireActivity(), ProfilePathManagerFragment.class, ProfilePathManagerFragment.TAG, null));
            } else {
                runOnUiThread(() -> Toast(requireContext(), R.string.profiles_path_task_in_progress));
            }
        });

        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        View mModsButton = view.findViewById(R.id.mod_manager_button);
        if (mModsButton != null) {
            mModsButton.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ModManagerFragment.class, ModManagerFragment.TAG, null));
        }

        mPlayButton.setOnClickListener(v -> ((LauncherActivity) requireActivity()).launchGame());
        mPlayButton.setOnLongClickListener(v -> {
            ((LauncherActivity) requireActivity()).launchGameSkippingIntegrityCheck();
            return true;
        });

    }

    /** Isi kartu ringkasan (RAM / renderer / Java) dengan nilai nyata dari preferensi. */
    private void bindQuickStats(View view) {
        android.widget.TextView ramView = view.findViewById(R.id.cli_stat_ram);
        android.widget.TextView rendererView = view.findViewById(R.id.cli_stat_renderer);
        android.widget.TextView javaView = view.findViewById(R.id.cli_stat_java);
        if (ramView == null) ramView = view.findViewById(R.id.cli_stat_ram_value);
        if (rendererView == null) rendererView = view.findViewById(R.id.cli_stat_renderer_value);
        if (javaView == null) javaView = view.findViewById(R.id.cli_stat_java_value);

        if (ramView != null) {
            ramView.setText(net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences.PREF_RAM_ALLOCATION + " MB");
        }
        if (rendererView != null) {
            rendererView.setText(rendererLabel(net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences.PREF_RENDERER));
        }
        if (javaView != null) {
            String runtime = net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences.PREF_DEFAULT_RUNTIME;
            javaView.setText(runtime == null || runtime.trim().isEmpty() ? "Otomatis" : runtime);
        }
    }

    private static String rendererLabel(String key) {
        if (key == null) return "Otomatis";
        switch (key) {
            case "opengles2": return "GL4ES 1.1.4";
            case "opengles2_ptitseb": return "GL4ES PtitSeb";
            case "opengles2_vgpu": return "VGPU";
            case "opengles2_vgpu_1": return "VGPU 1.3.6.8";
            case "opengles3_nggl4es": return "NG GL4ES";
            case "opengles3_mges": return "MobileGlues";
            case "opengles3_mggl": return "MobileGL";
            case "mesa_3d": return "Mesa 3D";
            case "vulkan_zink": return "Vulkan Zink";
            case "gallium_virgl": return "VirGL";
            case "gallium_freedreno": return "Freedreno";
            case "gallium_panfrost": return "Panfrost";
            default: return key;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionSpinner.reloadProfiles();
        View v = getView();
        if (v != null) bindQuickStats(v);
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG);
    }

    @Override
    public void onUpdateTaskCount(int taskCount) {
        mTasksRunning = taskCount != 0;
    }

    private void checkPermissions(PermissionGranted permissionGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            handlePermissionsForAndroid11AndAbove(permissionGranted);
        } else {
            handlePermissionsForAndroid10AndBelow(permissionGranted);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void handlePermissionsForAndroid11AndAbove(PermissionGranted permissionGranted) {
        if (!Environment.isExternalStorageManager()) {
            showPermissionRequestDialog(() -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_PERMISSIONS);
            });
        } else {
            permissionGranted.granted();
        }
    }

    private void handlePermissionsForAndroid10AndBelow(PermissionGranted permissionGranted) {
        if (!hasStoragePermissions()) {
            showPermissionRequestDialog(() -> ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CODE_PERMISSIONS));
        } else {
            permissionGranted.granted();
        }
    }

    private boolean hasStoragePermissions() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionRequestDialog(RequestPermissions requestPermissions) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.permissions_manage_external_storage)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> requestPermissions.onRequest())
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .show();
    }

    private interface RequestPermissions {
        void onRequest();
    }

    private interface PermissionGranted {
        void granted();
    }
}
