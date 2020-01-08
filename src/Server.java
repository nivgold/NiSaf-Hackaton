import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
        while (!stop){
            byte[] buffer = new byte[Util.BUFFER_SIZE];
            datagramPacket = new DatagramPacket(buffer, Util.BUFFER_SIZE);
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
        byte[] data = clientPacket.getData();
        byte[] teamNameBytes = new byte[32];
        char[] teamName = null;
        byte[] typeBytes = new byte[1];
        char[] type = null;
        byte[] hashBytes = new byte[40];
        char[] hash = null;
        byte[] lengthBytes = new byte[1];
        char[] length = null;
        byte[] startBytes = new byte[256];
        char[] start = null;
        byte[] endBytes = new byte[256];
        char[] end = null;
        try{
            for (int i=0; i<586; i++) {
                if (i<32)
                    teamNameBytes[i] = data[i];
                if (i==32)
                    typeBytes[0] = data[i];
                if (i>=33 && i<73)
                    hashBytes[i-33] = data[i];
                if (i==73)
                    lengthBytes[0] = data[i];
                if (i>=74 && i<330)
                    startBytes[i-74] = data[i];
                if (i>=330)
                    endBytes[i-330] = data[i];
            }
            teamName = new String(teamNameBytes, "UTF-8").toCharArray();
            type = new String(typeBytes, "UTF-8").toCharArray();
            hash = new String(hashBytes, "UTF-8").toCharArray();
            length = new String(lengthBytes, "UTF-8").toCharArray();
            start = new String(startBytes, "UTF-8").toCharArray();
            end = new String(endBytes, "UTF-8").toCharArray();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        switch (type[0]){
            case '1':
                // broadcast
                try{
                    byte[] toClientData = Util.makePacketData(teamName, "2".toCharArray()[0], hash, length[0], start, end);
                    DatagramPacket toClientPacket = new DatagramPacket(toClientData, Util.BUFFER_SIZE, clientPacket.getAddress(), clientPacket.getPort());
                    serverUDPSocket.send(toClientPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case '3':
                // Request
                System.out.println("Server Received Work");
                String startPos = new String(start);
                String endPos = new String(end);
                String originalHash = new String(hash);
                String result = jailbreak(startPos, endPos, originalHash, Integer.parseInt(length[0]+""));
                if (result == null){
                    // send NACK Packet
                    byte[] toClientData = Util.makePacketData(teamName, "5".toCharArray()[0],hash, length[0], start, end);
                    DatagramPacket toClientPacket = new DatagramPacket(toClientData, Util.BUFFER_SIZE, clientPacket.getAddress(), clientPacket.getPort());
                    try {
                        serverUDPSocket.send(toClientPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    // send ACK Packet
                    char[] resultChars = new char[256];
                    System.arraycopy(result.toCharArray(), 0, resultChars, 0, Integer.parseInt(length[0]+""));
                    byte[] toClientData = Util.makePacketData(teamName, "4".toCharArray()[0], hash, length[0], resultChars, end);
                    DatagramPacket toClientPacket = new DatagramPacket(toClientData, Util.BUFFER_SIZE, clientPacket.getAddress(), clientPacket.getPort());
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
