package com.groute.groute_server.common.docs;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.groute.groute_server.common.exception.ErrorCode;

/**
 * ErrorCode 정적 조회 페이지 Controller.
 *
 * <p>{@code prod} 프로파일에서는 빈이 등록되지 않아 경로가 404 처리된다. enum 값을 런타임에 그대로 조회하므로, 새로운 {@link ErrorCode} 추가
 * 시 별도 작업 없이 페이지에 자동 반영된다.
 */
@Controller
@Profile("!prod")
public class ErrorCodeDocsController {

    @GetMapping("/docs/error-code")
    public String errorCodeDocs(Model model) {
        model.addAttribute("errorCodes", ErrorCode.values());
        return "docs/error-code";
    }
}
