package com.yonyou.ncc.openapi.crypto;

import com.yonyou.ncc.openapi.model.SignResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * 与 nccloud.open.api.auto.token.cur.utils.SHA256Util 完全一致的签名实现。
 *
 * <p>签名值 = SHA256(明文 + 盐值) 的十六进制小写，其中盐值 =
 * Base64(SHA1PRNG(seed = 公钥).nextBytes(16))，去掉其中的换行符。
 * SHA1PRNG 在 setSeed 之后是确定性的，因此同一个公钥每次算出的盐值固定。
 */
public final class Signatures {

    private static final Pattern LINE_BREAKS = Pattern.compile("\r|\n");

    private Signatures() {
    }

    public static String stripLineBreaks(String value) {
        return value == null ? "" : LINE_BREAKS.matcher(value).replaceAll("");
    }

    /**
     * 公钥清洗：去掉真实换行、字面量 \n \r、引号、PEM 头尾与所有空白。
     * 从 Java 代码/文档里复制过来的公钥常带 \" 与 \n 转义，不清洗会导致盐值和加密都对不上。
     */
    public static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\\r", "").replace("\\n", "");
        normalized = normalized.replace("\"", "").replace("'", "");
        normalized = normalized.replaceAll("-----BEGIN[^-]*-----", "")
                .replaceAll("-----END[^-]*-----", "");
        return normalized.replaceAll("\\s", "");
    }

    public static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data.getBytes(StandardCharsets.UTF_8));
            return toHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    public static String deriveSalt(String key) {
        try {
            byte[] salt = new byte[16];
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(stripLineBreaks(key).getBytes(StandardCharsets.UTF_8));
            secureRandom.nextBytes(salt);
            return stripLineBreaks(Base64.getEncoder().encodeToString(salt));
        } catch (Exception e) {
            throw new IllegalStateException("盐值生成失败", e);
        }
    }

    /**
     * 按 data + salt 计算签名，并返回盐值与原文，便于排查签名对不上的问题。
     */
    public static SignResult sign(String data, String key) {
        String text = data == null ? "" : data;
        String normalizedKey = normalizeKey(key);
        String salt = deriveSalt(normalizedKey);
        return new SignResult(sha256Hex(text + salt), salt, text);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (value < 0x10) {
                builder.append('0');
            }
            builder.append(Integer.toHexString(value));
        }
        return builder.toString();
    }
}
