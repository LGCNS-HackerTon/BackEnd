package com.lgcns.jeonse.global.security;

import com.lgcns.jeonse.domain.member.repository.MemberRepository;
import com.lgcns.jeonse.global.exception.ApiException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String BEARER = "Bearer ";
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public LoginMemberArgumentResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class);
    }

    @Override
    public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            if (parameter.getParameterAnnotation(LoginMember.class).required()) throw ApiException.unauthorized();
            return null;
        }
        if (!header.startsWith(BEARER)) throw ApiException.unauthorized();
        long memberId = jwtProvider.parse(header.substring(BEARER.length()).trim());
        // 서명·만료가 정상이어도 탈퇴 등으로 회원이 없으면 401 (docs/03 6.1·8번)
        if (!memberRepository.existsById(memberId)) throw ApiException.unauthorized();
        return memberId;
    }
}
