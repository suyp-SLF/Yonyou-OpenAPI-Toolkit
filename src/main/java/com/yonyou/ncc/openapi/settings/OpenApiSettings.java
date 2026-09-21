package com.yonyou.ncc.openapi.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.yonyou.ncc.openapi.model.OpenApiConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 插件级默认配置。client_secret 走 IDE 凭证存储，不落配置文件。
 */
@State(name = "YonyouOpenApiSettings", storages = @Storage("yonyou-openapi.xml"))
public final class OpenApiSettings implements PersistentStateComponent<OpenApiSettings.State> {

    private static final CredentialAttributes SECRET_ATTRIBUTES =
            new CredentialAttributes("YonyouOpenApi.clientSecret");

    private State state = new State();

    public static OpenApiSettings getInstance() {
        return ApplicationManager.getApplication().getService(OpenApiSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public String getClientSecret() {
        String secret = PasswordSafe.getInstance().getPassword(SECRET_ATTRIBUTES);
        return secret == null ? "" : secret;
    }

    public void setClientSecret(String clientSecret) {
        PasswordSafe.getInstance().setPassword(SECRET_ATTRIBUTES, clientSecret == null ? "" : clientSecret);
    }

    public OpenApiConfig toConfig() {
        OpenApiConfig config = new OpenApiConfig();
        config.setBaseUrl(state.baseUrl);
        config.setBizCenter(state.bizCenter);
        config.setClientId(state.clientId);
        config.setClientSecret(getClientSecret());
        config.setPublicKey(state.publicKey);
        config.setSecretLevel(state.secretLevel);
        config.setApiUrl(state.apiUrl);
        config.setRequestBody(state.requestBody);
        return config;
    }

    public void save(OpenApiConfig config) {
        state.baseUrl = config.getBaseUrl();
        state.bizCenter = config.getBizCenter();
        state.clientId = config.getClientId();
        state.publicKey = config.getPublicKey();
        state.secretLevel = config.getSecretLevel();
        state.apiUrl = config.getApiUrl();
        state.requestBody = config.getRequestBody();
        setClientSecret(config.getClientSecret());
    }

    public static final class State {

        public String baseUrl = "http://127.0.0.1:80/";
        public String bizCenter = "";
        public String clientId = "";
        public String publicKey = "";
        public String secretLevel = OpenApiConfig.SecretLevel.LEVEL_0;
        public String apiUrl = "";
        public String requestBody = "";
    }
}

