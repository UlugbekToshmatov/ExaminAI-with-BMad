package com.examinai.review;

import java.util.regex.Pattern;

final class LlmOutputSanitizer {

    private static final Pattern REDACTED_THINKING = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern MARKDOWN_JSON_FENCE = Pattern.compile("(?s)^\\s*```(?:json)?\\s*");
    private static final Pattern TRAILING_FENCE = Pattern.compile("(?s)\\s*```\\s*$");

    private LlmOutputSanitizer() {}

    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = REDACTED_THINKING.matcher(raw).replaceAll("");
        s = MARKDOWN_JSON_FENCE.matcher(s).replaceFirst("");
        s = TRAILING_FENCE.matcher(s).replaceFirst("");
        return s.trim();
    }
}
