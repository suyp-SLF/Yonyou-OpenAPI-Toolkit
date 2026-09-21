import com.yonyou.ncc.openapi.crypto.OpenApiCipher;
import com.yonyou.ncc.openapi.crypto.Signatures;
import com.yonyou.ncc.openapi.model.SignResult;
import nccloud.open.api.auto.token.cur.utils.CompressUtil;
import nccloud.open.api.auto.token.cur.utils.Decryption;
import nccloud.open.api.auto.token.cur.utils.Encryption;

import nccloud.open.api.auto.token.cur.utils.SHA256Util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * 与插件 lib 中的 84_..._nccdev_OpenAPIUtil-1.0.jar 逐项对拍，确认插件实现与原实现完全一致。
 * 仅用于本地核对，不参与插件打包。
 */
public class VerifyAgainstJar {

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String publicKeyWithBreaks = publicKey.replaceAll("(.{64})", "$1\n");
        String clientId = "ZY_TEST_APP";
        String clientSecret = "3eb3ec4da18943TEST000000000000";
        String body = "{\"code\": [\"01\", \"T2001\"]}";

        String jarSign = SHA256Util.getSHA256(clientId + clientSecret + publicKey, publicKey);
        SignResult mine = Signatures.sign(clientId + clientSecret + publicKey, publicKey);
        report("登录签名 sign", jarSign, mine.getSign());
        report("盐值(带换行公钥)一致", Signatures.deriveSalt(publicKeyWithBreaks), Signatures.deriveSalt(publicKey));

        String jarApiSign = SHA256Util.getSHA256(clientId + body + publicKey, publicKey);
        report("接口签名 sign", jarApiSign, Signatures.sign(clientId + body + publicKey, publicKey).getSign());

        String longSecret = clientSecret + "x".repeat(260);
        String jarRsa = Encryption.pubEncrypt(publicKey, longSecret);
        String myRsa = OpenApiCipher.rsaEncrypt(publicKey, longSecret);
        report("RSA 密文长度一致(300 字节明文分块)", String.valueOf(jarRsa.length()), String.valueOf(myRsa.length()));
        report("RSA 密文不含换行(jar)", String.valueOf(jarRsa.contains("\n") || jarRsa.contains("\r")), "false");
        report("jar 密文可被私钥还原", longSecret, rsaDecrypt(keyPair, jarRsa));
        report("插件密文可被私钥还原", longSecret, rsaDecrypt(keyPair, myRsa));

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();
        String jarGzip = CompressUtil.gzipCompress(body);
        report("GZIP+Base64 压缩请求体", jarGzip, OpenApiCipher.gzipCompress(body));
        report("GZIP 解压回原文", body, OpenApiCipher.gzipDecompress(jarGzip));

        String securityKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        try {
            String jarAes = Encryption.symEncrypt(securityKey, body);
            report("AES/CTR 加密请求体", jarAes, OpenApiCipher.aesEncrypt(securityKey, body));
        } catch (Exception e) {
            System.out.println("[SKIP] 原 jar 的 AES 路径不可用（SecretKeySpec 算法名写成 AES/CTR/NoPadding）："
                    + e.getCause());
        }
        report("AES/CTR 插件自往返", body, OpenApiCipher.aesDecrypt(securityKey, OpenApiCipher.aesEncrypt(securityKey, body)));
        System.out.println("AES_KEY=" + securityKey);
    }

    private static String rsaDecrypt(KeyPair keyPair, String cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), new OAEPParameterSpec("SHA-256", "MGF1",
                new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT));
        byte[] source = Base64.getMimeDecoder().decode(cipherText);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int offset = 0; offset < source.length; offset += 256) {
            int length = Math.min(256, source.length - offset);
            out.write(cipher.doFinal(source, offset, length));
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void report(String name, String expected, String actual) {
        boolean ok = expected != null && expected.equals(actual);
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (!ok) {
            System.out.println("    jar = " + expected);
            System.out.println("    new = " + actual);
        }
    }
}
