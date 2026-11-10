package com.lgcns.jeonse.domain.ocr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.jeonse.global.exception.OcrUnreadableException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class ClovaOcrClientTest {
    final ObjectMapper mapper = new ObjectMapper();

    // CLOVA General OCR V2 응답 모양 (가짜 값)
    @Test
    void 응답의_글자를_줄바꿈대로_이어_붙인다() throws Exception {
        String json = """
                {"version":"V2","requestId":"r","timestamp":1,"images":[
                  {"uid":"u1","name":"document","inferResult":"SUCCESS","message":"SUCCESS","fields":[
                    {"valueType":"ALL","inferText":"【","inferConfidence":0.99,"type":"NORMAL","lineBreak":false},
                    {"valueType":"ALL","inferText":"갑구","inferConfidence":0.99,"type":"NORMAL","lineBreak":false},
                    {"valueType":"ALL","inferText":"】","inferConfidence":0.99,"type":"NORMAL","lineBreak":true},
                    {"valueType":"ALL","inferText":"1","inferConfidence":0.99,"type":"NORMAL","lineBreak":false},
                    {"valueType":"ALL","inferText":"소유권보존","inferConfidence":0.98,"type":"NORMAL","lineBreak":true}]},
                  {"uid":"u2","name":"document","inferResult":"SUCCESS","message":"SUCCESS","fields":[
                    {"valueType":"ALL","inferText":"이하여백","inferConfidence":0.97,"type":"NORMAL","lineBreak":true}]}]}
                """;

        assertThat(ClovaOcrClient.toText(mapper.readTree(json))).isEqualTo("【 갑구 】\n1 소유권보존\n\n이하여백\n\n");
    }

    @Test
    void 인식_실패면_판독_실패() throws Exception {
        String json = """
                {"images":[{"inferResult":"FAILURE","message":"UNKNOWN_ERROR","fields":[]}]}
                """;

        assertThatThrownBy(() -> ClovaOcrClient.toText(mapper.readTree(json))).isInstanceOf(OcrUnreadableException.class);
    }

    @Test
    void CLOVA_실패_메시지와_서류_내용은_로그에_남기지_않는다(CapturedOutput output) throws Exception {
        String json = """
                {"images":[{"inferResult":"소유자 홍길동","message":"서류 내용 홍길동","fields":[]}]}
                """;

        assertThatThrownBy(() -> ClovaOcrClient.toText(mapper.readTree(json)))
                .isInstanceOf(OcrUnreadableException.class);
        assertThat(output).contains("result=UNKNOWN").doesNotContain("홍길동", "서류 내용");
    }

    @Test
    void HTTP_오류는_상태만_로그에_남긴다(CapturedOutput output) {
        RestClient.Builder builder = RestClient.builder().requestInterceptor((request, body, execution) ->
                new MockClientHttpResponse("소유자 홍길동".getBytes(StandardCharsets.UTF_8), HttpStatus.BAD_GATEWAY));
        ClovaOcrClient client = new ClovaOcrClient(builder, "https://example.com/ocr", "test-secret");

        assertThatThrownBy(() -> client.read(new byte[]{1}, "png"))
                .isInstanceOf(OcrUnreadableException.class);
        assertThat(output).contains("status=502").doesNotContain("홍길동", "test-secret");
    }

    @Test
    void 키가_없으면_호출하지_않고_판독_실패() {
        ClovaOcrClient client = new ClovaOcrClient(RestClient.builder(), "", "");

        assertThatThrownBy(() -> client.read(new byte[]{1}, "png")).isInstanceOf(OcrUnreadableException.class);
    }

    @Test
    void HTTPS가_아닌_주소로는_보내지_않는다() {
        ClovaOcrClient client = new ClovaOcrClient(RestClient.builder(), "http://localhost:1/general", "secret");

        assertThatThrownBy(() -> client.read(new byte[]{1}, "png")).isInstanceOf(OcrUnreadableException.class);
    }
}
