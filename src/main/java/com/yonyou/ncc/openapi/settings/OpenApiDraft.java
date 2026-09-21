package com.yonyou.ncc.openapi.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.yonyou.ncc.openapi.model.OpenApiConfig;

/**
 * 应用级共享草稿：三个工具窗口共用一份参数，填一处其余自动同步。
 * 首次访问时用 Settings 里保存的默认值播种。
 */
@Service(Service.Level.APP)
public final class OpenApiDraft {

    private final DraftStore store = new DraftStore();
    private boolean seeded;

    public static OpenApiDraft getInstance() {
        return ApplicationManager.getApplication().getService(OpenApiDraft.class);
    }

    public DraftStore store() {
        seedIfNeeded();
        return store;
    }

    private void seedIfNeeded() {
        if (seeded) {
            return;
        }
        seeded = true;
        try {
            OpenApiConfig config = OpenApiSettings.getInstance().toConfig();
            store.set(DraftStore.BASE_URL, config.getBaseUrl());
            store.set(DraftStore.BIZ_CENTER, config.getBizCenter());
            store.set(DraftStore.CLIENT_ID, config.getClientId());
            store.set(DraftStore.CLIENT_SECRET, config.getClientSecret());
            store.set(DraftStore.PUBLIC_KEY, config.getPublicKey());
            store.set(DraftStore.SECRET_LEVEL, config.getSecretLevel());
            store.set(DraftStore.API_URL, config.getApiUrl());
            store.set(DraftStore.REQUEST_BODY, config.getRequestBody());
        } catch (Exception ignored) {
            // 凭证存储不可用时保持空草稿，不影响窗口使用
        }
    }
}

