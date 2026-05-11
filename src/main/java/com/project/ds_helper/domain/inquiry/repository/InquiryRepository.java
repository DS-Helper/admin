package com.project.ds_helper.domain.inquiry.repository;

import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, String> {
    @EntityGraph(attributePaths = {"images"})
    Page<Inquiry> findAllByUser_Id(String s, Pageable pageable);
}
