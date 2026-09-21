# Yonyou OpenAPI Toolkit

用友 NCC / BIP OpenAPI 的 IDEA 辅助插件。把原本要写一段 Java 才能跑通的「算签名 → 换 token → 调接口」流程，做成 IDE 里三个互相独立、开箱即用的工具窗口。

## 功能

三个入口彼此独立，可以只用一个、不影响另外两个；共用一份默认配置。

### 参数共享（填一处，其余自动同步）

三个窗口的公共参数走同一份内存草稿（`settings/DraftStore` + `ui/FieldBinder`），**任意一处改动立即同步到另外两个窗口**，不用重复填：

| 参数 | 生成签名 | 生成token | 发送接口 |
| --- | --- | --- | --- |
| `baseUrl` | | ✓ | ✓ |
| `biz_center` | | ✓ | |
| `client_id` | ✓ | ✓ | ✓ |
| 应用密文(明文) | ✓ | ✓ | |
| 公钥 | ✓ | ✓ | ✓ |
| 请求体 | ✓ | | ✓ |
| `username` / `password` | ✓ | ✓ | |
| `grant_type` / `secret_level` | | ✓ | ✓ |
| `access_token` / `security_key` | | ✓ 生成后推送 | ✓ 自动接收 |

- 首次打开窗口会用 `Settings | Tools | Yonyou OpenAPI` 里保存的默认值播种
- 「生成token」拿到的 `access_token` / `security_key` 会**自动填进「发送接口」**，不用手工复制
- 同步是双向的：任一窗口改动都会广播给其他窗口（值没变时不广播，避免回环）

| 入口(Tool Window) | 作用 | 是否发请求 | 依赖其他入口 |
| --- | --- | --- | --- |
| 生成签名 | 按开放平台规则计算 `signature`，并显示盐值与参与签名的原文 | 否 | 无 |
| 生成token | 调用 `nccloud/opm/accesstoken`，返回 `access_token` / `security_key` | 是 | 无 |
| 发送接口 | 用已有 token 调用业务接口，按 `secret_level` 自动加解密 | 是 | 只需手工粘贴 token |

### 生成签名

- 四种签名模式：登录签名、用户名密码签名、接口签名、自定义原文
- 输出除 `signature` 外，还给出**盐值**和**参与签名的原文**，签名对不上时可直接比对
- 顺带输出**手工调接口用的 `client_secret` 密文**（RSA-OAEP），可一键复制
- 支持「从设置载入」一键带入 `client_id` / `client_secret` / 公钥
- 结果可一键复制

关于「变不变」：

- `signature` 是**固定值** —— 同一 `client_id` + 同一明文密文 + 同一公钥，永远算出同一个签名（盐值由公钥确定性派生）
- `client_secret` 密文**每次计算都不同**（RSA-OAEP 带随机数），但**任意一个都能用、可长期复用**，服务端只是解密后与应用密文比对，不带时间戳与次数限制

关于公钥粘贴：

- 公钥会自动清洗：真实换行、字面量 `\n` / `\r`、`"` / `'` 引号、PEM 头尾（`-----BEGIN/END-----`）与空白都会被去掉，从 Java 代码或文档里直接复制也能用
- **注意签名与密文的差别**：签名只把公钥当字符串参与哈希，公钥不合法也能算出签名；而密文必须把公钥解析成真正的 RSA 公钥。所以「签名能算出来」不代表公钥可用——密文失败时界面上会单独提示原因，且不影响签名结果展示
- 「生成签名」窗口有 **校验公钥** 按钮：给出原始/清洗后长度、DER 字节数、解析结果与结论。2048 位公钥的 Base64 固定 **392 字符**、DER **294 字节**，长度不符基本就是复制被截断

### 生成token

- `grant_type` 支持 `client_credentials` 与 `password`（后者额外带 `username` / `password`，密码同样 RSA 加密）
- `client_secret` 走 `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` 加密后 URL 编码
- 展示 `access_token`、`security_key` 与格式化后的原始响应，`access_token` 可一键复制

### 发送接口

- 手工粘贴 `access_token` / `security_key` 即可发请求，与「生成token」解耦
- 按 `secret_level`(L0~L4) 自动处理请求体加密与响应体解密
- `apiUrl` 既可以是相对路径（与 `baseUrl` 拼接），也可以直接粘贴**完整 URL**（此时不再拼 `baseUrl`）
- 日志输出 HTTP 状态、请求 URL、请求头、请求体、还原后的响应与原始响应，可整段复制贴给后端
- 服务端返回 `success=false` 时，会额外提炼 `code` / `message` 并给出排查提示（见下）

### 设置页

`Settings | Tools | Yonyou OpenAPI`，可保存 `baseUrl`、`biz_center`、`client_id`、`client_secret`、公钥、`secret_level`、`apiUrl` 与默认请求体。
应用密文写入 IDE 凭证存储（不落配置文件），其余写入 `yonyou-openapi.xml`。

## 环境要求

- IntelliJ IDEA **2025.1**（`since-build 251`，`until-build 253.*`），Community / Ultimate 均可
- 构建用 JDK 21（插件目标字节码 21）；本机示例使用 `azul-21.0.11`
- Gradle 8.12（仓库内已带 wrapper，无需预装 Gradle）

## 出插件

### 1. 一键构建

```bash
cd /Users/suyp/IdeaProjects/YonyouSignature
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/azul-21.0.11/Contents/Home
./gradlew clean buildPlugin
```

产物：

```
build/distributions/yonyou-openapi-toolkit-1.0.0.zip
```

zip 结构为 `yonyou-openapi-toolkit/lib/*.jar`，插件描述符在 jar 的 `META-INF/plugin.xml` 里，这就是可直接安装的插件包。

校验产物内容：

```bash
unzip -l build/distributions/yonyou-openapi-toolkit-1.0.0.zip
unzip -p build/distributions/yonyou-openapi-toolkit-1.0.0.zip \
  yonyou-openapi-toolkit/lib/yonyou-openapi-toolkit-1.0.0.jar > /tmp/plug.jar
unzip -p /tmp/plug.jar META-INF/plugin.xml
shasum -a 256 build/distributions/yonyou-openapi-toolkit-1.0.0.zip
```

### 2. 安装到日常 IDE

1. Settings → Plugins → 右上 ⚙ → **Install Plugin from Disk…**
2. 选择 `build/distributions/yonyou-openapi-toolkit-1.0.0.zip`
3. 重启 IDE，右侧边栏出现「生成签名」「生成token」「发送接口」三个窗口

也可以手工解包安装：把 zip 内的 `yonyou-openapi-toolkit/` 目录整体拷到
`~/Library/Application Support/JetBrains/<产品>-<版本>/plugins/`，重启 IDE。

### 3. 沙箱试跑（不动日常 IDE）

```bash
./gradlew runIde
```

会拉起一个装了本插件的沙箱 IDE，装插件前想先看效果就用这个。

### 4. 官方插件校验

```bash
./gradlew verifyPlugin
```

校验器会对 `build.gradle.kts` 中 `pluginVerification.ides` 指定的 IDE 逐项检查 API 兼容性，报告写在
`build/reports/pluginVerifier/<IDE 版本>/`。当前结果：`Compatible`（IC-251.23774.435），且支持动态启停。

### 5. 平台依赖来源（构建卡住时看这里）

`build.gradle.kts` 默认指向本机已安装的 IDEA，避免下载整个平台发行包：

```kotlin
dependencies {
    intellijPlatform {
        local("/Applications/IntelliJ IDEA CE.app")
    }
}
```

换机器或 IDEA 装在别处时，改这个路径；也可以改成从仓库下载指定版本：

```kotlin
intellijPlatform {
    intellijIdeaCommunity("2025.1")   // 需要能访问 JetBrains 仓库，首次会下载约 1GB
}
```

### 6. 改版本号与兼容范围

- 插件版本：`build.gradle.kts` 里的 `version = "1.0.0"`（产物文件名随之变化）
- 兼容范围：`intellijPlatform.pluginConfiguration.ideaVersion` 的 `sinceBuild` / `untilBuild`
- 插件 ID / 名称 / 描述：`src/main/resources/META-INF/plugin.xml`（描述必须以拉丁字符开头且不少于 40 字符，否则官方校验器会判为无效描述符）
- 窗口显示名：`src/main/resources/messages/YonyouBundle.properties` 里的 `toolwindow.stripe.*`

## 签名与联调协议

### 签名规则（与 `nccdev_OpenAPIUtil` 的 `SHA256Util` 完全一致）

```
盐值 salt = Base64( SHA1PRNG(seed = 公钥去掉换行).nextBytes(16) )   再去掉其中的换行
signature = SHA256( 原文 + salt )                                  十六进制小写
```

- 登录签名原文：`client_id + client_secret + 公钥`
- 用户名密码签名原文：`client_id + client_secret + username + password + 公钥`
- 接口签名原文：`client_id + 请求体 + 公钥`
- `SHA1PRNG` 在 `setSeed` 之后是确定性的，同一个公钥每次算出的盐值固定，实测 JDK 8/17/21 结果一致

### 取 token

```
POST {baseUrl}nccloud/opm/accesstoken
     ?grant_type=client_credentials
     &client_id=..
     &client_secret=<URLEncode(RSA-OAEP-SHA256(应用密文))>
     &biz_center=..
     &signature=..
content-type: application/x-www-form-urlencoded
body: 空
```

用户名密码模式把 `grant_type` 换成 `password`，并追加 `username`、`password`（密码同样 RSA 加密）。
响应取 `data.access_token` 与 `data.security_key`。

### 调业务接口

```
POST {baseUrl}{apiUrl}
content-type: application/json;charset=utf-8
access_token: ..
client_id: ..
signature: <client_id + 请求体 + 公钥 的签名>
repeat_check: Y
ucg_flag: y
```

| secret_level | 请求体处理 | 响应体处理 |
| --- | --- | --- |
| L0 | 明文 | 明文 |
| L1 | AES/CTR/NoPadding | 同左，方向相反 |
| L2 | GZIP + Base64 | 同左，方向相反 |
| L3 | GZIP 后再 AES | 同左，方向相反 |
| L4 | AES 后再 GZIP | 同左，方向相反 |

AES 细节：`key = Base64Decode(security_key)`，`IV = security_key 前 16 个字符`，输出 Base64。
公钥加密细节：`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`，OAEP 参数 `SHA-256 / MGF1-SHA256`，超长内容按 117 字节分块后 Base64。

## 目录结构

```
src/main/java/com/yonyou/ncc/openapi/
  crypto/     Signatures（签名与盐值）、OpenApiCipher（RSA/AES/GZIP 与 L0~L4）
  model/      OpenApiConfig、SignResult、TokenInfo、CallResult
  service/    SignService（独立签名能力）、OpenApiClient（token 与接口调用）
  settings/   插件级默认配置与设置页
  ui/         三个独立入口 + 表单/日志公共组件
  util/       极简 JSON 取值与格式化
tools/        本地校验程序（不参与插件打包）
```

## 本地校验程序

两个校验程序都在 `tools/` 下，只用于本地核对，不会打进插件包。

### 与原 jar 逐项对拍

`tools/VerifyAgainstJar.java` 用真实 RSA 密钥对，把插件实现与 `84_..._nccdev_OpenAPIUtil-1.0.jar` 逐项对比：

- 登录签名 / 接口签名：与 jar 的 `SHA256Util` 完全一致
- 公钥带换行时算出的盐值一致（换行被正确忽略）
- RSA 密文长度一致（300 字节明文分块场景），jar 与插件的密文都能被私钥还原
- GZIP + Base64 压缩结果与 jar 一致，可还原
- AES/CTR 因 jar 自身在 JDK 21 上无法运行（`SecretKeySpec` 用了 `AES/CTR/NoPadding` 作为算法名），改为与 `openssl enc -aes-256-ctr` 独立对拍，结果一致

```bash
# classpath 需同时包含该 jar 与 commons-codec
JAR="/path/to/84_1639466653100_nccdev_OpenAPIUtil-1.0.jar"
CODEC="/path/to/commons-codec-1.16.1.jar"
javac -encoding UTF-8 -cp "$JAR:$CODEC" -d /tmp/plugincheck \
  $(find src/main/java -name '*.java') tools/VerifyAgainstJar.java
java -cp "$JAR:$CODEC:/tmp/plugincheck" VerifyAgainstJar

# AES 独立对拍
SK="<32 字节 key 的 Base64>"; PLAIN='{"a":1}'
KEYHEX=$(printf '%s' "$SK" | base64 -d | xxd -p | tr -d '\n')
IVHEX=$(printf '%s' "${SK:0:16}" | xxd -p | tr -d '\n')
printf '%s' "$PLAIN" | openssl enc -aes-256-ctr -K "$KEYHEX" -iv "$IVHEX" -A -base64
```

### 端到端流程校验

`tools/FlowCheck.java` 在本地 `127.0.0.1:18080` 起一个假的 OpenAPI 服务，走完「生成 token → 发送接口」两条链路，
校验 token 请求的 `grant_type`/`client_id`/`biz_center`/`signature`、`client_secret` 的 RSA 密文可还原、
接口请求的 `access_token`/`client_id`/`signature`/`repeat_check`/`ucg_flag` 头，以及 L0 明文与 L1 AES 的请求/响应加解密，**18 项全部通过**。

```bash
javac -encoding UTF-8 -d /tmp/flowcheck \
  $(find src/main/java/com/yonyou/ncc/openapi/{crypto,model,service,util} -name '*.java') \
  tools/FlowCheck.java
java -cp /tmp/flowcheck FlowCheck
```

> 该脚本会监听 18080 端口，注意别和本机已有服务冲突。

## 常见问题

- **签名对不上**：先在「生成签名」里核对**参与签名的原文**与**盐值**。原文里公钥的换行会被自动去掉；如果服务端仍校验失败，检查 `client_secret` 是否与开放平台一致（不是 RSA 加密后的值）。
- **取 token 返回失败**：多数是 `baseUrl` 写成了不带协议/端口的形式。插件按 `{baseUrl}nccloud/opm/accesstoken` 拼，`baseUrl` 需形如 `http://host:port/`。
- **报「无效的账套编码，请检查」**：这是服务端在校验 `biz_center`，不是签名问题（HTTP 200 且返回结构化 JSON，说明签名与 `client_secret` 加密都已通过）。`biz_center` 要填该环境真实的业务中心/账套编码，多中心环境下每个中心编码不同——去 NCC 系统管理的业务中心/账套列表查，或问系统管理员；单中心环境可先留空试一次。
- **L1~L4 解密报错**：`security_key` 必须是「生成token」返回的那个值；换 token 后旧 `security_key` 立即失效。
- **RSA 报 `Wrong algorithm`**：这是老 jar 在 JDK 21 上的已知问题（`SecretKeySpec` 算法名写成 `AES/CTR/NoPadding`），插件已自行实现，不依赖该 jar。
- **构建下载慢或失败**：把 `intellijPlatform { local(...) }` 指向本机 IDEA 即可跳过平台下载，见「平台依赖来源」。

### 服务端报错对照（实测返回，插件会直接给出提示）

| 服务端返回 | 原因 | 处理 |
| --- | --- | --- |
| `invalid_request, Bad request content type. Expecting: application/x-www-form-urlencoded` | 方法/Content-Type 不对 | 必须 `POST` + `Content-Type: application/x-www-form-urlencoded`（Body 用 `x-www-form-urlencoded`，不能用 none / form-data / raw JSON） |
| `解密失败Decryption error` | `client_secret` 传了明文 | 必须传「应用公钥加密后的密文」；插件会自动加密并 URL 编码 |
| `无效的账套编码，请检查` / `Invalid NCCloud busiCenter` | `biz_center` 查不到（`sm_busicenter.code`） | 填该环境真实的业务中心编码，它同时决定 token 绑定哪个数据源（账套） |
| `Third-party applications are not registered` | `client_id` 没注册 | 核对应用编码 |
| `Third-party applications are not authorized` | `client_secret` 解出来与应用密文不一致 | 核对应用密文与公钥是否同一套 |
| `Failed to verify signature for get token` | 取 token 的签名不对 | `signature = SHA256(client_id + 明文client_secret + 公钥 + 盐值)` |
| `Failed to verify signature for call api` | 业务接口签名不对 | `signature = SHA256(client_id + 明文请求体 + 公钥 + 盐值)`，用的是解密后的明文请求体 |
| `第三方应用【x】没有【/...】的权限` | 应用未授权该 API | 去开放平台给应用关联/授权该接口 |
| `The access_token has expired` | token 过期 | 重新取 token |
| `appid参数缺失` | 业务请求**少传 `client_id` 请求头**（服务端用它查第三方应用） | 在「发送接口」填好「应用编码(client_id)」，发送时自动带上该头 |
| `token失效，请重新获取token` | `access_token` 失效或不是本环境签发 | 去「生成token」重新取（会自动同步到「发送接口」） |

业务接口请求头（实测被服务端接受的组合）：

```
POST {baseUrl}{apiUrl}
content-type: application/json;charset=utf-8
access_token: <生成token 得到的 access_token>
client_id: <应用编码>          ← 缺这个就报 appid参数缺失
signature: SHA256(client_id + 明文请求体 + 公钥 + 盐值)
repeat_check: Y
ucg_flag: y
```

## 已对接环境的实测结论

对 NCC 环境 `http://192.168.4.43:8088/` 用真实应用跑通过：

- **取 token**：`biz_center=BIP` + `client_id=yunjian`，插件（`OpenApiClient.fetchToken`）与手工 curl 均返回
  `{"success":true,"data":{"access_token":"...","security_key":"...","security_level":"L0",...}}`
- **调业务接口**：`POST /nccloud/api/riaorg/orgmanage/org/queryOrgByCode`，服务端**验签通过**，返回业务层结果
  `{"success":false,"code":"1000000010","message":"第三方应用【yunjian】没有【...】的权限"}` —— 说明插件发送的
  `access_token` / `client_id` / `signature` / `repeat_check` / `ucg_flag` 头与 `content-type` 都被服务端接受，剩下的只是应用授权配置
- 服务端校验顺序（取自 `AccessTokenController#getToken` 与 `OpenCloudSecurityFilter#checkSign` 反编译）：
  `biz_center` → 应用是否注册 → `client_secret` 解密比对 → 验签 → 签发 token；业务请求先按 `secret_level` 解密请求体，再用**明文请求体**验签

## License

内部工具，未附开源许可。
