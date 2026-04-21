package com.groute.groute_server.common.webhook;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Discord 웹훅 embed 페이로드.
 *
 * <p>Discord 공식 스키마의 필드명을 그대로 직렬화하도록 필드명을 맞춘다. {@code null} 필드는 직렬화에서 제외된다.
 *
 * @see <a href="https://discord.com/developers/docs/resources/message#embed-object">Discord Embed
 *     Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordEmbed(
        String title,
        String description,
        Integer color,
        OffsetDateTime timestamp,
        List<Field> fields) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Field(String name, String value, Boolean inline) {}
}
