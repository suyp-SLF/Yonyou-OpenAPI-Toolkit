package com.yonyou.ncc.openapi.crypto;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 公钥体检：把"能不能用"讲清楚，避免只看到 Unable to decode key 这种难判的报错。
 * 2048 位 RSA 公钥的 X.509(SPKI) Base64 长度为 392 字符，DER 为 294 字节。
 */
public final class PublicKeyCheck {

    public static final int EXPECTED_BASE64_LENGTH = 392;
    public static final int EXPECTED_DER_LENGTH = 294;

    private PublicKeyCheck() {
    }

    public static String describe(String rawKey) {
        String key = Signatures.normalizeKey(rawKey);
        StringBuilder report = new StringBuilder();
        report.append("原始长度：").append(rawKey == null ? 0 : rawKey.length())
                .append("　清洗后长度：").append(key.length())
                .append("（2048 位公钥应为 ").append(EXPECTED_BASE64_LENGTH).append("）\n");
        if (key.isEmpty()) {
            return report.append("结论：公钥为空，请粘贴应用管理里的公钥。").toString();
        }
        if (!key.startsWith("MIIBIjANBgkqhkiG9w0BAQEF") && !key.startsWith("MIIBCgKCAQEA")) {
            report.append("提示：前缀不是常见的 SPKI(MIIBIjANBgkqhkiG9w0BAQEF) 或 PKCS#1(MIIBCgKCAQEA)，可能粘错了内容。\n");
        }
        byte[] der;
        try {
            der = Base64.getMimeDecoder().decode(key);
        } catch (Exception e) {
            return report.append("结论：不是合法 Base64。请确认复制完整、没有多余字符。").toString();
        }
        report.append("DER 字节数：").append(der.length)
                .append("（2048 位应为 ").append(EXPECTED_DER_LENGTH).append("）\n");
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
            int bits = ((RSAPublicKey) publicKey).getModulus().bitLength();
            report.append("解析结果：OK，").append(bits).append(" 位 RSA 公钥\n");
            return report.append("结论：公钥可用。").toString();
        } catch (Exception e) {
            report.append("解析失败：").append(e.getClass().getSimpleName());
            if (e.getMessage() != null) {
                report.append(" - ").append(e.getMessage());
            }
            report.append('\n');
            if (key.length() != EXPECTED_BASE64_LENGTH) {
                report.append("结论：长度与 2048 位公钥不符，八成是复制时被截断，请重新完整复制。");
            } else {
                report.append("结论：长度正常但结构解析失败，请确认粘贴的是「公钥」而不是私钥。");
            }
            return report.toString();
        }
    }
}

