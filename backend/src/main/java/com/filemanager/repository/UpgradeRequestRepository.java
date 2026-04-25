package com.filemanager.repository;

import com.filemanager.entity.UpgradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UpgradeRequestRepository extends JpaRepository<UpgradeRequest, Long> {
    List<UpgradeRequest> findByStatus(String status);
    Optional<UpgradeRequest> findByUserIdAndStatus(Long userId, String status);
}
