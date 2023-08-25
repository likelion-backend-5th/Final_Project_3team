package com.example.travelers.repos;

import com.example.travelers.entity.SenderRequestsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SenderRequestsRepository extends JpaRepository<SenderRequestsEntity, Long> {
    List<SenderRequestsEntity> findAllBySenderId(Long id);
    List<SenderRequestsEntity> findAllByBoardId(Long boardId);
    Optional<SenderRequestsEntity> findByBoardIdAndSenderId(Long boardId, Long id);
}
