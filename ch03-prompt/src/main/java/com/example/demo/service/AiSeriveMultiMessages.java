package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiSeriveMultiMessages {
    private ChatClient chatClient;

    public AiSeriveMultiMessages(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // chatMemory -> system message + user message + assistant message
    public String multiMessages(String question, List<Message> chatMemory) {
        SystemMessage systemMessage = SystemMessage.builder()
                .text("""
                            당신은 AI 도우미입니다.
                            제공되는 지난 대화 내용을 보고 우선적으로 답변해주세요.
                        """)
                .build();

        // 아무것도 저장되지 않았다면, 제일 첫 메시지로 딱 한번만 저장한다
        if (chatMemory.size() == 0) {
            chatMemory.add(systemMessage);
        }

        UserMessage userMessage = UserMessage.builder()
                .text(question)
                .build();

        chatMemory.add(userMessage);

        // LLM에게 요청하고 응답받기
        ChatResponse chatResponse = chatClient.prompt()
                .messages(chatMemory)
                .call()
                .chatResponse();

        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        chatMemory.add(assistantMessage);

        return assistantMessage.getText();
    }

}
