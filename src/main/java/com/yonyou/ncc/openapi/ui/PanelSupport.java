package com.yonyou.ncc.openapi.ui;

import com.intellij.openapi.application.ApplicationManager;

import javax.swing.JTextArea;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 界面公共能力：日志输出与后台调用，避免三个入口各写一套。
 */
public final class PanelSupport {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private PanelSupport() {
    }

    /** 在后台线程执行调用，回到 EDT 输出日志。 */
    public static void runAsync(JTextArea logArea, BackgroundTask task) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String message;
            try {
                message = task.run();
            } catch (Exception e) {
                message = "调用失败：" + e.getClass().getSimpleName()
                        + (e.getMessage() == null ? "" : " - " + e.getMessage());
            }
            String text = message;
            ApplicationManager.getApplication().invokeLater(() -> append(logArea, text));
        });
    }

    public static void append(JTextArea logArea, String message) {
        logArea.append("[" + LocalTime.now().format(TIME_FORMAT) + "] " + message + "\n\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @FunctionalInterface
    public interface BackgroundTask {
        String run() throws Exception;
    }
}

