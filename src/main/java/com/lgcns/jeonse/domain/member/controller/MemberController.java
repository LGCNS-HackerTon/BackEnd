package com.lgcns.jeonse.domain.member.controller;

import com.lgcns.jeonse.domain.member.dto.MemberRequestDTO;
import com.lgcns.jeonse.domain.member.dto.MemberResponseDTO;
import com.lgcns.jeonse.domain.member.service.MemberService;
import com.lgcns.jeonse.global.security.LoginMember;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService service;
    public MemberController(MemberService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponseDTO.Token signup(@RequestBody MemberRequestDTO.Signup request) { return service.signup(request); }

    @PostMapping("/login")
    public MemberResponseDTO.Token login(@RequestBody MemberRequestDTO.Login request) { return service.login(request); }

    @GetMapping("/me")
    public MemberResponseDTO.Info me(@LoginMember Long memberId) { return service.me(memberId); }
}
