package com.groute.groute_server.record.application.port.in.star;

/** STAR 이미지 삭제 유스케이스. S3 오브젝트 + DB 레코드를 함께 hard delete 한다. */
public interface DeleteStarImageUseCase {

    void delete(Long userId, Long imageId);
}
