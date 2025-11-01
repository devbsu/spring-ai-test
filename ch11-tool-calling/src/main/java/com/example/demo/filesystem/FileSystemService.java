package com.example.demo.filesystem;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileSystemService {
  private ChatClient chatClient;

  @Autowired
  private FileSystemTools fileSystemTools;

  public FileSystemService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
    this.chatClient = chatClientBuilder
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .defaultSystem("""
              HTML과 CSS를 사용해서 들여쓰기가 된 답변으로 출력하세요.
              <div>에 들어가는 내용으로만 답변을 주세요. <h1>, <h2>, <h3>태그는 사용하지 마세요.
              파일, 디렉토리 관련 질문은 반드시 도구를 사용하세요.
            """)
        .build();
  }

  public String chat(String question, String conversationId) {
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(advisorSpec -> advisorSpec.param(
            ChatMemory.CONVERSATION_ID, conversationId))
        .tools(fileSystemTools)
        .call()
        .content();
    return answer;
  }
}
