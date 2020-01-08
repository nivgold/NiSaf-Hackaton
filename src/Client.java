import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Client {
    private DatagramSocket clientSocket;
    private int defaultTimeOut;

    public Client() {
        try {
            this.clientSocket = new DatagramSocket(5000);
            defaultTimeOut = clientSocket.getSoTimeout();
            clientSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            ArrayList<InetAddress> addresses = new ArrayList<>();
            byte[] buffer = new byte[586];
            String teamName = "NiSaf                           ";
            System.arraycopy(teamName.getBytes(StandardCharsets.UTF_8), 0, buffer, 0, 32);
            String type = "1";
            buffer[32] = type.getBytes(StandardCharsets.UTF_8)[0];
            String hash = "68bb04bd54b8f6c530695e0b77de298276a0511d";
            System.arraycopy(hash.getBytes(StandardCharsets.UTF_8), 0, buffer, 33, 40);
            String length = "3";
            buffer[73] = length.getBytes(StandardCharsets.UTF_8)[0];
            String start = "";
            byte[] startBytes = new byte[256];
            //System.arraycopy(start.getBytes(StandardCharsets.UTF_8), 0, startBytes, 0, 3);
            System.arraycopy(startBytes, 0, buffer, 74, 256);
            String end = "";
            byte[] endBytes = new byte[256];
            //System.arraycopy(end.getBytes(StandardCharsets.UTF_8), 0, endBytes, 0, 3);
            System.arraycopy(endBytes, 0, buffer, 330, 256);

            InetAddress address = InetAddress.getByName("255.255.255.255");
            clientSocket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(buffer, 586, address, 3117);
            clientSocket.send(packet);
            clientSocket.setBroadcast(false);

            Thread.sleep(1000);

            long startOFFERTime = System.currentTimeMillis();
            long endOFFERTime = startOFFERTime + 1000;
            while (System.currentTimeMillis() <= endOFFERTime){
                byte[] data = new byte[Util.BUFFER_SIZE];
                DatagramPacket rcv = new DatagramPacket(data, Util.BUFFER_SIZE);
                try {
                    clientSocket.receive(rcv);
                }catch (SocketTimeoutException e){

                }

                if (rcv.getData() != null) {
                    char serverType = (char) rcv.getData()[32];
                    if (serverType == '2')
                        addresses.add(rcv.getAddress());
                }
            }

            System.out.println("Sending Work To Servers");
            divideWork(addresses, teamName.toCharArray(), "3".toCharArray()[0], hash.toCharArray(), length.toCharArray()[0]);

            System.out.println("waiting for servers");
            byte[] data = new byte[Util.BUFFER_SIZE];
            DatagramPacket rcv = new DatagramPacket(data, Util.BUFFER_SIZE);
            try {
                clientSocket.setSoTimeout(defaultTimeOut);
                clientSocket.receive(rcv);
                System.out.println("Received From Server");
                byte[] serverData = rcv.getData();
                byte serverType = serverData[32];
                if ((char)serverType == '4'){
                    // Received ACK
                    byte[] result = new byte[256];
                    for (int i=74; i< 330 ; i++)
                        result[i-74] = serverData[i];
                    String resultString = new String(result, StandardCharsets.UTF_8);
                    System.out.println("Cracked String:"+resultString);
                }
            }catch (SocketTimeoutException e){
                System.out.println("No Answer From Any Server");
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void divideWork(ArrayList<InetAddress> addresses, char[] teamName, char type, char[] hash, char length) throws IOException {
        int serverAmount = addresses.size();
        StringBuilder stringBuilder = new StringBuilder();
        int lengthInteger = Integer.parseInt(length+"");
        for (int i=0; i<lengthInteger; i++)
            stringBuilder.append('z');
        String maxString = stringBuilder.toString();
        int maxNumber = Util.convertStringToInt(maxString);
        int division = maxNumber/serverAmount;
        division = Math.max(1, division);
        String startPos;
        String endPos;
        for (int i=0; i<Math.min(serverAmount, maxNumber); i++){
            if (i==0) {
                startPos = Util.convertIntegerToString(i * division, lengthInteger);
                endPos = Util.convertIntegerToString((i + 1) * division, lengthInteger);
            }
            else if (i<serverAmount-1){
                startPos = Util.convertIntegerToString(i * division + 1, lengthInteger);
                endPos = Util.convertIntegerToString((i + 1) * division, lengthInteger);
            }
            else{
                startPos = Util.convertIntegerToString(i * division + 1, lengthInteger);
                endPos = maxString;
            }
            char[] startChars = new char[256];
            System.arraycopy(startPos.toCharArray(), 0, startChars, 0, startPos.toCharArray().length);
            char[] endChars = new char[256];
            System.arraycopy(endPos.toCharArray(), 0, endChars, 0, endPos.toCharArray().length);

            byte[] data = Util.makePacketData(teamName, type, hash, length, startChars, endChars);

            DatagramPacket toServer = new DatagramPacket(data, data.length, addresses.get(i), 3117);
            clientSocket.send(toServer);
            System.out.println("Sent Work Packet To Server");
        }
    }
}
