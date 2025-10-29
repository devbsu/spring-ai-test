package com.example.demo.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.FaceEmbedApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FaceService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private WebClient webClient;

    public FaceService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public float[] getFaceVector(MultipartFile mf) throws Exception {
        String fileName = mf.getOriginalFilename();
        String contentType = mf.getContentType();
        byte[] bytes = mf.getBytes();

        Resource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", resource);

        FaceEmbedApiResponse faceEmbedApiResponse = webClient.post()
                .uri("http://localhost:50001/get-face-vector")
                .body(BodyInserters.fromMultipartData(form))
                .retrieve()
                .bodyToMono(FaceEmbedApiResponse.class)
                .block();

        log.info("vector: {}", faceEmbedApiResponse.getVector());

        return faceEmbedApiResponse.getVector();
    }

    public void addFace(String personName, MultipartFile mf) throws Exception {
        float[] vector = getFaceVector(mf);

        // float[] -> String
        String strVector = Arrays.toString(vector).replace(" ", "");

        // SQL문 작성
        String sql = "INSERT INTO face_vector_store(content, embedding) VALUES (?, ?::vector)";

        // SQL문 실행
        // INSERT, UPDATE, DELETE 문을 실행할 때: update() 메소드 사용
        // SELECT문을 실행할 때: query() 메소드 사용
        jdbcTemplate.update(sql, personName, strVector);
    }

    public String findFace(MultipartFile mf) throws Exception {
        // 쿼리 이미지의 벡터 얻기
        float[] vector = getFaceVector(mf);
        // 벡터를 문자열로 변환
        String strVector = Arrays.toString(vector).replace(" ", "");

        // SQL문으로 유사도 직접 검색
        // <->: 유클리드 거리, <=>: 코사인 거리, 올림차순이 가장 가까운 거리
        String sql = """
                SELECT content, (embedding <-> ?::vector) as similarity
                FROM face_vector_store
                ORDER BY embedding <-> ?::vector
                LIMIT 3
                """;

        // 검색 결과를 출력해보기: 여러 개일 때 queryForList를 사용한다
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, strVector, strVector);
        for (Map<String, Object> map : list) {
            log.info("{} (L2 거리: {})", map.get("content"), map.get("similarity"));
        }

        // 검색 결과에서 거리가 가장 짧은 벡터의 유사도가 임계값 0.3 이상일 경우
        double similarity = (Double) list.get(0).get("similarity");
        if (similarity <= 0.3) {
            // 거리가 가장 짧은 사람의 이름 반환
            String personName = (String) list.get(0).get("content");

            return personName;
        } else {
            return "등록된 사람이 없습니다.";
        }

    }
}
