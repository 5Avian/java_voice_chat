package fiveavian.java_voice_chat.server;

import fiveavian.java_voice_chat.VoiceChat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class VoiceChatRelayServer implements Runnable {
    private final DatagramSocket socket;
    private boolean shouldStop = true;

    private final byte[] payload = new byte[VoiceChat.PAYLOAD_SIZE];
    private final ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
    private final DatagramPacket packet = new DatagramPacket(payload, payload.length);

    private final Map<Integer, SocketAddress> connections = Collections.synchronizedMap(new HashMap<>());

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
                System.err.println("Server: Caught an exception: " + ex.getMessage());
            }
        }
    }

    private void processPackets() throws Exception {
        packet.setLength(payload.length);
        socket.receive(packet);
        boolean isCorrectSize = packet.getLength() == VoiceChat.PAYLOAD_SIZE;
        boolean hasMagicNumber = payloadBuffer.getInt(0) == VoiceChat.MAGIC_NUMBER;
        if (!isCorrectSize || !hasMagicNumber) {
            System.out.println("Server: Received invalid packet, ignoring");
            return;
        }
        int clientId = payloadBuffer.getInt(4);
        SocketAddress clientAddress = packet.getSocketAddress();
        if (!connections.get(clientId).equals(clientAddress)) {
            System.out.println("Server: Received packet from unknown client, ignoring");
            return;
        }
        System.out.println("Server: Received packet from " + clientAddress);
        for (SocketAddress address : connections.values()) {
            System.out.println("Server: Sending packet to " + address);
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
