package com.example.demo.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AiService {
    @Autowired
    private OllamaChatModel chatModel;

    public String generateText(String question) {
        SystemMessage systemMessage = SystemMessage.builder()
                .text("사용자의 질문에 대해 친절하게 답변을 하세요.")
                .build();

        // 사용자 질문 부분
        UserMessage userMessage = UserMessage.builder()
                .text(question)
                .build();

        ChatOptions chatOptions = ChatOptions.builder()
                // .model("gpt-4o-mini")
                .model("gemma3:1b")
                // .maxTokens(100)
                .temperature(1.0)
                .build();

        Prompt prompt = Prompt.builder()
                .messages(systemMessage, userMessage)
                .chatOptions(chatOptions)
                .build();

        ChatResponse chatResponse = chatModel.call(prompt);
        log.info(chatResponse.toString());
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

        String answer = assistantMessage.getText();
        log.info("answer: " + answer);
        log.info("사용 토큰수: " + chatResponse.getMetadata().getUsage().getTotalTokens());

        return answer;
    }

    public Flux<String> generateStreamText(String question) {
        SystemMessage systemMessage = SystemMessage.builder()
                .text("사용자의 질문에 대해 친절하게 답변을 하세요.")
                .build();

        // 사용자 질문 부분
        UserMessage userMessage = UserMessage.builder()
                .text(question)
                .build();

        ChatOptions chatOptions = ChatOptions.builder()
                .model("gemma3:1b")
                .temperature(1.0)
                .build();

        Prompt prompt = Prompt.builder()
                .messages(systemMessage, userMessage)
                .chatOptions(chatOptions)
                .build();

        Flux<ChatResponse> fluxChatResponse = chatModel.stream(prompt);

        Flux<String> fluxString = fluxChatResponse.map(chatResponse -> {
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String str = assistantMessage.getText();
            return str;
        });

        // Flux<String> fluxString = chatModel.stream(systemMessage, userMessage);

        return fluxString;
    }

}
