package com.lgcns.jeonse.domain.ocr.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.lgcns.jeonse.global.exception.OcrUnreadableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Naver CLOVA OCR General API (V2, JSON 본문). 서류는 이 요청에만 쓰고 저장하지 않는다. */
@Component
public class ClovaOcrClient {
    private final String url;
    private final String secret;
    private final RestClient restClient;

    public ClovaOcrClient(RestClient.Builder builder,
                          @Value("${clova.ocr.url:}") String url,
                          @Value("${clova.ocr.secret:}") String secret) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.requestFactory(factory).build();
        this.url = url;
        this.secret = secret;
    }

    /** format: jpg · png · pdf */
    public String read(byte[] file, String format) {
        // 서류와 비밀키가 평문으로 나가지 않게 HTTPS 주소만 쓴다
        if (secret.isBlank() || !url.startsWith("https://")) throw new OcrUnreadableException();
        Map<String, Object> body = Map.of(
                "version", "V2",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "lang", "ko",
                "images", List.of(Map.of("format", format, "name", "document",
                        "data", Base64.getEncoder().encodeToString(file))));
        try {
            JsonNode response = restClient.post().uri(url)
                    .header("X-OCR-SECRET", secret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return toText(response);
        } catch (RestClientException e) {
            throw new OcrUnreadableException();
        }
    }

    /** images[].fields[].inferText를 줄바꿈(lineBreak)대로 이어 붙인다. PDF는 페이지마다 images가 하나씩 온다. */
    static String toText(JsonNode response) {
        StringBuilder text = new StringBuilder();
        if (response == null) throw new OcrUnreadableException();
        for (JsonNode image : response.path("images")) {
            if (!"SUCCESS".equals(image.path("inferResult").asText())) throw new OcrUnreadableException();
            for (JsonNode field : image.path("fields")) {
                text.append(field.path("inferText").asText()).append(field.path("lineBreak").asBoolean() ? '\n' : ' ');
            }
            text.append('\n');
        }
        if (text.toString().isBlank()) throw new OcrUnreadableException();
        return text.toString();
    }
}
