package com.project.ds_helper.domain.trashbin.repository;

import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrashBinRepository extends JpaRepository<TrashBin, String> {

    List<TrashBin> findAllByLatitude(Double latitude);

    Optional<TrashBin> findFirstByLatitudeAndLongitude(Double latitude, Double longitude);

    boolean existsByLatitudeAndLongitude(Double latitude, Double longitude);
}
