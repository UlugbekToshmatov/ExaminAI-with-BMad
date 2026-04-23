package com.examinai.review;

import java.util.regex.Pattern;

final class LlmOutputSanitizer {

    private static final Pattern REDACTED_THINKING = Pattern.compile("(?s)<think>.*?</think>");
    /** DeepSeek / Ollama reasoning models often emit these before the JSON answer. */
    private static final Pattern THINK_BLOCK = Pattern.compile(
        "(?s)\u003cthink\u003e.*?\u003c/think\u003e");
    private static final Pattern MARKDOWN_JSON_FENCE = Pattern.compile("(?s)^\\s*```(?:json)?\\s*");
    private static final Pattern TRAILING_FENCE = Pattern.compile("(?s)\\s*```\\s*$");

    private LlmOutputSanitizer() {}

    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = REDACTED_THINKING.matcher(raw).replaceAll("");
        s = THINK_BLOCK.matcher(s).replaceAll("");
        s = MARKDOWN_JSON_FENCE.matcher(s).replaceFirst("");
        s = TRAILING_FENCE.matcher(s).replaceFirst("");
        return s.trim();
    }

    /**
     * When the model adds prose around JSON, take the first top-level {...} span (brace-balanced).
     */
    static String extractFirstJsonObject(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
