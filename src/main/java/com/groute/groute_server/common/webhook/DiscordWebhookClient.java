package com.groute.groute_server.common.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.CompletableFuture;

/**
 * Discord 웹훅 전송 클라이언트.
 *
 * <p>{@code discord.webhook.enabled}가 {@code false}이거나 URL이 비어 있으면 전송을 건너뛴다.
 * 네트워크 오류는 로그로만 남기고 호출자에게 전파하지 않아, 에러 알림 실패가 애플리케이션 응답을
 * 막지 않도록 한다. 전송은 {@link Async}로 처리되어 요청 처리 스레드를 블로킹하지 않는다.</p>
 */
@Slf4j
@Component
public class DiscordWebhookClient {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private final DiscordWebhookProperties properties;
    private final RestClient restClient;

    public DiscordWebhookClient(DiscordWebhookProperties properties) {
        this.properties = properties;
        this.restClient = buildRestClient();
    }

    @Async
    public CompletableFuture<Void> send(DiscordEmbed embed) {
        if (!isActive()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            restClient.post()
                    .uri(properties.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(DiscordWebhookPayload.of(embed))
                    .retrieve()
                    .toBodilessEntity();
            return CompletableFuture.completedFuture(null);
        } catch (RestClientException e) {
            log.warn("Discord 웹훅 전송 실패: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private boolean isActive() {
        return properties.enabled()
                && properties.url() != null
                && !properties.url().isBlank();
    }

    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
