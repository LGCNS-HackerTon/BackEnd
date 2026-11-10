package com.lgcns.jeonse.domain.ocr.parser;

import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.AddressMatch;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.Answer;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.HousingType;
import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO.Status;
import com.lgcns.jeonse.global.exception.OcrUnreadableException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 글자 → 질문 10개 판정 (docs/01 1.1). 점수는 계산하지 않는다.
 * 확실하지 않으면 UNCONFIRMED로 둔다. 판독 실패를 NO로 채우지 않는다 (D6).
 * shortcut: 가상 서류 텍스트로만 맞춘 규칙이라 실제 CLOVA 출력의 줄 나눔이 다르면 UNCONFIRMED가 늘어난다. 실제 키로 샘플을 돌려 보고 정규식을 보강할 것.
 */
public final class RegistryParser {
    private static final Pattern ENTRY = Pattern.compile(
            "^(\\d{1,3}(?:-\\d{1,3})?)(?=소유권|근저당|전세권|주택임차권|임차권|가압류|압류|가처분|가등기|임의경매|강제경매|경매|신탁|\\d{1,3}(?:-\\d{1,3})?번)");
    private static final Pattern BARE_NUMBER = Pattern.compile("^\\d{1,3}(?:-\\d{1,3})?$");
    // "1번근저당권설정등기말소"처럼 앞 순위번호를 지우는 기록. 빨간 줄은 OCR이 못 보지만 이 기록은 글자로 남는다.
    private static final Pattern CANCEL_REF = Pattern.compile("(\\d{1,3}(?:-\\d{1,3})?)번[가-힣]{0,20}?말소");
    private static final Pattern DATE = Pattern.compile("(\\d{4})년(\\d{1,2})월(\\d{1,2})일");
    private static final Pattern MAX_AMOUNT = Pattern.compile("채권최고액금?(\\d[\\d,]{0,14})원");
    private static final Pattern OWNER = Pattern.compile("소유자\\s*([가-힣]{2,5})(?![가-힣])");
    private static final Pattern HO = Pattern.compile("(?<![\\d-])(?:제\\s*)?(\\d{1,5})\\s*호(?![가-힣])");
    private static final Pattern LEDGER_UNIT_HO = Pattern.compile("호명칭(?:제)?(\\d{1,5})호");
    private static final Pattern LEDGER_UNIT_DONG = Pattern.compile("동명칭(?:제)?(\\d{1,4})동");
    private static final Pattern DONG = Pattern.compile("(?<![\\d가-힣])(?:제\\s*)?(\\d{1,4})\\s*동(?![가-힣])");
    private static final Pattern UNIT_HO = Pattern.compile("제(\\d{1,3})층제(\\d{1,5})호");
    private static final Pattern ROAD = Pattern.compile("(?<!\\S)(\\S*?(?:로|길))\\s*(\\d{1,5}(?:-\\d{1,5})?)(?![\\d동호층길번])");
    // 건축물대장 표에서는 동 이름과 숫자 사이에 열 제목 '지번'이 끼어 있다.
    private static final Pattern LOT = Pattern.compile("(?<!\\S)([가-힣][가-힣\\d]*(?:동|리|가))\\s*(?:지번\\s*)?(\\d{1,5}(?:-\\d{1,5})?)(?![\\d동호층번])");
    private static final Map<HousingType, Pattern> USAGE = new LinkedHashMap<>();

    static {
        USAGE.put(HousingType.APARTMENT, Pattern.compile("아파트"));
        USAGE.put(HousingType.MULTIPLEX, Pattern.compile("다세대주택|연립주택"));
        USAGE.put(HousingType.MULTI_HOUSEHOLD, Pattern.compile("다가구주택"));
        USAGE.put(HousingType.DETACHED, Pattern.compile("단독주택"));
        USAGE.put(HousingType.OFFICETEL, Pattern.compile("오피스텔"));
        USAGE.put(HousingType.NON_RESIDENTIAL, Pattern.compile("(?:제[12]종)?근린생활시설|사무소|숙박시설|판매시설"));
    }

    private static final String MISSING_LEDGER = "건축물대장을 읽지 못했어요. 용도·위반건축물 질문은 직접 답해 주세요.";

    private RegistryParser() { }

    /** ledgerText는 건축물대장을 읽지 못했으면 null */
    public static OcrResponseDTO parse(String address, String registryText, String ledgerText, LocalDate today) {
        List<String> warnings = new ArrayList<>();
        List<Line> lines = lines(registryText);
        int gapguAt = indexOf(lines, "갑구", "소유권에관한사항");
        int eulguAt = indexOf(lines, "을구", "소유권이외의권리에관한사항");
        int endAt = -1;
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).compact.contains("이하여백")) { endAt = i; break; }
        int firstSection = gapguAt >= 0 ? gapguAt : eulguAt >= 0 ? eulguAt : lines.size();
        List<Line> title = lines.subList(0, firstSection);
        Section gapgu = section("갑구", lines, gapguAt, eulguAt > gapguAt ? eulguAt : -1, endAt);
        Section eulgu = section("을구", lines, eulguAt, -1, endAt);
        if (gapgu == null && eulgu == null && !compact(registryText).contains("등기사항")) throw new OcrUnreadableException();
        if (gapgu == null || !gapgu.complete) warnings.add("갑구 일부를 읽지 못했어요. 해당 질문은 직접 답해 주세요.");
        if (eulgu == null || !eulgu.complete) warnings.add("을구 일부를 읽지 못했어요. 해당 질문은 직접 답해 주세요.");

        Unit input = Unit.of(address);
        AddressMatch addressMatch = compareAddress(input, title);

        String ledger = null;
        boolean ledgerMismatch = false;
        boolean ledgerUnverified = false;
        Set<String> ledgerHos = ledgerText == null ? Set.of() : ledgerUnitHos(ledgerText);
        Set<String> ledgerDongs = ledgerText == null ? Set.of() : ledgerUnitDongs(ledgerText);
        Boolean ledgerBaseMatch = ledgerText == null ? null : sameBase(input.text, ledgerText);
        if (ledgerText == null || !compact(ledgerText).contains("건축물대장")) {
            warnings.add(MISSING_LEDGER);
        } else if (Boolean.FALSE.equals(ledgerBaseMatch)
                || (input.ho != null && !ledgerHos.isEmpty() && !ledgerHos.contains(input.ho))
                || (input.dong != null && !ledgerDongs.isEmpty() && !ledgerDongs.contains(input.dong))) {
            ledgerMismatch = true;
            warnings.add("건축물대장의 주소·동·호수가 입력한 주소와 달라요. 용도·위반건축물 질문은 직접 답해 주세요.");
        } else if (!Boolean.TRUE.equals(ledgerBaseMatch)
                || (input.ho != null && ledgerHos.size() != 1)
                || (input.ho == null && compact(ledgerText).contains("집합건축물대장"))
                || (input.dong != null && ledgerDongs.size() != 1)
                || (input.dong == null && !ledgerDongs.isEmpty())) {
            ledgerUnverified = true;
            warnings.add("건축물대장의 주소·동·호수를 확인하지 못했어요. 용도·위반건축물 질문은 직접 답해 주세요.");
        } else {
            ledger = compact(ledgerText);
        }
        Map<HousingType, String> ledgerUsage = ledger == null ? Map.of() : currentLedgerUsage(ledger);
        HousingType housingType = ledgerUsage.size() == 1 ? ledgerUsage.keySet().iterator().next() : null;

        Mortgage mortgage = mortgage(eulgu);
        List<Answer> answers = List.of(
                address(addressMatch),
                ledgerMismatch || ledgerUnverified ? unconfirmed("T2_USAGE", "건축물대장 주소·동·호수를 확인해 주세요")
                        : usage(ledgerUsage, usage(String.join("", title.stream().map(Line::compact).toList()))),
                owner(gapgu),
                recentOwner(gapgu, today),
                keyword("A3_TRUST", gapgu, "신탁", List.of("신탁")),
                keyword("A4_SEIZURE", gapgu, "압류·가압류·가처분·가등기·경매",
                        List.of("가압류", "압류", "가처분", "가등기", "경매")),
                mortgage.answer,
                keyword("B2_LEASE_REG", eulgu, "주택임차권", List.of("임차권")),
                keyword("B3_JEONSE_RIGHT", eulgu, "전세권", List.of("전세권")),
                ledger == null ? unconfirmed("R1_VIOLATION", null)
                        : ledger.contains("위반건축물") ? confirmed("R1_VIOLATION", "YES", "건축물대장에 위반건축물 표시 있음")
                        : confirmed("R1_VIOLATION", "NO", "건축물대장에 위반건축물 표시 없음"));
        return new OcrResponseDTO(addressMatch, answers, mortgage.max, housingType, warnings);
    }

    // ---- 표제부: 주소 ----

    private static AddressMatch compareAddress(Unit input, List<Line> title) {
        String header = title.stream().map(l -> l.raw).filter(l -> l.contains("건물]")).findFirst().orElse("");
        String titleText = String.join(" ", title.stream().map(l -> l.raw).toList());
        Unit doc = Unit.of(header);
        String ho = doc.ho;
        String dong = doc.dong;
        if (ho == null) {
            Matcher m = UNIT_HO.matcher(compact(titleText));
            if (m.find()) ho = m.group(2);
        }
        if (dong == null) {
            Matcher m = Pattern.compile("제(\\d{1,4})동(?![가-힣])").matcher(compact(titleText));
            if (m.find()) dong = m.group(1);
        }
        Boolean base = sameBase(input.text, titleText);
        if (Boolean.FALSE.equals(base) || differs(input.ho, ho) || differs(input.dong, dong)) return AddressMatch.MISMATCH;
        boolean hoMatches = input.ho == null ? ho == null : input.ho.equals(ho);
        return Boolean.TRUE.equals(base) && hoMatches ? AddressMatch.MATCH : AddressMatch.UNKNOWN;
    }

    /** 도로명끼리, 지번끼리 비교. 하나라도 같으면 true, 비교할 수 있는데 모두 다르면 false, 비교 못 하면 null */
    private static Boolean sameBase(String input, String doc) {
        Boolean result = null;
        for (Pattern p : new Pattern[]{ROAD, LOT}) {
            Set<String> a = pairs(p, input), b = pairs(p, doc);
            if (a.isEmpty() || b.isEmpty()) continue;
            a.retainAll(b);
            if (!a.isEmpty()) return true;
            result = false;
        }
        return result;
    }

    private static Set<String> pairs(Pattern p, String text) {
        Set<String> set = new HashSet<>();
        Matcher m = p.matcher(text);
        while (m.find()) set.add(m.group(1) + " " + m.group(2));
        return set;
    }

    /** 변동사항 등의 다른 호수는 제외하고 대장 호명칭에서 대상 호수를 찾는다. */
    private static Set<String> ledgerUnitHos(String text) {
        Set<String> set = new HashSet<>();
        Matcher m = LEDGER_UNIT_HO.matcher(ledgerHeader(text));
        while (m.find()) set.add(m.group(1));
        return set;
    }

    /** 대장 머리글의 동명칭만 읽어 변동사항의 다른 동을 대상에서 제외한다. */
    private static Set<String> ledgerUnitDongs(String text) {
        Set<String> set = new HashSet<>();
        Matcher m = LEDGER_UNIT_DONG.matcher(ledgerHeader(text));
        while (m.find()) set.add(m.group(1));
        return set;
    }

    private static String ledgerHeader(String text) {
        String header = compact(text);
        for (String marker : List.of("전유부분", "변동사항")) {
            int at = header.indexOf(marker);
            if (at >= 0) header = header.substring(0, at);
        }
        return header;
    }

    private static boolean differs(String a, String b) {
        return a != null && b != null && !a.equals(b);
    }

    private static Answer address(AddressMatch match) {
        return switch (match) {
            case MATCH -> confirmed("T1_ADDRESS", "YES", "표제부 주소·동·호수가 입력 주소와 같음");
            case MISMATCH -> confirmed("T1_ADDRESS", "NO", "표제부 주소·동·호수가 입력 주소와 다름");
            case UNKNOWN -> unconfirmed("T1_ADDRESS", "표제부 주소를 입력 주소와 비교하지 못함");
        };
    }

    // ---- 용도 (건축물대장 우선, 없으면 표제부) ----

    /** 집합건축물대장은 전유부분의 현재 용도만 읽고, 일반대장은 기존 주용도 판정을 사용한다. */
    private static Map<HousingType, String> currentLedgerUsage(String ledger) {
        int start = ledger.indexOf("전유부분");
        if (start < 0) return ledger.contains("집합건축물대장") ? Map.of() : usage(ledger);
        int end = ledger.length();
        for (String marker : List.of("공용부분", "변동사항")) {
            int at = ledger.indexOf(marker, start + "전유부분".length());
            if (at >= 0 && at < end) end = at;
        }
        return usage(ledger.substring(start, end));
    }

    /** 가격표의 아파트 문구를 제외하고 주거·비주거 용도 표현을 찾는다. */
    private static Map<HousingType, String> usage(String compactText) {
        // 가격표의 '공동주택(아파트)가격'은 해당 호실의 건물 용도가 아니다.
        String usageText = compactText.replaceAll("공동주택\\(?아파트\\)?가격", "");
        Map<HousingType, String> found = new EnumMap<>(HousingType.class);
        USAGE.forEach((type, p) -> {
            Matcher m = p.matcher(usageText);
            if (m.find()) found.put(type, m.group());
        });
        if (found.containsKey(HousingType.MULTI_HOUSEHOLD)) found.remove(HousingType.DETACHED); // 다가구주택은 단독주택(다가구주택)으로 적힌다
        if (!found.containsKey(HousingType.OFFICETEL) && usageText.contains("업무시설")) found.putIfAbsent(HousingType.NON_RESIDENTIAL, "업무시설");
        if (found.size() > 1) { // 층별 용도가 섞여 있으면 주용도만 본다
            Matcher m = Pattern.compile("주용도([가-힣()·,]{0,20})").matcher(usageText);
            if (m.find()) {
                Map<HousingType, String> main = usage(m.group(1));
                if (main.size() == 1) return main;
            }
        }
        return found;
    }

    private static Answer usage(Map<HousingType, String> ledger, Map<HousingType, String> title) {
        Map<HousingType, String> chosen;
        String source;
        if (ledger.size() == 1) {
            chosen = ledger;
            source = "건축물대장 용도";
            if (title.size() == 1 && residential(title.keySet().iterator().next()) != residential(ledger.keySet().iterator().next())) {
                return unconfirmed("T2_USAGE", "표제부와 건축물대장의 용도가 서로 다름");
            }
        } else if (ledger.isEmpty() && title.size() == 1) {
            chosen = title;
            source = "표제부 건물내역";
        } else {
            return unconfirmed("T2_USAGE", ledger.size() + title.size() > 1 ? "용도가 여러 개 적혀 있어 계약할 집의 용도를 확인하지 못함" : null);
        }
        HousingType type = chosen.keySet().iterator().next();
        String evidence = source + ": " + chosen.get(type);
        if (type == HousingType.OFFICETEL) return unconfirmed("T2_USAGE", evidence + " (주거용인지 확인해 주세요)");
        return confirmed("T2_USAGE", residential(type) ? "YES" : "NO", evidence);
    }

    private static boolean residential(HousingType type) {
        return type != HousingType.NON_RESIDENTIAL && type != HousingType.OFFICETEL;
    }

    // ---- 갑구 ----

    private static Entry latestOwnership(Section gapgu) {
        Entry latest = null;
        for (Entry e : gapgu.live()) {
            String text = e.clean();
            if ((text.contains("소유권보존") || text.contains("소유권이전") || text.contains("소유권일부이전"))
                    && !text.contains("가등기") && !text.contains("청구권")) latest = e;
        }
        return latest;
    }

    private static Answer owner(Section gapgu) {
        Entry latest = gapgu == null ? null : latestOwnership(gapgu);
        Matcher m = latest == null ? null : OWNER.matcher(latest.spaced);
        if (m == null || !m.find()) return unconfirmed("A1_OWNER", null);
        String name = m.group(1);
        // 이름은 저장·로그 없이 화면 비교용으로만 가려서 보여준다 (NFR-06, docs/03 예시)
        String masked = name.charAt(0) + "○".repeat(name.length() - 1);
        return unconfirmed("A1_OWNER", "등기부상 소유자: " + masked + " (계약 상대방과 같은지 확인해 주세요)");
    }

    private static Answer recentOwner(Section gapgu, LocalDate today) {
        String code = "A2_RECENT_OWNER";
        Entry latest = gapgu == null ? null : latestOwnership(gapgu);
        Matcher m = latest == null ? null : DATE.matcher(latest.text);
        if (m == null || !m.find()) return unconfirmed(code, gapgu == null ? null : "갑구에서 소유권 접수일을 찾지 못함");
        LocalDate received;
        try {
            received = LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        } catch (java.time.DateTimeException e) {
            return unconfirmed(code, "갑구에서 소유권 접수일을 찾지 못함");
        }
        String purpose = latest.clean().contains("소유권보존") ? "소유권보존" : "소유권이전";
        String evidence = "갑구 " + latest.no + "번 " + purpose + " " + m.group() + " 접수";
        if (!received.isBefore(today.minusMonths(3))) return confirmed(code, "YES", evidence);
        // 뒤쪽을 못 읽었으면 더 최근 이전 기록이 있을 수 있다
        return gapgu.complete ? confirmed(code, "NO", evidence) : unconfirmed(code, "갑구 일부를 읽지 못함");
    }

    // ---- 갑구·을구 공통: 유효한(말소 안 된) 기록 찾기 ----

    private static Answer keyword(String code, Section section, String record, List<String> keywords) {
        if (section == null) return unconfirmed(code, null);
        String label = section.name;
        for (Entry e : section.live()) {
            for (String k : keywords) {
                if (e.clean().contains(k)) return confirmed(code, "YES", label + " " + e.no + "번 " + k + " 기록");
            }
        }
        String loose = clean(section.loose);
        if (keywords.stream().anyMatch(loose::contains)) return unconfirmed(code, label + "의 " + record + " 기록이 지금도 유효한지 확인하지 못함");
        if (section.complete) return confirmed(code, "NO", label + "에 유효한 " + record + " 기록 없음");
        return unconfirmed(code, label + " 일부를 읽지 못함");
    }

    private record Mortgage(Answer answer, Integer max) { }

    private static Mortgage mortgage(Section eulgu) {
        Answer base = keyword("B1_MORTGAGE", eulgu, "근저당권", List.of("근저당"));
        if (!"YES".equals(base.answer())) return new Mortgage(base, "NO".equals(base.answer()) ? 0 : null);
        Map<String, Long> amounts = new LinkedHashMap<>(); // 순위번호 → 채권최고액(원). 부기(1-1 변경)가 있으면 나중 값으로 바뀐다
        Map<String, String> shown = new LinkedHashMap<>();
        for (Entry e : eulgu.live()) {
            if (!e.clean().contains("근저당")) continue;
            String no = e.no.split("-")[0];
            Matcher m = MAX_AMOUNT.matcher(e.text);
            if (m.find()) {
                amounts.put(no, Long.parseLong(m.group(1).replace(",", "")));
                shown.put(no, "을구 " + no + "번 근저당권 채권최고액 금" + m.group(1) + "원");
            } else {
                amounts.putIfAbsent(no, null);
                shown.putIfAbsent(no, "을구 " + no + "번 근저당권 (채권최고액을 읽지 못함)");
            }
        }
        Answer answer = confirmed("B1_MORTGAGE", "YES", String.join(", ", shown.values()));
        if (amounts.containsValue(null)) return new Mortgage(answer, null);
        long man = (amounts.values().stream().mapToLong(Long::longValue).sum() + 9_999) / 10_000; // 만원 미만은 올림 (빚을 적게 보지 않도록)
        return new Mortgage(answer, man > Integer.MAX_VALUE ? null : (int) man); // 말이 안 되게 큰 값은 못 읽은 것으로 본다
    }

    // ---- 텍스트 구조 ----

    private record Line(String raw, String compact) { }

    private record Entry(String no, String text, String spaced) {
        String clean() { return RegistryParser.clean(text); }
    }

    private record Section(String name, List<Entry> entries, String loose, Set<String> cancelled, boolean complete) {
        List<Entry> live() {
            return entries.stream()
                    .filter(e -> !cancelled.contains(e.no) && !cancelled.contains(e.no.split("-")[0]))
                    .toList();
        }
    }

    private static String clean(String compactText) {
        return CANCEL_REF.matcher(compactText).replaceAll("").replace("신탁재산의귀속", "");
    }

    private static List<Line> lines(String text) {
        List<Line> lines = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String c = compact(raw);
            if (!c.isEmpty()) lines.add(new Line(raw.trim().replaceAll("\\s+", " "), c));
        }
        return lines;
    }

    private static String compact(String text) {
        return text.replaceAll("\\s+", "");
    }

    private static int indexOf(List<Line> lines, String name, String subtitle) {
        for (int i = 0; i < lines.size(); i++) {
            String c = lines.get(i).compact;
            if (c.contains(name) && (c.contains(subtitle) || c.contains("【" + name + "】") || c.contains("[" + name + "]") || c.equals(name))) return i;
        }
        return -1;
    }

    /** 다음 구역 머리글이나 "이하여백"까지. 둘 다 못 찾으면 뒤가 잘렸을 수 있어 complete=false */
    private static Section section(String name, List<Line> lines, int start, int next, int endAt) {
        if (start < 0) return null;
        int end = lines.size();
        boolean complete = false;
        if (next > start) { end = next; complete = true; }
        if (endAt > start && endAt < end) { end = endAt; complete = true; }
        List<Line> body = lines.subList(start + 1, end);

        List<Entry> entries = new ArrayList<>();
        StringBuilder loose = new StringBuilder();
        StringBuilder text = null, spaced = null;
        String no = null;
        for (int i = 0; i < body.size(); i++) {
            String c = body.get(i).compact;
            String r = body.get(i).raw;
            if (BARE_NUMBER.matcher(c).matches() && i + 1 < body.size() && ENTRY.matcher(c + body.get(i + 1).compact).find()) {
                c = c + body.get(i + 1).compact; // 순위번호만 따로 한 줄로 읽힌 경우
                r = r + " " + body.get(i + 1).raw;
                i++;
            }
            Matcher m = ENTRY.matcher(c);
            if (m.find()) {
                if (no != null) entries.add(new Entry(no, text.toString(), spaced.toString()));
                no = m.group(1);
                text = new StringBuilder(c.substring(m.end()));
                spaced = new StringBuilder(r);
            } else if (no != null) {
                text.append(c);
                spaced.append(' ').append(r);
            } else {
                loose.append(c);
            }
        }
        if (no != null) entries.add(new Entry(no, text.toString(), spaced.toString()));

        Set<String> cancelled = new HashSet<>();
        for (Entry e : entries) {
            Matcher m = CANCEL_REF.matcher(e.text);
            while (m.find()) cancelled.add(m.group(1));
        }
        if (entries.isEmpty() && loose.toString().contains("기록사항없음")) complete = true;
        return new Section(name, entries, loose.toString(), cancelled, complete);
    }

    private record Unit(String text, String dong, String ho) {
        static Unit of(String text) {
            Matcher h = HO.matcher(text);
            Matcher d = DONG.matcher(text);
            String ho = null;
            while (h.find()) ho = h.group(1); // 호수는 주소 맨 끝에 온다
            return new Unit(text, d.find() ? d.group(1) : null, ho);
        }
    }

    private static Answer confirmed(String code, String answer, String evidence) {
        return new Answer(code, Status.CONFIRMED, answer, evidence);
    }

    private static Answer unconfirmed(String code, String evidence) {
        return new Answer(code, Status.UNCONFIRMED, null, evidence);
    }
}
