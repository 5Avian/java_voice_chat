package fiveavian.java_voice_chat.client;

import fiveavian.java_voice_chat.VoiceChat;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.openal.AL11.*;

/**
 * An abstraction around an OpenAL source that allows queuing of sample buffers.
 * Useful for streaming short bits of audio.
 */
public class StreamingAudioSource implements AutoCloseable {
    public final int source = alGenSources();
    private final int[] buffers;
    // A native allocation to dispose unqueued buffers into.
    // Exists because you can't pass `NULL` to `nalSourceUnqueueBuffers`.
    private final long unqueueTrash;
    private int bufferIndex = 0;
    private int buffersAvailable;

    public StreamingAudioSource() {
        this(8);
    }

    public StreamingAudioSource(int numBuffers) {
        buffers = new int[numBuffers];
        alGenBuffers(buffers);
        buffersAvailable = numBuffers;
        unqueueTrash = MemoryUtil.nmemAlloc(4L * numBuffers);
    }

    /**
     * Copies the provided samples to the queue.
     * @return `true` if the samples have been added to the queue, `false` if the queue was too full
     */
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
        buffersAvailable -= 1;
        bufferIndex += 1;
        bufferIndex %= buffers.length;
        return true;
    }

    @Override
    public void close() {
        MemoryUtil.nmemFree(unqueueTrash);
        alDeleteBuffers(buffers);
        alDeleteSources(source);
    }
}
