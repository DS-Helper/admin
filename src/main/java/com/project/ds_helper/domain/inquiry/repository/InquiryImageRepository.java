package com.project.ds_helper.domain.inquiry.repository;

import com.project.ds_helper.domain.inquiry.entity.InquiryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryImageRepository extends JpaRepository<InquiryImage, String> {
}
