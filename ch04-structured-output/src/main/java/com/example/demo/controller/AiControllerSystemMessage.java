package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ReviewClassification;
import com.example.demo.service.AiServiceSystemMessage;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerSystemMessage {
  @Autowired
  private AiServiceSystemMessage aiService;

  @PostMapping(value = "/system-message", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ReviewClassification beanOutputConverter(@RequestParam("review") String review) {
    ReviewClassification reviewClassification = aiService.classifyReview(review);
    return reviewClassification;
  }
}
