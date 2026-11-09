package com.lgcns.jeonse.domain.ocr.service;

import com.lgcns.jeonse.domain.ocr.client.ClovaOcrClient;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO;
import com.lgcns.jeonse.domain.ocr.parser.RegistryParser;
import com.lgcns.jeonse.global.exception.InvalidInputException;
import com.lgcns.jeonse.global.exception.OcrUnreadableException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

/** 서류는 메모리에서만 읽고 버린다. 디스크·DB·로그에 남기지 않는다 (NFR-06). */
@Service
public class OcrService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private final ClovaOcrClient clova;

    public OcrService(ClovaOcrClient clova) { this.clova = clova; }

    public OcrResponseDTO read(String address, MultipartFile registry, MultipartFile buildingLedger) {
        if (address == null || address.isBlank()) throw new InvalidInputException("계약할 집 주소를 입력해 주세요.", "address");
        byte[] registryBytes = bytes(registry, "registry", "등기부등본");
        byte[] ledgerBytes = bytes(buildingLedger, "buildingLedger", "건축물대장");

        String registryText = text(registryBytes);
        String ledgerText;
        try {
            ledgerText = text(ledgerBytes);
        } catch (OcrUnreadableException e) {
            ledgerText = null; // 등기부만 읽혀도 진행하고, 건축물대장 질문은 직접 답하게 한다
        }
        return RegistryParser.parse(address.trim(), registryText, ledgerText, LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    private static byte[] bytes(MultipartFile file, String field, String label) {
        if (file == null || file.isEmpty()) throw new InvalidInputException(label + "을 올려 주세요.", field);
        if (file.getSize() > MAX_SIZE) throw new InvalidInputException("서류는 파일당 10MB 이하로 올려 주세요.", field);
        try {
            byte[] bytes = file.getBytes();
            if (format(bytes) == null) throw new InvalidInputException("JPG·PNG·PDF 파일만 올릴 수 있어요.", field);
            return bytes;
        } catch (IOException e) {
            throw new InvalidInputException(label + "을 다시 올려 주세요.", field);
        }
    }

    /** 확장자 대신 파일 앞부분으로 형식을 판단한다 */
    private static String format(byte[] b) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "pdf";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        return null;
    }

    /** 글자가 들어 있는 PDF(인터넷등기소 발급본 등)는 바로 읽고, 사진·스캔본만 CLOVA OCR로 보낸다 */
    private String text(byte[] bytes) {
        String format = format(bytes);
        if ("pdf".equals(format)) {
            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(pdf);
                if (text.replaceAll("\\s+", "").length() >= 20) return text;
            } catch (IOException e) {
                // 깨졌거나 암호가 걸린 PDF는 OCR로 다시 시도한다
            }
        }
        return clova.read(bytes, format);
    }
}
