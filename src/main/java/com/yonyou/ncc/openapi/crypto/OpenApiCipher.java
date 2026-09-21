package com.yonyou.ncc.openapi.crypto;

import com.yonyou.ncc.openapi.model.OpenApiConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 与 nccloud.open.api.auto.token.cur.utils 中的 Encryption / KeysFactory / CompressUtil 行为一致。
 */
public final class OpenApiCipher {

    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/CTR/NoPadding";
    private static final int RSA_MAX_BLOCK = 117;
    private static final int IV_LENGTH = 16;

    private OpenApiCipher() {
    }

    /** 公钥直接加密，RSA/ECB/OAEPWithSHA-256AndMGF1Padding，超长内容按 117 字节分块。 */
    public static String rsaEncrypt(String publicKeyBase64, String data) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decodePublicKey(publicKeyBase64)));
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1",
                new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, spec);

        byte[] source = data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int offset = 0; offset < source.length; offset += RSA_MAX_BLOCK) {
            int length = Math.min(RSA_MAX_BLOCK, source.length - offset);
            out.write(cipher.doFinal(source, offset, length));
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public static String aesEncrypt(String securityKey, String plain) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, symKey(securityKey), iv(securityKey));
        byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String aesDecrypt(String securityKey, String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, symKey(securityKey), iv(securityKey));
        byte[] decrypted = cipher.doFinal(decodeBase64(cipherText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static String gzipCompress(String plain) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(plain.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public static String gzipDecompress(String cipherText) throws Exception {
        byte[] source = decodeBase64(cipherText);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(source))) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzip.read(buffer)) >= 0) {
                out.write(buffer, 0, length);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    /** 按 secret_level 处理请求体，L0 原样返回。 */
    public static String encryptBody(String body, String securityKey, String secretLevel) throws Exception {
        String level = secretLevel == null ? "" : secretLevel.trim();
        if (level.isEmpty() || OpenApiConfig.SecretLevel.LEVEL_0.equals(level)) {
            return body;
        }
        if (OpenApiConfig.SecretLevel.LEVEL_1.equals(level)) {
            return aesEncrypt(securityKey, body);
        }
        if (OpenApiConfig.SecretLevel.LEVEL_2.equals(level)) {
            return gzipCompress(body);
        }
        if (OpenApiConfig.SecretLevel.LEVEL_3.equals(level)) {
            return aesEncrypt(securityKey, gzipCompress(body));
        }
        if (OpenApiConfig.SecretLevel.LEVEL_4.equals(level)) {
            return gzipCompress(aesEncrypt(securityKey, body));
        }
        throw new IllegalArgumentException("无效的 secret_level: " + secretLevel);
    }

    /** 按 secret_level 还原响应体，L0 原样返回。 */
    public static String decryptBody(String body, String securityKey, String secretLevel) throws Exception {
        String level = secretLevel == null ? "" : secretLevel.trim();
        if (level.isEmpty() || OpenApiConfig.SecretLevel.LEVEL_0.equals(level)) {
            return body;
        }
        if (OpenApiConfig.SecretLevel.LEVEL_1.equals(level)) {
            return aesDecrypt(securityKey, body);
        }
        if (OpenApiConfig.SecretLevel.LEVEL_2.equals(level)) {
            return gzipDecompress(body);
        }
        if (OpenApiConfig.SecretLevel.LEVEL_3.equals(level)) {
            return gzipDecompress(aesDecrypt(securityKey, body));
        }
        if (OpenApiConfig.SecretLevel.LEVEL_4.equals(level)) {
            return aesDecrypt(securityKey, gzipDecompress(body));
        }
        throw new IllegalArgumentException("无效的 secret_level: " + secretLevel);
    }

    public static byte[] decodeBase64(String value) {
        return Base64.getMimeDecoder().decode(Signatures.stripLineBreaks(value));
    }

    /** 公钥先清洗再解码，顺手给出可读的错误信息。 */
    private static byte[] decodePublicKey(String publicKeyBase64) {
        String normalized = Signatures.normalizeKey(publicKeyBase64);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("公钥为空");
        }
        try {
            return Base64.getMimeDecoder().decode(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("公钥不是合法的 Base64（清洗后长度 " + normalized.length() + "）", e);
        }
    }

    private static SecretKeySpec symKey(String securityKey) {
        return new SecretKeySpec(decodeBase64(securityKey), "AES");
    }

    private static IvParameterSpec iv(String securityKey) {
        return new IvParameterSpec(securityKey.substring(0, IV_LENGTH).getBytes(StandardCharsets.UTF_8));
    }
}
