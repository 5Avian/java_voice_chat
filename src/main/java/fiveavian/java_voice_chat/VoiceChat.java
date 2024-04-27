package fiveavian.java_voice_chat;

import fiveavian.java_voice_chat.client.AudioInputDevice;
import fiveavian.java_voice_chat.client.VoiceChatInputClient;
import fiveavian.java_voice_chat.client.VoiceChatOutputClient;
import fiveavian.java_voice_chat.server.VoiceChatRelayServer;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.lwjgl.openal.AL11.AL_POSITION;
import static org.lwjgl.openal.AL11.alSource3f;
import static org.lwjgl.openal.ALC11.*;

public class VoiceChat {
    public static final int PORT = 24454;
    public static final int MAGIC_NUMBER = 0x5ABEDADC;
    public static final int SAMPLE_RATE = 16384;
    public static final int SAMPLE_SIZE = 2;
    public static final int PACKET_RATE = 32;
    public static final int BUFFER_LENGTH = SAMPLE_RATE / PACKET_RATE;
    public static final int BUFFER_SIZE = BUFFER_LENGTH * SAMPLE_SIZE;
    public static final int HEADER_SIZE = 8;
    public static final int PAYLOAD_SIZE = HEADER_SIZE + BUFFER_SIZE;

    public static void main(String[] args) throws Exception {
        long device = alcOpenDevice((String) null);
        long context = alcCreateContext(device, (int[]) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.getCapabilities());

        SocketAddress serverAddress = new InetSocketAddress("localhost", PORT);
        try (
                DatagramSocket serverSocket = new DatagramSocket(serverAddress);
                DatagramSocket clientSocket = new DatagramSocket();
                AudioInputDevice inputDevice = new AudioInputDevice(null);
        ) {
            VoiceChatRelayServer relayServer = new VoiceChatRelayServer(serverSocket);
            Thread relayServerThread = new Thread(relayServer);
            VoiceChatInputClient inputClient = new VoiceChatInputClient(clientSocket, inputDevice, serverAddress, 0);
            Thread inputClientThread = new Thread(inputClient);
            VoiceChatOutputClient outputClient = new VoiceChatOutputClient(clientSocket);
            Thread outputClientThread = new Thread(outputClient);

            clientSocket.connect(serverAddress);
            relayServer.addConnection(0, clientSocket.getLocalSocketAddress());
            outputClient.addSource(0);

            relayServerThread.start();
            inputClientThread.start();
            outputClientThread.start();

            // Circle your voice around yourself
            int source = outputClient.getSource(0).source;
            while (true) {
                double time = System.currentTimeMillis() / 500d;
                alSource3f(source, AL_POSITION, (float) Math.cos(time), 0f, (float) Math.sin(time));
                Thread.sleep(50);
            }
        }
    }
}