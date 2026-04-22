package com.groute.groute_server.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groute.groute_server.auth.entity.SocialAccount;
import com.groute.groute_server.auth.enums.SocialProvider;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUid(
            SocialProvider provider, String providerUid);
}
