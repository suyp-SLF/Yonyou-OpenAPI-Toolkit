package com.yonyou.ncc.openapi.model;

/**
 * 调用 OpenAPI 所需的全部配置项。字段名与开放平台应用管理页面保持一致。
 */
public class OpenApiConfig {

    public static final String TOKEN_PATH = "nccloud/opm/accesstoken";
    public static final String GRANT_TYPE_CLIENT = "client_credentials";
    public static final String GRANT_TYPE_PASSWORD = "password";

    private String baseUrl = "http://127.0.0.1:80/";
    private String bizCenter = "";
    private String clientId = "";
    private String clientSecret = "";
    private String publicKey = "";
    private String secretLevel = SecretLevel.LEVEL_0;
    private String apiUrl = "";
    private String requestBody = "";
    private String userName = "";
    private String password = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBizCenter() {
        return bizCenter;
    }

    public void setBizCenter(String bizCenter) {
        this.bizCenter = bizCenter;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSecretLevel() {
        return secretLevel;
    }

    public void setSecretLevel(String secretLevel) {
        this.secretLevel = secretLevel;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String tokenUrl() {
        return normalizedBaseUrl() + TOKEN_PATH;
    }

    public String apiFullUrl() {
        String path = apiUrl == null ? "" : apiUrl.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return normalizedBaseUrl() + path;
    }

    private String normalizedBaseUrl() {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url : url + "/";
    }

    /** secret_level 取值，与开放平台一致。 */
    public static final class SecretLevel {

        public static final String LEVEL_0 = "L0";
        public static final String LEVEL_1 = "L1";
        public static final String LEVEL_2 = "L2";
        public static final String LEVEL_3 = "L3";
        public static final String LEVEL_4 = "L4";

        public static final String[] ALL = {LEVEL_0, LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4};

        private SecretLevel() {
        }
    }
}
