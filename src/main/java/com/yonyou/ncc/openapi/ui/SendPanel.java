package com.yonyou.ncc.openapi.ui;

import com.yonyou.ncc.openapi.model.CallResult;
import com.yonyou.ncc.openapi.model.OpenApiConfig;
import com.yonyou.ncc.openapi.model.TokenInfo;
import com.yonyou.ncc.openapi.service.OpenApiClient;
import com.yonyou.ncc.openapi.service.ServerHints;
import com.yonyou.ncc.openapi.settings.OpenApiSettings;
import com.yonyou.ncc.openapi.util.JsonUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * 「发送接口」独立入口：用已有的 access_token / security_key 直接调用业务接口。
 * 不负责取 token，避免与「生成token」入口耦合。
 */
public final class SendPanel extends JPanel {

    private final OpenApiClient client = new OpenApiClient();

    private final JTextField baseUrlField = new JTextField();
    private final JTextField apiUrlField = new JTextField();
    private final JTextField clientIdField = new JTextField();
    private final JTextArea publicKeyArea = FormPanel.monoArea(3);
    private final JTextField accessTokenField = new JTextField();
    private final JTextField securityKeyField = new JTextField();
    private final JComboBox<String> secretLevelBox = new JComboBox<>(OpenApiConfig.SecretLevel.ALL);
    private final JTextArea requestBodyArea = FormPanel.monoArea(4);
    private final JTextArea logArea = FormPanel.monoArea(10);

    public SendPanel() {
        setLayout(new BorderLayout());
        add(FormPanel.scrollable(buildContent()), BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel container = new ScrollablePanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel hint = new JLabel("发送接口：把 access_token 与 security_key 填进来即可直接请求，请求/响应按 secret_level 自动加解密。");
        container.add(hint);
        container.add(Box.createVerticalStrut(6));

        FormPanel form = new FormPanel();
        form.addRow("服务地址(baseUrl)", baseUrlField);
        form.addRow("接口路径(apiUrl)", apiUrlField);
        form.addRow("应用编码(client_id)", clientIdField);
        form.addRow("公钥(publicKey)", FormPanel.scroll(publicKeyArea, 56));
        form.addRow("access_token", accessTokenField);
        form.addRow("security_key", securityKeyField);
        form.addRow("安全级别(secret_level)", secretLevelBox);
        form.addRow("请求体", FormPanel.scroll(requestBodyArea, 80));
        container.add(form);

        container.add(buttons());
        container.add(Box.createVerticalStrut(4));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("调用日志"));
        logArea.setEditable(false);
        logPanel.add(FormPanel.scroll(logArea, 240), BorderLayout.CENTER);
        container.add(logPanel);
        return container;
    }

    private JPanel buttons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));

        JButton send = new JButton("发送接口");
        send.addActionListener(e -> send());
        panel.add(send);

        JButton load = new JButton("从设置载入");
        load.addActionListener(e -> loadFromSettings());
        panel.add(load);

        JButton copyLog = new JButton("复制日志");
        copyLog.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(logArea.getText()), null));
        panel.add(copyLog);

        JButton clear = new JButton("清空日志");
        clear.addActionListener(e -> logArea.setText(""));
        panel.add(clear);
        return panel;
    }

    private void send() {
        OpenApiConfig config = readConfig();
        String accessToken = accessTokenField.getText() == null ? "" : accessTokenField.getText().trim();
        if (accessToken.isEmpty()) {
            PanelSupport.append(logArea, "请先填写 access_token（可用「生成token」入口获取）。");
            return;
        }
        if (config.apiFullUrl().isEmpty()) {
            PanelSupport.append(logArea, "请先填写服务地址与接口路径。");
            return;
        }
        TokenInfo token = new TokenInfo(accessToken,
                securityKeyField.getText() == null ? "" : securityKeyField.getText().trim(), "");
        PanelSupport.append(logArea, "===== 发送接口 " + config.apiFullUrl()
                + " (secret_level=" + config.getSecretLevel() + ") =====");
        PanelSupport.runAsync(logArea, () -> {
            CallResult result = client.invokeApi(config, token);
            StringBuilder builder = new StringBuilder();
            builder.append("HTTP ").append(result.getStatus()).append('\n');
            builder.append("请求 URL：").append(result.getRequestUrl()).append('\n');
            builder.append("请求头：\n").append(result.getRequestHeaders()).append('\n');
            builder.append("请求体：\n").append(result.getRequestBody()).append('\n');
            builder.append("响应（已按 secret_level 还原）：\n")
                    .append(JsonUtils.prettyPrint(result.getResponseBody()));
            if (!result.getResponseBody().equals(result.getRawResponseBody())) {
                builder.append("\n\n原始响应：\n").append(result.getRawResponseBody());
            }
            String code = JsonUtils.findString(result.getResponseBody(), "code");
            String message = JsonUtils.findString(result.getResponseBody(), "message");
            if (JsonUtils.isFalse(result.getResponseBody(), "success")) {
                builder.append("\n\n服务端结果：").append(ServerHints.summarize(code, message));
                String hint = ServerHints.hintFor(message);
                if (!hint.isEmpty()) {
                    builder.append("\n提示：").append(hint);
                }
            }
            return builder.toString();
        });
    }

    private void loadFromSettings() {
        OpenApiConfig config = OpenApiSettings.getInstance().toConfig();
        baseUrlField.setText(config.getBaseUrl());
        apiUrlField.setText(config.getApiUrl());
        clientIdField.setText(config.getClientId());
        publicKeyArea.setText(config.getPublicKey());
        secretLevelBox.setSelectedItem(config.getSecretLevel());
        requestBodyArea.setText(config.getRequestBody());
        PanelSupport.append(logArea, "已从设置载入默认配置。");
    }

    private OpenApiConfig readConfig() {
        OpenApiConfig config = new OpenApiConfig();
        config.setBaseUrl(baseUrlField.getText().trim());
        config.setApiUrl(apiUrlField.getText().trim());
        config.setClientId(clientIdField.getText().trim());
        config.setPublicKey(publicKeyArea.getText());
        Object level = secretLevelBox.getSelectedItem();
        config.setSecretLevel(level == null ? OpenApiConfig.SecretLevel.LEVEL_0 : level.toString());
        config.setRequestBody(requestBodyArea.getText());
        return config;
    }
}
