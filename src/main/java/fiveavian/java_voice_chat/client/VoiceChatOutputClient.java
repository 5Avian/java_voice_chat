package fiveavian.java_voice_chat.client;

import fiveavian.java_voice_chat.VoiceChat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class VoiceChatOutputClient implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceChatOutputClient.class);

    private final DatagramSocket socket;
    private boolean shouldStop = true;

    private final byte[] payload = new byte[VoiceChat.PAYLOAD_SIZE];
    private final ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
    private final DatagramPacket packet = new DatagramPacket(payload, payload.length);

    // Working variable to prevent unnecessary allocations.
    private final short[] samples = new short[VoiceChat.BUFFER_LENGTH];
    private final Int2ObjectMap<StreamingAudioSource> sources = new Int2ObjectOpenHashMap<>();

    public VoiceChatOutputClient(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        shouldStop = false;
        while (!shouldStop && !socket.isClosed()) {
            try {
                processOutput();
            } catch (Exception ex) {
                LOGGER.error("Caught an exception while running", ex);
            }
        }
    }

    private void processOutput() throws Exception {
        packet.setLength(payload.length);
        socket.receive(packet);
        boolean isCorrectSize = packet.getLength() == VoiceChat.PAYLOAD_SIZE;
        boolean hasMagicNumber = payloadBuffer.getInt(0) == VoiceChat.MAGIC_NUMBER;
        if (!isCorrectSize || !hasMagicNumber) {
            LOGGER.info("Received invalid packet, ignoring");
            return;
        }
        int id = payloadBuffer.getInt(4);
        StreamingAudioSource source = sources.get(id);
        if (source == null) {
            LOGGER.info("Received packet from unknown client, ignoring");
        }
        LOGGER.info("Received packet from {}", packet.getSocketAddress());
        for (int i = 0; i < VoiceChat.BUFFER_LENGTH; i++)
            samples[i] = payloadBuffer.getShort(8 + 2 * i);
        source.queueSamples(samples);
    }

    public void addSource(int id) {
        if (!sources.containsKey(id))
            sources.put(id, new StreamingAudioSource());
    }

    public void removeSource(int id) {
        if (sources.containsKey(id))
            sources.remove(id).close();
    }

    public StreamingAudioSource getSource(int id) {
        return sources.get(id);
    }

    public void stop() {
        shouldStop = true;
    }
}
