package com.yonyou.ncc.openapi.ui;

import com.yonyou.ncc.openapi.settings.DraftStore;

import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 把窗口里的输入框接到共享草稿：改一处 → 写进草稿 → 其余窗口的回灌。
 * 回灌时设置 updating 标记，避免"写回去又触发推送"的死循环。
 */
public final class FieldBinder {

    private final DraftStore store;
    private final Map<String, Supplier<String>> readers = new LinkedHashMap<>();
    private final Map<String, Consumer<String>> writers = new LinkedHashMap<>();
    private boolean updating;

    public FieldBinder(DraftStore store) {
        this.store = store;
    }

    public void bind(String key, JTextField field) {
        readers.put(key, field::getText);
        writers.put(key, value -> {
            if (!Objects.equals(field.getText(), value)) {
                field.setText(value);
            }
        });
        field.getDocument().addDocumentListener(new PushOnChange(key));
    }

    public void bindPassword(String key, JPasswordField field) {
        readers.put(key, () -> new String(field.getPassword()));
        writers.put(key, value -> {
            if (!Objects.equals(new String(field.getPassword()), value)) {
                field.setText(value);
            }
        });
        field.getDocument().addDocumentListener(new PushOnChange(key));
    }

    public void bindArea(String key, JTextArea area) {
        readers.put(key, area::getText);
        writers.put(key, value -> {
            if (!Objects.equals(area.getText(), value)) {
                area.setText(value);
            }
        });
        area.getDocument().addDocumentListener(new PushOnChange(key));
    }

    public void bindCombo(String key, JComboBox<String> box) {
        readers.put(key, () -> Objects.toString(box.getSelectedItem(), ""));
        writers.put(key, value -> {
            if (!Objects.equals(Objects.toString(box.getSelectedItem(), ""), value)) {
                box.setSelectedItem(value);
            }
        });
        box.addActionListener(e -> push(key));
    }

    /** 开始双向同步：先把草稿里的值灌进本窗口，再监听后续变化。 */
    public void start() {
        store.addListener(changedKey -> pullAll());
        pullAll();
    }

    /** 主动往外推一次（例如程序里改过字段后想让其他窗口同步）。 */
    public void pushAll() {
        readers.keySet().forEach(this::push);
    }

    private void push(String key) {
        if (updating) {
            return;
        }
        Supplier<String> reader = readers.get(key);
        if (reader != null) {
            store.set(key, reader.get());
        }
    }

    private void pullAll() {
        updating = true;
        try {
            writers.forEach((key, writer) -> writer.accept(store.get(key)));
        } finally {
            updating = false;
        }
    }

    private final class PushOnChange implements DocumentListener {

        private final String key;

        private PushOnChange(String key) {
            this.key = key;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            push(key);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            push(key);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            push(key);
        }
    }
}

