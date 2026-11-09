package com.lgcns.jeonse.domain.ocr.controller;

import com.lgcns.jeonse.domain.ocr.client.ClovaOcrClient;
import com.lgcns.jeonse.domain.ocr.service.OcrService;
import com.lgcns.jeonse.global.exception.OcrUnreadableException;
import com.lgcns.jeonse.global.security.JwtProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static com.lgcns.jeonse.domain.ocr.parser.RegistryParserTest.fixture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OcrController.class)
@Import(OcrService.class)
class OcrControllerTest {
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};

    @Autowired MockMvc mvc;
    @MockitoBean ClovaOcrClient clova;
    @MockitoBean JwtProvider jwtProvider; // WebConfig의 로그인 회원 resolver가 필요로 한다

    static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile(name, name + ".bin", "application/octet-stream", bytes);
    }

    @Test
    void 서류를_읽어_질문별_결과를_돌려준다() throws Exception {
        when(clova.read(argThat(b -> b != null && b.length == PNG.length), eq("png"))).thenReturn(fixture("registry-case-b.txt"));
        when(clova.read(argThat(b -> b != null && b.length == JPG.length), eq("jpg"))).thenReturn(fixture("ledger-case-b.txt"));

        mvc.perform(multipart("/api/ocr/registry")
                        .file(file("registry", PNG)).file(file("buildingLedger", JPG))
                        .param("address", "서울 가상구 테스트로 12, 101동 1204호"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressMatch").value("MATCH"))
                .andExpect(jsonPath("$.answers.length()").value(10))
                .andExpect(jsonPath("$.answers[0].code").value("T1_ADDRESS"))
                .andExpect(jsonPath("$.answers[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.answers[0].answer").value("YES"))
                .andExpect(jsonPath("$.answers[2].status").value("UNCONFIRMED"))
                .andExpect(jsonPath("$.answers[2].answer").isEmpty())
                .andExpect(jsonPath("$.mortgageMax").value(3600))
                .andExpect(jsonPath("$.housingType").value("NON_RESIDENTIAL"))
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    @Test
    void 주소가_없으면_400() throws Exception {
        mvc.perform(multipart("/api/ocr/registry").file(file("registry", PNG)).file(file("buildingLedger", PNG)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value("address"));
    }

    @Test
    void 건축물대장이_없으면_400() throws Exception {
        mvc.perform(multipart("/api/ocr/registry").file(file("registry", PNG)).param("address", "서울 가상구 테스트로 12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("buildingLedger"))
                .andExpect(jsonPath("$.error.message").value("건축물대장을 올려 주세요."));
    }

    @Test
    void JPG_PNG_PDF가_아니면_400() throws Exception {
        mvc.perform(multipart("/api/ocr/registry")
                        .file(file("registry", "hello".getBytes())).file(file("buildingLedger", PNG))
                        .param("address", "서울 가상구 테스트로 12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("registry"))
                .andExpect(jsonPath("$.error.message").value("JPG·PNG·PDF 파일만 올릴 수 있어요."));
    }

    @Test
    void 파일이_10MB를_넘으면_400() throws Exception {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        mvc.perform(multipart("/api/ocr/registry")
                        .file(file("registry", PNG)).file(file("buildingLedger", big))
                        .param("address", "서울 가상구 테스트로 12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("buildingLedger"));
    }

    @Test
    void 등기부를_못_읽으면_422() throws Exception {
        when(clova.read(any(), any())).thenThrow(new OcrUnreadableException());

        mvc.perform(multipart("/api/ocr/registry")
                        .file(file("registry", PNG)).file(file("buildingLedger", PNG))
                        .param("address", "서울 가상구 테스트로 12"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("OCR_UNREADABLE"))
                .andExpect(jsonPath("$.error.field").isEmpty());
    }

    @Test
    void 건축물대장만_못_읽으면_경고와_함께_200() throws Exception {
        when(clova.read(any(), eq("png"))).thenReturn(fixture("registry-case-b.txt"));
        when(clova.read(any(), eq("jpg"))).thenThrow(new OcrUnreadableException());

        mvc.perform(multipart("/api/ocr/registry")
                        .file(file("registry", PNG)).file(file("buildingLedger", JPG))
                        .param("address", "서울 가상구 테스트로 12, 101동 1204호"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers[9].status").value("UNCONFIRMED"))
                .andExpect(jsonPath("$.housingType").isEmpty())
                .andExpect(jsonPath("$.warnings[0]").value("건축물대장을 읽지 못했어요. 용도·위반건축물 질문은 직접 답해 주세요."));
    }

    @Test
    void 글자가_있는_PDF는_OCR을_부르지_않고_읽는다() throws Exception {
        byte[] pdf = pdf("This is a plain English receipt, not a registry document.");

        mvc.perform(multipart("/api/ocr/registry")
                        .file(file("registry", pdf)).file(file("buildingLedger", pdf))
                        .param("address", "서울 가상구 테스트로 12"))
                .andExpect(status().isUnprocessableEntity()); // 등기부가 아니어서 422
        verify(clova, never()).read(any(), any());
    }

    static byte[] pdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
