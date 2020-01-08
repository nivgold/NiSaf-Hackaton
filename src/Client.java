import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    private DatagramSocket clientSocket;
    private int defaultTimeOut;
    private final String TEAM_NAME = "Doofenshmirtz Evil Inc.         ";

    public Client() {
        try {
            this.clientSocket = new DatagramSocket(5000);
            defaultTimeOut = clientSocket.getSoTimeout();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        Scanner scanner = new Scanner(System.in);
        while (true){
            try {
                clientSocket.setSoTimeout(750);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            System.out.print("Welcome to "+TEAM_NAME+". Please enter the hash:");
            String hash = scanner.nextLine();
            System.out.println("Please enter the input string length:");
            String lengthString = scanner.nextLine();
            Integer length;
            while ((length = Util.isInteger(lengthString))==null){
                System.out.println("Invalid length. enter the input string length:");
                lengthString = scanner.nextLine();
            }
            int payloadSize = 74+length*2;
            byte[] discoverPacketPayload = new byte[payloadSize];
            System.arraycopy(TEAM_NAME.getBytes(StandardCharsets.UTF_8), 0, discoverPacketPayload, 0, 32);
            String type = "1";
            discoverPacketPayload[32] = type.getBytes(StandardCharsets.UTF_8)[0];
            //TODO check that hash is indeed 40 chars
            System.arraycopy(hash.getBytes(StandardCharsets.UTF_8), 0, discoverPacketPayload, 33, 40);
            discoverPacketPayload[73] = length.byteValue();
            // start and end data
            System.arraycopy(new byte[length*2], 0, discoverPacketPayload, 74, length*2);

            ArrayList<InetAddress> serversAddresses = new ArrayList<>();

            // sending DISCOVER Packet
            try {
                InetAddress broadcastAddress = InetAddress.getByAddress(new byte[] {(byte)255, (byte)255, (byte)255, (byte)255});
                clientSocket.setBroadcast(true);
                DatagramPacket discoverPacket = new DatagramPacket(discoverPacketPayload, discoverPacketPayload.length, broadcastAddress, 3117);
                clientSocket.send(discoverPacket);
                clientSocket.setBroadcast(false);

                //DatagramPacket discoverPacket = new DatagramPacket(discoverPacketPayload, discoverPacketPayload.length, InetAddress.getByName("192.168.43.38"), 3117);
                //clientSocket.send(discoverPacket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            //catch (SocketException e) {
            //    e.printStackTrace();
            //}
            catch (IOException e) {
                e.printStackTrace();
            }

            // Receiving OFFER Packets
            long startOFFERTime = System.currentTimeMillis();
            long endOFFERTime = startOFFERTime + 1000;
            while (System.currentTimeMillis() <= endOFFERTime){
                byte[] offerPacketPayload = new byte[payloadSize];
                DatagramPacket offerPacket = new DatagramPacket(offerPacketPayload, payloadSize);
                try {
                    clientSocket.receive(offerPacket);
                }catch (SocketTimeoutException e){
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (offerPacket.getData() != null) {
                    char serverType = (char) offerPacket.getData()[32];
                    if (serverType == '2')
                        serversAddresses.add(offerPacket.getAddress());
                }
            }

            if (serversAddresses.size()==0){
                System.out.println("No Servers Found");
                continue;
            }

            // Sending REQUEST Packets
            System.out.println("Sending Work To Servers");
            try {
                divideWork(serversAddresses, TEAM_NAME.toCharArray(), "3".toCharArray()[0], hash.toCharArray(), length);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Receiving ACK/NACK Packets
            System.out.println("Waiting For Servers ACK/NACK Respond");
            long startACKTime = System.currentTimeMillis();
            // 15 seconds timeout
            long endACKTime = startACKTime + 15000;
            while (System.currentTimeMillis() <= endACKTime){
                byte[] resultPacketPayload = new byte[payloadSize];
                DatagramPacket resultPacket = new DatagramPacket(resultPacketPayload, payloadSize);
                try {
                    clientSocket.setSoTimeout(15000);
                    clientSocket.receive(resultPacket);
                    System.out.println("Received From Server");
                    byte[] resultServerData = resultPacket.getData();
                    byte serverType = resultServerData[32];
                    if ((char)serverType == '4'){
                        // Received ACK
                        byte[] result = new byte[length];
                        for (int i=74; i< 74+length ; i++)
                            result[i-74] = resultServerData[i];
                        String resultString = new String(result, StandardCharsets.UTF_8);
                        System.out.println("Cracked String:"+resultString);
                        break;
                    }
                    else if ((char)serverType == '5'){
                        // Received NACK
                        System.out.println("Client Received NACK From Server:"+resultPacket.getAddress().getHostAddress());
                    }
                }catch (SocketTimeoutException e){
                    System.out.println("No Answer From Any Server");
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void divideWork(ArrayList<InetAddress> addresses, char[] teamName, char type, char[] hash, int length) throws IOException {
        int serverAmount = addresses.size();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<length; i++)
            stringBuilder.append('z');
        String maxString = stringBuilder.toString();
        int maxNumber = Util.convertStringToInt(maxString);
        int division = maxNumber/serverAmount;
        division = Math.max(1, division);
        String startPos;
        String endPos;
        for (int i=0; i<Math.min(serverAmount, maxNumber); i++){
            if (i==0) {
                startPos = Util.convertIntegerToString(i * division, length);
                endPos = Util.convertIntegerToString((i + 1) * division, length);
            }
            else if (i<serverAmount-1){
                startPos = Util.convertIntegerToString(i * division + 1, length);
                endPos = Util.convertIntegerToString((i + 1) * division, length);
            }
            else{
                startPos = Util.convertIntegerToString(i * division + 1, length);
                endPos = maxString;
            }

            byte[] requestPacketPayload = Util.makePacketData(teamName, type, hash, length, startPos.toCharArray(), endPos.toCharArray());

            DatagramPacket toServer = new DatagramPacket(requestPacketPayload, requestPacketPayload.length, addresses.get(i), 3117);
            clientSocket.send(toServer);
            System.out.println("Sent Work Packet To Server:"+addresses.get(i).getHostAddress());
        }
    }
}
