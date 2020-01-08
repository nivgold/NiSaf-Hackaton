import java.nio.charset.StandardCharsets;

public class Util {
    public static final int BUFFER_SIZE = 586;
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

    public static byte[] makePacketData(char[] teamName, char type, char[] hash, char length, char[] start, char[] end){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(teamName);
        while (stringBuilder.toString().length() < 32)
            stringBuilder.append(' ');
        stringBuilder.append(type);
        stringBuilder.append(hash);
        stringBuilder.append(length);
        stringBuilder.append(start);
        stringBuilder.append(end);
        return stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
