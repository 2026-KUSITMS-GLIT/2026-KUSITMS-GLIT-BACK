package com.groute.groute_server.common.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Discord 웹훅 전송 설정.
 *
 * <p>{@code enabled}가 {@code false}이거나 {@code url}이 비어 있으면 전송을 건너뛴다. 환경별 URL은 SSM Parameter Store의
 * {@code /groute/{env}/DISCORD_WEBHOOK_URL}에서 주입되며, 배포 파이프라인이 컨테이너 환경변수 {@code
 * DISCORD_WEBHOOK_URL}로 변환한다.
 */
@ConfigurationProperties(prefix = "discord.webhook")
public record DiscordWebhookProperties(boolean enabled, String url) {}
