package com.yonyou.ncc.openapi.model;

/**
 * 一次签名计算的结果，除了签名值本身，还把盐值与参与签名的原文一并带出来，便于排查对不上的问题。
 */
public final class SignResult {

    private final String sign;
    private final String salt;
    private final String signedText;

    public SignResult(String sign, String salt, String signedText) {
        this.sign = sign;
        this.salt = salt;
        this.signedText = signedText;
    }

    public String getSign() {
        return sign;
    }

    public String getSalt() {
        return salt;
    }

    /** 参与签名的原文（未包含盐值）。 */
    public String getSignedText() {
        return signedText;
    }
}

