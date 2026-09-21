package com.yonyou.ncc.openapi.util;

/**
 * 极简 JSON 工具：只做取值与格式化，避免给插件引入额外依赖。
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    /** 取顶层或任意层级中第一个同名字段的字符串值，找不到返回 null。 */
    public static String findString(String json, String fieldName) {
        if (json == null || fieldName == null) {
            return null;
        }
        String needle = "\"" + fieldName + "\"";
        int index = json.indexOf(needle);
        while (index >= 0) {
            int cursor = index + needle.length();
            cursor = skipWhitespace(json, cursor);
            if (cursor < json.length() && json.charAt(cursor) == ':') {
                cursor = skipWhitespace(json, cursor + 1);
                if (cursor < json.length() && json.charAt(cursor) == '"') {
                    return readString(json, cursor + 1);
                }
            }
            index = json.indexOf(needle, index + needle.length());
        }
        return null;
    }

    /** 缩进格式化，格式化失败时原样返回。 */
    public static String prettyPrint(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        StringBuilder builder = new StringBuilder(json.length() + 64);
        int indent = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                builder.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            switch (c) {
                case '"':
                    inString = true;
                    builder.append(c);
                    break;
                case '{':
                case '[':
                    builder.append(c);
                    indent++;
                    newLine(builder, indent);
                    break;
                case '}':
                case ']':
                    indent = Math.max(0, indent - 1);
                    newLine(builder, indent);
                    builder.append(c);
                    break;
                case ',':
                    builder.append(c);
                    newLine(builder, indent);
                    break;
                case ':':
                    builder.append(": ");
                    break;
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.toString();
    }

    private static void newLine(StringBuilder builder, int indent) {
        builder.append('\n');
        for (int i = 0; i < indent; i++) {
            builder.append("  ");
        }
    }

    private static int skipWhitespace(String json, int cursor) {
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static String readString(String json, int cursor) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = cursor; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(unescape(c));
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return value.toString();
            }
            value.append(c);
        }
        return value.toString();
    }

    private static char unescape(char c) {
        switch (c) {
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            default:
                return c;
        }
    }
}

