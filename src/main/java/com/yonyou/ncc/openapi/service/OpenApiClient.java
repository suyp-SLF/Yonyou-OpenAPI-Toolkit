package com.yonyou.ncc.openapi.service;

import com.yonyou.ncc.openapi.crypto.OpenApiCipher;
import com.yonyou.ncc.openapi.model.CallResult;
import com.yonyou.ncc.openapi.model.OpenApiConfig;
import com.yonyou.ncc.openapi.model.SignResult;
import com.yonyou.ncc.openapi.model.TokenInfo;
import com.yonyou.ncc.openapi.util.JsonUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAPI 联调客户端：获取 access_token、调用业务接口。
 * 行为对齐 nccloud.open.api.auto.token.cur.utils.APICurUtils。
 */
public final class OpenApiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(50);
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=utf-8";

    private final SignService signService = new SignService();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** grantType 取 client_credentials 或 password。 */
    public TokenInfo fetchToken(OpenApiConfig config, String grantType) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        boolean byPassword = OpenApiConfig.GRANT_TYPE_PASSWORD.equals(grantType);
        if (byPassword) {
            params.put("grant_type", OpenApiConfig.GRANT_TYPE_PASSWORD);
        } else {
            params.put("grant_type", OpenApiConfig.GRANT_TYPE_CLIENT);
        }
        params.put("client_id", config.getClientId());
        params.put("client_secret", urlEncode(OpenApiCipher.rsaEncrypt(config.getPublicKey(), config.getClientSecret())));
        SignResult signature;
        if (byPassword) {
            params.put("username", config.getUserName());
            params.put("password", urlEncode(OpenApiCipher.rsaEncrypt(config.getPublicKey(), config.getPassword())));
            signature = signService.passwordSign(config.getClientId(), config.getClientSecret(),
                    config.getUserName(), config.getPassword(), config.getPublicKey());
        } else {
            signature = signService.loginSign(config.getClientId(), config.getClientSecret(), config.getPublicKey());
        }
        params.put("biz_center", config.getBizCenter());
        params.put("signature", signature.getSign());

        String url = config.tokenUrl() + "?" + toQuery(params);
        HttpResponse<String> response = post(url, FORM_CONTENT_TYPE, Map.of(), "");
        String body = response.body();
        String accessToken = JsonUtils.findString(body, "access_token");
        String securityKey = JsonUtils.findString(body, "security_key");
        if (accessToken == null) {
            throw new IllegalStateException(describeTokenFailure(response.statusCode(), body));
        }
        return new TokenInfo(accessToken, securityKey, body);
    }

    /** 把服务端返回的失败原因挑出来，并对常见的 biz_center 取值问题给出提示。 */
    static String describeTokenFailure(int status, String body) {
        String code = JsonUtils.findString(body, "code");
        String message = JsonUtils.findString(body, "message");
        StringBuilder builder = new StringBuilder("获取 token 失败，HTTP ").append(status);
        builder.append("，").append(ServerHints.summarize(code, message == null || message.isEmpty() ? body : message));
        String hint = ServerHints.hintFor(message);
        if (!hint.isEmpty()) {
            builder.append("\n提示：").append(hint);
        }
        return builder.toString();
    }

    public CallResult invokeApi(OpenApiConfig config, TokenInfo token) throws Exception {
        String url = config.apiFullUrl();
        String requestBody = config.getRequestBody() == null ? "" : config.getRequestBody();
        SignResult signature = signService.apiSign(config.getClientId(), requestBody, config.getPublicKey());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-type", JSON_CONTENT_TYPE);
        headers.put("access_token", token.getAccessToken());
        headers.put("client_id", config.getClientId());
        headers.put("signature", signature.getSign());
        headers.put("repeat_check", "Y");
        headers.put("ucg_flag", "y");

        String encryptedBody = OpenApiCipher.encryptBody(requestBody, token.getSecurityKey(), config.getSecretLevel());
        HttpResponse<String> response = post(url, JSON_CONTENT_TYPE, headers, encryptedBody);
        String rawBody = response.body();
        String decoded;
        try {
            decoded = OpenApiCipher.decryptBody(rawBody, token.getSecurityKey(), config.getSecretLevel());
        } catch (Exception e) {
            decoded = rawBody + "\n\n[响应报文按 secret_level=" + config.getSecretLevel() + " 解密失败：" + e.getMessage() + "]";
        }
        return new CallResult(response.statusCode(), url, toHeaderText(headers), encryptedBody, decoded, rawBody);
    }

    private HttpResponse<String> post(String url, String contentType, Map<String, String> headers, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("content-type", contentType);
        headers.forEach(builder::header);
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String toQuery(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        params.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(key).append('=').append(value == null ? "" : value);
        });
        return builder.toString();
    }

    private static String toHeaderText(Map<String, String> headers) {
        StringBuilder builder = new StringBuilder();
        headers.forEach((key, value) -> builder.append(key).append(": ").append(value).append('\n'));
        return builder.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
