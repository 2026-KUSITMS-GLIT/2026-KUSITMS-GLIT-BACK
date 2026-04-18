package com.groute.groute_server.common.webhook;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unhandled 서버 에러를 Discord 웹훅으로 알린다.
 *
 * <p>PII 유출 방지를 위해 요청 바디/헤더/쿼리스트링은 포함하지 않고,
 * HTTP method, path, 프로파일, 예외 타입·메시지, 스택 트레이스 헤드만 전송한다.</p>
 */
@Component
@RequiredArgsConstructor
public class ErrorWebhookNotifier {

    private static final int COLOR_RED = 0xE74C3C;
    private static final int MAX_STACK_LINES = 20;
    private static final int MAX_MESSAGE_LEN = 500;
    private static final int MAX_STACK_CHARS = 3500;

    private final DiscordWebhookClient webhookClient;
    private final Environment environment;

    public void notifyUnhandledError(Exception e, HttpServletRequest request) {
        DiscordEmbed embed = new DiscordEmbed(
                "🚨 서버가 처리하지 못하는 에러가 발생했어요!",
                buildDescription(e),
                COLOR_RED,
                OffsetDateTime.now(),
                List.of(
                        new DiscordEmbed.Field("Profile", resolveProfile(), true),
                        new DiscordEmbed.Field("Method", request.getMethod(), true),
                        new DiscordEmbed.Field("Path", request.getRequestURI(), false),
                        new DiscordEmbed.Field("Exception", e.getClass().getName(), false)
                )
        );
        webhookClient.send(embed);
    }

    private String buildDescription(Throwable e) {
        return "**Message:** " + safeMessage(e) + "\n```\n" + formatStackHead(e) + "\n```";
    }

    private String safeMessage(Throwable e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return "(no message)";
        }
        return msg.length() > MAX_MESSAGE_LEN
                ? msg.substring(0, MAX_MESSAGE_LEN) + "..."
                : msg;
    }

    private String formatStackHead(Throwable e) {
        String joined = Arrays.stream(e.getStackTrace())
                .limit(MAX_STACK_LINES)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
        return joined.length() > MAX_STACK_CHARS
                ? joined.substring(0, MAX_STACK_CHARS) + "\n... (truncated)"
                : joined;
    }

    private String resolveProfile() {
        String[] active = environment.getActiveProfiles();
        return active.length == 0 ? "default" : String.join(",", active);
    }
}