package com.groute.groute_server.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.groute.groute_server.common.config.OffsetDateTimeProvider;

/**
 * DataJpa 슬라이스 테스트 베이스 클래스.
 *
 * <p>PostgreSQLContainer를 띄워 실제 DB에서 Flyway 마이그레이션 전체를 적용한 뒤 테스트한다. {@code @ServiceConnection}이
 * 컨테이너의 JDBC URL을 자동으로 DataSource에 연결하므로 별도 프로퍼티 설정이 필요 없다.
 *
 * <p>{@code @DataJpaTest}는 {@code @Component} 빈을 제외하므로, JPA Auditing에 필요한 {@code
 * OffsetDateTimeProvider}를 명시적으로 import한다.
 *
 * <p>{@code src/test/resources/application.yaml}이 Flyway를 비활성화하므로, 이 베이스 클래스에서 {@code
 * spring.flyway.enabled=true}로 재활성화한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(OffsetDateTimeProvider.class)
@TestPropertySource(properties = "spring.flyway.enabled=true")
public abstract class DataJpaTestBase {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
