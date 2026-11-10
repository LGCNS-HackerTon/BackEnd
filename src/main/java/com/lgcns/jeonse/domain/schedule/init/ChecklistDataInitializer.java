package com.lgcns.jeonse.domain.schedule.init;

import com.lgcns.jeonse.domain.schedule.entity.ChecklistItem;
import com.lgcns.jeonse.domain.schedule.repository.ChecklistItemRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// db/seed.sql의 checklist_items와 같은 16개 (docs/02). 둘 중 하나를 고치면 다른 쪽도 고친다
@Configuration
public class ChecklistDataInitializer {
    private static final Logger log = LoggerFactory.getLogger(ChecklistDataInitializer.class);
    private static final String IROS = "인터넷등기소", IROS_URL = "https://www.iros.go.kr";
    private static final String GOV = "정부24", GOV_URL = "https://www.gov.kr";
    private static final String HUG = "안심전세 앱(HUG)", HUG_URL = "https://www.khug.or.kr";
    private static final String OWNER_ACCOUNT_REASON = "엉뚱한 사람에게 돈이 가는 걸 막으려고";

    @Bean
    CommandLineRunner seedChecklist(ChecklistItemRepository repository) {
        return args -> fillMissing(repository);
    }

    /**
     * 기본 할 일 중 테이블에 없는 순서 번호만 추가한다. 이미 있는 행은 지우거나 덮어쓰지 않는다.
     *
     * @return 추가한 할 일 수
     */
    public static int fillMissing(ChecklistItemRepository repository) {
        Set<Integer> orders = repository.findAll().stream().map(ChecklistItem::getSortOrder).collect(Collectors.toSet());
        List<ChecklistItem> missing = defaults().stream().filter(item -> !orders.contains(item.getSortOrder())).toList();
        if (!missing.isEmpty()) {
            repository.saveAll(missing);
            log.info("빠진 기본 할 일 {}개를 추가했어요: {}번", missing.size(),
                    missing.stream().map(ChecklistItem::getSortOrder).toList());
        }
        return missing.size();
    }

    /** docs/02의 할 일 16개. */
    public static List<ChecklistItem> defaults() {
        return List.of(
                new ChecklistItem(1, "CONTRACT", -1, "등기부등본 발급해서 등기부 도우미로 점검", "계약 전에 위험 신호를 걸러내려고", false, IROS, IROS_URL),
                new ChecklistItem(2, "CONTRACT", -1, "건축물대장에서 위반건축물·용도 확인", "불법 건물이면 보증보험이 막힐 수 있어요", false, GOV, GOV_URL),
                new ChecklistItem(3, "CONTRACT", -1, "주변 시세 확인", "시세보다 보증금이 너무 높으면 깡통전세 위험이 있어요", false, "국토부 실거래가 공개시스템", "https://rt.molit.go.kr"),
                new ChecklistItem(4, "CONTRACT", 0, "집주인 신분증과 등기부 소유자 이름 대조", "가짜 집주인이나 권한 없는 대리인과 계약하는 걸 막으려고", false, null, null),
                new ChecklistItem(5, "CONTRACT", 0, "특약 문구 넣기 (진단 결과 화면에서 복사)", "잔금일 기습 대출 같은 일을 막으려고", false, null, null),
                new ChecklistItem(6, "CONTRACT", 0, "계약금 송금 직전에 등기부를 다시 열어 새 권리가 없는지 보고, 계약서의 수령인과 같은 등기부상 소유자 명의 계좌로만 송금", "엉뚱한 사람에게 돈이 가는 것과 계약 당일 기습 대출을 막으려고", false, IROS, IROS_URL),
                new ChecklistItem(7, "CONTRACT", 1, "계약서로 확정일자 미리 받기", "확정일자는 계약서만 있으면 바로 받을 수 있어요. 일찍 받을수록 유리해요.", false, IROS, IROS_URL),
                new ChecklistItem(8, "BALANCE", -14, "(대출 받는 경우) 전세자금대출 진행 상황 확인", "잔금일에 돈이 안 나오면 계약 위반이 될 수 있어요", false, null, null),
                new ChecklistItem(9, "BALANCE", 0, "송금 직전에 등기부 다시 발급 → 계약일 이후 새 근저당·압류가 생겼는지 확인", "잔금일 기습 대출 수법을 막으려고", true, IROS, IROS_URL),
                new ChecklistItem(10, "BALANCE", 0, "잔금도 소유자 명의 계좌로만 송금", OWNER_ACCOUNT_REASON, false, null, null),
                new ChecklistItem(11, "MOVE_IN", 0, "당일 전입신고 (+ 확정일자 안 받았으면 같이)", "대항력은 전입신고 다음 날 0시에 생겨요. 하루라도 늦으면 그 사이에 생긴 빚보다 순위가 밀려요.", true, GOV, GOV_URL),
                new ChecklistItem(12, "MOVE_IN", 1, "전세보증보험 가입 신청", "집주인이 보증금을 못 돌려줘도 보증기관이 대신 돌려줘요", false, HUG, HUG_URL),
                new ChecklistItem(13, "EXPIRY", -180, "만기 6개월 전: 연장할지, 나갈지 생각하기", "집주인은 만기 6~2개월 전에 갱신 거절을 알려야 해요", false, null, null),
                new ChecklistItem(14, "EXPIRY", -60, "만기 2개월 전: 나갈 거면 집주인에게 알리기", "세입자는 만기 2개월 전까지 알려야 해요", false, null, null),
                new ChecklistItem(15, "CONTRACT", -1, "전세보증금 반환보증 가입 가능 여부 미리 확인", "계약 후에 가입이 거절되면 보증금을 지킬 방법이 줄어들어요. 거절될 수 있으면 특약(\"가입 거절 시 계약 무효\")을 넣으세요.", false, HUG, HUG_URL),
                new ChecklistItem(16, "CONTRACT", -1, "집주인에게 국세·지방세 납세증명서 요청", "체납 세금은 내 보증금보다 먼저 가져가요", false, null, null));
    }
}
