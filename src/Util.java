import java.nio.charset.StandardCharsets;

public class Util {
    public static int MAX_BUFFER_SIZE = 586;
    public static final String BROADCAST_ADDRESS = "255.255.255.255";
    public static int convertStringToInt(String string){
        char[] charArray = string.toCharArray();
        int result = 0;
        for (char ch : charArray){
            if (ch >= 'a' && ch <= 'z'){
                result *=26;
                result += ch - 'a';
            }
        }
        return result;
    }

    public static String convertIntegerToString(int number, int length){
        StringBuilder stringBuilder = new StringBuilder(length);
        while (number > 0){
            int c = number % 26;
            stringBuilder.insert(0, (char)(c + 'a'));
            number/=26;
            length--;
        }
        while (length>0){
            stringBuilder.insert(0, 'a');
            length--;
        }
        return stringBuilder.toString();
    }

    public static byte[] makePacketData(char[] teamName, char type, char[] hash, int length, char[] start, char[] end){
        //StringBuilder stringBuilder = new StringBuilder();
        //stringBuilder.append(teamName);
        //stringBuilder.append(type);
        //stringBuilder.append(hash);
        //for (int i=hash.length; i<40;i++)
        //    stringBuilder.append(' ');
        //stringBuilder.append(length);
        //stringBuilder.append(start);
        //for (int i=start.length; i<length; i++)
        //    stringBuilder.append(' ');
        //stringBuilder.append(end);
        //for (int i=end.length; i<length; i++)
        //    stringBuilder.append(' ');
        //return stringBuilder.toString().getBytes(StandardCharsets.UTF_8);

        int payloadSize = 74+2*length;
        byte[] buffer = new byte[payloadSize];
        System.arraycopy(new String(teamName).getBytes(StandardCharsets.UTF_8), 0, buffer, 0, 32);
        buffer[32] = (byte)type;
        String hashString = new String(hash);
        while (hashString.length()<40)
            hashString = hashString+' ';
        System.arraycopy(hashString.getBytes(StandardCharsets.UTF_8), 0, buffer, 33, 40);
        buffer[73] = (byte)length;
        String startString = new String(start);
        while (startString.length()<length)
            startString = startString+' ';
        System.arraycopy(startString.getBytes(StandardCharsets.UTF_8), 0, buffer, 74, length);
        String endString = new String(end);
        while (endString.length()<length)
            endString = endString+' ';
        System.arraycopy(endString.getBytes(StandardCharsets.UTF_8), 0, buffer, 74+length, length);
        return buffer;
    }

    public static Integer isInteger(String input){
        try{
            return Integer.parseInt(input);
        }catch (Exception e){
            return null;
        }
    }
}
