package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdvisorC implements CallAdvisor { 
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // A, B, C중에 우선순위가 가장 낮다
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("[전처리]");
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        
        log.info("[후처리]");

        return response;
    }
}
