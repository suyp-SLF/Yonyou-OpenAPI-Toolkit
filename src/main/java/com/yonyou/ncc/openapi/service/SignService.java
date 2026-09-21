package com.yonyou.ncc.openapi.service;

import com.yonyou.ncc.openapi.crypto.Signatures;
import com.yonyou.ncc.openapi.model.SignResult;

/**
 * 独立的签名能力，供「开放API签名」入口单独使用，也被联调入口复用。
 */
public final class SignService {

    /** 客户端模式取 token 时的签名：client_id + client_secret + 公钥。 */
    public SignResult loginSign(String clientId, String clientSecret, String publicKey) {
        return sign(join(clientId, clientSecret, publicKey), publicKey);
    }

    /** 用户名密码模式取 token 时的签名：client_id + client_secret + 用户名 + 密码 + 公钥。 */
    public SignResult passwordSign(String clientId, String clientSecret, String userName, String password, String publicKey) {
        return sign(join(clientId, clientSecret, userName, password, publicKey), publicKey);
    }

    /** 业务接口调用的签名：client_id + 请求体 + 公钥。 */
    public SignResult apiSign(String clientId, String requestBody, String publicKey) {
        return sign(join(clientId, requestBody, publicKey), publicKey);
    }

    /** 指定参与签名的原文与公钥，直接计算签名。 */
    public SignResult sign(String signedText, String publicKey) {
        return Signatures.sign(signedText, publicKey);
    }

    private static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part == null ? "" : part);
        }
        return builder.toString();
    }
}

