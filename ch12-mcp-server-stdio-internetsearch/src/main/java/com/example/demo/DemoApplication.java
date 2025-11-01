package com.example.demo;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.demo.tool.InternetSearchTools;

@SpringBootApplication
public class DemoApplication {
  @Autowired
  private InternetSearchTools internetSearchTools;

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Bean
  public ToolCallbackProvider getToolCallbackProvider() {
    return MethodToolCallbackProvider.builder()
            .toolObjects(internetSearchTools)
            .build();
  }
}
