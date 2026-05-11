package com.project.ds_helper.domain.reply.repository;

import com.project.ds_helper.domain.reply.entity.Reply;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, String> {

    @EntityGraph(attributePaths = {"inquiry"})
    Optional<Reply> findByInquiry_Id(String inquiryId);
}
