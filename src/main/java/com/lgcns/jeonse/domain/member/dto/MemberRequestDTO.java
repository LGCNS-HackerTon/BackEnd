package com.lgcns.jeonse.domain.member.dto;

public class MemberRequestDTO {
    public record Signup(String name, String email, String password) { }
    public record Login(String email, String password) { }
}
