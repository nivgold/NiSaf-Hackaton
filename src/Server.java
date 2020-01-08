import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    private final int PORT = 3117;
    private InetAddress serverIP;
    private DatagramSocket serverUDPSocket;
    private boolean stop;
    public Server() {
        try {
            serverIP = InetAddress.getLocalHost();
            serverUDPSocket = new DatagramSocket(PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        DatagramPacket datagramPacket = null;
        System.out.println("Server Is Running");
        while (!stop){
            byte[] buffer = new byte[Util.MAX_BUFFER_SIZE];
            datagramPacket = new DatagramPacket(buffer, Util.MAX_BUFFER_SIZE);
            try {
                serverUDPSocket.receive(datagramPacket);
                System.out.println("Server Received Packet");
                DatagramPacket clientPacket = datagramPacket;
                pool.execute(() -> serverStrategy(clientPacket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void serverStrategy(DatagramPacket clientPacket){
        int length = (clientPacket.getData()[73]) & 0xFF;
        byte[] clientPacketData = clientPacket.getData();
        int payloadSize = 74+length*2;
        char teamNameChars[] = new char[32];
        for (int i=0; i<32; i++)
            teamNameChars[i] = (char)clientPacketData[i];

        char type = (char)clientPacketData[32];
        switch (type){
            case '1':
                // broadcast
                try{
                    byte[] toClientData = Util.makePacketData(teamNameChars, "2".toCharArray()[0], "".toCharArray(), length, "".toCharArray(), "".toCharArray());
                    DatagramPacket toClientPacket = new DatagramPacket(toClientData, payloadSize, clientPacket.getAddress(), clientPacket.getPort());
                    serverUDPSocket.send(toClientPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case '3':
                // Request
                System.out.println("Server Received Work");
                char[] startChars = new char[length];
                char[] endChars = new char[length];
                for (int i=74 ;i<74+length; i++)
                    startChars[i-74] = (char)clientPacketData[i];
                for (int i=74+length; i<74+length*2; i++)
                    endChars[i-74-length] = (char)clientPacketData[i];

                char[] hash = new char[40];
                for (int i=33; i<73; i++)
                    hash[i-33] = (char)clientPacketData[i];
                String startPos = new String(startChars);
                String endPos = new String(endChars);
                String originalHash = new String(hash);
                String result = jailbreak(startPos, endPos, originalHash, length);
                if (result == null){
                    // send NACK Packet
                    byte[] toClientData = Util.makePacketData(teamNameChars, "5".toCharArray()[0],hash, length, startChars, endChars);
                    DatagramPacket toClientPacket = new DatagramPacket(toClientData, payloadSize, clientPacket.getAddress(), clientPacket.getPort());
                    try {
                        serverUDPSocket.send(toClientPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    // send ACK Packet
                    byte[] toClientData = Util.makePacketData(teamNameChars, "4".toCharArray()[0], hash, length, result.toCharArray(), endChars);
                    DatagramPacket toClientPacket = new DatagramPacket(toClientData, payloadSize, clientPacket.getAddress(), clientPacket.getPort());
                    try {
                        serverUDPSocket.send(toClientPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                System.out.println("Invalid Type");
                break;
        }
    }

    public void stop(){
        this.stop=true;
    }

    private String jailbreak(String startPos, String endPos, String originalHash, int length){
        int start = Util.convertStringToInt(startPos);
        int end = Util.convertStringToInt(endPos);
        for (int i=start; i<=end; i++) {
            String currentString = Util.convertIntegerToString(i, length);
            String hashValue = hash(currentString);
            if (hashValue.equals(originalHash)){
                return currentString;
            }
        }
        return null;
    }

    private String hash(String string){
        try{
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = sha1.digest(string.getBytes(StandardCharsets.UTF_8));
            BigInteger value = new BigInteger(1, messageDigest);
            StringBuilder stringBuilder = new StringBuilder(value.toString(16));
            while (stringBuilder.length()<32){
                stringBuilder.insert(0, '0');
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
