package com.example.demo;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.demo.tool.BoomBarrierTools;
import com.example.demo.tool.CarCheckTools;

@SpringBootApplication
public class DemoApplication {

	@Autowired
	private CarCheckTools carCheckTools;

	@Autowired
	private BoomBarrierTools boomBarrierTools;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider getToolCallbackProvider() {
		return MethodToolCallbackProvider.builder()
				.toolObjects(carCheckTools, boomBarrierTools)
				.build();
	}
}
