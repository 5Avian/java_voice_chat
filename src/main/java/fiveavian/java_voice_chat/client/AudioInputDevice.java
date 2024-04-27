package fiveavian.java_voice_chat.client;

import fiveavian.java_voice_chat.VoiceChat;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC11.*;

public class AudioInputDevice implements AutoCloseable {
    private static final int DEVICE_BUFFER_LENGTH = VoiceChat.BUFFER_LENGTH * 8;

    private final long device;
    private final short[] samples = new short[VoiceChat.BUFFER_LENGTH];

    public AudioInputDevice(String deviceName) {
        device = alcCaptureOpenDevice(deviceName, VoiceChat.SAMPLE_RATE, AL_FORMAT_MONO16, DEVICE_BUFFER_LENGTH);
        alcCaptureStart(device);
    }

    public short[] pollSamples() {
        int samplesReady = alcGetInteger(device, ALC_CAPTURE_SAMPLES);
        if (samplesReady < VoiceChat.BUFFER_LENGTH)
            return null;
        alcCaptureSamples(device, samples, VoiceChat.BUFFER_LENGTH);
        return samples;
    }

    @Override
    public void close() {
        alcCaptureStop(device);
        alcCaptureCloseDevice(device);
    }
}
