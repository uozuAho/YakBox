package aho.uozu.yakbox;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import wav.WaveFile;

class Storage {

    private static Storage instance;
    private final Context context;
    private static final String TAG = "Yakbox-Storage";

    private Storage(Context context) {
        this.context = context;
    }

    public static Storage getInstance(Context context) {
        if (instance == null) {
            instance = new Storage(context);
        }
        return instance;
    }

    /**
     * Returns an array of paths to all wave files in this app's
     * external storage directory.
     */
    public List<String> getSavedRecordingNames() {
        List<File> waveFiles = getAllWaveFiles();
        List<String> names = new ArrayList<>(waveFiles.size());
        for (int i = 0; i < waveFiles.size(); i++) {
            names.add(fileToRecordingName(waveFiles.get(i)));
        }
        return names;
    }

    public void loadRecordingToBuffer(AudioBuffer buffer, String name)
            throws IOException {
        WaveFile wav = loadSavedRecording(name);
        wav.getAudioData(buffer.getBuffer());
        buffer.resetIdx();
        buffer.incrementIdx(wav.getNumFrames());
    }

    public WaveFile loadSavedRecording(String name)
            throws IOException {
        File f = recordingNameToFile(name);
        return WaveFile.fromFile(f.getPath());
    }

    /** Delete the specified recording */
    public void deleteRecording(String name)
            throws FileNotFoundException {
        File f = recordingNameToFile(name);
        if (!f.delete()) {
            Log.e(TAG, name + " not deleted");
        }
    }

    /** Returns true if the given recording name exists */
    public boolean exists(String name) {
        List<String> recordings = getSavedRecordingNames();
        return recordings.contains(name);
    }

    private List<File> getAllWaveFiles() {
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

    private boolean isExternalStorageReadWriteable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private String fileToRecordingName(File file) {
        String filename = file.getName();
        return filename.substring(0, filename.length() - 4);
    }

    private File recordingNameToFile(String name)
            throws FileNotFoundException {
        File file = null;
        if (isExternalStorageReadWriteable()) {
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                String path = dir.toString() + "/" + name + ".wav";
                file = new File(path);
                if (!file.exists()) throw new FileNotFoundException();
            }
        }
        return file;
    }
}
