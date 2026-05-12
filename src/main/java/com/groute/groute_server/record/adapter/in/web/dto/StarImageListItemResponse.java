package com.groute.groute_server.record.adapter.in.web.dto;

import com.groute.groute_server.record.application.port.in.star.QueryStarImagesResult;

public record StarImageListItemResponse(Long starImageId, String imageUrl) {

    public static StarImageListItemResponse from(QueryStarImagesResult result) {
        return new StarImageListItemResponse(result.imageId(), result.imageUrl());
    }
}
