package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import com.example.demo.advisor.MaxCharLengthAdvisor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService3 {
    private ChatClient chatClient;

    public AiService3(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        new MaxCharLengthAdvisor(Ordered.HIGHEST_PRECEDENCE),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();
    }

    public String advisorLogging(String question) {
        String response = chatClient.prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(MaxCharLengthAdvisor.MAX_CHAR_LENGTH, 100))
                .call()
                .content();

        return response;
    }
}
