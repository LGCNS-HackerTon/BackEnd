package com.lgcns.jeonse.domain.member.dto;

import com.lgcns.jeonse.domain.member.entity.Member;

public class MemberResponseDTO {
    public record Token(String accessToken, Info member) { }

    public record Info(long id, String name, String email) {
        public static Info from(Member member) {
            return new Info(member.getId(), member.getName(), member.getEmail());
        }
    }
}
