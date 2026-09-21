package com.yonyou.ncc.openapi.service;

/**
 * 把服务端返回的英文/简短报错翻成人话，并给出对应的排查动作。
 * 这些文案来自 NCC 开放平台服务端（AccessTokenController / OpenCloudSecurityFilter）的实测返回。
 */
public final class ServerHints {

    private ServerHints() {
    }

    public static String hintFor(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        if (message.contains("Decryption error") || message.contains("解密失败")) {
            return "client_secret 必须是「应用公钥加密后的密文」，不能直接填应用管理里的明文。"
                    + "用本插件会自动加密；手工拼请求时才需要自己加密并 URL 编码。";
        }
        if (message.contains("Bad request content type") || message.contains("invalid_request")) {
            return "接口只接受 POST，且必须带 Content-Type: application/x-www-form-urlencoded；"
                    + "Body 不能用 none / form-data / raw JSON。";
        }
        if (message.contains("Failed to verify signature for get token")) {
            return "取 token 的 signature = SHA256(client_id + 明文client_secret + 公钥 + 盐值)，"
                    + "盐值由公钥经 SHA1PRNG 派生；公钥要和应用管理里注册的一致，换行会被忽略。";
        }
        if (message.contains("Failed to verify signature for call api")) {
            return "业务接口的 signature = SHA256(client_id + 明文请求体 + 公钥 + 盐值)；"
                    + "注意用的是解密后的明文请求体，且请求体要与实际发送的完全一致。";
        }
        if (message.contains("not registered")) {
            return "该 client_id 在开放平台没有注册，检查应用编码是否填错。";
        }
        if (message.contains("not authorized")) {
            return "应用已注册但校验未通过：多为 client_secret 密文解出来的值与应用密文不一致。";
        }
        if (message.contains("Invalid NCCloud busiCenter") || message.contains("账套编码")) {
            return "biz_center 要填该环境真实的业务中心编码（表 sm_busicenter.code），"
                    + "它同时决定本次 token 绑定哪个数据源（账套）。";
        }
        if (message.contains("access_token has expired") || message.contains("has expired")) {
            return "access_token 已失效，重新取一次 token 再调用。";
        }
        if (message.contains("appid参数缺失")) {
            return "业务接口少传了 client_id 请求头（服务端用它查第三方应用）。"
                    + "在「发送接口」窗口填好「应用编码(client_id)」再发送，它会随参数同步自动带过来。";
        }
        if (message.contains("token失效")) {
            return "access_token 已失效或不是本环境签发的，去「生成token」重新取一个（会自动同步到本窗口）。";
        }
        if (message.contains("权限")) {
            return "应用没有该接口的权限：去开放平台给这个应用关联/授权该 API 后再试。";
        }
        return "";
    }

    /** 服务端返回 success=false 时，把 code/message 提炼成一行摘要。 */
    public static String summarize(String code, String message) {
        StringBuilder builder = new StringBuilder();
        if (code != null && !code.isEmpty()) {
            builder.append("code=").append(code).append(" ");
        }
        builder.append(message == null || message.isEmpty() ? "(无 message)" : message);
        return builder.toString();
    }
}
