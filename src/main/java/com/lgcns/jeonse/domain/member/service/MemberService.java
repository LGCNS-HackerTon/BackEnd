package com.lgcns.jeonse.domain.member.service;

import com.lgcns.jeonse.domain.member.dto.MemberRequestDTO;
import com.lgcns.jeonse.domain.member.dto.MemberResponseDTO;
import com.lgcns.jeonse.domain.member.entity.Member;
import com.lgcns.jeonse.domain.member.repository.MemberRepository;
import com.lgcns.jeonse.global.exception.ApiException;
import com.lgcns.jeonse.global.exception.InvalidInputException;
import com.lgcns.jeonse.global.security.JwtProvider;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** docs/03 6~8번: 회원가입·로그인·내 정보. */
@Service
public class MemberService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final MemberRepository repository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberService(MemberRepository repository, JwtProvider jwtProvider) {
        this.repository = repository;
        this.jwtProvider = jwtProvider;
    }

    public MemberResponseDTO.Token signup(MemberRequestDTO.Signup request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 30) throw new InvalidInputException("이름을 1~30자로 입력해 주세요.", "name");
        String email = normalize(request.email());
        if (email.length() > 254 || !EMAIL.matcher(email).matches())
            throw new InvalidInputException("이메일 형식을 확인해 주세요.", "email");
        String password = request.password();
        if (password == null || password.length() < 8)
            throw new InvalidInputException("비밀번호는 8자 이상으로 입력해 주세요.", "password");
        // BCrypt는 72바이트까지만 쓴다
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new InvalidInputException("비밀번호가 너무 길어요. 영문·숫자 기준 72자 이하로 입력해 주세요.", "password");
        if (repository.existsByEmail(email)) throw duplicateEmail();
        try {
            return token(repository.saveAndFlush(new Member(email, passwordEncoder.encode(password), name)));
        } catch (DataIntegrityViolationException e) {
            throw duplicateEmail(); // 같은 이메일로 동시에 가입하면 UNIQUE 제약이 막는다
        }
    }

    public MemberResponseDTO.Token login(MemberRequestDTO.Login request) {
        String email = normalize(request.email());
        if (email.isEmpty()) throw new InvalidInputException("이메일을 입력해 주세요.", "email");
        String password = request.password();
        if (password == null || password.isEmpty()) throw new InvalidInputException("비밀번호를 입력해 주세요.", "password");
        return repository.findByEmail(email)
                .filter(member -> passwordEncoder.matches(password, member.getPasswordHash()))
                .map(this::token)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "이메일 또는 비밀번호를 확인해 주세요.", "password"));
    }

    public MemberResponseDTO.Info me(long memberId) {
        return repository.findById(memberId).map(MemberResponseDTO.Info::from).orElseThrow(ApiException::unauthorized);
    }

    private MemberResponseDTO.Token token(Member member) {
        return new MemberResponseDTO.Token(jwtProvider.issue(member.getId()), MemberResponseDTO.Info.from(member));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException duplicateEmail() {
        return new ApiException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일이에요.", "email");
    }
}
