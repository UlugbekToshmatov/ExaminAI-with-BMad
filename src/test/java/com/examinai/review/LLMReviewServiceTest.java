package com.examinai.review;

import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import static org.assertj.core.api.Assertions.assertThat;

class LLMReviewServiceTest {

    @Test
    void sanitizer_stripsThinkingAndFences() {
        String raw = "<think>secret</think>\n```json\n{\"verdict\":\"OK\",\"issues\":[]}\n```\n";
        assertThat(LlmOutputSanitizer.sanitize(raw)).isEqualTo("{\"verdict\":\"OK\",\"issues\":[]}");
    }

    @Test
    void beanOutputConverter_parsesReviewFeedback() {
        BeanOutputConverter<ReviewFeedback> converter = new BeanOutputConverter<>(ReviewFeedback.class);
        String json = """
            {"verdict":"NEEDS_WORK","issues":[{"line":10,"code":"a++","issue":"bad","improvement":"use a += 1"}]}
            """;
        ReviewFeedback rf = converter.convert(LlmOutputSanitizer.sanitize(json));
        assertThat(rf.verdict()).isEqualTo("NEEDS_WORK");
        assertThat(rf.issues()).hasSize(1);
        assertThat(rf.issues().get(0).line()).isEqualTo(10);
        assertThat(rf.issues().get(0).issue()).isEqualTo("bad");
    }

    @Test
    void sanitizer_handlesPlainJson() {
        assertThat(LlmOutputSanitizer.sanitize("{\"verdict\":\"X\",\"issues\":[]}")).isEqualTo("{\"verdict\":\"X\",\"issues\":[]}");
    }

    @Test
    void sanitizer_stripsThinkTags() {
        String raw = "\u003cthink\u003ereasoning\u003c/think\u003e\n{\"verdict\":\"OK\",\"issues\":[]}\n";
        assertThat(LlmOutputSanitizer.sanitize(raw)).isEqualTo("{\"verdict\":\"OK\",\"issues\":[]}");
    }

    @Test
    void extractFirstJsonObject_pullsObjectFromProse() {
        String prose = "Here you go: {\"verdict\":\"OK\",\"issues\":[]} trailing";
        assertThat(LlmOutputSanitizer.extractFirstJsonObject(prose)).isEqualTo("{\"verdict\":\"OK\",\"issues\":[]}");
    }
}
