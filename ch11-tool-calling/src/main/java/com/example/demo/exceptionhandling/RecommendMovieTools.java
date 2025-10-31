package com.example.demo.exceptionhandling;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component("recommendMovieTools2")
@Slf4j
public class RecommendMovieTools {

  @Tool(description = "사용자가 관람한 영화 목록을 제공합니다.")
  public List<String> getMovieListByUserId(
      @ToolParam(description = "사용자 ID", required = true) String userId) {
    throw new RuntimeException("사용자 ID가 존재하지 않습니다.");
  }

  @Tool(description = "주어진 쟝르의 추천 영화 목록을 제공합니다.", returnDirect = true)
  public List<String> recommendMovie(
      @ToolParam(description = "쟝르", required = true) String genre) {
    // 데이터베이스에서 검색해서 가져온 내용
    List<String> movies = List.of("크레이븐", "베놈", "메이드");
    return movies;
  }

}
