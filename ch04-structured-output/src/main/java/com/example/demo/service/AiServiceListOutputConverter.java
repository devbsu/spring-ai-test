package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiServiceListOutputConverter {
    private ChatClient chatClient;

    public AiServiceListOutputConverter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<String> listOutputConverterLowLevel(String city) {
        // 구조화된 출력 변환기 생성
        ListOutputConverter converter = new ListOutputConverter();

        // 프롬프트 템플릿 생성
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template("""
                        {city}에서 유명한 호텔 목록 5개를 다음 형식으로 출력하시오.
                         형식: {format}
                        """)
                .build();

        String json = chatClient.prompt()
                .user(promptTemplate.render(Map.of("city", city, "format", converter.getFormat())))
                .call()
                .content();

        List<String> hotels = converter.convert(json);
        return hotels;
    }

    public List<String> listOutputConverterHighLevel(String city) {
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template("""
                        {city}에서 유명한 호텔 목록 5개를 출력하시오.
                        """)
                .build();

        List<String> hotels = chatClient.prompt()
                .user(promptTemplate.render(Map.of("city", city)))
                .call()
                .entity(new ListOutputConverter());
        return hotels;
    }
}
