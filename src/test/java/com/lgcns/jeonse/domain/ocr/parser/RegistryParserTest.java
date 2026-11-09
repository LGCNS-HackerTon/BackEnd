package com.lgcns.jeonse.domain.ocr.parser;

import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.AddressMatch;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.Answer;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.HousingType;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.Status;
import com.lgcns.jeonse.global.exception.OcrUnreadableException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 픽스처는 모두 가상의 주소·이름이다
public class RegistryParserTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    public static String fixture(String name) {
        try (InputStream in = RegistryParserTest.class.getResourceAsStream("/ocr/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Answer answer(OcrResponseDTO result, String code) {
        return result.answers().stream().filter(a -> a.code().equals(code)).findFirst().orElseThrow();
    }

    static void assertAnswer(OcrResponseDTO result, String code, Status status, String answer) {
        Answer a = answer(result, code);
        assertThat(a.status()).as(code).isEqualTo(status);
        assertThat(a.answer()).as(code).isEqualTo(answer);
    }

    @Test
    void caseB_근생빌라_최근이전_근저당() {
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호",
                fixture("registry-case-b.txt"), fixture("ledger-case-b.txt"), TODAY);

        assertThat(r.answers()).extracting(Answer::code).containsExactly("T1_ADDRESS", "T2_USAGE", "A1_OWNER",
                "A2_RECENT_OWNER", "A3_TRUST", "A4_SEIZURE", "B1_MORTGAGE", "B2_LEASE_REG", "B3_JEONSE_RIGHT", "R1_VIOLATION");
        assertThat(r.addressMatch()).isEqualTo(AddressMatch.MATCH);
        assertAnswer(r, "T1_ADDRESS", Status.CONFIRMED, "YES");
        assertAnswer(r, "T2_USAGE", Status.CONFIRMED, "NO");
        assertAnswer(r, "A1_OWNER", Status.UNCONFIRMED, null);
        assertThat(answer(r, "A1_OWNER").evidence()).contains("홍○○").doesNotContain("홍길동");
        assertAnswer(r, "A2_RECENT_OWNER", Status.CONFIRMED, "YES");
        assertThat(answer(r, "A2_RECENT_OWNER").evidence()).isEqualTo("갑구 3번 소유권이전 2026년9월1일 접수");
        assertAnswer(r, "A3_TRUST", Status.CONFIRMED, "NO");
        assertAnswer(r, "A4_SEIZURE", Status.CONFIRMED, "NO");
        assertAnswer(r, "B1_MORTGAGE", Status.CONFIRMED, "YES");
        assertThat(answer(r, "B1_MORTGAGE").evidence()).isEqualTo("을구 1번 근저당권 채권최고액 금36,000,000원");
        assertAnswer(r, "B2_LEASE_REG", Status.CONFIRMED, "NO");
        assertAnswer(r, "B3_JEONSE_RIGHT", Status.CONFIRMED, "NO");
        assertAnswer(r, "R1_VIOLATION", Status.CONFIRMED, "NO");
        assertThat(r.mortgageMax()).isEqualTo(3600);
        assertThat(r.housingType()).isEqualTo(HousingType.NON_RESIDENTIAL);
        assertThat(r.warnings()).isEmpty();
    }

    @Test
    void 말소된_가압류와_근저당은_유효한_기록으로_보지_않는다() {
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 샘플로 7, 103동 702호",
                fixture("registry-case-a-cancelled.txt"), fixture("ledger-case-a.txt"), TODAY);

        assertThat(r.addressMatch()).isEqualTo(AddressMatch.MATCH);
        assertAnswer(r, "T2_USAGE", Status.CONFIRMED, "YES");
        assertAnswer(r, "A2_RECENT_OWNER", Status.CONFIRMED, "NO");
        assertThat(answer(r, "A1_OWNER").evidence()).contains("이○○");
        assertAnswer(r, "A4_SEIZURE", Status.CONFIRMED, "NO");
        assertAnswer(r, "B1_MORTGAGE", Status.CONFIRMED, "NO");
        assertThat(r.mortgageMax()).isZero();
        assertThat(r.housingType()).isEqualTo(HousingType.APARTMENT);
    }

    @Test
    void 말소_기록이_없으면_가압류는_유효하다() {
        String text = fixture("registry-case-a-cancelled.txt").replace("4 3번가압류등기말소 2021년6월1일 제404호 2021년5월30일 해제\n", "");
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 샘플로 7, 103동 702호", text, fixture("ledger-case-a.txt"), TODAY);

        assertAnswer(r, "A4_SEIZURE", Status.CONFIRMED, "YES");
        assertThat(answer(r, "A4_SEIZURE").evidence()).isEqualTo("갑구 3번 가압류 기록");
    }

    @Test
    void caseC_신탁_오피스텔은_용도를_사용자에게_묻는다() {
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 예시로 77, 905호",
                fixture("registry-case-c.txt"), fixture("ledger-case-c.txt"), TODAY);

        assertThat(r.addressMatch()).isEqualTo(AddressMatch.MATCH);
        assertAnswer(r, "A3_TRUST", Status.CONFIRMED, "YES");
        assertAnswer(r, "T2_USAGE", Status.UNCONFIRMED, null);
        assertAnswer(r, "B1_MORTGAGE", Status.CONFIRMED, "NO");
        assertAnswer(r, "B2_LEASE_REG", Status.CONFIRMED, "NO");
        assertThat(r.housingType()).isEqualTo(HousingType.OFFICETEL);
    }

    @Test
    void 호수가_다르면_MISMATCH() {
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1205호",
                fixture("registry-case-b.txt"), fixture("ledger-case-b.txt"), TODAY);

        assertThat(r.addressMatch()).isEqualTo(AddressMatch.MISMATCH);
        assertAnswer(r, "T1_ADDRESS", Status.CONFIRMED, "NO");
    }

    @Test
    void 주소를_비교할_수_없으면_UNKNOWN() {
        OcrResponseDTO r = RegistryParser.parse("우리 집", fixture("registry-case-b.txt"), fixture("ledger-case-b.txt"), TODAY);

        assertThat(r.addressMatch()).isEqualTo(AddressMatch.UNKNOWN);
        assertAnswer(r, "T1_ADDRESS", Status.UNCONFIRMED, null);
    }

    @Test
    void 을구_끝을_못_읽으면_없다는_답은_채우지_않는다() {
        String text = fixture("registry-case-b.txt").replace("-- 이 하 여 백 --", "");
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호", text, fixture("ledger-case-b.txt"), TODAY);

        assertAnswer(r, "B1_MORTGAGE", Status.CONFIRMED, "YES"); // 있다는 것은 확실하다
        assertAnswer(r, "B2_LEASE_REG", Status.UNCONFIRMED, null);
        assertAnswer(r, "B3_JEONSE_RIGHT", Status.UNCONFIRMED, null);
        assertAnswer(r, "A3_TRUST", Status.CONFIRMED, "NO"); // 갑구는 을구 머리글까지 다 읽었다
        assertThat(r.warnings()).contains("을구 일부를 읽지 못했어요. 해당 질문은 직접 답해 주세요.");
    }

    @Test
    void 순위번호가_따로_읽히고_금액을_못_읽으면_금액만_null() {
        String text = fixture("registry-case-b.txt").replace(
                "1 근저당권설정 2026년9월1일 제891호 2026년9월1일 설정계약 채권최고액 금36,000,000원 채무자 홍길동 근저당권자 주식회사가상은행",
                "1\n근저당권설정\n2026년9월1일 제891호\n채권최고액 금36,000,000원\n2\n근저당권설정\n2026년9월2일 제892호\n채권최고액 금##,###원");
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호", text, fixture("ledger-case-b.txt"), TODAY);

        assertAnswer(r, "B1_MORTGAGE", Status.CONFIRMED, "YES");
        assertThat(r.mortgageMax()).isNull();
    }

    @Test
    void 근저당이_여러_개면_합계를_만원으로() {
        String text = fixture("registry-case-b.txt").replace("-- 이 하 여 백 --",
                "2 근저당권설정 2026년9월2일 제892호 채권최고액 금12,345,000원 근저당권자 가상캐피탈\n-- 이 하 여 백 --");
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호", text, fixture("ledger-case-b.txt"), TODAY);

        assertThat(r.mortgageMax()).isEqualTo(3600 + 1235); // 만원 미만은 올림
    }

    @Test
    void 위반건축물_표시가_있으면_YES() {
        String ledger = fixture("ledger-case-b.txt").replace("집합건축물대장(전유부, 갑)", "위반건축물\n집합건축물대장(전유부, 갑)");
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호", fixture("registry-case-b.txt"), ledger, TODAY);

        assertAnswer(r, "R1_VIOLATION", Status.CONFIRMED, "YES");
    }

    @Test
    void 건축물대장을_못_읽으면_표제부_용도로_보고_위반건축물은_묻는다() {
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호",
                fixture("registry-case-b.txt"), null, TODAY);

        assertAnswer(r, "T2_USAGE", Status.CONFIRMED, "NO");
        assertThat(answer(r, "T2_USAGE").evidence()).isEqualTo("표제부 건물내역: 제2종근린생활시설");
        assertAnswer(r, "R1_VIOLATION", Status.UNCONFIRMED, null);
        assertThat(r.housingType()).isNull();
        assertThat(r.warnings()).containsExactly("건축물대장을 읽지 못했어요. 용도·위반건축물 질문은 직접 답해 주세요.");
    }

    @Test
    void 건축물대장_호수가_다르면_쓰지_않는다() {
        String ledger = fixture("ledger-case-b.txt").replace("1204호", "1301호");
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호", fixture("registry-case-b.txt"), ledger, TODAY);

        assertAnswer(r, "R1_VIOLATION", Status.UNCONFIRMED, null);
        assertThat(r.housingType()).isNull();
    }

    @Test
    void 다가구주택은_주용도로_판정하고_표제부와_다르면_용도는_묻는다() {
        String ledger = "일반건축물대장(갑)\n대지위치 서울특별시 가상구 다가동 9\n주용도 단독주택(다가구주택)\n"
                + "층별현황 1층 제1종근린생활시설 2층 다가구주택 3층 다가구주택";
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 다가동 9", fixture("registry-case-b.txt"), ledger, TODAY);

        assertThat(r.housingType()).isEqualTo(HousingType.MULTI_HOUSEHOLD);
        assertAnswer(r, "T2_USAGE", Status.UNCONFIRMED, null); // 표제부(케이스 B)는 근린생활시설
        assertThat(answer(r, "T2_USAGE").evidence()).isEqualTo("표제부와 건축물대장의 용도가 서로 다름");
    }

    @Test
    void 등기부가_아니면_판독_실패() {
        assertThatThrownBy(() -> RegistryParser.parse("서울 가상구 테스트로 12", "영수증 합계 12,000원", null, TODAY))
                .isInstanceOf(OcrUnreadableException.class);
    }

    @Test
    void 판독하지_못한_질문은_NO로_채우지_않는다() {
        OcrResponseDTO r = RegistryParser.parse("서울 가상구 테스트로 12, 101동 1204호",
                "등기사항전부증명서 [집합건물] 글자가 흐려서 읽을 수 없음", null, TODAY);

        assertThat(r.answers()).hasSize(10).allSatisfy(a -> {
            assertThat(a.status()).isEqualTo(Status.UNCONFIRMED);
            assertThat(a.answer()).isNull();
        });
        assertThat(r.mortgageMax()).isNull();
        assertThat(r.warnings()).containsAll(List.of(
                "갑구 일부를 읽지 못했어요. 해당 질문은 직접 답해 주세요.",
                "을구 일부를 읽지 못했어요. 해당 질문은 직접 답해 주세요."));
    }
}
