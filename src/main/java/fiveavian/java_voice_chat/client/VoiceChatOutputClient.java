package fiveavian.java_voice_chat.client;

import fiveavian.java_voice_chat.VoiceChat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class VoiceChatOutputClient implements Runnable {
    private final DatagramSocket socket;
    private final AudioOutputDevice outputDevice;
    private final SocketAddress serverAddress;
    private boolean shouldStop = true;

    private final byte[] payload = new byte[VoiceChat.PAYLOAD_SIZE];
    private final ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
    private final DatagramPacket packet = new DatagramPacket(payload, payload.length);

    private final short[] samples = new short[VoiceChat.BUFFER_LENGTH];

    public VoiceChatOutputClient(DatagramSocket socket, AudioOutputDevice outputDevice, SocketAddress serverAddress) {
        this.socket = socket;
        this.outputDevice = outputDevice;
        this.serverAddress = serverAddress;
    }

    @Override
    public void run() {
        shouldStop = false;
        while (!shouldStop && !socket.isClosed()) {
            try {
                processOutput();
            } catch (Exception ex) {
                System.err.println("Client: Caught an exception: " + ex.getMessage());
            }
        }
    }

    private void processOutput() throws Exception {
        packet.setLength(payload.length);
        socket.receive(packet);
        boolean isCorrectSize = packet.getLength() == VoiceChat.PAYLOAD_SIZE;
        boolean hasMagicNumber = payloadBuffer.getInt(0) == VoiceChat.MAGIC_NUMBER;
        if (!isCorrectSize || !hasMagicNumber)
            return;
        System.out.println("Client: Received packet from " + serverAddress);
        for (int i = 0; i < VoiceChat.BUFFER_LENGTH; i++)
            samples[i] = payloadBuffer.getShort(8 + 2 * i);
        outputDevice.queueSamples(samples);
    }

    public void stop() {
        shouldStop = true;
    }
}
