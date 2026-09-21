package com.yonyou.ncc.openapi.settings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 三个窗口之间共享的参数草稿。纯内存实现、不依赖平台 API，便于单测。
 * 任何一处 set 都会通知所有监听者，监听者再把值灌回自己的输入框。
 */
public final class DraftStore {

    public static final String BASE_URL = "baseUrl";
    public static final String BIZ_CENTER = "bizCenter";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String PUBLIC_KEY = "publicKey";
    public static final String SECRET_LEVEL = "secretLevel";
    public static final String GRANT_TYPE = "grantType";
    public static final String USER_NAME = "userName";
    public static final String PASSWORD = "password";
    public static final String API_URL = "apiUrl";
    public static final String REQUEST_BODY = "requestBody";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String SECURITY_KEY = "securityKey";

    private final Map<String, String> values = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public String get(String key) {
        String value = values.get(key);
        return value == null ? "" : value;
    }

    /** 值真的变化时才落库并广播，避免回环。 */
    public void set(String key, String value) {
        String normalized = value == null ? "" : value;
        if (Objects.equals(values.get(key), normalized)) {
            return;
        }
        values.put(key, normalized);
        for (Consumer<String> listener : listeners) {
            listener.accept(key);
        }
    }

    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void clear() {
        values.clear();
    }
}

