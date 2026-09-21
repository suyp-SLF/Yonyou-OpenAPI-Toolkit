package com.yonyou.ncc.openapi.ui;

import com.yonyou.ncc.openapi.model.OpenApiConfig;
import com.yonyou.ncc.openapi.crypto.PublicKeyCheck;
import com.yonyou.ncc.openapi.model.SignResult;
import com.yonyou.ncc.openapi.service.SignService;
import com.yonyou.ncc.openapi.settings.DraftStore;
import com.yonyou.ncc.openapi.settings.OpenApiDraft;
import com.yonyou.ncc.openapi.settings.OpenApiSettings;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * 「开放API签名」独立入口：只做签名计算，不发起任何请求，也不依赖联调界面的状态。
 */
public final class SignPanel extends JPanel {

    private static final String MODE_LOGIN = "登录签名：client_id + client_secret + 公钥";
    private static final String MODE_PASSWORD = "用户名密码签名：client_id + client_secret + 用户名 + 密码 + 公钥";
    private static final String MODE_API = "接口签名：client_id + 请求体 + 公钥";
    private static final String MODE_CUSTOM = "自定义原文：原文 + 盐值";

    private final SignService signService = new SignService();

    private final JComboBox<String> modeBox = new JComboBox<>(
            new String[]{MODE_LOGIN, MODE_PASSWORD, MODE_API, MODE_CUSTOM});
    private final JTextField clientIdField = new JTextField();
    private final JPasswordField clientSecretField = new JPasswordField();
    private final JTextField userNameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextArea publicKeyArea = FormPanel.monoArea(3);
    private final JTextArea requestBodyArea = FormPanel.monoArea(3);
    private final JTextArea customTextArea = FormPanel.monoArea(3);
    private final JTextField signField = FormPanel.readOnlyField();
    private final JTextField saltField = FormPanel.readOnlyField();
    private final JTextArea signedTextField = FormPanel.monoArea(2);
    private final JTextArea cipherArea = FormPanel.monoArea(3);

    public SignPanel() {
        setLayout(new BorderLayout());
        add(FormPanel.scrollable(buildContent()), BorderLayout.CENTER);
        modeBox.addActionListener(e -> updateFieldState());
        updateFieldState();
        bindSharedFields();
    }

    /** 与「生成token」「发送接口」共用一份参数：改一处，其余窗口自动同步。 */
    private void bindSharedFields() {
        FieldBinder binder = new FieldBinder(OpenApiDraft.getInstance().store());
        binder.bind(DraftStore.CLIENT_ID, clientIdField);
        binder.bindPassword(DraftStore.CLIENT_SECRET, clientSecretField);
        binder.bind(DraftStore.USER_NAME, userNameField);
        binder.bindPassword(DraftStore.PASSWORD, passwordField);
        binder.bindArea(DraftStore.PUBLIC_KEY, publicKeyArea);
        binder.bindArea(DraftStore.REQUEST_BODY, requestBodyArea);
        binder.start();
    }

    private JPanel buildContent() {
        JPanel container = new ScrollablePanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel hint = new JLabel("<html>独立签名入口(v" + PluginInfo.version()
                + ")：signature = SHA256(原文 + 盐值)，盐值由公钥派生，<b>签名是固定值</b>；"
                + "<br/>顺带给出手工调接口用的 client_secret 密文（RSA-OAEP 每次不同，任意一个都能用、可复用）。"
                + "<br/><b>三个窗口共用参数</b>：client_id / 应用密文 / 公钥 / 请求体在本窗口改动，会同步到另外两个窗口。</html>");
        hint.setAlignmentX(LEFT_ALIGNMENT);
        container.add(hint);
        container.add(Box.createVerticalStrut(6));

        FormPanel form = new FormPanel();
        form.setAlignmentX(LEFT_ALIGNMENT);
        form.addRow("签名类型", modeBox);
        form.addRow("应用编码(client_id)", clientIdField);
        form.addRow("应用密文(client_secret)", clientSecretField);
        form.addRow("用户名(username)", userNameField);
        form.addRow("密码(password)", passwordField);
        form.addRow("公钥(publicKey)", FormPanel.scroll(publicKeyArea, 56));
        form.addRow("请求体(请求签名用)", FormPanel.scroll(requestBodyArea, 56));
        form.addRow("自定义原文", FormPanel.scroll(customTextArea, 56));
        container.add(form);

        container.add(buttons());
        container.add(Box.createVerticalStrut(6));
        container.add(resultPanel());
        return container;
    }

    private JPanel buttons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JButton compute = new JButton("计算签名");
        compute.addActionListener(e -> compute());
        panel.add(compute);

        JButton copy = new JButton("复制签名");
        copy.addActionListener(e -> copySign());
        panel.add(copy);

        JButton copyCipher = new JButton("复制密文");
        copyCipher.addActionListener(e -> copyCipher());
        panel.add(copyCipher);

        JButton checkKey = new JButton("校验公钥");
        checkKey.addActionListener(e -> JOptionPane.showMessageDialog(this,
                PublicKeyCheck.describe(publicKeyArea.getText()), "公钥体检", JOptionPane.INFORMATION_MESSAGE));
        panel.add(checkKey);

        JButton load = new JButton("从设置载入");
        load.addActionListener(e -> loadFromSettings());
        panel.add(load);

        JButton clear = new JButton("清空");
        clear.addActionListener(e -> clear());
        panel.add(clear);
        return panel;
    }

    private JPanel resultPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createTitledBorder("签名结果"));

        FormPanel form = new FormPanel();
        form.addRow("signature", signField);
        form.addRow("盐值(salt)", saltField);
        form.addRow("参与签名的原文", FormPanel.scroll(signedTextField, 60));
        form.addRow("client_secret 密文(手工请求用)", FormPanel.scroll(cipherArea, 60));
        signedTextField.setEditable(false);
        cipherArea.setEditable(false);
        panel.add(form);
        return panel;
    }

    private void compute() {
        try {
            String clientId = trimmed(clientIdField);
            String publicKey = publicKeyArea.getText();
            String mode = (String) modeBox.getSelectedItem();
            SignResult result;
            if (MODE_PASSWORD.equals(mode)) {
                result = signService.passwordSign(clientId, secret(), trimmed(userNameField),
                        new String(passwordField.getPassword()), publicKey);
            } else if (MODE_API.equals(mode)) {
                result = signService.apiSign(clientId, requestBodyArea.getText(), publicKey);
            } else if (MODE_CUSTOM.equals(mode)) {
                result = signService.sign(customTextArea.getText(), publicKey);
            } else {
                result = signService.loginSign(clientId, secret(), publicKey);
            }
            signField.setText(result.getSign());
            saltField.setText(result.getSalt());
            signedTextField.setText(result.getSignedText());
            try {
                cipherArea.setText(cipherText(mode));
            } catch (Exception cipherError) {
                // 密文失败不影响签名结果展示，单独把原因写在密文栏
                cipherArea.setText(cipherError.getMessage());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "签名计算失败：" + ex.getMessage(),
                    "Yonyou OpenAPI", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copySign() {
        String sign = signField.getText();
        if (sign == null || sign.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先计算签名", "Yonyou OpenAPI", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sign), null);
    }

    private void copyCipher() {
        String cipher = cipherArea.getText();
        if (cipher == null || cipher.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "当前模式没有 client_secret 密文可复制（只有登录签名、用户名密码签名模式会生成）",
                    "Yonyou OpenAPI", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(cipher), null);
    }

    /** 只有用到 client_secret 的模式才给密文；插件自己发请求时会重新加密。 */
    private String cipherText(String mode) {
        if (MODE_API.equals(mode) || MODE_CUSTOM.equals(mode)) {
            return "";
        }
        return signService.clientSecretCipher(secret(), publicKeyArea.getText());
    }

    private void loadFromSettings() {
        OpenApiConfig config = OpenApiSettings.getInstance().toConfig();
        clientIdField.setText(config.getClientId());
        clientSecretField.setText(config.getClientSecret());
        publicKeyArea.setText(config.getPublicKey());
        requestBodyArea.setText(config.getRequestBody());
    }

    private void clear() {
        signField.setText("");
        saltField.setText("");
        signedTextField.setText("");
        cipherArea.setText("");
        customTextArea.setText("");
    }

    private void updateFieldState() {
        String mode = (String) modeBox.getSelectedItem();
        boolean passwordMode = MODE_PASSWORD.equals(mode);
        boolean apiMode = MODE_API.equals(mode);
        boolean customMode = MODE_CUSTOM.equals(mode);
        clientIdField.setEnabled(!customMode);
        clientSecretField.setEnabled(!customMode && !apiMode);
        userNameField.setEnabled(passwordMode);
        passwordField.setEnabled(passwordMode);
        requestBodyArea.setEnabled(apiMode);
        customTextArea.setEnabled(customMode);
    }

    private String secret() {
        return new String(clientSecretField.getPassword());
    }

    private static String trimmed(JTextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
