package com.truongquycode.identity_service.repository;

import com.truongquycode.identity_service.entity.EditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EditRequestRepository extends JpaRepository<EditRequest, Long> {
    List<EditRequest> findAllByOrderByCreatedAtDesc();
}