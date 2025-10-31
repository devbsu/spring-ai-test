package com.example.demo.datetime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DateTimeService {
    private ChatClient chatClient;

    @Autowired
    private DateTimeTools dateTimeTools;

    public DateTimeService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .build();
    }

    public String chat(String question) {
        String answer = this.chatClient.prompt()
                .user(question)
                .tools(dateTimeTools)
                .call()
                .content();
        return answer;
    }
}
