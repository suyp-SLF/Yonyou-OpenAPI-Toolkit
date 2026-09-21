package com.yonyou.ncc.openapi.ui;

import com.yonyou.ncc.openapi.model.OpenApiConfig;
import com.yonyou.ncc.openapi.model.TokenInfo;
import com.yonyou.ncc.openapi.service.OpenApiClient;
import com.yonyou.ncc.openapi.settings.OpenApiSettings;
import com.yonyou.ncc.openapi.util.JsonUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * 「生成token」独立入口：只负责调用 accesstoken 接口，不发送业务请求。
 */
public final class TokenPanel extends JPanel {

    private final OpenApiClient client = new OpenApiClient();

    private final JTextField baseUrlField = new JTextField();
    private final JTextField bizCenterField = new JTextField();
    private final JTextField clientIdField = new JTextField();
    private final JPasswordField clientSecretField = new JPasswordField();
    private final JTextArea publicKeyArea = FormPanel.monoArea(3);
    private final JComboBox<String> grantTypeBox = new JComboBox<>(
            new String[]{OpenApiConfig.GRANT_TYPE_CLIENT, OpenApiConfig.GRANT_TYPE_PASSWORD});
    private final JTextField userNameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField accessTokenField = FormPanel.readOnlyField();
    private final JTextField securityKeyField = FormPanel.readOnlyField();
    private final JTextArea responseArea = FormPanel.monoArea(10);

    public TokenPanel() {
        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.NORTH);
        grantTypeBox.addActionListener(e -> updateFieldState());
        updateFieldState();
    }

    private JPanel buildContent() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel hint = new JLabel("生成 token：调用 " + OpenApiConfig.TOKEN_PATH
                + "，client_secret 走 RSA(OAEP-SHA256) 加密，signature 由签名服务生成。");
        container.add(hint);
        container.add(Box.createVerticalStrut(6));

        FormPanel form = new FormPanel();
        form.addRow("服务地址(baseUrl)", baseUrlField);
        form.addRow("业务中心/账套编码(biz_center)", bizCenterField);
        form.addRow("应用编码(client_id)", clientIdField);
        form.addRow("应用密文(client_secret)", clientSecretField);
        form.addRow("公钥(publicKey)", FormPanel.scroll(publicKeyArea, 70));
        form.addRow("token 模式(grant_type)", grantTypeBox);
        form.addRow("用户名(username)", userNameField);
        form.addRow("密码(password)", passwordField);
        container.add(form);

        container.add(buttons());
        container.add(Box.createVerticalStrut(4));

        FormPanel result = new FormPanel();
        result.addRow("access_token", accessTokenField);
        result.addRow("security_key", securityKeyField);
        container.add(result);

        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("原始响应"));
        responseArea.setEditable(false);
        responsePanel.add(FormPanel.scroll(responseArea, 200), BorderLayout.CENTER);
        container.add(responsePanel);
        return container;
    }

    private JPanel buttons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));

        JButton generate = new JButton("生成 token");
        generate.addActionListener(e -> generate());
        panel.add(generate);

        JButton copyToken = new JButton("复制 access_token");
        copyToken.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(accessTokenField.getText() == null ? "" : accessTokenField.getText()), null));
        panel.add(copyToken);

        JButton load = new JButton("从设置载入");
        load.addActionListener(e -> loadFromSettings());
        panel.add(load);

        JButton clear = new JButton("清空");
        clear.addActionListener(e -> {
            accessTokenField.setText("");
            securityKeyField.setText("");
            responseArea.setText("");
        });
        panel.add(clear);
        return panel;
    }

    private void generate() {
        OpenApiConfig config = readConfig();
        String grantType = (String) grantTypeBox.getSelectedItem();
        PanelSupport.append(responseArea, "===== 生成 token (grant_type=" + grantType + ") =====");
        PanelSupport.runAsync(responseArea, () -> {
            TokenInfo token = client.fetchToken(config, grantType);
            accessTokenField.setText(token.getAccessToken());
            securityKeyField.setText(token.getSecurityKey());
            return "token 生成成功\ntoken 接口：" + config.tokenUrl()
                    + "\naccess_token：" + token.getAccessToken()
                    + "\nsecurity_key：" + token.getSecurityKey()
                    + "\n响应：\n" + JsonUtils.prettyPrint(token.getRawResponse());
        });
    }

    private void loadFromSettings() {
        OpenApiConfig config = OpenApiSettings.getInstance().toConfig();
        baseUrlField.setText(config.getBaseUrl());
        bizCenterField.setText(config.getBizCenter());
        clientIdField.setText(config.getClientId());
        clientSecretField.setText(config.getClientSecret());
        publicKeyArea.setText(config.getPublicKey());
        PanelSupport.append(responseArea, "已从设置载入默认配置。");
    }

    private OpenApiConfig readConfig() {
        OpenApiConfig config = new OpenApiConfig();
        config.setBaseUrl(baseUrlField.getText().trim());
        config.setBizCenter(bizCenterField.getText().trim());
        config.setClientId(clientIdField.getText().trim());
        config.setClientSecret(new String(clientSecretField.getPassword()));
        config.setPublicKey(publicKeyArea.getText());
        config.setUserName(userNameField.getText().trim());
        config.setPassword(new String(passwordField.getPassword()));
        return config;
    }

    private void updateFieldState() {
        boolean passwordMode = OpenApiConfig.GRANT_TYPE_PASSWORD.equals(grantTypeBox.getSelectedItem());
        userNameField.setEnabled(passwordMode);
        passwordField.setEnabled(passwordMode);
    }
}
