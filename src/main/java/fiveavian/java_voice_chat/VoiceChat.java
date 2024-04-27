package fiveavian.java_voice_chat;

import fiveavian.java_voice_chat.client.AudioInputDevice;
import fiveavian.java_voice_chat.client.AudioOutputDevice;
import fiveavian.java_voice_chat.client.VoiceChatInputClient;
import fiveavian.java_voice_chat.client.VoiceChatOutputClient;
import fiveavian.java_voice_chat.server.VoiceChatRelayServer;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

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
                AudioOutputDevice outputDevice = new AudioOutputDevice();
        ) {
            VoiceChatRelayServer relayServer = new VoiceChatRelayServer(serverSocket);
            Thread relayServerThread = new Thread(relayServer);
            VoiceChatInputClient inputClient = new VoiceChatInputClient(clientSocket, inputDevice, serverAddress, 0);
            Thread inputClientThread = new Thread(inputClient);
            VoiceChatOutputClient outputClient = new VoiceChatOutputClient(clientSocket, outputDevice, serverAddress);
            Thread outputClientThread = new Thread(outputClient);

            clientSocket.connect(serverAddress);
            relayServer.addConnection(0, clientSocket.getLocalSocketAddress());

            relayServerThread.start();
            inputClientThread.start();
            outputClientThread.start();
            relayServerThread.join();
            inputClientThread.join();
            outputClientThread.join();
        }
    }
}