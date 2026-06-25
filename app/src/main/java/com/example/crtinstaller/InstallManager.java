package com.example.crtinstaller;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

public class InstallManager {

    private static final String TAG = "InstallManager";
    private static final String SYSTEM_CACERTS_DIR = "/system/etc/security/cacerts";

    // ----------------------------------------------------
    // אפשרות 1: התקנה קבועה לדיסק הקשיח (מחיצת המערכת)
    // ----------------------------------------------------
    public static void installToSystemPartition(Context context, String sourceFilePath) {
        String certHashName = getAndroidCertHashName(sourceFilePath);
        if (certHashName == null) {
            showToast(context, "שגיאה בפיענוח התעודה");
            return;
        }

        // פקודות רוט להפיכת ה-System לבר-כתיבה והעתקת הקובץ
        String[] commands = {
            "mount -o rw,remount /system",
            "mount -o rw,remount /",
            "cp " + sourceFilePath + " " + SYSTEM_CACERTS_DIR + "/" + certHashName,
            "chmod 644 " + SYSTEM_CACERTS_DIR + "/" + certHashName,
            "mount -o ro,remount /system"
        };

        if (executeRootCommands(commands)) {
            showToast(context, "התקנה קבועה במערכת הצליחה!");
        } else {
            showToast(context, "ההתקנה נכשלה. ודא שיש הרשאות רוט ומחיצת המערכת ניתנת לכתיבה.");
        }
    }

    // ----------------------------------------------------
    // אפשרות 2: התקנה זמנית ל-RAM (נעלמת במיחול/הפעלה מחדש)
    // ----------------------------------------------------
    public static void installToRamTemp(Context context, String sourceFilePath) {
        String certHashName = getAndroidCertHashName(sourceFilePath);
        if (certHashName == null) {
            showToast(context, "שגיאה בפיענוח התעודה");
            return;
        }

        // טריק RAM באנדרואיד: מעתיקים את כל התעודות הקיימות לתיקייה זמנית ב-RAM,
        // מוסיפים את שלנו, ומבצעים Bind Mount על התיקייה המקורית של המערכת ב-RAM.
        String[] commands = {
            "mkdir -p /data/local/tmp/cacerts",
            "mount -t tmpfs tmpfs /data/local/tmp/cacerts",
            "cp " + SYSTEM_CACERTS_DIR + "/* /data/local/tmp/cacerts/",
            "cp " + sourceFilePath + " /data/local/tmp/cacerts/" + certHashName,
            "chmod 644 /data/local/tmp/cacerts/*",
            "chown root:root /data/local/tmp/cacerts/*",
            "mount --bind /data/local/tmp/cacerts " + SYSTEM_CACERTS_DIR
        };

        if (executeRootCommands(commands)) {
            showToast(context, "התעודה הזרקה ל-RAM בהצלחה!");
        } else {
            showToast(context, "ההזרקה ל-RAM נכשלה.");
        }
    }

    // ----------------------------------------------------
    // אפשרות 3: התקנה קבועה ל-RAM (העתקה ל-Data והזרקה בכל הפעלה)
    // ----------------------------------------------------
    
    // שלב א': שמירת הקובץ בתיקיית ה-Data הפרטית של האפליקציה
    public static void saveToAppDataForRamAutoInject(Context context, String sourceFilePath) {
        String certHashName = getAndroidCertHashName(sourceFilePath);
        if (certHashName == null) {
            showToast(context, "שגיאה בפיענוח התעודה");
            return;
        }

        File internalTargetFile = new File(context.getFilesDir(), certHashName);
        try (InputStream in = new FileInputStream(sourceFilePath);
             FileOutputStream out = new FileOutputStream(internalTargetFile)) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            
            showToast(context, "הקובץ נשמר ב-Data. יתבצע שיוך ל-RAM בכל הפעלה!");
            // מפעילים מיד את ההזרקה לראם עבור הפעם הנוכחית
            injectSavedCertsToRam(context);

        } catch (Exception e) {
            Log.e(TAG, "Error saving cert to internal storage", e);
            showToast(context, "שגיאה בשמירת הקובץ בתוך ה-Data");
        }
    }

    // שלב ב': הפונקציה שתקרא מתוך ה-MainActivity בכל הפעלה של האפליקציה
    public static void injectSavedCertsToRam(Context context) {
        File internalDir = context.getFilesDir();
        File[] files = internalDir.listFiles();
        
        if (files == null || files.length == 0) {
            return; // אין תעודות שמורות ב-Data
        }

        // מייצרים את פקודת ה-RAM עבור כל הקבצים השמורים ב-Data
        StringBuilder script = new StringBuilder();
        script.append("mkdir -p /data/local/tmp/cacerts\n");
        script.append("mount -t tmpfs tmpfs /data/local/tmp/cacerts\n");
        script.append("cp ").append(SYSTEM_CACERTS_DIR).append("/* /data/local/tmp/cacerts/\n");
        
        for (File file : files) {
            if (file.getName().endsWith(".0")) {
                script.append("cp ").append(file.getAbsolutePath()).append(" /data/local/tmp/cacerts/").append(file.getName()).append("\n");
            }
        }
        
        script.append("chmod 644 /data/local/tmp/cacerts/*\n");
        script.append("chown root:root /data/local/tmp/cacerts/*\n");
        script.append("mount --bind /data/local/tmp/cacerts ").append(SYSTEM_CACERTS_DIR).append("\n");

        String[] commands = script.toString().split("\n");
        executeRootCommands(commands);
    }

    // ----------------------------------------------------
    // פונקציות עזר: הרצת פקודות רוט וחישוב ה-Hash של אנדרואיד
    // ----------------------------------------------------

    private static boolean executeRootCommands(String[] commands) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                os.writeBytes(command + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            int exitVal = process.waitFor();
            return exitVal == 0;
        } catch (Exception e) {
            Log.e(TAG, "Root execution failed", e);
            return false;
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static String getAndroidCertHashName(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
            X500Principal principal = cert.getSubjectX500Principal();
            
            // חישוב ה-Hash הייחודי של מערכת אנדרואיד לכותרת התעודה
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(principal.getEncoded());
            
            // אנדרואיד לוקחת את 4 הבייטים הראשונים בסדר הפוך (Little Endian) וממירה לטקסט הקסדצימלי
            long unsignedHash = ((long) (hash[3] & 0xFF) << 24) |
                               ((long) (hash[2] & 0xFF) << 16) |
                               ((long) (hash[1] & 0xFF) << 8)  |
                               ((long) (hash[0] & 0xFF));
            
            return String.format("%08x.0", unsignedHash);
        } catch (Exception e) {
            Log.e(TAG, "Failed to calculate cert hash", e);
            return null;
        }
    }

    private static void showToast(final Context context, final String text) {
        if (context != null) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    }
}
