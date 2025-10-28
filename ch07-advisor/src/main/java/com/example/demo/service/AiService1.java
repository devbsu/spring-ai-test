package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.demo.advisor.AdvisorA;
import com.example.demo.advisor.AdvisorB;
import com.example.demo.advisor.AdvisorC;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService1 {
    private ChatClient chatClient;

    public AiService1(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
        .defaultAdvisors(new AdvisorA(), new AdvisorB()) // 공통 부분 (순서 상관 없음 getOrder의 우선순위)
        .build();
    }

    public String advisorChin1(String question) {
        String answer = chatClient.prompt()
                .user(question)
                .advisors(new AdvisorC()) // 객체 추가
                .call()
                .content(); // 동기 방식

        return answer;
    }
    public String advisorChin2(String question) {
        String answer = chatClient.prompt()
                .user(question)
                .call()
                .content();

        return answer;
    }
}
