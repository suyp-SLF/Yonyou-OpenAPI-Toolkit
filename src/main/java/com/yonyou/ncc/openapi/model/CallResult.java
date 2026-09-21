package com.yonyou.ncc.openapi.model;

/**
 * 一次接口联调的完整现场，含请求与响应，便于直接贴给后端定位。
 */
public final class CallResult {

    private final int status;
    private final String requestUrl;
    private final String requestHeaders;
    private final String requestBody;
    private final String responseBody;
    private final String rawResponseBody;

    public CallResult(int status, String requestUrl, String requestHeaders, String requestBody,
                      String responseBody, String rawResponseBody) {
        this.status = status;
        this.requestUrl = requestUrl;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.rawResponseBody = rawResponseBody;
    }

    public int getStatus() {
        return status;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    /** 按 secret_level 解密/解压后的响应。 */
    public String getResponseBody() {
        return responseBody;
    }

    public String getRawResponseBody() {
        return rawResponseBody;
    }
}

