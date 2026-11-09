-- jeonse DB 기본 데이터 (MySQL 8 기준, MariaDB 10.5 이상에서도 실행됨)
-- 원본: docs/01-registry-questions.md (질문 10개), docs/02-schedule-rules.md (할 일 14개)
-- schema.sql 다음에 실행한다. 여러 번 실행해도 같은 행을 덮어써서 중복되지 않는다.
-- ON DUPLICATE KEY UPDATE에서 VALUES(컬럼)을 쓰는 이유: MySQL 8 전용 문법(AS new)은 MariaDB에서 실행되지 않는다.
-- MySQL 8.0.20 이상은 VALUES()에 '앞으로 없어질 문법' 경고만 띄우고 정상 실행한다.

USE jeonse;

SET NAMES utf8mb4;

-- ============================================================
-- 1. 등기부 질문 10개 (docs/01 1절 + 4절)
--    help는 해요체. risky_answer: 이 답이면 risk_score를 더한다 (UNKNOWN은 항상 위험 쪽)
--    B1_MORTGAGE는 YES면 부채비율로 계산하고, risk_score 10은 UNKNOWN일 때만 쓴다
--    special_clause가 NULL이면 화면에 "계약 보류를 권해요"를 보여준다
-- ============================================================
INSERT INTO registry_questions
    (code, section, sort_order, text, help, risky_answer, risk_score, needs_amount, factor_title, ask_agent, special_clause)
VALUES
    ('T1_ADDRESS', 'TITLE', 1,
     '표제부의 주소(동·호수까지)가 계약할 집 주소와 똑같나요?',
     '다른 집 등기부를 보고 있을 수 있어요. 동·호수까지 맞는 등기부로 다시 발급해야 해요.',
     'NO', 60, FALSE,
     '주소 불일치',
     '계약할 집의 정확한 동·호수가 어떻게 되나요?',
     NULL),

    ('T2_USAGE', 'TITLE', 2,
     '건물 용도에 아파트·다세대주택·연립주택·다가구주택·단독주택 중 하나가 적혀 있나요?',
     '"근린생활시설", "사무소"면 원래 사람이 사는 집이 아니에요. 전세보증보험 가입이 거절될 수 있어요.',
     'NO', 20, FALSE,
     '주택이 아닌 용도',
     '이 건물 용도가 주택이 아닌데, 전세보증보험 가입이 되나요?',
     '전세보증금반환보증 가입이 거절될 경우 본 계약은 무효로 하며, 임대인은 받은 계약금을 즉시 반환한다.'),

    ('A1_OWNER', 'GAPGU', 3,
     '갑구 맨 아래 소유자 이름이 계약하러 나온 집주인 이름과 같나요?',
     '주인이 아닌 사람과 계약하면 보증금을 못 돌려받을 수 있어요. 대리인이면 위임장과 인감증명서가 꼭 필요해요.',
     'NO', 30, FALSE,
     '집주인과 소유자가 다름',
     '계약하시는 분이 등기부상 소유자 본인인가요? 대리인이면 위임장과 인감증명서를 보여주실 수 있나요?',
     '계약금·잔금은 등기부상 소유자 명의 계좌로만 지급한다.'),

    ('A2_RECENT_OWNER', 'GAPGU', 4,
     '소유권이 바뀐 날짜(접수일)가 최근 3개월 안인가요?',
     '집을 사자마자 전세를 놓는 경우 "바지 집주인" 수법일 수 있어요. 꼭 나쁜 건 아니지만 한 번 더 확인해 보세요.',
     'YES', 10, FALSE,
     '최근 3개월 안에 집주인 변경',
     '최근에 집주인이 바뀌었는데, 매매와 전세 계약이 동시에 진행되는 건가요?',
     '임대인은 잔금일 다음 날까지 소유권을 이전하지 않으며, 위반 시 임차인은 계약을 해제할 수 있다.'),

    ('A3_TRUST', 'GAPGU', 5,
     '갑구에 "신탁"이라는 단어가 (빨간 줄 없이) 있나요?',
     '집의 진짜 주인이 신탁회사라는 뜻이에요. 신탁회사 동의 없이 한 계약은 효력이 없을 수 있어요.',
     'YES', 30, FALSE,
     '신탁 등기',
     '신탁회사(수탁자)의 임대 동의서를 받을 수 있나요? 신탁원부도 보여주세요.',
     '임대인은 신탁회사의 임대 동의서를 계약서에 첨부하며, 동의서가 없으면 본 계약은 무효로 한다.'),

    ('A4_SEIZURE', 'GAPGU', 6,
     '"압류, 가압류, 가처분, 가등기, 경매개시결정" 중 하나라도 (빨간 줄 없이) 있나요?',
     '집주인이 빚 문제로 이 집을 빼앗기기 직전이거나, 소유권 다툼이 있다는 신호예요. 계약하지 않는 게 좋아요.',
     'YES', 60, FALSE,
     '압류·가처분 등 권리 문제',
     '갑구에 있는 압류(가압류 등)는 언제, 어떻게 정리되나요?',
     NULL),

    ('B1_MORTGAGE', 'EULGU', 7,
     '을구에 "근저당권"이 (빨간 줄 없이) 있나요?',
     '이 집을 담보로 은행에서 돈을 빌렸다는 뜻이에요. 그 자체로 불법은 아니지만, 금액이 크면 위험해요.',
     'YES', 10, TRUE,
     '근저당권 있음',
     '근저당 채권최고액이 얼마이고, 잔금일에 갚고 말소해 주실 수 있나요?',
     '임대인은 잔금 지급과 동시에 을구의 근저당권을 말소하며, 말소되지 않으면 임차인은 계약을 해제하고 계약금 반환을 청구할 수 있다.'),

    ('B2_LEASE_REG', 'EULGU', 8,
     '을구에 "주택임차권"(임차권등기)이 있나요?',
     '이전 세입자가 보증금을 못 돌려받았다는 기록이에요. 아주 강한 위험 신호예요.',
     'YES', 60, FALSE,
     '이전 세입자 임차권등기',
     '이전 세입자가 보증금을 못 받아 임차권등기를 한 건가요?',
     NULL),

    ('B3_JEONSE_RIGHT', 'EULGU', 9,
     '을구에 다른 사람 이름의 "전세권"이 (빨간 줄 없이) 있나요?',
     '이전 세입자의 권리가 아직 정리되지 않았어요. 나보다 먼저 돈을 받아 갈 사람이 있다는 뜻이에요.',
     'YES', 20, FALSE,
     '다른 사람의 전세권',
     '이전 세입자의 전세권은 언제 말소되나요?',
     '임대인은 잔금일까지 을구의 기존 전세권을 말소한다.'),

    ('R1_VIOLATION', 'BUILDING', 10,
     '건축물대장 맨 위에 "위반건축물" 표시가 있나요?',
     '불법으로 고치거나 늘린 건물이에요. 전세보증보험·전세대출이 막힐 수 있어요.',
     'YES', 20, FALSE,
     '위반건축물',
     '위반건축물인데 전세대출과 보증보험이 가능한가요?',
     '전세자금대출 또는 보증보험이 불가할 경우 본 계약은 무효로 하며, 임대인은 계약금을 즉시 반환한다.')
ON DUPLICATE KEY UPDATE
    section        = VALUES(section),
    sort_order     = VALUES(sort_order),
    text           = VALUES(text),
    help           = VALUES(help),
    risky_answer   = VALUES(risky_answer),
    risk_score     = VALUES(risk_score),
    needs_amount   = VALUES(needs_amount),
    factor_title   = VALUES(factor_title),
    ask_agent      = VALUES(ask_agent),
    special_clause = VALUES(special_clause);

-- ============================================================
-- 2. 계약 일정표 할 일 14개 (docs/02)
--    날짜 = anchor 날짜 + offset_days. important는 9번·11번
--    링크는 한 줄에 하나, 여러 개면 첫 번째. 링크 표에 없는 서비스(은행)는 NULL
-- ============================================================
INSERT INTO checklist_items
    (id, sort_order, anchor, offset_days, title, reason, important, link_name, link_url)
VALUES
    (1,  1,  'CONTRACT', -1,
     '등기부등본 발급해서 등기부 도우미로 점검',
     '계약 전에 위험 신호를 걸러내려고',
     FALSE, '인터넷등기소', 'https://www.iros.go.kr'),
    (2,  2,  'CONTRACT', -1,
     '건축물대장에서 위반건축물·용도 확인',
     '불법 건물이면 보증보험이 막힐 수 있어요',
     FALSE, '정부24', 'https://www.gov.kr'),
    (3,  3,  'CONTRACT', -1,
     '주변 시세 확인',
     '시세보다 보증금이 너무 높으면 깡통전세 위험이 있어요',
     FALSE, '국토부 실거래가 공개시스템', 'https://rt.molit.go.kr'),
    (4,  4,  'CONTRACT', 0,
     '집주인 신분증과 등기부 소유자 이름 대조',
     '가짜 집주인이나 권한 없는 대리인과 계약하는 걸 막으려고',
     FALSE, NULL, NULL),
    (5,  5,  'CONTRACT', 0,
     '특약 문구 넣기 (진단 결과 화면에서 복사)',
     '잔금일 기습 대출 같은 일을 막으려고',
     FALSE, NULL, NULL),
    (6,  6,  'CONTRACT', 0,
     '계약금은 등기부상 소유자 명의 계좌로만 송금',
     '엉뚱한 사람에게 돈이 가는 걸 막으려고',
     FALSE, NULL, NULL),
    (7,  7,  'CONTRACT', 1,
     '계약서로 확정일자 미리 받기',
     '확정일자는 계약서만 있으면 바로 받을 수 있어요. 일찍 받을수록 유리해요.',
     FALSE, '인터넷등기소', 'https://www.iros.go.kr'),
    (8,  8,  'BALANCE', -14,
     '(대출 받는 경우) 전세자금대출 진행 상황 확인',
     '잔금일에 돈이 안 나오면 계약 위반이 될 수 있어요',
     FALSE, NULL, NULL),
    (9,  9,  'BALANCE', 0,
     '송금 직전에 등기부 다시 발급 → 계약일 이후 새 근저당·압류가 생겼는지 확인',
     '잔금일 기습 대출 수법을 막으려고',
     TRUE, '인터넷등기소', 'https://www.iros.go.kr'),
    (10, 10, 'BALANCE', 0,
     '잔금도 소유자 명의 계좌로만 송금',
     '엉뚱한 사람에게 돈이 가는 걸 막으려고',
     FALSE, NULL, NULL),
    (11, 11, 'MOVE_IN', 0,
     '당일 전입신고 (+ 확정일자 안 받았으면 같이)',
     '대항력은 전입신고 다음 날 0시에 생겨요. 하루라도 늦으면 그 사이에 생긴 빚보다 순위가 밀려요.',
     TRUE, '정부24', 'https://www.gov.kr'),
    (12, 12, 'MOVE_IN', 1,
     '전세보증보험 가입 신청',
     '집주인이 보증금을 못 돌려줘도 보증기관이 대신 돌려줘요',
     FALSE, '안심전세 앱(HUG)', 'https://www.khug.or.kr'),
    (13, 13, 'EXPIRY', -180,
     '만기 6개월 전: 연장할지, 나갈지 생각하기',
     '집주인은 만기 6~2개월 전에 갱신 거절을 알려야 해요',
     FALSE, NULL, NULL),
    (14, 14, 'EXPIRY', -60,
     '만기 2개월 전: 나갈 거면 집주인에게 알리기',
     '세입자는 만기 2개월 전까지 알려야 해요',
     FALSE, NULL, NULL)
ON DUPLICATE KEY UPDATE
    sort_order  = VALUES(sort_order),
    anchor      = VALUES(anchor),
    offset_days = VALUES(offset_days),
    title       = VALUES(title),
    reason      = VALUES(reason),
    important   = VALUES(important),
    link_name   = VALUES(link_name),
    link_url    = VALUES(link_url);
