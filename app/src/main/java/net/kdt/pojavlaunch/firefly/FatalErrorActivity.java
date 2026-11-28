package net.kdt.pojavlaunch.firefly;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.firefly.R;

public class FatalErrorActivity extends AppCompatActivity {
    private static final String TAG = "FatalErrorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent() != null ? getIntent().getExtras() : null;
        if (extras == null) {
            finish();
            return;
        }

        boolean storageAllow = extras.getBoolean("storageAllow", false);
        String strSavePath = extras.getString("savePath");

        // The stack trace is passed as a pre-rendered String. Passing a Throwable through an
        // Intent used to come back with null entries in its suppressed-exception array, which made
        // Throwable.printStackTrace() throw a NullPointerException inside this activity and hide
        // the real crash. A String cannot be corrupted that way.
        String stackTrace = extras.getString("stackTrace");
        if (stackTrace == null || stackTrace.isEmpty()) {
            Object legacy = extras.getSerializable("throwable");
            if (legacy instanceof Throwable) {
                stackTrace = Tools.printToString((Throwable) legacy);
            }
        }
        if (stackTrace == null || stackTrace.isEmpty()) {
            stackTrace = "No stack trace was provided.";
        }

        String errHeader = storageAllow
                ? "Crash stack trace saved to " + strSavePath + "."
                : "Storage permission is required to save crash stack trace!";

        final String finalStackTrace = stackTrace;
        try {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error_fatal)
                    .setMessage(errHeader + "\n\n" + finalStackTrace)
                    .setPositiveButton(android.R.string.ok, (p1, p2) -> finish())
                    .setNegativeButton(R.string.global_restart, (p1, p2) -> {
                        startActivity(new Intent(FatalErrorActivity.this, LauncherActivity.class));
                        finish();
                    })
                    .setNeutralButton(android.R.string.copy, (p1, p2) -> {
                        try {
                            ClipboardManager mgr = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            if (mgr != null) {
                                mgr.setPrimaryClip(ClipData.newPlainText("error", finalStackTrace));
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Unable to copy stack trace", t);
                        }
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Throwable t) {
            // Never let the error screen crash the app a second time.
            Log.e(TAG, "Unable to show fatal error dialog", t);
            finish();
        }
    }

    public static void showError(Context ctx, String savePath, boolean storageAllow, Throwable th) {
        String rendered;
        try {
            rendered = Tools.printToString(th);
        } catch (Throwable t) {
            rendered = "Unable to render stack trace: " + t;
        }
        showError(ctx, savePath, storageAllow, rendered);
    }

    public static void showError(Context ctx, String savePath, boolean storageAllow, String stackTrace) {
        try {
            Intent fatalErrorIntent = new Intent(ctx, FatalErrorActivity.class);
            fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            fatalErrorIntent.putExtra("stackTrace", stackTrace);
            fatalErrorIntent.putExtra("savePath", savePath);
            fatalErrorIntent.putExtra("storageAllow", storageAllow);
            ctx.startActivity(fatalErrorIntent);
        } catch (Throwable t) {
            Log.e(TAG, "Unable to launch FatalErrorActivity", t);
        }
    }
}
