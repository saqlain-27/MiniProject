package com.securechat.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

public class SecurityUtil {
   private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 16;
    //here
    private static final byte[] STATIC_KEY_BYTES= "ThisIsASecretKey1234567890123456".getBytes();
//    static {
//        new SecureRandom().nextBytes(STATIC_KEY_BYTES);
//    }
    
    public static SecretKey getStaticKey()
    {
        return new SecretKeySpec(STATIC_KEY_BYTES,"AES");
    }
    
    //here
//    static {
//        Security.addProvider(new BouncyCastleProvider());
//    }
    
//    public static SecretKey generateKey() throws Exception {
//        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
//        keyGenerator.init(KEY_SIZE);
//        return keyGenerator.generateKey();
//    }
    
    public static String encrypt(String input, SecretKey key) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(input.getBytes("UTF-8"));
        
        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    public static String decrypt(String input, SecretKey key) throws Exception {
        byte[] combined = Base64.getDecoder().decode(input);
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] encrypted = new byte[combined.length - IV_SIZE];
        System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, "UTF-8");
    }
    
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    public static String hashPassword(String password, String salt) {
        try {
            String combined = salt + password;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    } 
}
