package com.example.demo.boombarrier;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BoomBarrierTools {

  @Tool(description = "차단 봉을 올립니다.")
  public void boomBarrierUp() {
    log.info("차단 봉을 올립니다.");
  }

  @Tool(description = "차단 봉을 내립니다.")
  public void boomBarrierDown() {
    log.info("차단 봉을 내립니다.");
  }
}
