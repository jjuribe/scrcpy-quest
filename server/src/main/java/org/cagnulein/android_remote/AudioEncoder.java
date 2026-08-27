package org.cagnulein.android_remote;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import org.cagnulein.android_remote.model.AudioPacket;

import java.io.IOException;
import java.io.OutputStream;

public class AudioEncoder {

    private static final int SAMPLE_RATE = 44100; // Hz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNELS = 2;

    private AudioRecord audioRecord;
    private volatile boolean running = false;

    public AudioEncoder() {
    }

    public void start() {
        if (running) {
            return;
        }

        try {
            createAudioRecord();
            running = true;
            audioRecord.startRecording();
            Ln.i("AudioEncoder started");
        } catch (Exception e) {
            Ln.e("Failed to start audio encoder: " + e.getMessage());
            throw new RuntimeException("Cannot start AudioEncoder", e);
        }
    }

    public void stop() {
        running = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Ln.i("AudioEncoder stopped");
            } catch (Exception e) {
                Ln.e("Error stopping audio encoder: " + e.getMessage());
            }
        }
    }

    private void createAudioRecord() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        int bufferSize = minBufferSize * 4;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use Workarounds.createAudioRecord for Android 11+
            audioRecord = Workarounds.createAudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    CHANNELS,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AUDIO_FORMAT
            );
        } else {
            // Fallback for older Android versions
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new RuntimeException("AudioRecord failed to initialize");
        }
    }

    public void streamAudio(OutputStream outputStream) throws IOException {
        if (audioRecord == null) {
            throw new IllegalStateException("AudioEncoder not started");
        }

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        byte[] buffer = new byte[minBufferSize];

        Ln.i("Starting audio stream - Sample Rate: " + SAMPLE_RATE + ", Channels: " + CHANNELS);

        while (running) {
            try {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // Create and send audio packet
                    AudioPacket audioPacket = new AudioPacket(
                            System.nanoTime(),
                            SAMPLE_RATE,
                            CHANNELS,
                            bytesRead,
                            buffer
                    );

                    byte[] packetData = audioPacket.toByteArray();
                    outputStream.write(packetData);
                    outputStream.flush();
                } else if (bytesRead < 0) {
                    Ln.w("AudioRecord read error: " + bytesRead);
                }
            } catch (IOException e) {
                if (running) {
                    Ln.e("Error writing audio packet: " + e.getMessage());
                    throw e;
                }
                // If not running anymore, exit gracefully
                break;
            }
        }

        Ln.i("Audio stream ended");
    }

    public int getSampleRate() {
        return SAMPLE_RATE;
    }

    public int getChannels() {
        return CHANNELS;
    }

    public boolean isRunning() {
        return running;
    }
}
