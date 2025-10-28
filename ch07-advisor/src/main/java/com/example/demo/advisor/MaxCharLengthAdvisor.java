package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxCharLengthAdvisor implements CallAdvisor {
    private int order;
    private int maxCharLength = 300;
    // 기본 값을 300이지만, MAX_CHAR_LENGTH 상수를 공유 객체의 키 값으로 사용한다
    public static final String MAX_CHAR_LENGTH = "maxCharLength";

    public MaxCharLengthAdvisor(int order) {
        this.order = order;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // order 값은 정해져있지 않다
        return order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // chatClientRequest는 변경이 안되지만.. 새로운 chatClientRequest를 얻는다
        // 전처리
        ChatClientRequest mutatedRequest = augmentPrompt(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(mutatedRequest);

        // 후처리 없음
        return chatClientResponse;
    }

    private ChatClientRequest augmentPrompt(ChatClientRequest chatClientRequest) {
        // this.maxCharLength는 300을 갖고있다
        String userText = this.maxCharLength + "자 이내로 답변해 주세요.";

        // chatClientRequest.context() 공유 객체에 .get(MAX_CHAR_LENGTH); 해당 키를 갖고온다
        Integer maxCharLength = (Integer) chatClientRequest.context().get(MAX_CHAR_LENGTH);

        // maxCharLength가 null이 아니라면
        if (maxCharLength != null) {
            userText = maxCharLength + "자 이내로 답변해 주세요.";
        }

        // 자바에는 로컬 변수를 중첩된 클래스에서 변경할 수 없다
        String finalUserText = userText;

        Prompt prevPrompt = chatClientRequest.prompt();
        Prompt newPrompt = prevPrompt.augmentUserMessage(userMessage -> {
            return UserMessage.builder()
                    // 기존 userMessage에 finalUserText를 추가한다
                    .text(userMessage.getText() + "\n" + finalUserText)
                    .build();
        });

        ChatClientRequest newChatClientRequest = chatClientRequest.mutate()
                .prompt(newPrompt)
                .build();

        return newChatClientRequest;
    }

}
