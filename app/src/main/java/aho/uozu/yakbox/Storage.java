package aho.uozu.yakbox;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import aho.uozu.android.audio.AudioBuffer;
import aho.uozu.audio.wav.WaveFile;

/**
 * Handles saving / loading audio files
 */
class Storage {

    private static final String TAG = "Yakbox-Storage";
    private static final String BUFFER_FILENAME = "yakbox-sound.bin";
    private static Storage instance;
    private final Context context;

    /**
     * Used when errors regarding external media occur.
     */
    public static class StorageUnavailableException extends IOException {
    }

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
     * Returns all saved recordings.
     */
    public List<String> getSavedRecordingNames() throws StorageUnavailableException {
        List<File> waveFiles = getAllSavedRecordings();
        List<String> names = new ArrayList<>(waveFiles.size());
        for (int i = 0; i < waveFiles.size(); i++) {
            names.add(fileToRecordingName(waveFiles.get(i)));
        }
        return names;
    }

    /**
     * Save the given audio buffer to file.
     *
     * @param buffer buffer containing audio data
     * @param name name to give the saved file
     * @param samplingRate sampling rate of the audio
     *
     * @throws StorageUnavailableException if storage is unavailable
     * @throws IOException if any other IO errors occur
     */
    public void saveRecording(AudioBuffer buffer, String name, int samplingRate)
            throws StorageUnavailableException, IOException {
        String path = recordingNameToPath(name);
        saveRecordingToPath(buffer, path, samplingRate);
    }

    /**
     * Save the given audio buffer to a temporary file. Temporary recordings
     * are not returned by {@code #getSavedRecordingNames}.
     *
     * @param buffer buffer containing audio data
     * @param name name to give the saved file
     * @param samplingRate sampling rate of the audio
     *
     * @throws StorageUnavailableException if storage is unavailable
     * @throws IOException if any other IO errors occur
     *
     * @return The saved file. Saved to a public directory, therefore is
     * accessible by other apps.
     */
    public File saveTempRecording(AudioBuffer buffer, String name, int samplingRate)
            throws StorageUnavailableException, IOException {
        File dir = getTempStorageDir();
        if (!dir.exists()) {
            dir.mkdir();
        }
        File f = new File(dir, name + ".wav");
        saveRecordingToPath(buffer, f.getAbsolutePath(), samplingRate);
        return f;
        // TODO: delete these recordings at some point
    }

    private void saveRecordingToPath(AudioBuffer buffer, String path, int samplingRate)
            throws IOException {
        WaveFile wav = new WaveFile.Builder()
                .data(buffer.getBuffer())
                .numFrames(buffer.getIdx())
                .sampleRate(samplingRate)
                .bitDepth(16)
                .channels(1)
                .build();
        wav.writeToFile(path);
    }

    /**
     * Save buffer. Overwrites previous saves.
     */
    public void saveBuffer(AudioBuffer buffer) {
        File f = getBufferTempFile();
        try {
            buffer.saveToFile(f);
        } catch (IOException e) {
            Log.e(TAG, "Error saving buffer to file", e);
        }
    }

    /**
     * Load the last saved buffer to the given buffer.
     */
    public void loadBuffer(AudioBuffer buffer) {
        File f = getBufferTempFile();
        if (f.exists()) {
            try {
                buffer.loadFromFile(f);
            } catch (IOException e) {
                Log.e(TAG, "Error loading saved buffer", e);
            }
        }
    }

    /**
     * Get the file used to store audio buffer data
     */
    private File getBufferTempFile() {
        return new File(context.getFilesDir(), BUFFER_FILENAME);
    }

    public void loadRecordingToBuffer(AudioBuffer buffer, String name)
            throws IOException {
        File f = recordingNameToFile(name);
        WaveFile wav = WaveFile.fromFile(f.getPath());
        wav.getAudioData(buffer.getBuffer());
        buffer.resetIdx();
        buffer.incrementIdx(wav.getNumFrames());
    }

    /** Delete the specified recording */
    public void deleteRecording(String name)
            throws FileNotFoundException, StorageUnavailableException {
        File f = recordingNameToFile(name);
        if (!f.delete()) {
            Log.e(TAG, name + " not deleted");
        }
    }

    /** Returns true if the given recording name exists */
    public boolean exists(String name) throws StorageUnavailableException {
        List<String> recordings = getSavedRecordingNames();
        return recordings.contains(name);
    }

    /** Returns the directory under which user's recordings are saved */
    public File getStorageDir() throws StorageUnavailableException {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            throw new StorageUnavailableException();
        }
        return dir;
    }

    /**
     * Returns the directory for temp files. Files here aren't returned by
     * {@code #getSavedRecordingNames}.
     */
    public File getTempStorageDir() throws StorageUnavailableException {
        return new File(getStorageDir().toString() + "/temp");
    }

    /** Get all the wave files in the storage directory */
    private List<File> getAllSavedRecordings() throws StorageUnavailableException {
        List<File> waveFiles = new ArrayList<>();
        File dir = getStorageDir();
        for (File f : dir.listFiles()) {
            if (f.toString().endsWith(".wav")) {
                waveFiles.add(f);
            }
        }
        return waveFiles;
    }

    private String fileToRecordingName(File file) {
        String filename = file.getName();
        return filename.substring(0, filename.length() - 4);
    }

    private String recordingNameToPath(String name) throws StorageUnavailableException {
        File dir = getStorageDir();
        return dir.toString() + "/" + name + ".wav";
    }

    /**
     * Returns the saved recording with the given name.
     *
     * @throws aho.uozu.yakbox.Storage.StorageUnavailableException if storage is unavailable
     * @throws FileNotFoundException if file doesn't exist.
     */
    private File recordingNameToFile(String name)
            throws FileNotFoundException, StorageUnavailableException {
        File dir = getStorageDir();
        File file = new File(dir, name + ".wav");
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        return file;
    }
}
