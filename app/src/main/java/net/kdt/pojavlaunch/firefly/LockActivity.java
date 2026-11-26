package net.kdt.pojavlaunch.firefly;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.firefly.utils.AppLock;

/** Cliorius: layar kunci PIN sebelum masuk launcher. */
public class LockActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);
        EditText pin = findViewById(R.id.lock_pin_input);
        Button unlock = findViewById(R.id.lock_unlock_button);
        unlock.setOnClickListener(v -> {
            String input = pin.getText().toString().trim();
            if (AppLock.verify(this, input)) {
                AppLock.markUnlocked(this);
                startActivity(new Intent(this, LauncherActivity.class));
                finish();
            } else {
                Toast.makeText(this, R.string.cli_lock_wrong_pin, Toast.LENGTH_SHORT).show();
                pin.setText("");
            }
        });
    }
}
