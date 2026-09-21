import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yonyou.ncc.openapi.crypto.OpenApiCipher;
import com.yonyou.ncc.openapi.crypto.Signatures;
import com.yonyou.ncc.openapi.model.CallResult;
import com.yonyou.ncc.openapi.model.OpenApiConfig;
import com.yonyou.ncc.openapi.model.TokenInfo;
import com.yonyou.ncc.openapi.service.OpenApiClient;
import com.yonyou.ncc.openapi.service.SignService;
import com.yonyou.ncc.openapi.service.ServerHints;
import com.yonyou.ncc.openapi.settings.DraftStore;
import com.yonyou.ncc.openapi.util.JsonUtils;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 端到端流程校验：本地起一个假的 OpenAPI 服务，走完「生成 token → 发送接口」两条链路，
 * 校验请求参数、签名、header 与 body 加解密，不需要真实环境。
 */
public class FlowCheck {

    private static final int PORT = 18080;
    private static final String CLIENT_ID = "ZY_TEST_APP";
    private static final String CLIENT_SECRET = "3eb3ec4da18943TEST000000000000";

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String securityKey = Base64.getEncoder().encodeToString(randomAesKey());

        AtomicReference<String> tokenQuery = new AtomicReference<>("");
        AtomicReference<Map<String, String>> apiHeaders = new AtomicReference<>(Map.of());
        AtomicReference<String> apiBody = new AtomicReference<>("");
        AtomicReference<String> apiContentType = new AtomicReference<>("");
        AtomicBoolean encryptResponse = new AtomicBoolean(false);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/nccloud/opm/accesstoken", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            tokenQuery.set(query);
            if (query != null && query.contains("biz_center=INVALID")) {
                respond(exchange, "{\"success\":false,\"code\":\"\",\"message\":\"无效的账套编码，请检查\"}");
                return;
            }
            respond(exchange, "{\"code\":\"200\",\"data\":{\"access_token\":\"TOKEN-123\","
                    + "\"security_key\":\"" + securityKey + "\"}}");
        });
        server.createContext("/nccloud/api/test", exchange -> {
            try {
            apiContentType.set(exchange.getRequestHeaders().getFirst("content-type"));
            Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((key, values) ->
                    headers.put(key.toLowerCase(), values.isEmpty() ? "" : values.get(0)));
            apiHeaders.set(headers);
            apiBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"code\":\"200\",\"data\":{\"ok\":true},\"msg\":\"success\"}";
            respond(exchange, encryptResponse.get() ? OpenApiCipher.aesEncrypt(securityKey, json) : json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        server.start();
        try {
            OpenApiClient client = new OpenApiClient();

            OpenApiConfig config = new OpenApiConfig();
            config.setBaseUrl("http://127.0.0.1:" + PORT + "/");
            config.setBizCenter("0001");
            config.setClientId(CLIENT_ID);
            config.setClientSecret(CLIENT_SECRET);
            config.setPublicKey(publicKey);
            config.setSecretLevel(OpenApiConfig.SecretLevel.LEVEL_0);
            config.setApiUrl("nccloud/api/test");
            config.setRequestBody("{\"code\": [\"01\", \"T2001\"]}");

            TokenInfo token = client.fetchToken(config, OpenApiConfig.GRANT_TYPE_CLIENT);
            Map<String, String> params = parseQuery(tokenQuery.get());
            check("token 接口 grant_type 正确", OpenApiConfig.GRANT_TYPE_CLIENT, params.get("grant_type"));
            check("token 接口 client_id 正确", CLIENT_ID, params.get("client_id"));
            check("token 接口 biz_center 正确", "0001", params.get("biz_center"));
            check("token 接口 signature 正确",
                    Signatures.sign(CLIENT_ID + CLIENT_SECRET + publicKey, publicKey).getSign(),
                    params.get("signature"));
            check("client_secret 为 URL 编码后的 RSA 密文",
                    CLIENT_SECRET, rsaDecrypt(keyPair, params.get("client_secret")));
            check("解析出 access_token", "TOKEN-123", token.getAccessToken());
            check("解析出 security_key", securityKey, token.getSecurityKey());

            CallResult result = client.invokeApi(config, token);
            Map<String, String> headers = apiHeaders.get();
            check("接口 HTTP 状态", "200", String.valueOf(result.getStatus()));
            check("接口 content-type", "application/json;charset=utf-8", apiContentType.get());
            check("接口 access_token header", "TOKEN-123", headers.get("access_token"));
            check("接口 client_id header", CLIENT_ID, headers.get("client_id"));
            check("接口 repeat_check header", "Y", headers.get("repeat_check"));
            check("接口 ucg_flag header", "y", headers.get("ucg_flag"));
            check("接口 signature header",
                    Signatures.sign(CLIENT_ID + config.getRequestBody() + publicKey, publicKey).getSign(),
                    headers.get("signature"));
            check("L0 请求体保持明文", config.getRequestBody(), apiBody.get());
            check("L0 响应体保持明文", "{\"code\":\"200\",\"data\":{\"ok\":true},\"msg\":\"success\"}",
                    result.getResponseBody());

            config.setSecretLevel(OpenApiConfig.SecretLevel.LEVEL_1);
            encryptResponse.set(true);
            CallResult encrypted = client.invokeApi(config, token);
            check("L1 请求体为 AES 密文且可还原", config.getRequestBody(),
                    OpenApiCipher.aesDecrypt(securityKey, apiBody.get()));
            check("L1 响应体加解密往返一致", "{\"code\":\"200\",\"data\":{\"ok\":true},\"msg\":\"success\"}",
                    encrypted.getResponseBody());

            OpenApiConfig badCenter = new OpenApiConfig();
            badCenter.setBaseUrl(config.getBaseUrl());
            badCenter.setBizCenter("INVALID");
            badCenter.setClientId(CLIENT_ID);
            badCenter.setClientSecret(CLIENT_SECRET);
            badCenter.setPublicKey(publicKey);
            String failure;
            try {
                client.fetchToken(badCenter, OpenApiConfig.GRANT_TYPE_CLIENT);
                failure = "未抛异常";
            } catch (Exception e) {
                failure = e.getMessage();
            }
            check("账套编码错误时给出服务端原因", "true",
                    String.valueOf(failure.contains("无效的账套编码")));
            check("账套编码错误时附带排查提示", "true",
                    String.valueOf(failure.contains("sm_busicenter")));

            check("解密失败提示指向密文", "true",
                    String.valueOf(ServerHints.hintFor("解密失败Decryption error").contains("密文")));
            check("content-type 报错提示指向 POST+form", "true",
                    String.valueOf(ServerHints.hintFor("invalid_request, Bad request content type")
                            .contains("application/x-www-form-urlencoded")));
            check("取 token 验签失败提示含拼接规则", "true",
                    String.valueOf(ServerHints.hintFor("Failed to verify signature for get token")
                            .contains("明文client_secret")));
            check("接口验签失败提示含明文请求体", "true",
                    String.valueOf(ServerHints.hintFor("Failed to verify signature for call api")
                            .contains("明文请求体")));
            check("权限报错提示指向开放平台授权", "true",
                    String.valueOf(ServerHints.hintFor("第三方应用【yunjian】没有【/x】的权限").contains("授权")));

            String tokenLike = "{\"success\":true,\"data\":{\"access_token\":\"T\",\"expires_in\":1000000,"
                    + "\"security_level\":\"L0\"}}";
            check("布尔字段解析", "true", String.valueOf(JsonUtils.isFalse("{\"success\":false}", "success")));
            check("数字字段解析", "1000000", JsonUtils.findScalar(tokenLike, "expires_in"));
            check("absolute apiUrl 不被拼接", "http://other:8080/nccloud/api/x",
                    absoluteUrl("http://other:8080/nccloud/api/x"));

            SignService signService = new SignService();
            String sign1 = signService.loginSign(CLIENT_ID, CLIENT_SECRET, publicKey).getSign();
            String sign2 = signService.loginSign(CLIENT_ID, CLIENT_SECRET, publicKey).getSign();
            check("签名是固定值（两次一致）", sign1, sign2);

            String cipher1 = signService.clientSecretCipher(CLIENT_SECRET, publicKey);
            String cipher2 = signService.clientSecretCipher(CLIENT_SECRET, publicKey);
            check("密文每次不同（OAEP 随机）", "false", String.valueOf(cipher1.equals(cipher2)));
            check("密文长度固定(344)", "344", String.valueOf(cipher1.length()));
            check("密文可被对应私钥解回明文", CLIENT_SECRET, rsaDecrypt(keyPair, cipher1));
            check("旧密文同样可解（可复用）", CLIENT_SECRET, rsaDecrypt(keyPair, cipher2));

            DraftStore draft = new DraftStore();
            check("草稿初始为空", "", draft.get(DraftStore.CLIENT_ID));
            java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
            draft.addListener(key -> notifications.incrementAndGet());
            draft.set(DraftStore.CLIENT_ID, CLIENT_ID);
            draft.set(DraftStore.CLIENT_SECRET, CLIENT_SECRET);
            check("草稿写入后可读", CLIENT_ID, draft.get(DraftStore.CLIENT_ID));
            check("两个字段各广播一次", "2", String.valueOf(notifications.get()));
            draft.set(DraftStore.CLIENT_ID, CLIENT_ID);
            check("同值写入不广播（防回环）", "2", String.valueOf(notifications.get()));
            draft.set(DraftStore.CLIENT_ID, "another");
            check("值变化继续广播", "3", String.valueOf(notifications.get()));
            check("null 归一为空串", "", draftGetNull(draft));

            String cleanKey = publicKey;
            String dirtyKey = "\"" + wrap(publicKey) + "\"";
            check("公钥清洗：换行/引号/转义都被去掉", cleanKey, Signatures.normalizeKey(dirtyKey));
            check("脏公钥与干净公钥算出的签名一致", signService.loginSign(CLIENT_ID, CLIENT_SECRET, cleanKey).getSign(),
                    signService.loginSign(CLIENT_ID, CLIENT_SECRET, dirtyKey).getSign());
            check("脏公钥也能生成可解密的密文", CLIENT_SECRET,
                    rsaDecrypt(keyPair, signService.clientSecretCipher(CLIENT_SECRET, dirtyKey)));
            check("PEM 头尾也被清掉", cleanKey,
                    Signatures.normalizeKey("-----BEGIN PUBLIC KEY-----\n" + publicKey + "\n-----END PUBLIC KEY-----"));
            check("空公钥报错可读", "true", String.valueOf(emptyKeyMessage().contains("密文生成失败")));
            check("空公钥报错带排查提示", "true", String.valueOf(emptyKeyMessage().contains("公钥为空")));
        } finally {
            server.stop(0);
        }
    }

    private static byte[] randomAesKey() throws Exception {
        javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey().getEncoded();
    }

    private static String draftGetNull(DraftStore draft) {
        draft.set(DraftStore.API_URL, null);
        return draft.get(DraftStore.API_URL);
    }

    /** 每 64 字符换行的公钥，模拟从文档/界面复制出来的形态。 */
    private static String wrap(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i += 64) {
            builder.append(value, i, Math.min(value.length(), i + 64)).append('\n');
        }
        return builder.toString();
    }

    private static String emptyKeyMessage() {
        try {
            new SignService().clientSecretCipher(CLIENT_SECRET, "");
            return "未报错";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /** 直接给完整 URL 时不应再拼 baseUrl。 */
    private static String absoluteUrl(String apiUrl) {
        OpenApiConfig config = new OpenApiConfig();
        config.setBaseUrl("http://127.0.0.1:18080/");
        config.setApiUrl(apiUrl);
        return config.apiFullUrl();
    }

    private static String rsaDecrypt(KeyPair keyPair, String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), new OAEPParameterSpec("SHA-256", "MGF1",
                new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT));
        return new String(cipher.doFinal(Base64.getMimeDecoder().decode(cipherText)), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int index = pair.indexOf('=');
            if (index > 0) {
                params.put(pair.substring(0, index),
                        URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static void respond(HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void check(String name, String expected, String actual) {
        boolean ok = expected != null && expected.equals(actual);
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (!ok) {
            System.out.println("    expected = " + expected);
            System.out.println("    actual   = " + actual);
        }
    }
}
