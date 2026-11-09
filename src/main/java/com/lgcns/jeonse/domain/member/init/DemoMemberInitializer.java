package com.lgcns.jeonse.domain.member.init;

import com.lgcns.jeonse.domain.member.entity.Member;
import com.lgcns.jeonse.domain.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// FE "데모 계정으로 시작" 버튼용 회원 (docs/03 6장). 이름은 FE readProfile과 같다. 기록은 미리 넣지 않는다
@Configuration
public class DemoMemberInitializer {
    static final String EMAIL = "demo@example.com";

    @Bean
    CommandLineRunner seedDemoMember(MemberRepository repository, @Value("${demo.password}") String password) {
        return args -> {
            if (repository.existsByEmail(EMAIL)) return;
            repository.save(new Member(EMAIL, new BCryptPasswordEncoder().encode(password), "데모 사용자"));
        };
    }
}
