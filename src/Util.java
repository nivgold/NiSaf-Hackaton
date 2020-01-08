import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class Util {
    public static int MAX_BUFFER_SIZE = 586;
    public static final String BROADCAST_ADDRESS = "255.255.255.255";
    public static BigInteger convertStringToInt(String string){
        char[] charArray = string.toCharArray();
        BigInteger result = new BigInteger("0");
        for (char ch : charArray){
            if (ch >= 'a' && ch <= 'z'){
                result = result.multiply(new BigInteger("26"));
                int x = ch - 'a';
                result = result.add(new BigInteger(Integer.toString(x)));
            }
        }
        return result;
    }

    public static String convertIntegerToString(BigInteger number, int length){
        StringBuilder stringBuilder = new StringBuilder(length);
        while (number.compareTo(new BigInteger("0"))> 0){
            BigInteger c = number.mod(new BigInteger("26"));
            stringBuilder.insert(0, (char)(c.intValue() + 'a'));
            number = number.divide(new BigInteger("26"));
            length--;
        }
        while (length>0){
            stringBuilder.insert(0, 'a');
            length--;
        }
        return stringBuilder.toString();
    }

    public static byte[] makePacketData(char[] teamName, byte type, char[] hash, int length, char[] start, char[] end){
        int payloadSize = 74+2*length;
        byte[] buffer = new byte[payloadSize];
        System.arraycopy(new String(teamName).getBytes(StandardCharsets.UTF_8), 0, buffer, 0, 32);
        buffer[32] = type;
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

    public static boolean isValidHash(String hash){
        if (hash.length()!=40)
            return false;
        for(char ch : hash.toCharArray()){
            if (!(ch >= 'a' && ch <='f') && !(ch >= '0' && ch <='9'))
                return false;
        }
        return true;
    }
}
