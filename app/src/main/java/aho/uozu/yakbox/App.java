package aho.uozu.yakbox;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(
        mailTo = "uozu.aho@gmail.com",
        mode = ReportingInteractionMode.DIALOG,

        // dialog text
        resDialogTitle = R.string.acra_dialog_title,
        resDialogText = R.string.acra_dialog_text,
        resDialogNegativeButtonText = R.string.acra_dialog_btn_no,
        resDialogPositiveButtonText = R.string.acra_dialog_btn_yes
)
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
