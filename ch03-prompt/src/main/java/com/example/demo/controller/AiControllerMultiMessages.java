package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiSeriveMultiMessages;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiControllerMultiMessages {
        @Autowired
        private AiSeriveMultiMessages aiService;

        @PostMapping(value = "/multi-messages", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
        public String multiMessages(@RequestParam("question") String question, HttpSession session) {
                // chatMemory로 저장된 대화 기억이 있으면 가져온다
                List<Message> chatMemory = (List<Message>) session.getAttribute("chatMemory");
                if (chatMemory == null) {
                        chatMemory = new ArrayList<>();
                        session.setAttribute("chatMemory", chatMemory);
                }

                String answer = aiService.multiMessages(question, chatMemory);
                return answer;
        }
}
