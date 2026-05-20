package com.groute.groute_server.record.application.port.in.star;

import java.util.List;

public interface UploadStarImageUseCase {

    List<UploadStarImageResult> upload(UploadStarImageCommand command);
}
