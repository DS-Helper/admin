package com.project.ds_helper.domain.trashbin.dto.response;

import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "쓰레기통 목록 조회 응답 DTO")
public class GetTrashBinsResponseDto {

    private List<TrashBinItem> trashBins;
    private PageResponseDto page;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "쓰레기통 정보 DTO")
    public static class TrashBinItem {

        @Schema(description = "쓰레기통 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        private String id;

        @Schema(description = "시도명", example = "대구광역시")
        private String provinceName;

        @Schema(description = "시군구명", example = "달성군")
        private String cityCountyName;

        @Schema(description = "설치주소", example = "화원읍 비슬로 2679")
        private String address;

        @Schema(description = "대표 사진 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/20260503120000_uuid")
        private String photoUrl;

        @Schema(description = "위치 설명", example = "한우아파트 입구 좌측에 있어요.")
        private String locationDescription;

        @Schema(description = "설치지점", example = "아파트안")
        private String installationPoint;

        @Schema(description = "쓰레기통 종류", example = "일반쓰레기/재활용")
        private String binType;

        @Schema(description = "관리기관명", example = "달성군청")
        private String managementAgencyName;

        @Schema(description = "관리기관전화번호", example = "053-668-2714")
        private String managementAgencyPhoneNumber;

        @Schema(description = "위도", example = "35.808057")
        private Double latitude;

        @Schema(description = "경도", example = "128.508827")
        private Double longitude;

        @Schema(description = "데이터기준일자", example = "2026-02-20")
        private LocalDate dataReferenceDate;

        @Schema(description = "존재여부", example = "")
        private String existsYn;

        public static TrashBinItem toDto(TrashBin trashBin) {
            return TrashBinItem.builder()
                    .id(trashBin.getId())
                    .provinceName(trashBin.getProvinceName())
                    .cityCountyName(trashBin.getCityCountyName())
                    .address(trashBin.getAddress())
                    .photoUrl(trashBin.getPhotoUrl())
                    .locationDescription(trashBin.getLocationDescription())
                    .installationPoint(trashBin.getInstallationPoint())
                    .binType(trashBin.getBinType())
                    .managementAgencyName(trashBin.getManagementAgencyName())
                    .managementAgencyPhoneNumber(trashBin.getManagementAgencyPhoneNumber())
                    .latitude(trashBin.getLatitude())
                    .longitude(trashBin.getLongitude())
                    .dataReferenceDate(trashBin.getDataReferenceDate())
                    .existsYn(trashBin.getExistsYn())
                    .build();
        }
    }
}
