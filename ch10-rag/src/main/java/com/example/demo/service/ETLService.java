package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ETLService {

    @Autowired
    private VectorStore vectorStore;

    public String etlFromFile(String title, String author, MultipartFile attach) throws Exception {
        // E: 추출(텍스트를 Docume)
        List<Document> documents = extractFormFile(attach);

        // metadata 없기때문에 쪼개기 전에 미리 추가
        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            metadata.put("title", title);
            metadata.put("author", author);
            metadata.put("source", attach.getOriginalFilename());
        }

        // T: 변환 (잘게 쪼개는 과정) ->
        List<Document> splitted_documents = transForm(documents);

        // L: 적재 (VectorStore에 저장)
        vectorStore.add(splitted_documents);

        return "%d개를 %d로 쪼개어 Vector Store에 저장".formatted(documents.size(), splitted_documents.size());
    }

    private List<Document> extractFormFile(MultipartFile attach) throws Exception {
        String fileName = attach.getOriginalFilename();
        String contentType = attach.getContentType();
        log.info("contentType: {}", contentType);
        byte[] bytes = attach.getBytes();

        Resource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        List<Document> documents = new ArrayList<>();

        if (contentType.equals("text/plain")) {
            DocumentReader reader = new TextReader(resource);
            documents = reader.read();
        } else if (contentType.equals("application/pdf")) {
            DocumentReader reader = new PagePdfDocumentReader(resource);
            documents = reader.read();
        } else if (contentType.contains("word")) {
            DocumentReader reader = new TikaDocumentReader(resource);
            documents = reader.read();
        }

        log.info("추출된 Document 수: {}", documents.size());
        return documents;
    }

    private List<Document> transForm(List<Document> documents) {
        // 기본값 800 토큰
        // TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(200, 50, 0, 10000, true);
        List<Document> splitted_docs = tokenTextSplitter.apply(documents);
        return splitted_docs;
    }

    public String etlFromHtml(String title, String author, String url) throws Exception {
        Resource resource = new UrlResource(url);

        JsoupDocumentReader reader = new JsoupDocumentReader(resource,
                JsoupDocumentReaderConfig.builder()
                        .charset("UTF-8")
                        .selector("#content")
                        .additionalMetadata(Map.of(
                                "title", title,
                                "author", author,
                                "url", url)) // 3개의 메타데이터를 추가한다
                        .build());

        // E
        List<Document> documents = reader.read();

        // T
        DocumentTransformer transformer = new TokenTextSplitter(
                200, 50, 0, 10000, false);
        List<Document> splitted_documents = transformer.apply(documents);

        // L
        vectorStore.add(splitted_documents);

        return "%d개를 %d로 쪼개어 Vector Store에 저장".formatted(documents.size(), splitted_documents.size());
    }

    public String etlFromJson(String url) throws Exception {
        Resource resource = new UrlResource(url);

        JsonReader reader = new JsonReader(resource, new JsonMetadataGenerator() {

            @Override
            public Map<String, Object> generate(Map<String, Object> jsonMap) {
                return Map.of(
                        "title", jsonMap.get("title"),
                        "author", jsonMap.get("author"),
                        "url", url);
            }
        },
                // data, content 값만 document에 넣고 싶을 경우
                "date", "content");

        /*
         * JsonReader reader = new JsonReader(resource,
         * jsonMap -> Map.of(
         * "title", jsonMap.get("title"),
         * "author", jsonMap.get("author"),
         * "url", url));
         */

        // E
        List<Document> documents = reader.read();

        // T
        DocumentTransformer transformer = new TokenTextSplitter(
                200, 50, 0, 10000, false);

        List<Document> splitted_transformer = transformer.apply(documents);

        // L
        vectorStore.add(splitted_transformer);

        return "%d개를 %d로 쪼개어 Vector Store에 저장".formatted(documents.size(), splitted_transformer.size());
    }
}
