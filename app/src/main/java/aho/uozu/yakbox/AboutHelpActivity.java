package aho.uozu.yakbox;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;

public class AboutHelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_help);

        TextView versionTxt = (TextView) findViewById(R.id.txt_version);
        try {
            versionTxt.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

}
