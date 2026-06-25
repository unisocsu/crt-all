package com.example.crtinstaller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileExplorerActivity extends AppCompatActivity {

    private ListView listView;
    private File currentDir = Environment.getExternalStorageDirectory();
    private List<File> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // יצירת ListView בצורה תוכנתית והצגתה
        listView = new ListView(this);
        setContentView(listView);

        // טעינת תיקיית השורש הראשונית
        loadDirectory(currentDir);

        // האזנה ללחיצות על פריטים ברשימה
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = fileList.get(position);
                if (selectedFile.isDirectory()) {
                    // כניסה לתיקייה
                    loadDirectory(selectedFile);
                } else {
                    // החזרת הקובץ הנבחר
                    finishWithResult(selectedFile);
                }
            }
        });
    }

    // סריקה והצגה של תוכן התיקייה
    private void loadDirectory(File dir) {
        currentDir = dir;
        setTitle(dir.getAbsolutePath()); // הצגת הנתיב הנוכחי בכותרת המסך

        File[] files = dir.listFiles();
        if (files != null) {
            fileList.clear();

            // סינון: רק תיקיות או קבצים מסוג crt/pem
            for (File file : files) {
                if (file.isDirectory() || file.getName().toLowerCase().endsWith(".crt") || file.getName().toLowerCase().endsWith(".pem")) {
                    fileList.add(file);
                }
            }

            // מיון: תיקיות יוצגו תמיד קודם, ולאחר מכן קבצים לפי סדר אלפביתי
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
                }
            });

            // יצירת רשימת שמות מעוצבת לתצוגה
            List<String> displayNames = new ArrayList<>();
            for (File file : fileList) {
                if (file.isDirectory()) {
                    displayNames.add("📁 " + file.getName());
                } else {
                    displayNames.add("📜 " + file.getName());
                }
            }

            // חיבור הנתונים ל-ListView
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayNames);
            listView.setAdapter(adapter);
        } else {
            Toast.makeText(this, "אין גישה לתיקייה זו", Toast.LENGTH_SHORT).show();
        }
    }

    // שליחת נתיב הקובץ בחזרה ל-MainActivity וסגירת המסך
    private void finishWithResult(File file) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_file_path", file.getAbsolutePath());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    // טיפול בלחיצה על כפתור חזור במכשיר
    @Override
    public void onBackPressed() {
        File parent = currentDir.getParentFile();
        // אם יש תיקיית אב ואנחנו לא בתיקיית השורש הראשית - נעלה שלב למעלה
        if (parent != null && !currentDir.equals(Environment.getExternalStorageDirectory())) {
            loadDirectory(parent);
        } else {
            super.onBackPressed(); // יציאה מהאקטיביטי
        }
    }
}
