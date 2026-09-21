package com.yonyou.ncc.openapi.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 统一的表单排版，避免两个界面各写一套 GridBag 代码。
 */
public final class FormPanel extends JPanel {

    private int row;

    public FormPanel() {
        setLayout(new GridBagLayout());
    }

    public void addRow(String label, JComponent field) {
        addRow(label, field, false);
    }

    public void addRow(String label, JComponent field, boolean stretchVertical) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(4, 4, 4, 8);
        add(new JLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = stretchVertical
                ? GridBagConstraints.BOTH
                : GridBagConstraints.HORIZONTAL;
        if (stretchVertical) {
            fieldConstraints.weighty = 1;
        }
        fieldConstraints.insets = new Insets(4, 0, 4, 4);
        add(field, fieldConstraints);
        row++;
    }

    public void addFullWidth(JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(6, 4, 6, 4);
        add(component, constraints);
        row++;
    }

    public void addStretch(JComponent component, int height) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setPreferredSize(new Dimension(400, height));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(6, 4, 6, 4);
        add(scrollPane, constraints);
        row++;
    }

    public static JTextArea monoArea(int rows) {
        JTextArea area = new JTextArea(rows, 40);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    /** 把内容套进纵向滚动容器，窗口再小也能滚到最下面。 */
    public static JScrollPane scrollable(JComponent content) {
        JScrollPane pane = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(null);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
    }

    public static JTextField readOnlyField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return field;
    }

    public static JScrollPane scroll(JComponent component, int height) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setPreferredSize(new Dimension(560, height));
        return scrollPane;
    }
}
