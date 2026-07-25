package com.igdm.repository;

import com.igdm.entity.InstagramAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstagramAccountRepository extends JpaRepository<InstagramAccount, UUID> {

    /**
     * Find an Instagram account by its Instagram-scoped user ID.
     * Used by the consumer to look up which account a webhook event belongs to.
     */
    Optional<InstagramAccount> findByIgUserId(String igUserId);

    /**
     * Find all connected accounts for a given SaaS user.
     */
    List<InstagramAccount> findByUserIdAndIsConnectedTrue(UUID userId);

    boolean existsByIgUserId(String igUserId);
}
