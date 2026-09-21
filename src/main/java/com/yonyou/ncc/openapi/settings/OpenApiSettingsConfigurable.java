package com.yonyou.ncc.openapi.settings;

import com.intellij.openapi.options.Configurable;
import com.yonyou.ncc.openapi.model.OpenApiConfig;
import com.yonyou.ncc.openapi.ui.FormPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Settings | Tools | Yonyou OpenAPI 的设置页，供联调入口的默认值使用。
 */
public final class OpenApiSettingsConfigurable implements Configurable {

    private JTextField baseUrlField;
    private JTextField bizCenterField;
    private JTextField clientIdField;
    private JPasswordField clientSecretField;
    private JTextArea publicKeyArea;
    private JComboBox<String> secretLevelBox;
    private JTextField apiUrlField;
    private JTextArea requestBodyArea;

    @Override
    public String getDisplayName() {
        return "Yonyou OpenAPI";
    }

    @Override
    public @Nullable JComponent createComponent() {
        FormPanel panel = new FormPanel();

        baseUrlField = new JTextField();
        panel.addRow("服务地址(baseUrl)", baseUrlField);

        bizCenterField = new JTextField();
        panel.addRow("业务中心(biz_center)", bizCenterField);

        clientIdField = new JTextField();
        panel.addRow("应用编码(client_id)", clientIdField);

        clientSecretField = new JPasswordField();
        panel.addRow("应用密文(client_secret)", clientSecretField);

        publicKeyArea = new JTextArea(4, 60);
        publicKeyArea.setLineWrap(true);
        publicKeyArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        JScrollPane publicKeyScroll = new JScrollPane(publicKeyArea);
        publicKeyScroll.setPreferredSize(new Dimension(560, 90));
        panel.addRow("公钥(publicKey)", publicKeyScroll);

        secretLevelBox = new JComboBox<>(OpenApiConfig.SecretLevel.ALL);
        panel.addRow("安全级别(secret_level)", secretLevelBox);

        apiUrlField = new JTextField();
        panel.addRow("接口路径(apiUrl)", apiUrlField);

        requestBodyArea = new JTextArea(5, 60);
        requestBodyArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        JScrollPane bodyScroll = new JScrollPane(requestBodyArea);
        bodyScroll.setPreferredSize(new Dimension(560, 110));
        panel.addRow("默认请求体", bodyScroll);

        JComponent wrapper = new javax.swing.JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    @Override
    public boolean isModified() {
        OpenApiConfig current = OpenApiSettings.getInstance().toConfig();
        return !text(baseUrlField).equals(current.getBaseUrl())
                || !text(bizCenterField).equals(current.getBizCenter())
                || !text(clientIdField).equals(current.getClientId())
                || !secret().equals(current.getClientSecret())
                || !publicKeyArea.getText().equals(current.getPublicKey())
                || !selectedLevel().equals(current.getSecretLevel())
                || !text(apiUrlField).equals(current.getApiUrl())
                || !requestBodyArea.getText().equals(current.getRequestBody());
    }

    @Override
    public void apply() {
        OpenApiConfig config = new OpenApiConfig();
        config.setBaseUrl(text(baseUrlField));
        config.setBizCenter(text(bizCenterField));
        config.setClientId(text(clientIdField));
        config.setClientSecret(secret());
        config.setPublicKey(publicKeyArea.getText());
        config.setSecretLevel(selectedLevel());
        config.setApiUrl(text(apiUrlField));
        config.setRequestBody(requestBodyArea.getText());
        OpenApiSettings.getInstance().save(config);
    }

    @Override
    public void reset() {
        OpenApiConfig config = OpenApiSettings.getInstance().toConfig();
        baseUrlField.setText(config.getBaseUrl());
        bizCenterField.setText(config.getBizCenter());
        clientIdField.setText(config.getClientId());
        clientSecretField.setText(config.getClientSecret());
        publicKeyArea.setText(config.getPublicKey());
        secretLevelBox.setSelectedItem(config.getSecretLevel());
        apiUrlField.setText(config.getApiUrl());
        requestBodyArea.setText(config.getRequestBody());
    }

    private String selectedLevel() {
        Object selected = secretLevelBox.getSelectedItem();
        return selected == null ? OpenApiConfig.SecretLevel.LEVEL_0 : selected.toString();
    }

    private String secret() {
        return new String(clientSecretField.getPassword());
    }

    private static String text(JTextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}

