package aho.uozu.yakbox;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wav.WaveFile;

class Storage {

    private Storage() {}

    /**
     * Returns an array of paths to all wave files in this app's
     * external storage directory.
     */
    public static String[] getSavedRecordingNames(Context context) {
        List<File> waveFiles = getAllWaveFiles(context);
        String[] names = new String[waveFiles.size()];
        for (int i = 0; i < waveFiles.size(); i++) {
            names[i] = fileToRecordingName(waveFiles.get(i));
        }
        Arrays.sort(names);
        return names;
    }

    public static WaveFile loadSavedRecording(Context context, String name)
            throws IOException {
        File f = recordingNameToFile(context, name);
        return WaveFile.fromFile(f.getPath());
    }

    private static List<File> getAllWaveFiles(Context context) {
        List<File> waveFiles = new ArrayList<>();
        if (isExternalStorageReadWriteable()) {
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                for (File f : dir.listFiles()) {
                    if (f.toString().endsWith(".wav")) {
                        waveFiles.add(f);
                    }
                }
            }
        }
        return waveFiles;
    }

    private static boolean isExternalStorageReadWriteable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private static String fileToRecordingName(File file) {
        String filename = file.getName();
        return filename.substring(0, filename.length() - 4);
    }

    private static File recordingNameToFile(Context context, String name)
            throws FileNotFoundException {
        File file = null;
        if (isExternalStorageReadWriteable()) {
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                String path = dir.toString() + "/" + name + ".wav";
                file = new File(path);
            }
        }
        if (!file.exists()) throw new FileNotFoundException();
        return file;
    }
}
