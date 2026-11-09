package com.lgcns.jeonse.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터({@code Long memberId})에 로그인 회원 id를 넣는다.
 * required=false면 토큰이 없을 때 null(비회원)로 통과한다. 토큰이 있는데 잘못됐으면 항상 401.
 * 이 파라미터가 없는 API(질문·일정표·OCR)는 Authorization 헤더를 아예 보지 않는다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
    boolean required() default true;
}
