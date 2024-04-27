package fiveavian.java_voice_chat.client;

import fiveavian.java_voice_chat.VoiceChat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class VoiceChatInputClient implements Runnable {
    private final DatagramSocket socket;
    private final AudioInputDevice inputDevice;
    private final SocketAddress serverAddress;
    private final int id;
    private boolean shouldStop = true;

    private final byte[] payload = new byte[VoiceChat.PAYLOAD_SIZE];
    private final ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
    private final DatagramPacket packet = new DatagramPacket(payload, payload.length);

    public VoiceChatInputClient(DatagramSocket socket, AudioInputDevice inputDevice, SocketAddress serverAddress, int id) {
        this.socket = socket;
        this.inputDevice = inputDevice;
        this.serverAddress = serverAddress;
        this.id = id;
    }

    @Override
    public void run() {
        shouldStop = false;
        while (!shouldStop && !socket.isClosed()) {
            try {
                processInput();
            } catch (Exception ex) {
                System.err.println("Client: Caught an exception: " + ex.getMessage());
            }
        }
    }

    private void processInput() throws Exception {
        short[] samples = inputDevice.pollSamples();
        if (samples == null)
            return;
        System.out.println("Client: Sending packet to " + serverAddress);
        payloadBuffer.putInt(0, VoiceChat.MAGIC_NUMBER);
        payloadBuffer.putInt(4, id);
        for (int i = 0; i < VoiceChat.BUFFER_LENGTH; i++)
            payloadBuffer.putShort(8 + 2 * i, samples[i]);
        packet.setSocketAddress(serverAddress);
        socket.send(packet);
    }

    public void stop() {
        shouldStop = true;
    }
}
