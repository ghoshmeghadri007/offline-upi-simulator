package com.meghadri.offlineupi.crypto;

import com.meghadri.offlineupi.mesh.model.PaymentInstruction;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class HybridCryptoService {

    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int RSA_KEY_BYTES = 256;

    private final ServerKeyHolder keyHolder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom random = new SecureRandom();

    public HybridCryptoService(ServerKeyHolder keyHolder) {
        this.keyHolder = keyHolder;
    }

    public static class DecryptionException extends RuntimeException {
        public DecryptionException(String msg, Throwable cause) { super(msg, cause); }
    }

    public String encrypt(PaymentInstruction instruction) {
        try {
            // Convert PaymentInstruction object into JSON bytes
            byte[] plaintext = objectMapper.writeValueAsBytes(instruction);

            // Generate a new 256-bit AES key for this transaction
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(AES_KEY_SIZE);
            SecretKey aesKey = kg.generateKey();

            // Generate a random 12-byte IV (nonce)
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);

            // Create AES-GCM cipher
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");

            // Initialize cipher in ENCRYPT mode using AES key and IV
            aesCipher.init(
                    Cipher.ENCRYPT_MODE,
                    aesKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );

            // Encrypt the plaintext
            byte[] aesCiphertext = aesCipher.doFinal(plaintext);

            // Create RSA cipher
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

            // Initialize RSA cipher using server's public key
            rsaCipher.init(Cipher.ENCRYPT_MODE, keyHolder.getPublicKey());

            // Encrypt (wrap) the AES key using RSA
            byte[] wrappedKey = rsaCipher.doFinal(aesKey.getEncoded());

            // Combine RSA-encrypted AES key + IV + AES ciphertext
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(wrappedKey);
            out.write(iv);
            out.write(aesCiphertext);

            // Convert binary data to Base64 string for transmission
            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public PaymentInstruction decrypt(String base64Ciphertext) {
        try {
            // Convert Base64 string back into binary data
            byte[] data = Base64.getDecoder().decode(base64Ciphertext);

            // Wrap data in ByteBuffer for easy reading
            ByteBuffer buffer = ByteBuffer.wrap(data);

            // Read the RSA-encrypted AES key (first 256 bytes)
            byte[] wrappedKey = new byte[RSA_KEY_BYTES];
            buffer.get(wrappedKey);

            // Read the 12-byte IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // Remaining bytes are the AES-encrypted transaction
            byte[] aesCiphertext = new byte[buffer.remaining()];
            buffer.get(aesCiphertext);

            // Create RSA cipher
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

            // Decrypt AES key using the server's private key
            rsaCipher.init(Cipher.DECRYPT_MODE, keyHolder.getPrivateKey());
            byte[] aesKeyBytes = rsaCipher.doFinal(wrappedKey);

            // Convert decrypted bytes back into an AES key
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // Create AES-GCM cipher
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");

            // Initialize cipher for decryption
            aesCipher.init(
                    Cipher.DECRYPT_MODE,
                    aesKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );

            // Decrypt the transaction
            byte[] plaintext = aesCipher.doFinal(aesCiphertext);

            // Convert JSON bytes back into PaymentInstruction object
            return objectMapper.readValue(plaintext, PaymentInstruction.class);

        } catch (Exception e) {
            // Happens if packet is corrupted or tampered with
            throw new DecryptionException(
                    "Decryption/verification failed — packet may be tampered",
                    e
            );
        }
    }

    public String hashCiphertext(String base64Ciphertext) {
        try {
            // Create SHA-256 hash generator
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Generate hash of the ciphertext
            byte[] hash = digest.digest(base64Ciphertext.getBytes());

            // Convert hash bytes into hexadecimal string
            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            // Return hexadecimal hash
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
