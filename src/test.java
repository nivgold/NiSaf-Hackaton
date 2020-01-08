import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class test {
    public static void main(String[] args) {
        Server server = new Server();
        new Thread(server).start();

        Client client = new Client();
        client.start();
    }

    private static String hash(String string){
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
