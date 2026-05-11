package com.groute.groute_server.record.domain;

import java.util.Objects;

import jakarta.persistence.*;

import com.groute.groute_server.common.entity.BaseTimeEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * STAR Result 단계 첨부 이미지.
 *
 * <p>STAR당 최대 2장 첨부 가능(REC005). R 페이지 텍스트 에어리어 하단에서만 첨부 가능.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "star_images")
public class StarImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "star_record_id", nullable = false)
    private StarRecord starRecord;

    /** S3 오브젝트 키. 삭제 시 S3 hard delete에 사용. */
    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    /** S3/CDN URL. */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /** MIME 타입. image/jpeg, image/png, image/webp만 허용(REC005). */
    @Column(name = "mime_type", nullable = false, length = 20)
    private String mimeType;

    /** 장당 최대 10MB = 10,485,760 bytes(REC005). */
    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    /** 표시 순서(0~1). N/2장 표시 UI에서 사용. */
    @Column(name = "sort_order", nullable = false)
    private Short sortOrder = 0;

    public static StarImage create(
            StarRecord starRecord,
            String imageKey,
            String imageUrl,
            String mimeType,
            Integer sizeBytes,
            Short sortOrder) {
        StarImage image = new StarImage();
        image.starRecord = Objects.requireNonNull(starRecord, "starRecord");
        image.imageKey = Objects.requireNonNull(imageKey, "imageKey");
        image.imageUrl = Objects.requireNonNull(imageUrl, "imageUrl");
        image.mimeType = Objects.requireNonNull(mimeType, "mimeType");
        image.sizeBytes = Objects.requireNonNull(sizeBytes, "sizeBytes");
        image.sortOrder = Objects.requireNonNull(sortOrder, "sortOrder");
        return image;
    }
}
