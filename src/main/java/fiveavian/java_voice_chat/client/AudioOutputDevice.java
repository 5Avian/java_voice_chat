package fiveavian.java_voice_chat.client;

import fiveavian.java_voice_chat.VoiceChat;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.openal.AL11.*;

public class AudioOutputDevice implements AutoCloseable {
    private static final int BUFFER_COUNT = 8;

    private final int source = alGenSources();
    private final int[] buffers = new int[BUFFER_COUNT];
    private final long unqueueTrash = MemoryUtil.nmemAlloc(4 * BUFFER_COUNT);
    private int bufferIndex = 0;
    private int buffersAvailable = BUFFER_COUNT;

    public AudioOutputDevice() {
        alGenBuffers(buffers);
    }

    public boolean queueSamples(short[] samples) {
        int buffersToUnqueue = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        buffersAvailable += buffersToUnqueue;
        nalSourceUnqueueBuffers(source, buffersToUnqueue, unqueueTrash);
        if (buffersAvailable == 0)
            return false;
        AL10.alBufferData(buffers[bufferIndex], AL_FORMAT_MONO16, samples, VoiceChat.SAMPLE_RATE);
        alSourceQueueBuffers(source, buffers[bufferIndex]);
        int state = alGetSourcei(source, AL_SOURCE_STATE);
        if (state != AL_PLAYING)
            alSourcePlay(source);
        bufferIndex = (bufferIndex + 1) % BUFFER_COUNT;
        buffersAvailable -= 1;
        return true;
    }

    @Override
    public void close() {
        MemoryUtil.nmemFree(unqueueTrash);
        alDeleteBuffers(buffers);
    }
}
