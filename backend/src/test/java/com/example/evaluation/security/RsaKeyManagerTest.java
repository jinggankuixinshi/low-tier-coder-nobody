package com.example.evaluation.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyManagerTest {

    @Test
    @DisplayName("init generates valid key pair (not null)")
    void testInit_GeneratesValidKeyPair() {
        RsaKeyManager keyManager = new RsaKeyManager();
        String pem = keyManager.getPublicKeyPem();
        assertNotNull(pem);
        assertFalse(pem.isEmpty());
    }

    @Test
    @DisplayName("getPublicKeyPem returns PEM string starting with BEGIN PUBLIC KEY")
    void testGetPublicKeyPem_ReturnsProperPemFormat() {
        RsaKeyManager keyManager = new RsaKeyManager();
        String pem = keyManager.getPublicKeyPem();

        assertNotNull(pem);
        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertTrue(pem.contains("-----END PUBLIC KEY-----"));
    }

    @Test
    @DisplayName("decrypt can decrypt what was encrypted with the public key (round-trip)")
    void testDecrypt_RoundTrip() throws Exception {
        RsaKeyManager keyManager = new RsaKeyManager();
        String pem = keyManager.getPublicKeyPem();

        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        String original = "Hello RSA Test 123!";
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(original.getBytes(StandardCharsets.UTF_8));
        String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);

        String decrypted = keyManager.decrypt(encryptedBase64);
        assertEquals(original, decrypted);
    }

    @Test
    @DisplayName("multiple calls to getPublicKeyPem return same key")
    void testGetPublicKeyPem_MultipleCallsReturnSameKey() {
        RsaKeyManager keyManager = new RsaKeyManager();
        String pem1 = keyManager.getPublicKeyPem();
        String pem2 = keyManager.getPublicKeyPem();
        String pem3 = keyManager.getPublicKeyPem();

        assertEquals(pem1, pem2);
        assertEquals(pem1, pem3);
    }

    @Test
    @DisplayName("each new instance produces unique keys")
    void testNewInstance_ProducesUniqueKeys() {
        RsaKeyManager km1 = new RsaKeyManager();
        RsaKeyManager km2 = new RsaKeyManager();

        String pem1 = km1.getPublicKeyPem();
        String pem2 = km2.getPublicKeyPem();

        assertNotEquals(pem1, pem2);
    }
}
