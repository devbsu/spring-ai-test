package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.OpenAIImageEditResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AiService {
  private ChatClient chatClient;

  @Autowired
  private ImageModel imageModel;

  public AiService(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // 이미지 분석
  public Flux<String> imageAnalysis(String question, String contentType, byte[] bytes) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
              당신은 이미지 분석 전문가입니다.
              사용자 질문에 맞게 이미지를 분석하고 답변을 한국어로 하세요.
            """)
        .build();

    Resource resource = new ByteArrayResource(bytes);
    // 미디어 생성
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(resource)
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .media(media)
        .build();

    // LLM에 요청하고, 응답받기
    Flux<String> flux = chatClient.prompt()
        .messages(systemMessage, userMessage)
        .stream()
        .content();
    return flux;
  }

  // 한글을 영어로 번역
  private String koToEn(String str) {
    String translatedStr = chatClient.prompt()
        .system("당신은 번역사입니다. 사용자의 한글 질문을 영어 질문으로 변환시키세요.")
        .user(str)
        .call()
        .content();
    return translatedStr;
  }

  public String generateImage(String description) {
    String englishDescription = koToEn(description);

    // List<ImageMessage> listImageMessages = List.of();
    List<ImageMessage> imageMessageList = new ArrayList<>();
    ImageMessage imageMessage = new ImageMessage(englishDescription);
    imageMessageList.add(imageMessage);

    ImageOptions imageOptions = OpenAiImageOptions.builder()
        // .model("dall-e-3")
        .model("gpt-image-1")
        // .responseFormat("b64_json")
        .width(1024)
        .height(1024)
        .N(1)
        .build();

    ImagePrompt imagePrompt = new ImagePrompt(imageMessageList, imageOptions);
    ImageResponse imageResponse = imageModel.call(imagePrompt);
    String b64json = imageResponse.getResult().getOutput().getB64Json();

    return b64json;
  }

  // application.properties 사용
  @Value("${spring.ai.openai.api-key}")
  private String openAiApiKey;

  public String editImage(String description, byte[] originalImage, byte[] maskImage) {
    WebClient webClient = WebClient.builder()
        .baseUrl("https://api.openai.com/v1/images/edits")
        // .defaultHeader("Authorization", Bearer " + System.getenv("OPENAI_API_KEY"))
        .defaultHeader("Authorization", "Bearer " + openAiApiKey)
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(1024 * 1024 * 10))
            .build())
        .build();

    Resource originalResource = new ByteArrayResource(originalImage) {
      // .png 파일명만 주면 내부적으로 타입을 자동적으로 찾아준다.
      @Override
      public String getFilename() {
        return "original.png"; // baseUrl에 대한 가상의 파일명
      }
    };

    Resource maskResource = new ByteArrayResource(maskImage) {
      @Override
      public String getFilename() {
        return "mask.png";
      }
    };

    MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
    multiValueMap.add("model", "gpt-image-1");
    multiValueMap.add("image", originalResource); // 파일
    multiValueMap.add("mask", maskResource); // 파일
    multiValueMap.add("prompt", koToEn(description));
    multiValueMap.add("n", "1");
    multiValueMap.add("size", "1536x1024");
    multiValueMap.add("quality", "low");

    // {"data": [{"url": "xxxxx", "b64_json": "xxxxx"}, ... ]}
    Mono<OpenAIImageEditResponse> mono = webClient.post()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multiValueMap))
        .retrieve() // 요청하다
        // json을 OpenAIImageEditResponse 타입의 객체로 받겠다 -> Mono이기 때문에 비동기 처리
        .bodyToMono(OpenAIImageEditResponse.class);

    // 비동기 응답이 완료될때까지 기다리고 완료된 객체 얻기
    OpenAIImageEditResponse response = mono.block();
    // 데이터를 List에서 첫 번째 이미지를 json 데이터로 가져온다 -> ai가 생성한 첫 번째 이미지 결과를 문자열 형태로 추출하는 코드
    String b64Json = response.getData().get(0).getB64_json();

    return b64Json;
  }

}
