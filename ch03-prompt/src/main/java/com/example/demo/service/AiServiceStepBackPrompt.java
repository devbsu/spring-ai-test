package com.example.demo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiServiceStepBackPrompt {
    public ChatClient chatClient;

    public AiServiceStepBackPrompt(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String stepBackPrompt2(String question) throws Exception {
        String answer = chatClient.prompt()
                .user(question)
                .call()
                .content();

        return answer;
    }

    public String stepBackPrompt(String question) throws Exception {
        // 질문들 -> 질문과 나눈다
        String questions = chatClient.prompt()
                .user("""
                        사용자 질문을 처리할때 Step-Back 프롬프트 기법을 사용하려고 합니다.
                        사용자 질문을 단계별 질문들로 재구성해주세요.
                        맨 마지막 질문은 사용자 질문과 일치해야 합니다.
                        단계별 질문을 항목으로 하는 JSON 배열로 출력해 주세요.
                        예시: ["...", "...", "...", ...]
                        사용자 질문: %s
                        """.formatted(question))

                // LLM으로 질문을 보내서 세부 질문으로 나눈다
                .call()
                .content();

        // ["...", "...", "...", ...] -> 대괄호 부분 추출하기 [...]
        String json = questions.substring(
                questions.indexOf("["),
                questions.indexOf("]") + 1);

        // [...] -> List<String>으로 변환하기
        // json을 자바 객체로 매핑 시켜준다: object mapper 역할
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> listQuestion = objectMapper.readValue(json, new TypeReference<List<String>>() {
        });

        log.info(listQuestion.toString());

        // json 외부 라이브러리
        // JSONArray jsonArray = new JSONArray(json);

        // 단계별 질문에 대한 답변을 얻고 다음 단계 질문에 포함시키기
        // 1단계, 배경지식(1단계 답변)+2단계, 배경지식(1단계+2단계 답변)+3단계 답변
        String[] answerArray = new String[listQuestion.size()];

        for (int i = 0; i < listQuestion.size(); i++) {
            String stepQuestion = listQuestion.get(i);
            String stepAnswer = getStepAnswer(stepQuestion, answerArray);
            answerArray[i] = stepAnswer;
        }
        // 마지막 질문 값
        String finalAnswer = answerArray[answerArray.length - 1];
        return finalAnswer;
    }

    public String getStepAnswer(String question, String... answerArray) {
        // 이전 답변글을 모두 contetxt로 누석
        String context = "";

        for (String answer : answerArray) {
            context += Objects.requireNonNullElse(answer, "");
            if (answer != null) {
                context += answer;
            }
        }

        // 현재 단계 질문에 대해 답변 얻기
        String answer = chatClient.prompt()
                .user("""
                        다음 question에 대한 답변을 context를 기반으로 답변해 주세요.
                        question: %s
                        context: %s
                        """.formatted(question, context))
                .call()
                .content();

        return answer;
    }
}
