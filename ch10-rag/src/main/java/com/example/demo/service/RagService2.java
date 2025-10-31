package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RagService2 {
  private ChatClient chatClient;

  @Autowired
  private ChatModel chatModel;

  @Autowired
  private ChatMemory chatMemory;

  @Autowired
  private VectorStore vectorStore;

  // 생성자의 chatClientBuilder는 최종 답변을 쓰는 목적
  public RagService2(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
        .build();
  }

  // 검색전 모듈 생성 메소드
  private CompressionQueryTransformer createCompressionQueryTransformer() {
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
        .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));
    CompressionQueryTransformer cqt = CompressionQueryTransformer.builder()
        // 완전한 질문을 만들기 위해 별도로 chatClientBuilder 생성
        .chatClientBuilder(chatClientBuilder)
        .build();

    return cqt;
  }

  // 검색 모듈 생성 메소드
  // vector store에 유사도 검색 수행하는 모듈
  private VectorStoreDocumentRetriever createVectorStoreDocumentRetriever(double score, String source) {
    VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
        .vectorStore(vectorStore)
        .similarityThreshold(score) // 0~1 사이
        .topK(3)
        .filterExpression(() -> {
          FilterExpressionBuilder builder = new FilterExpressionBuilder();
          // 출처 안에서만 검색한다: 전체 검색보다 유사도 검사가 빠르다
          if (source != null && !source.equals("")) {
            return builder.eq("source", source).build();
          } else {
            return null;
          }
        })
        .build();

    return retriever;
  }

  // question: 불완전한 질문, score: 유사도, conversationId: 대화기억
  public String chatWithCompression(String question, double score, String source, String conversationId) {
    // 어떤 모듈들을 포함시킬 것인지
    // 사용자의 모호한 질문을 완전한 질문으로 바꾸는 작업 -> 이미 이전 대화 내용이 들어가있다
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
        // vector db 검색 전 모듈 추가
        .queryTransformers(createCompressionQueryTransformer())
        // vector db 검색 모듈 추가
        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
        .build();

    // advisor는 총 3개를 사용하고 있다. 우선순위:
    String answer = chatClient.prompt()
        .user(question)
        .advisors(
            // 대화 기억 유지
            MessageChatMemoryAdvisor.builder(chatMemory).build(),
            retrievalAugmentationAdvisor

        )
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();

    return answer;
  }

  // -------------------------------------------------------------------------------
  // ##### RewriteQueryTransformer 생성하고 반환하는 메소드 #####
  private RewriteQueryTransformer createRewriteQueryTransformer() {
    // 새로운 ChatClient 생성하는 빌더 생성
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
        .defaultAdvisors(
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

    // 질문 재작성기 생성
    RewriteQueryTransformer rewriteQueryTransformer = RewriteQueryTransformer.builder()
        .chatClientBuilder(chatClientBuilder)
        .build();

    return rewriteQueryTransformer;
  }

  public String chatWithRewriteQuery(String question, double score, String source) {
    // RetrievalAugmentationAdvisor 생성
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
        .queryTransformers(createRewriteQueryTransformer())
        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
        .build();

    // 프롬프트를 LLM으로 전송하고 응답을 받는 코드
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(retrievalAugmentationAdvisor)
        .call()
        .content();
    return answer;
  }

  // -------------------------------------------------------------------------------
  // ##### TranslationQueryTransformer 생성하고 반환하는 메소드 #####
  private TranslationQueryTransformer createTranslationQueryTransformer() {
    // 새로운 ChatClient를 생성하는 빌더 생성
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
        .defaultAdvisors(
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

    // 질문 번역기 생성
    TranslationQueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
        .chatClientBuilder(chatClientBuilder)
        .targetLanguage("korean")
        .build();

    return translationQueryTransformer;
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chatWithTranslation(String question, double score, String source) {
    // RetrievalAugmentationAdvisor 생성
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
        .queryTransformers(createTranslationQueryTransformer())
        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
        .build();

    // 프롬프트를 LLM으로 전송하고 응답을 받는 코드
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(retrievalAugmentationAdvisor)
        .call()
        .content();
    return answer;
  }

  // -------------------------------------------------------------------------------
  // ##### MultiQueryExpander 생성하고 반환하는 메소드 #####
  private MultiQueryExpander createMultiQueryExpander() {
    // 새로운 ChatClient 빌더 생성
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
        .defaultAdvisors(
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

    // 질문 확장기 생성
    MultiQueryExpander multiQueryExpander = MultiQueryExpander.builder()
        .chatClientBuilder(chatClientBuilder)
        .includeOriginal(true)
        .numberOfQueries(3)
        .build();

    return multiQueryExpander;
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chatWithMultiQuery(String question, double score, String source) {
    // RetrievalAugmentationAdvisor 생성
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
        .queryExpander(createMultiQueryExpander())
        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
        .build();

    // 프롬프트를 LLM으로 전송하고 응답을 받는 코드
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(retrievalAugmentationAdvisor)
        .call()
        .content();
    return answer;
  }

}