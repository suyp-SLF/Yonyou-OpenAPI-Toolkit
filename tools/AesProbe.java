import com.yonyou.ncc.openapi.crypto.OpenApiCipher;

/**
 * 打印插件 AES/CTR 加密结果，供 openssl 独立对拍使用。
 */
public class AesProbe {

    public static void main(String[] args) throws Exception {
        System.out.print(OpenApiCipher.aesEncrypt(args[0], args[1]));
    }
}

