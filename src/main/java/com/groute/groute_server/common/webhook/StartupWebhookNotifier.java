package com.groute.groute_server.common.webhook;

import java.time.OffsetDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 기동 완료 시 Discord 웹훅 헬스체크 메시지를 전송한다.
 *
 * <p>{@link ApplicationReadyEvent}를 수신하여 활성 프로파일과 함께 기동 알림을 보낸다. 웹훅 비활성 상태면 조용히 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupWebhookNotifier {

    private static final int COLOR_PINK = 0xFF69B4;

    private final DiscordWebhookClient webhookClient;
    private final DiscordWebhookProperties properties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void sendStartupHeartbeat() {
        if (!properties.enabled()) {
            log.debug("Discord 웹훅 비활성 상태, 기동 헬스체크 생략");
            return;
        }

        String profile = resolveActiveProfile();
        DiscordEmbed embed =
                new DiscordEmbed(
                        "\uD83C\uDF80 서버 기동 완료",
                        "`" + profile + "` 프로파일 서버가 정상 기동되었습니다.",
                        COLOR_PINK,
                        OffsetDateTime.now(),
                        null);

        webhookClient
                .send(embed)
                .thenRun(() -> log.info("Discord 헬스체크 메시지 전송 성공 (profile={})", profile));
    }

    private String resolveActiveProfile() {
        String[] active = environment.getActiveProfiles();
        return active.length == 0 ? "default" : String.join(",", active);
    }
}
