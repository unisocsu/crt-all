package com.example.crtinstaller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private Button btnInstallExecute;
    private RadioGroup rgInstallType;
    private RadioButton rbSystem;
    private RadioButton rbRamTemp;
    private RadioButton rbRamPerm;

    private String selectedFilePath = null;

    private final ActivityResultLauncher<Intent> fileExplorerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFilePath = result.getData().getStringExtra("selected_file_path");
                        if (selectedFilePath != null) {
                            tvSelectedFile.setText("קובץ שנבחר:\n" + selectedFilePath);
                            // הפעלת כפתור ההתקנה ברגע שיש קובץ
                            btnInstallExecute.setEnabled(true);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // הזרקה אוטומטית של תעודות שמורות ל-RAM בעליית האפליקציה
        InstallManager.injectSavedCertsToRam(this);

        // אתחול רכיבים מה-XML
        tvSelectedFile = findViewById(R.id.tv_selected_file);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnInstallExecute = findViewById(R.id.btn_install_execute);
        rgInstallType = findViewById(R.id.rg_install_type);
        rbSystem = findViewById(R.id.rb_system);
        rbRamTemp = findViewById(R.id.rb_ram_temp);
        rbRamPerm = findViewById(R.id.rb_ram_perm);

        // כפתור ההתקנה חסום עד שייבחר קובץ
        btnInstallExecute.setEnabled(false);

        // מעבר לסייר הקבצים בלחיצה על "עיון בקבצים"
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FileExplorerActivity.class);
                fileExplorerLauncher.launch(intent);
            }
        });

        // כפתור התקן - מריץ את ההתקנה לפי מה שמסומן ב-V
        btnInstallExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedFilePath == null || selectedFilePath.isEmpty()) {
                    Toast.makeText(MainActivity.this, "אנא בחר קובץ תעודה תחילה", Toast.LENGTH_SHORT).show();
                    return;
                }

                // בדיקה איזה כפתור מסומן והרצת הפקודה המתאימה
                if (rbSystem.isChecked()) {
                    InstallManager.installToSystemPartition(MainActivity.this, selectedFilePath);
                } else if (rbRamTemp.isChecked()) {
                    InstallManager.installToRamTemp(MainActivity.this, selectedFilePath);
                } else if (rbRamPerm.isChecked()) {
                    InstallManager.saveToAppDataForRamAutoInject(MainActivity.this, selectedFilePath);
                }
            }
        });

        // קביעת פוקוס ראשוני על כפתור העיון בשביל מקשים פיזיים
        btnSelectFile.requestFocus();
    }
}
