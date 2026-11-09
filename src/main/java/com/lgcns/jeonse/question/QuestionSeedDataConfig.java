package com.lgcns.jeonse.question;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuestionSeedDataConfig {
    private static final String GUARANTEE = "전세보증금반환보증 가입이 거절될 경우 본 계약은 무효로 하며, 임대인은 받은 계약금을 즉시 반환한다.";

    @Bean
    CommandLineRunner seedQuestions(RegistryQuestionRepository repository) {
        return args -> {
            if (repository.count() != 0) return;
            repository.save(new RegistryQuestion("T1_ADDRESS", "TITLE", 1, "표제부의 주소(동·호수까지)가 계약할 집 주소와 똑같나요?", "다른 집 등기부를 보고 있을 수 있어요. 동·호수까지 맞는 등기부로 다시 발급해야 해요.", "NO", 60, false, "주소 불일치", "계약할 집의 정확한 동·호수가 어떻게 되나요?", null));
            repository.save(new RegistryQuestion("T2_USAGE", "TITLE", 2, "건물 용도에 아파트·다세대주택·연립주택·다가구주택·단독주택 중 하나가 적혀 있나요?", "근린생활시설, 사무소면 원래 사람이 사는 집이 아니에요. 전세보증보험 가입이 거절될 수 있어요.", "NO", 20, false, "주택이 아닌 용도", "이 건물 용도가 주택이 아닌데, 전세보증보험 가입이 되나요?", GUARANTEE));
            repository.save(new RegistryQuestion("A1_OWNER", "GAPGU", 3, "갑구 맨 아래 소유자 이름이 계약하러 나온 집주인 이름과 같나요?", "주인이 아닌 사람과 계약하면 보증금을 못 돌려받을 수 있어요. 대리인이면 위임장과 인감증명서가 꼭 필요해요.", "NO", 30, false, "집주인과 소유자가 다름", "계약하시는 분이 등기부상 소유자 본인인가요? 대리인이면 위임장과 인감증명서를 보여주실 수 있나요?", "계약금·잔금은 등기부상 소유자 명의 계좌로만 지급한다."));
            repository.save(new RegistryQuestion("A2_RECENT_OWNER", "GAPGU", 4, "소유권이 바뀐 날짜(접수일)가 최근 3개월 안인가요?", "집을 사자마자 전세를 놓는 경우 바지 집주인 수법일 수 있어요. 꼭 나쁜 건 아니지만 한 번 더 확인해요.", "YES", 10, false, "최근 3개월 안에 집주인 변경", "최근에 집주인이 바뀌었는데, 매매와 전세 계약이 동시에 진행되는 건가요?", "임대인은 잔금일 다음 날까지 소유권을 이전하지 않으며, 위반 시 임차인은 계약을 해제할 수 있다."));
            repository.save(new RegistryQuestion("A3_TRUST", "GAPGU", 5, "갑구에 신탁이라는 단어가 (빨간 줄 없이) 있나요?", "집의 진짜 주인이 신탁회사라는 뜻이에요. 신탁회사 동의 없이 한 계약은 효력이 없을 수 있어요.", "YES", 30, false, "신탁 등기", "신탁회사(수탁자)의 임대 동의서를 받을 수 있나요? 신탁원부도 보여주세요.", "임대인은 신탁회사의 임대 동의서를 계약서에 첨부하며, 동의서가 없으면 본 계약은 무효로 한다."));
            repository.save(new RegistryQuestion("A4_SEIZURE", "GAPGU", 6, "압류, 가압류, 가처분, 가등기, 경매개시결정 중 하나라도 (빨간 줄 없이) 있나요?", "집주인이 빚 문제로 이 집을 빼앗기기 직전이거나, 소유권 다툼이 있다는 신호예요. 계약하지 않는 게 좋아요.", "YES", 60, false, "압류·가처분 등 권리 문제", "갑구에 있는 압류(가압류 등)는 언제, 어떻게 정리되나요?", null));
            repository.save(new RegistryQuestion("B1_MORTGAGE", "EULGU", 7, "을구에 근저당권이 (빨간 줄 없이) 있나요?", "이 집을 담보로 은행에서 돈을 빌렸다는 뜻이에요. 금액이 크면 위험할 수 있어요.", "YES", 10, true, "근저당권 있음", "근저당 채권최고액이 얼마이고, 잔금일에 갚고 말소해 주실 수 있나요?", "임대인은 잔금 지급과 동시에 을구의 근저당권을 말소하며, 말소되지 않으면 임차인은 계약을 해제하고 계약금 반환을 청구할 수 있다."));
            repository.save(new RegistryQuestion("B2_LEASE_REG", "EULGU", 8, "을구에 주택임차권(임차권등기)이 있나요?", "이전 세입자가 보증금을 못 돌려받았다는 기록이에요. 아주 강한 위험 신호예요.", "YES", 60, false, "이전 세입자 임차권등기", "이전 세입자가 보증금을 못 받아 임차권등기를 한 건가요?", null));
            repository.save(new RegistryQuestion("B3_JEONSE_RIGHT", "EULGU", 9, "을구에 다른 사람 이름의 전세권이 (빨간 줄 없이) 있나요?", "이전 세입자의 권리가 아직 정리되지 않았어요. 나보다 먼저 돈을 받아 갈 사람이 있다는 뜻이에요.", "YES", 20, false, "다른 사람의 전세권", "이전 세입자의 전세권은 언제 말소되나요?", "임대인은 잔금일까지 을구의 기존 전세권을 말소한다."));
            repository.save(new RegistryQuestion("R1_VIOLATION", "BUILDING", 10, "건축물대장 맨 위에 위반건축물 표시가 있나요?", "불법으로 고치거나 늘린 건물이에요. 전세보증보험·전세대출이 막힐 수 있어요.", "YES", 20, false, "위반건축물", "위반건축물인데 전세대출과 보증보험이 가능한가요?", "전세자금대출 또는 보증보험이 불가할 경우 본 계약은 무효로 하며, 임대인은 계약금을 즉시 반환한다."));
        };
    }
}
