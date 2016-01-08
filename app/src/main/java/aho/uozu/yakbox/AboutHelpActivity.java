package aho.uozu.yakbox;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class AboutHelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_help);

        // Set version text
        TextView versionTxt = (TextView) findViewById(R.id.txt_version);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            versionTxt.setText(getString(R.string.about_version_label) + " " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        // Set storage text
        TextView storageTxt = (TextView) findViewById(R.id.txt_storage_dir);
        try {
            storageTxt.setText(Storage.getInstance(this).getStorageDir().getAbsolutePath());
        } catch (Storage.StorageUnavailableException e) {
            storageTxt.setText(getString(R.string.about_storage_inaccessible_error));
        }
    }

}
