package com.groute.groute_server.record.adapter.in.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groute.groute_server.common.annotation.CurrentUser;
import com.groute.groute_server.common.response.ApiResponse;
import com.groute.groute_server.record.adapter.in.web.dto.UploadStarImageRequest;
import com.groute.groute_server.record.adapter.in.web.dto.UploadStarImageResponse;
import com.groute.groute_server.record.application.port.in.star.UploadStarImageUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "StarImage", description = "STAR 이미지 업로드")
@RestController
@RequestMapping("/api/star-records")
@RequiredArgsConstructor
public class StarImageController {

    private final UploadStarImageUseCase uploadStarImageUseCase;

    @Operation(
            summary = "이미지 업로드 Presigned URL 발급",
            description =
                    "STAR R 단계 이미지 업로드용 S3 Presigned PUT URL을 발급한다."
                            + " 응답의 presignedUrl로 직접 PUT 요청해 업로드한다. STAR당 최대 2장.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "발급 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "mimeType 형식 오류 / sizeBytes 초과 / 이미지 2장 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "미인증"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인 소유가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "심화기록을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "이미 완료된 심화기록")
    })
    @PostMapping("/{starRecordId}/images/presigned-url")
    public ApiResponse<UploadStarImageResponse> uploadImage(
            @CurrentUser Long userId,
            @PathVariable Long starRecordId,
            @Valid @RequestBody UploadStarImageRequest request) {
        return ApiResponse.ok(
                "이미지 업로드 URL 발급 성공",
                UploadStarImageResponse.from(
                        uploadStarImageUseCase.upload(request.toCommand(userId, starRecordId))));
    }
}