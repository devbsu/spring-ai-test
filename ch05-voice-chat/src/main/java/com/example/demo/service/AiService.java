package com.example.demo.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.AudioParameters;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AiService {
    private ChatClient chatClient;
    private OpenAiAudioTranscriptionModel audioTranscriptionModel;
    private OpenAiAudioSpeechModel audioSpeechModel;

    public AiService(ChatClient.Builder chatClientBuilder,
            OpenAiAudioTranscriptionModel audioTranscriptionModel,
            OpenAiAudioSpeechModel audioSpeechModel) {
        this.chatClient = chatClientBuilder.build();
        this.audioTranscriptionModel = audioTranscriptionModel;
        this.audioSpeechModel = audioSpeechModel;
    }

    public String stt(String fileName, byte[] bytes) {
        Resource audioResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .model("whisper-1")
                .language("ko") // en
                .build();

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

        AudioTranscriptionResponse response = audioTranscriptionModel.call(prompt);
        String text = response.getResult().getOutput();

        return text;
    }

    public byte[] tts(String text) {
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .model("gpt-4o-mini-tts")
                .voice(SpeechRequest.Voice.ALLOY)
                .speed(1.0f)
                .responseFormat(SpeechRequest.AudioResponseFormat.MP3)
                .build();

        SpeechPrompt speechPrompt = new SpeechPrompt(text, speechOptions);

        SpeechResponse speechResponse = audioSpeechModel.call(speechPrompt);
        byte[] bytes = speechResponse.getResult().getOutput();

        return bytes;
    }

    public Map<String, Object> chatText(String question) {
        Assert.hasText(question, "질문이 비어 있습니다.");

        // LLM 사용
        String textAnswer = chatClient.prompt()
                .system("50자 이내로 한국어로 답변해주세요.")
                .user(question)
                .call()
                .content();

        // TTS 사용
        byte[] audio = tts(textAnswer);
        String base64Audio = Base64.getEncoder().encodeToString(audio);

        // Map에 담기
        Map<String, Object> map = new HashMap<>();
        map.put("text", textAnswer);
        map.put("audio", base64Audio);

        return map;
    }

    public Flux<byte[]> ttsFlux(String text) {
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .model("gpt-4o-mini-tts")
                .voice(SpeechRequest.Voice.ALLOY)
                .speed(1.0f)
                .responseFormat(SpeechRequest.AudioResponseFormat.MP3)
                .build();

        SpeechPrompt speechPrompt = new SpeechPrompt(text, speechOptions);

        Flux<SpeechResponse> fluxSpeechResponse = audioSpeechModel.stream(speechPrompt);
        Flux<byte[]> fluxBSytes = fluxSpeechResponse.map(speechResponse -> speechResponse.getResult().getOutput());

        return fluxBSytes;
    }

    public Flux<byte[]> chatVoiceSttLlmTts(byte[] audioBytes) {
        // 음성 -> STT -> 텍스트
        String textQuestion = stt("speech.mp3", audioBytes);
        // 텍스트 -> LLM -> 텍스트
        String textAnswer = chatClient.prompt()
                .system("50자 이내로 답변해주세요.")
                .user(textQuestion)
                .call()
                .content();

        // 텍스트 -> TTS -> 음성
        Flux<byte[]> flux = ttsFlux(textAnswer);

        return flux;
    }

    public byte[] chatVoiceOneModel(byte[] audioBytes, String contentType) {
        Resource resource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "speech.mp3";
            }
        };

        UserMessage userMessage = UserMessage.builder()
                .text("제공되는 음성에 맞는 자연스러운 대화로 이어주세요.")
                .media(new Media(MimeType.valueOf(contentType), resource))
                .build();

        ChatOptions chatOptions = OpenAiChatOptions.builder()
                // .model("gpt-4o-mini-audio")
                .model(OpenAiApi.ChatModel.GPT_4_O_AUDIO_PREVIEW)
                .outputModalities(List.of("text", "audio"))
                // .outputAudio(new AudioParameters(Voice.ALLOY, AudioResponseFormat.MP3))
                .outputAudio(new AudioParameters(
                        ChatCompletionRequest.AudioParameters.Voice.ALLOY,
                        ChatCompletionRequest.AudioParameters.AudioResponseFormat.MP3))
                .build();

        ChatResponse response = chatClient.prompt()
                .system("50자 이내로 답변해주세요.")
                .messages(userMessage)
                .options(chatOptions)
                .call()
                .chatResponse(); // content()는 String을 받아야 사용 가능하다

        AssistantMessage assistantMessage = response.getResult().getOutput();
        String textAnswer = assistantMessage.getText();
        log.info("textAnswer: ", textAnswer);
        byte[] audioAnswer = assistantMessage.getMedia().get(0).getDataAsByteArray();

        return audioAnswer;
    }
}
