package com.example.demo.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

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
}
