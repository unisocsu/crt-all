package com.example.screentool;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private CheckBox installCheckbox;
    private List<Uri> selectedFiles = new ArrayList<>();

    // יצירת ה-Launcher לפתיחת קבצים
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // כאן אנחנו מקבלים את הבחירה של המשתמש
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            selectedFiles.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        selectedFiles.add(result.getData().getData());
                    }
                    Toast.makeText(this, "נבחרו " + selectedFiles.size() + " קבצים", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        installCheckbox = new CheckBox(this);
        installCheckbox.setText("אפשר התקנה אוטומטית");
        
        Button pickFilesBtn = new Button(this);
        pickFilesBtn.setText("עיין בקבצים וסמן");
        pickFilesBtn.setOnClickListener(v -> openFilePicker());

        Button actionBtn = new Button(this);
        actionBtn.setText("בצע פעולה");
        actionBtn.setOnClickListener(v -> executeAction());

        layout.addView(installCheckbox);
        layout.addView(pickFilesBtn);
        layout.addView(actionBtn);
        setContentView(layout);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // מאפשר בחירת כמה קבצים
        filePickerLauncher.launch(Intent.createChooser(intent, "בחר קבצים"));
    }

    private void executeAction() {
        boolean isInstallEnabled = installCheckbox.isChecked();
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "לא נבחרו קבצים!", Toast.LENGTH_SHORT).show();
            return;
        }
        // כאן תוסיף את הלוגיקה שלך לטיפול בקבצים שנבחרו
        Toast.makeText(this, "מתחיל לעבד " + selectedFiles.size() + " קבצים. התקנה: " + isInstallEnabled, Toast.LENGTH_LONG).show();
    }
}
