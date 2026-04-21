package com.groute.groute_server.common.webhook;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Discord 웹훅 요청 바디. 최상위 페이로드로 하나 이상의 {@link DiscordEmbed}를 감싼다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordWebhookPayload(String username, String content, List<DiscordEmbed> embeds) {

    public static DiscordWebhookPayload of(DiscordEmbed embed) {
        return new DiscordWebhookPayload(null, null, List.of(embed));
    }
}
