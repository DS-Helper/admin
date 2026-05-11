package com.project.ds_helper.domain.admin.repository;

import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminInquiryRepository extends JpaRepository<Inquiry,String> {
    @EntityGraph(attributePaths = {"user"})
    Page<Inquiry> findAllByStatus(InquiryStatus status, Pageable pageRequest);
}
