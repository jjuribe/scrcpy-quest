package org.cagnulein.android_remote;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import org.cagnulein.android_remote.model.AudioPacket;

import java.io.DataInputStream;
import java.io.IOException;

public class AudioDecoder extends Thread {

    private static final int BUFFER_SIZE = 65536;
    private DataInputStream inputStream;
    private AudioTrack audioTrack;
    private volatile boolean running = false;

    public AudioDecoder(DataInputStream inputStream) {
        this.inputStream = inputStream;
        setName("AudioDecoder");
    }

    @Override
    public void run() {
        try {
            while (running) {
                try {
                    // Read packet size (4 bytes)
                    int packetSize = inputStream.readInt();
                    if (packetSize <= 0 || packetSize > BUFFER_SIZE) {
                        continue;
                    }

                    // Read packet data
                    byte[] packetData = new byte[packetSize];
                    inputStream.readFully(packetData);

                    // Parse audio packet
                    AudioPacket audioPacket = AudioPacket.fromArray(packetData);

                    // Initialize audio track if needed
                    if (audioTrack == null) {
                        initializeAudioTrack(audioPacket.sampleRate, audioPacket.channels);
                    }

                    // Write audio data to track
                    if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.write(audioPacket.audioData, 0, audioPacket.dataLength);
                    }

                } catch (IOException e) {
                    if (running) {
                        android.util.Log.e("AudioDecoder", "Error reading audio packet: " + e.getMessage());
                    }
                    break;
                }
            }
        } finally {
            stop();
        }
    }

    private void initializeAudioTrack(int sampleRate, int channels) {
        try {
            int channelConfig;
            if (channels == 1) {
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            } else if (channels == 2) {
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            } else {
                android.util.Log.w("AudioDecoder", "Unsupported channel count: " + channels);
                return;
            }

            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build();

            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = minBufferSize * 4;

            audioTrack = new AudioTrack(
                    audioAttributes,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioTrack.AUDIO_SESSION_ID_GENERATE
            );

            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.play();
                android.util.Log.i("AudioDecoder", "AudioTrack initialized - Sample Rate: " + sampleRate + "Hz, Channels: " + channels);
            } else {
                android.util.Log.e("AudioDecoder", "Failed to initialize AudioTrack");
                audioTrack = null;
            }
        } catch (Exception e) {
            android.util.Log.e("AudioDecoder", "Error initializing AudioTrack: " + e.getMessage());
            audioTrack = null;
        }
    }

    public void startDecoding() {
        if (!running) {
            running = true;
            start();
        }
    }

    public void stop() {
        running = false;
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
                android.util.Log.i("AudioDecoder", "AudioTrack stopped and released");
            } catch (Exception e) {
                android.util.Log.e("AudioDecoder", "Error stopping AudioTrack: " + e.getMessage());
            }
        }
    }

    public boolean isPlaying() {
        return running && audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }
}
