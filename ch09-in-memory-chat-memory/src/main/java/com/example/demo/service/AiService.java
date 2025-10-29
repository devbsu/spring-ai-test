package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {
    private ChatClient chatClient;

    /*
     * public AiService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory)
     * {
     * this.chatClient = chatClientBuilder
     * // 이전 대화 내용 프롬프트에 추가 (기본 20개)
     * .defaultAdvisors(MessageChatMemoryAdvisor
     * .builder(chatMemory)
     * .build(), new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
     * .build();
     * }
     */

    public AiService(ChatClient.Builder chatClientBuilder) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(100)
                .build();

        this.chatClient = chatClientBuilder
                // 이전 대화 내용 프롬프트에 추가 (기본 20개)
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .build(), new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();
    }

    public String chat(String question, String conversationId) {
        String answer = chatClient.prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return answer;
    }
}
