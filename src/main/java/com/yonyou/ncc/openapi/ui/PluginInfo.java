package com.yonyou.ncc.openapi.ui;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

/**
 * 读插件自身版本，界面上显示出来，方便确认装的是哪一版。
 */
public final class PluginInfo {

    private static final String PLUGIN_ID = "com.yonyou.ncc.openapi.toolkit";

    private PluginInfo() {
    }

    public static String version() {
        try {
            var descriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
            if (descriptor != null && descriptor.getVersion() != null) {
                return descriptor.getVersion();
            }
        } catch (Exception ignored) {
            // 开发环境或描述符缺失时退化为 dev
        }
        return "dev";
    }
}

