package com.example.travelers.controller;

import com.example.travelers.dto.*;
import com.example.travelers.jwt.JwtTokenDto;
import com.example.travelers.service.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {
    private final UsersService usersService;

    // 로그인 endpoint
    @PostMapping("/login")
    public ResponseEntity<JwtTokenDto> login(
            @RequestBody LoginRequestDto loginRequestDto
    ) {
        JwtTokenDto jwtTokenDto = usersService.loginUser(loginRequestDto);
        return ResponseEntity.ok(jwtTokenDto);
    }

    // 로그아웃 endpoint
    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout(
            HttpServletRequest request
    ) {
        MessageResponseDto responseDto = usersService.logoutUser(request);
        return ResponseEntity.ok(responseDto);
    }

    // 회원가입 endpoint
    @PostMapping("/register")
    public ResponseEntity<MessageResponseDto> register(
            @RequestBody RegisterRequestDto registerRequestDto
    ) {
        MessageResponseDto responseDto = usersService.registerUser(registerRequestDto);
        return ResponseEntity.ok(responseDto);
    }

    // 회원 정보 조회 (본인) endpoint
    // userId 가 아닌 본인 Jwt 를 사용해서 조회
    @GetMapping("/my-profile")
    public ResponseEntity<UserProfileDto> getMyProfile() {
        UserProfileDto user = usersService.getMyProfile();
        return ResponseEntity.ok(user);
    }

    // 회원 정보 리스트 조회 (관리자용) endpoint
    @GetMapping("/profile-list")
    public ResponseEntity<Page<UserProfileDto>> getProfileList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 페이지 번호를 1부터 시작하도록 조정
        page = Math.max(1, page);

        Page<UserProfileDto> userList = usersService.getProfileList(page - 1, size);
        return ResponseEntity.ok(userList);
    }

    // 프로필 이미지 업데이트 endpoint
    @PutMapping(value = "/update-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> userUpdateImage(
            @RequestParam(value = "image") MultipartFile multipartFile
    ) {
        // 이미지 업로드 서비스 호출
        MessageResponseDto responseDto = usersService.uploadProfileImage(multipartFile);

        return ResponseEntity.ok(responseDto);
    }

    // 사용자 정보 Password 수정 endpoint
    @PutMapping("/update-password")
    public ResponseEntity<MessageResponseDto> updatePassword(
            @RequestBody UpdatePasswordDto updatePasswordDto
    ) {
        MessageResponseDto responseDto = usersService.updatePassword(updatePasswordDto);
        return ResponseEntity.ok(responseDto);
    }

    // TODO 사용자 정보 email 수정 endpoint
//    @PutMapping("/update-email")

    // TODO 사용자 정보 mbti 수정 endpoint
//    @PutMapping("/update-mbti")


    // TODO 사용자 탈퇴 ( role = 회원 ) endpoint

    // TODO 사용자 삭제 ( role = 관리자 ) endpoint


}
