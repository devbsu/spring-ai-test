package com.example.demo.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.stereotype.Service;

import com.example.demo.dto.Hotel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiServiceBeanOutputConverter {
    private ChatClient chatClient;

    public AiServiceBeanOutputConverter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Hotel beanOutputConverterLowLevel(String city) {
        // 구조화된 출력 변환기 생성
        BeanOutputConverter<Hotel> converter = new BeanOutputConverter<>(Hotel.class);

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

        Hotel hotels = converter.convert(json);

        return hotels;
    }

    public Hotel beanOutputConverterHighLevel(String city) {
        // 프롬프트 템플릿 생성
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template("{city}에서 유명한 호텔 목록 5개를 출력하시오.")
                .build();

        Hotel hotel = chatClient.prompt()
                // prompt template를 안 쓸 경우, 한 줄로 사용 가능하다.
                // .user("%s에서 유명한 호텔 목록 5개를 출력하시오,".formatted(city))
                .user(promptTemplate.render(Map.of("city", city)))
                .call()
                .entity(Hotel.class);

        return hotel;
    }
}
