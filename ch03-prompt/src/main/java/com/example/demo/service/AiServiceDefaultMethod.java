package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AiServiceDefaultMethod {
    private ChatClient chatClient;

    public AiServiceDefaultMethod(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                // chat client를 사용하면 기본적으로 적용된다
                .defaultSystem("적절한 감탄사, 웃음을 넣어서 친절하게 대화를 해주세요.")
                .defaultOptions(ChatOptions.builder()
                        .temperature(1.0)
                        .maxTokens(300)
                        .build())
                .build();
    }

    public Flux<String> defaultMethod(String question) {
        Flux<String> fluxString = chatClient.prompt()
                .user(question)
                .stream()
                .content();
        return fluxString;
    }
}
