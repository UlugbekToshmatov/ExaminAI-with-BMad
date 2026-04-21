package com.examinai.review;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class LLMReviewService {

    private final ChatModel chatModel;
    private final PromptTemplate promptTemplate;
    private final BeanOutputConverter<ReviewFeedback> converter = new BeanOutputConverter<>(ReviewFeedback.class);

    public LLMReviewService(ChatModel chatModel, @Value("classpath:prompts/review-diff.st") Resource promptResource) {
        this.chatModel = chatModel;
        this.promptTemplate = new PromptTemplate(promptResource);
    }

    public ReviewFeedback review(String taskDescription, String prDiff) {
        Prompt prompt = promptTemplate.create(Map.of(
            "taskDescription", taskDescription == null ? "" : taskDescription,
            "diff", prDiff == null ? "" : prDiff,
            "format", converter.getFormat()
        ));
        String raw = chatModel.call(prompt).getResult().getOutput().getText();
        String cleaned = LlmOutputSanitizer.sanitize(raw);
        ReviewFeedback feedback = converter.convert(cleaned);
        if (feedback == null) {
            throw new IllegalStateException("LLM output could not be parsed into ReviewFeedback: " + cleaned);
        }
        return feedback;
    }
}
