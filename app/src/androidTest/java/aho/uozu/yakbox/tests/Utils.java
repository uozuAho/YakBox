package aho.uozu.yakbox.tests;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;

import aho.uozu.android.audio.AudioBuffer;
import aho.uozu.yakbox.Storage;

class Utils {

    public static void deleteAllRecordings(Context context)
            throws Storage.StorageUnavailableException, FileNotFoundException {
        Storage storage = Storage.getInstance(context);
        for (String name : storage.getSavedRecordingNames()) {
            storage.deleteRecording(name);
        }
    }

    public static void saveDummyRecording(Context context, String name) throws IOException {
        Storage storage = Storage.getInstance(context);
        storage.saveRecording(new AudioBuffer(100), name, 22050);
    }
}
