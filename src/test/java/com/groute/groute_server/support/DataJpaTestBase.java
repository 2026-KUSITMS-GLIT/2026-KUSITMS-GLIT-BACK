package com.groute.groute_server.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * DataJpa 슬라이스 테스트 베이스 클래스.
 *
 * <p>PostgreSQLContainer를 띄워 실제 DB에서 Flyway 마이그레이션 전체를 적용한 뒤 테스트한다.
 * {@code @ServiceConnection}이 컨테이너의 JDBC URL을 자동으로 DataSource에 연결하므로 별도 프로퍼티 설정이 필요 없다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public abstract class DataJpaTestBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}