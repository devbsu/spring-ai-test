package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdvisorA implements CallAdvisor { // 동기 방식
    // bean이 아닌 logging용 클래스

    @Override
    public String getName() {
        // 현재 클래스 이름 return "AdvisorA";으로 사용해도 된다
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // 가장 우선 순위가 높다. +1을 추가하면 우선 순위의 다음 순위로 지정한다
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 전처리: nextCall(다음 advisor로 넘어가라)이 전처리이고, nextCall 이후는 후처리이다.
        log.info("[전처리]");
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        
        // 후처리
        log.info("[후처리]");

        return response;
    }
}
