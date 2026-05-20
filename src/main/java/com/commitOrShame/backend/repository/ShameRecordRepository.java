package com.commitOrShame.backend.repository;

import com.commitOrShame.backend.entity.ShameRecord;
import com.commitOrShame.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShameRecordRepository extends JpaRepository<ShameRecord, Long> {

    // Prevent duplicate shame records for same user same day
    Optional<ShameRecord> findByShamedUserAndShameDate(User user, LocalDate date);

    // All shame records for a user (for Android shame history screen)
    List<ShameRecord> findByShamedUserOrderByShameDateDesc(User user);

    // Unnotified records (for when push notifications are wired up)
    List<ShameRecord> findByNotificationSentFalse();
}