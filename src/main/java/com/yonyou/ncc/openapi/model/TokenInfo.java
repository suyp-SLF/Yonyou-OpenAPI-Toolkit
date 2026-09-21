package com.yonyou.ncc.openapi.model;

/**
 * accesstoken 接口的返回内容。
 */
public final class TokenInfo {

    private final String accessToken;
    private final String securityKey;
    private final String rawResponse;

    public TokenInfo(String accessToken, String securityKey, String rawResponse) {
        this.accessToken = accessToken;
        this.securityKey = securityKey;
        this.rawResponse = rawResponse;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getSecurityKey() {
        return securityKey;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}

