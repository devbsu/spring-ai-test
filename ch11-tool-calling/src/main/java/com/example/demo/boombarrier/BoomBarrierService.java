package com.example.demo.boombarrier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BoomBarrierService {
  private ChatClient chatClient;

  @Autowired
  private CarCheckTools carCheckTools;

  @Autowired
  private BoomBarrierTools boomBarrierTools;

  public BoomBarrierService(ChatModel chatModel) {
    this.chatClient = ChatClient.builder(chatModel).build();
  }

  // 사진 형식: contentType
  public String chat(String contentType, byte[] bytes) {
    // 미디어 생성
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(new ByteArrayResource(bytes))
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text("""
                이미지에서 '(숫자 2개~3개)-(한글 1자)-(숫자 4개)'로 구성된
                차량 번호를 인식하세요. 예: 78라1234, 567바2558
                인식된 차량 번호가 등록된 차량 번호인지 도구로 확인을 하세요.
                등록된 번호라면 도구로 차단 봉을 올리고, 답변은 '차단기 올림'으로 하세요.
                등록된 번호가 아니라면 도구로 차단 봉을 내리고, 답변은 '차단기 내림'으로 하세요.
            """)
        .media(media)
        .build();

    // LLM으로 요청하고 응답받기
    String answer = chatClient.prompt()
        .messages(userMessage)
        .tools(carCheckTools, boomBarrierTools)
        .call()
        .content();
    return answer;
  }
}
