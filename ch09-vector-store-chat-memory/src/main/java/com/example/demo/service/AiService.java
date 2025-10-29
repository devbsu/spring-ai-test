package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiService {
    private ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder,
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {

        // 이미 만들어진 테이블이 있을 경우 테이블 별도 생성
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                // 테이블을 자동으로 만들것인지
                .initializeSchema(false)
                .schemaName("public")
                // 테이블명
                .vectorTableName("chat_memory_vector_store")
                .build();

        this.chatClient = chatClientBuilder
                // 만들어진 vector는 advisor가 사용한다
                .defaultAdvisors(VectorStoreChatMemoryAdvisor.builder(vectorStore)
                        .build(), new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();
    }

    public String chat(String userText, String conversationId) {
        String answer = chatClient.prompt()
                .user(userText)
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return answer;
    }
}
