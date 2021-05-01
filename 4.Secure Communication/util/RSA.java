package util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Base64;


public class RSA
{
    //Using RSA algorithm
    public final static String ALGORITHM = "RSA";



    public static KeyPair genKeyPair(int keySize)
    {
        KeyPair keyPair = null;


        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(keySize);
            keyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }



        return keyPair;
    }


    public static byte[] encrypt(PublicKey pKey, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        byte[] encryptedData;

        final Cipher cipher = Cipher.getInstance(ALGORITHM);

        cipher.init(Cipher.ENCRYPT_MODE, pKey);
        encryptedData = cipher.doFinal(data);

        return encryptedData;
    }


    public static byte[] decrypt(PrivateKey privateKey, byte[] encryptedData) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
        byte[] decryptedData;
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        decryptedData = cipher.doFinal(encryptedData);

        return decryptedData;
    }


    public static String getPublicKeyAsBase64Encoded(PublicKey publicKey)
    {
        return Base64.getEncoder().encodeToString(getPublicKeyBytes(publicKey));
    }


    private static byte[] getPublicKeyBytes(PublicKey publicKey)
    {
        return publicKey.getEncoded();
    }


    public static String getAlgorithm()
    {
        return ALGORITHM;
    }


}