package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdvisorB implements CallAdvisor { 
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // +2 를 추가하면 AdvisorA 다음 순위로 지정된다.
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("[전처리]");
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        
        log.info("[후처리]");

        return response;
    }
}
