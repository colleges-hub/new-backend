package ru.ncti.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ncti.backend.model.FCM;

/**
 * user: ichuvilin
 */
@Repository
public interface FCMRepository extends JpaRepository<FCM, Long> {
}
