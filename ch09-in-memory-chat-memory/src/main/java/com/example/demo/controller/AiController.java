package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiService;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {
  @Autowired
  private AiService aiService;

  @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String inMemoryChatMemory(
      @RequestParam("question") String question, HttpSession session) {
    String answer = aiService.chat(question, session.getId());
    return answer;
  }
}
