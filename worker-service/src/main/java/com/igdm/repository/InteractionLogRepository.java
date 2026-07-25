package com.igdm.repository;

import com.igdm.entity.InteractionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InteractionLogRepository extends JpaRepository<InteractionLog, UUID> {

    /**
     * Check if a comment has already been processed (deduplication).
     */
    boolean existsByIgCommentId(String igCommentId);

    /**
     * Find a log entry by comment ID.
     */
    Optional<InteractionLog> findByIgCommentId(String igCommentId);

    /**
     * Check if we've already sent a DM to this commenter from this rule.
     * Used for "first_comment" trigger type deduplication.
     */
    boolean existsByIgCommenterIdAndRuleIdAndStatus(String igCommenterId, UUID ruleId, String status);

    /**
     * Paginated interaction logs for the dashboard, filtered by rule.
     */
    Page<InteractionLog> findByRuleIdOrderByCreatedAtDesc(UUID ruleId, Pageable pageable);

    /**
     * Paginated interaction logs for a set of rules (for a user's dashboard).
     */
    @Query("SELECT l FROM InteractionLog l WHERE l.rule.igAccount.user.id = :userId ORDER BY l.createdAt DESC")
    Page<InteractionLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * Count DMs sent for a specific rule within a time range (for analytics).
     */
    @Query("SELECT COUNT(l) FROM InteractionLog l WHERE l.rule.id = :ruleId AND l.status = 'SENT' AND l.createdAt >= :since")
    long countSentByRuleSince(UUID ruleId, OffsetDateTime since);
}
