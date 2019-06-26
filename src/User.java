//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
//用户信息类
public class User{
    private String name;
    private String ip;

    public User(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public User(){

    }
    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    //~ --- [INSTANCE FIELDS] ------------------------------------------------------------------------------------------

    private DHPrivateKey privateKey;
    public DHPublicKey  publicKey;
    public byte[] publickey;
    public DHPublicKey  receivedPublicKey;
    public byte[]     secretKey;
    private String     secretMessage;
    private String     encrypedMessage;
    private String iv  = "aabbccddeeffgghh";


    //~ --- [METHODS] --------------------------------------------------------------------------------------------------

    public String encryptAndSendMessage(final String message) {

        try {
            byte[] newsecretKey = shortenSecretKey(secretKey);
            //BASE64Encoder base64encoder = new BASE64Encoder();
            Encoder encoder = Base64.getEncoder();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(newsecretKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec,new IvParameterSpec(iv.getBytes()));
            byte[] encryptedMessage =  cipher.doFinal(message.getBytes());
            secretMessage = encoder.encodeToString(encryptedMessage);
            //secretMessage = base64encoder.encode(encryptedMessage);
            return secretMessage;
            //user.receiveAndDecryptMessage(encryptedMessage);
            //encrypedMessage=new String( cipher.doFinal(message.getBytes()));
            //System.out.println("   " + encrypedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    //~ ----------------------------------------------------------------------------------------------------------------

    public void generateCommonSecretKey() {

        try {
            final KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(receivedPublicKey, true);

            //secretKey = shortenSecretKey(keyAgreement.generateSecret());
            secretKey = keyAgreement.generateSecret();
            //BASE64Encoder base64encoder = new BASE64Encoder();
            //Encoder encoder = Base64.getEncoder();
            //String encode = base64encoder.encode(secretKey);
            //encode = encoder.encodeToString(secretKey);
            //System.out.println("secretKey:" + secretKey);
            //return encode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //return null;
    }

    public DHPublicKey byteToKey(byte[] key){
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(key);

            //PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(key);
            DHPublicKey publicKey1 = (DHPublicKey)keyFactory.generatePublic(x509EncodedKeySpec);
            return publicKey1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //~ ----------------------------------------------------------------------------------------------------------------

    public void generateKeys() {
            try{
                final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
                keyPairGenerator.initialize(512);

                final KeyPair keyPair = keyPairGenerator.generateKeyPair();

                privateKey = (DHPrivateKey)keyPair.getPrivate();
                publicKey  = (DHPublicKey)keyPair.getPublic();
            }catch (Exception e){
                e.printStackTrace();
            }


    }



    //~ ----------------------------------------------------------------------------------------------------------------

    public PublicKey getPublicKey() {

        //System.out.println("   " + publicKey);
        return publicKey;
    }



    //~ ----------------------------------------------------------------------------------------------------------------

    public String receiveAndDecryptMessage(final String message) {

        try {
            Decoder decoder = Base64.getDecoder();
            //BASE64Decoder base64decoder = new BASE64Decoder();
            byte[] newsecretKey = shortenSecretKey(secretKey);
            // You can use Blowfish or another symmetric algorithm but you must adjust the key size.
            //final SecretKeySpec keySpec = new SecretKeySpec(newsecretKey, "DES");
            //final Cipher        cipher  = Cipher.getInstance("DES/ECB/PKCS5Padding");

            //cipher.init(Cipher.DECRYPT_MODE, keySpec);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(newsecretKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec,new IvParameterSpec(iv.getBytes()));
            //byte[] encryptedMessage =  cipher.doFinal(base64decoder.decodeBuffer(message));
            byte[] encryptedMessage =  cipher.doFinal(decoder.decode(message));
            //secretMessage = new String(cipher.doFinal(message));
            secretMessage = new String(encryptedMessage);

            //secretMessage = base64encoder.encode(encryptedMessage);
            return secretMessage;
            // System.out.println("密文：" + secretMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    //~ ---------------------------------------------------------------------------------------------------------------



    //~ ----------------------------------------------------------------------------------------------------------------

    public void whisperTheSecretMessage() {

        System.out.println(secretMessage);
    }



    //~ ----------------------------------------------------------------------------------------------------------------

    /**
     * 1024 bit symmetric key size is so big for DES so we must shorten the key size. You can get first 8 longKey of the
     * byte array or can use a key factory
     *
     * @param   longKey
     *
     * @return
     */
    private byte[] shortenSecretKey(final byte[] longKey) {

        try {

            // Use 8 bytes (64 bits) for DES, 6 bytes (48 bits) for Blowfish
            //final byte[] shortenedKey = new byte[8];
            final byte[] shortenedKey = new byte[32];
            System.arraycopy(longKey, 0, shortenedKey, 0, shortenedKey.length);

            return shortenedKey;

            // Below lines can be more secure
            // final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            // final DESKeySpec       desSpec    = new DESKeySpec(longKey);
            //
            // return keyFactory.generateSecret(desSpec).getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String byteArrayToStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        //String str = new String(a);
        //return str;
        int len = byteArray.length;
        int ints[] = new int[len];
        for(int i=0;i<len;i++) {
            ints[i] = byteArray[i] & 0xff;
            ints[i] = ints[i]%127;
            //System.out.print(" "+ints[i]+" ");
        }
        //System.out.println();

        byte byteA[] = new byte[byteArray.length];
        for(int i=0;i<len;i++) {
            byteA[i] = (byte)ints[i];
        }

        String str = new String(byteA);
        return str;
    }




    public static String stringToAscii(String value)
    {
        StringBuffer sbu = new StringBuffer();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if(i != chars.length - 1)
            {
                sbu.append((int)chars[i]).append(",");
            }
            else {
                sbu.append((int)chars[i]);
            }
        }
        return sbu.toString();
    }
}