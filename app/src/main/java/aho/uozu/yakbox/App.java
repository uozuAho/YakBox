package aho.uozu.yakbox;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(
        mailTo = "uozu.aho@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        customReportContent = {
                ReportField.ANDROID_VERSION,
                ReportField.APP_VERSION_NAME,
                ReportField.BRAND,
                ReportField.PHONE_MODEL,
                ReportField.STACK_TRACE
        },

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
        ACRA.init(this);
    }
}
