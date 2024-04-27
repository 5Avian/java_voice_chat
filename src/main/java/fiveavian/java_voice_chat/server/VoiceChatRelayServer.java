package fiveavian.java_voice_chat.server;

import fiveavian.java_voice_chat.VoiceChat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class VoiceChatRelayServer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceChatRelayServer.class);

    private final DatagramSocket socket;
    private boolean shouldStop = true;

    private final byte[] payload = new byte[VoiceChat.PAYLOAD_SIZE];
    private final ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
    private final DatagramPacket packet = new DatagramPacket(payload, payload.length);

    private final Int2ObjectMap<SocketAddress> connections = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    public VoiceChatRelayServer(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        shouldStop = false;
        while (!shouldStop && !socket.isClosed()) {
            try {
                processPackets();
            } catch (Exception ex) {
                LOGGER.error("Caught an exception while running", ex);
            }
        }
    }

    private void processPackets() throws Exception {
        packet.setLength(payload.length);
        socket.receive(packet);
        boolean isCorrectSize = packet.getLength() == VoiceChat.PAYLOAD_SIZE;
        boolean hasMagicNumber = payloadBuffer.getInt(0) == VoiceChat.MAGIC_NUMBER;
        if (!isCorrectSize || !hasMagicNumber) {
            LOGGER.info("Received invalid packet, ignoring");
            return;
        }
        int clientId = payloadBuffer.getInt(4);
        SocketAddress clientAddress = packet.getSocketAddress();
        boolean isKnownClient = connections.containsKey(clientId) && connections.get(clientId).equals(clientAddress);
        if (!isKnownClient) {
            LOGGER.info("Received packet from unknown client, ignoring");
            return;
        }
        LOGGER.info("Received packet from {}", clientAddress);
        for (SocketAddress address : connections.values()) {
            LOGGER.info("Sending packet to {}", address);
            packet.setSocketAddress(address);
            socket.send(packet);
        }
    }

    public void addConnection(int id, SocketAddress address) {
        connections.put(id, address);
    }

    public void removeConnection(int id) {
        connections.remove(id);
    }

    public void stop() {
        shouldStop = true;
    }
}
