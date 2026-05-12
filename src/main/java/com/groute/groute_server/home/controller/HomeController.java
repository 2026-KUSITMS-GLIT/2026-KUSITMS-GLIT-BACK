package com.groute.groute_server.home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.home.dto.BrandingTitleResponse;
import com.groute.groute_server.home.dto.RadarResponse;
import com.groute.groute_server.home.service.HomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Home", description = "홈 화면")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "역량 레이더 차트 조회", description = "완료된 STAR 기록 기준 5대 역량 분포를 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰")
    })
    @GetMapping("/radar")
    public ApiResponse<RadarResponse> getRadar(@CurrentUser Long userId) {
        return ApiResponse.ok(RadarResponse.from(homeService.getRadar(userId)));
    }

    @Operation(summary = "직무 브랜딩 문구 조회", description = "사용자의 직무 브랜딩 문구를 반환한다. 신규 사용자는 null.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증 또는 만료된 액세스 토큰"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/branding")
    public ApiResponse<BrandingTitleResponse> getBranding(@CurrentUser Long userId) {
        return ApiResponse.ok(BrandingTitleResponse.from(homeService.getBrandingTitle(userId)));
    }
}
