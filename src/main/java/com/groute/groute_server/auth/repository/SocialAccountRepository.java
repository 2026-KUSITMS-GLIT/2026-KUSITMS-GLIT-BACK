package com.groute.groute_server.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groute.groute_server.auth.entity.SocialAccount;
import com.groute.groute_server.auth.enums.SocialProvider;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUid(
            SocialProvider provider, String providerUid);

    /**
     * 회원 탈퇴 hard delete 배치(MYP-005) 진입. 해당 사용자의 모든 소셜 계정 매핑 row 물리 삭제.
     *
     * <p>복구 불가. 같은 사용자가 재가입하면 새 SocialAccount row가 생성된다(재가입 차단은 미적용 정책).
     *
     * @return 삭제된 row 수 (로깅용)
     */
    @Modifying
    @Query("DELETE FROM SocialAccount s WHERE s.user.id = :userId")
    int hardDeleteAllByUserId(@Param("userId") Long userId);
}
