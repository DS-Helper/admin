package com.project.ds_helper.domain.trashbin.repository;

import com.project.ds_helper.domain.trashbin.entity.TrashBinImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrashBinImageRepository extends JpaRepository<TrashBinImage, String> {

    Optional<TrashBinImage> findFirstByTrashBin_Id(String trashBinId);
}
