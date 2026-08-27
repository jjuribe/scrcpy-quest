package org.cagnulein.android_remote.model;

import java.nio.ByteBuffer;

public class AudioPacket {

    public enum Type {
        AUDIO((byte) 3);

        private final byte type;

        Type(byte type) {
            this.type = type;
        }

        public byte getType() {
            return type;
        }

        public static Type getType(byte value) {
            for (Type type : Type.values()) {
                if (type.getType() == value) {
                    return type;
                }
            }
            return null;
        }
    }

    public Type type;
    public long presentationTimeStamp;
    public int sampleRate;
    public int channels;
    public byte[] audioData;
    public int dataLength;

    public AudioPacket() {
    }

    public AudioPacket(long presentationTimeStamp, int sampleRate, int channels, int dataLength, byte[] audioData) {
        this.type = Type.AUDIO;
        this.presentationTimeStamp = presentationTimeStamp;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.dataLength = dataLength;
        this.audioData = new byte[dataLength];
        System.arraycopy(audioData, 0, this.audioData, 0, dataLength);
    }

    // Create packet from byte array
    public static AudioPacket fromArray(byte[] values) {
        if (values.length < 26) {
            throw new IllegalArgumentException("Invalid audio packet size");
        }

        AudioPacket audioPacket = new AudioPacket();

        // Packet type - 1 byte
        byte typeValue = values[0];
        audioPacket.type = Type.getType(typeValue);

        // Presentation timestamp - 8 bytes
        byte[] tsBytes = new byte[8];
        System.arraycopy(values, 1, tsBytes, 0, 8);
        audioPacket.presentationTimeStamp = ByteBuffer.wrap(tsBytes).getLong();

        // Sample rate - 4 bytes
        byte[] srBytes = new byte[4];
        System.arraycopy(values, 9, srBytes, 0, 4);
        audioPacket.sampleRate = ByteBuffer.wrap(srBytes).getInt();

        // Channels - 4 bytes
        byte[] chBytes = new byte[4];
        System.arraycopy(values, 13, chBytes, 0, 4);
        audioPacket.channels = ByteBuffer.wrap(chBytes).getInt();

        // Data length - 4 bytes
        byte[] dlBytes = new byte[4];
        System.arraycopy(values, 17, dlBytes, 0, 4);
        audioPacket.dataLength = ByteBuffer.wrap(dlBytes).getInt();

        // Audio data - remaining bytes
        if (values.length < 21 + audioPacket.dataLength) {
            throw new IllegalArgumentException("Audio data size mismatch");
        }

        audioPacket.audioData = new byte[audioPacket.dataLength];
        System.arraycopy(values, 21, audioPacket.audioData, 0, audioPacket.dataLength);

        return audioPacket;
    }

    // Create byte array from packet
    public byte[] toByteArray() {
        // Total packet size: 1 (type) + 8 (timestamp) + 4 (sampleRate) + 4 (channels) + 4 (dataLength) + dataLength
        int packetSize = 21 + dataLength;
        byte[] packet = new byte[packetSize];

        int offset = 0;

        // Type
        packet[offset++] = type.getType();

        // Presentation timestamp (8 bytes, big-endian)
        ByteBuffer.wrap(packet, offset, 8).putLong(presentationTimeStamp);
        offset += 8;

        // Sample rate (4 bytes, big-endian)
        ByteBuffer.wrap(packet, offset, 4).putInt(sampleRate);
        offset += 4;

        // Channels (4 bytes, big-endian)
        ByteBuffer.wrap(packet, offset, 4).putInt(channels);
        offset += 4;

        // Data length (4 bytes, big-endian)
        ByteBuffer.wrap(packet, offset, 4).putInt(dataLength);
        offset += 4;

        // Audio data
        System.arraycopy(audioData, 0, packet, offset, dataLength);

        return packet;
    }

    @Override
    public String toString() {
        return "AudioPacket{" +
                "type=" + type +
                ", timestamp=" + presentationTimeStamp +
                ", sampleRate=" + sampleRate +
                ", channels=" + channels +
                ", dataLength=" + dataLength +
                '}';
    }
}
