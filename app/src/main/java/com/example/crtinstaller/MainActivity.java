package com.example.crtinstaller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvSelectedFile;
    private Button btnSelectFile;
    private Button btnInstallSystem;
    private Button btnInstallRamTemp;
    private Button btnInstallRamPerm;

    private String selectedFilePath = null;

    // רישום קולט (Launcher) לקבלת תוצאה מסייר הקבצים שבנינו
    private final ActivityResultLauncher<Intent> fileExplorerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFilePath = result.getData().getStringExtra("selected_file_path");
                        if (selectedFilePath != null) {
                            tvSelectedFile.setText("קובץ שנבחר:\n" + selectedFilePath);
                            // הפעלת כפתורי ההתקנה לאחר בחירת קובץ
                            setInstallationButtonsEnabled(true);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔥 מילוי הדרישה: הזרקה אוטומטית של תעודות שמורות ל-RAM מיד עם הפעלת האפליקציה
        InstallManager.injectSavedCertsToRam(this);

        // אתחול רכיבי ממשק המשתמש (UI)
        tvSelectedFile = findViewById(R.id.tv_selected_file);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnInstallSystem = findViewById(R.id.btn_install_system);
        btnInstallRamTemp = findViewById(R.id.btn_install_ram_temp);
        btnInstallRamPerm = findViewById(R.id.btn_install_ram_perm);

        // חסימת כפתורי התקנה עד שייבחר קובץ תקין
        setInstallationButtonsEnabled(false);

        // לחיצה על כפתור בחירת קובץ - פותח את האקטיביטי של הסייר
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FileExplorerActivity.class);
                fileExplorerLauncher.launch(intent);
            }
        });

        // אפשרות 1: התקנה קבועה לדיסק הקשיח (System Partition)
        btnInstallSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateSelection()) {
                    InstallManager.installToSystemPartition(MainActivity.this, selectedFilePath);
                }
            }
        });

        // אפשרות 2: התקנה זמנית ל-RAM (נמחקת בכיבוי המכשיר)
        btnInstallRamTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateSelection()) {
                    InstallManager.installToRamTemp(MainActivity.this, selectedFilePath);
                }
            }
        });

        // אפשרות 3: התקנה קבועה ל-RAM (העתקה ל-Data והזרקה אוטומטית בכל הפעלה מחדש)
        btnInstallRamPerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateSelection()) {
                    InstallManager.saveToAppDataForRamAutoInject(MainActivity.this, selectedFilePath);
                }
            }
        });
    }

    private boolean validateSelection() {
        if (selectedFilePath == null || selectedFilePath.isEmpty()) {
            Toast.makeText(this, "אנא בחר קובץ תעודה תחילה", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setInstallationButtonsEnabled(boolean enabled) {
        btnInstallSystem.setEnabled(enabled);
        btnInstallRamTemp.setEnabled(enabled);
        btnInstallRamPerm.setEnabled(enabled);
    }
}
