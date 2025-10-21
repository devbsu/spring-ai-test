package com.example.demo.controller;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.UserMessage.Builder;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final OllamaChatModel chatModel;

    public AiController(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestParam("question") String question) {
        SystemMessage systemMessage = SystemMessage.builder()
                .text("사용자의 질문에 대해 친절하게 답변을 하세요.")
                .build();

        // 방법 1) --------------------------------------------------
        // 사용자 질문 부분
        UserMessage userMessage = UserMessage.builder()
                .text(question)
                .build();

        ChatOptions chatOptions = ChatOptions.builder()
                .model("gemma3:1b")
                .maxTokens(100)
                .temperature(1.0) 
                .build();

        Prompt prompt = Prompt.builder()
                .messages(systemMessage, userMessage)
                .chatOptions(chatOptions)
                .build();

        ChatResponse chatResponse = chatModel.call(prompt);
        log.info(chatResponse.toString());
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

        String answer = assistantMessage.getText();
        log.info("answer: " + answer);
        log.info("사용 토큰수: " + chatResponse.getMetadata().getUsage().getTotalTokens());

        /*
         * // 방법 2) --------------------------------------------------
         * //String answer = chatModel.call(question);
         * 
         * 
         * // 방법 3) --------------------------------------------------
         * UserMessage userMessage = UserMessage.builder()
         * .text(question)
         * .build();
         * 
         * String answer = chatModel.call(userMessage);
         */
        return answer;
    }

}
