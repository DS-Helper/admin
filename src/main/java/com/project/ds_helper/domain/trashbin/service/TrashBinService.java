package com.project.ds_helper.domain.trashbin.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.trashbin.dto.response.GetTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinsResponseDto;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.entity.TrashBinImage;
import com.project.ds_helper.domain.trashbin.repository.TrashBinImageRepository;
import com.project.ds_helper.domain.trashbin.repository.TrashBinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TrashBinService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "provinceName",
            "cityCountyName",
            "address",
            "latitude",
            "longitude",
            "dataReferenceDate"
    );

    private static final String HEADER_PROVINCE_NAME = "시도명";
    private static final String HEADER_ADDRESS = "설치주소";
    private static final String HEADER_PHOTO_URL = "사진";
    private static final String HEADER_LOCATION_DESCRIPTION = "위치 설명";
    private static final String HEADER_INSTALLATION_POINT = "설치지점";
    private static final String HEADER_BIN_TYPE = "쓰레기통 종류";
    private static final String HEADER_MANAGEMENT_AGENCY_NAME = "관리기관명";
    private static final String HEADER_MANAGEMENT_AGENCY_PHONE_NUMBER = "관리기관전화번호";
    private static final String HEADER_CITY_COUNTY_NAME = "도시구군명";
    private static final String HEADER_LATITUDE = "위도";
    private static final String HEADER_LONGITUDE = "경도";
    private static final String HEADER_DATA_REFERENCE_DATE = "데이터기준일자";
    private static final String HEADER_EXISTS_YN = "존재여부";

    private final TrashBinRepository trashBinRepository;
    private final TrashBinImageRepository trashBinImageRepository;
    private final ImageUtil imageUtil;
    private final ImageCompressionUtil imageCompressionUtil;
    private final S3Util s3Util;

    public UploadTrashBinsResponseDto uploadTrashBins(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw invalidParameter("CSV file is required");
        }

        log.debug("TrashBinService.uploadTrashBins started. originalFilename={}, size={}",
                file.getOriginalFilename(), file.getSize());

        // 1. 업로드된 CSV를 프로젝트 Entity 기준 필드로 변환한다.
        List<TrashBin> trashBins = parseCsv(file);

        // 2. 중복 좌표가 하나라도 있으면 전체 업로드를 실패시켜 기존 DB 상태를 보존한다.
        validateNoDuplicateCoordinates(trashBins);
        trashBinRepository.saveAll(trashBins);

        log.debug("TrashBinService.uploadTrashBins completed. savedCount={}", trashBins.size());
        return new UploadTrashBinsResponseDto(trashBins.size());
    }

    @Transactional(readOnly = true)
    public GetTrashBinsResponseDto getTrashBins(int page, int size, String sort, String sortBy) {
        log.debug("TrashBinService.getTrashBins started. page={}, size={}, sort={}, sortBy={}",
                page, size, sort, sortBy);

        Pageable pageable = PageRequest.of(page, size, resolveSort(sort, sortBy));
        Page<TrashBin> trashBins = trashBinRepository.findAll(pageable);

        List<GetTrashBinsResponseDto.TrashBinItem> items = trashBins.getContent().stream()
                .map(GetTrashBinsResponseDto.TrashBinItem::toDto)
                .toList();

        log.debug("TrashBinService.getTrashBins completed. resultCount={}, totalElements={}",
                items.size(), trashBins.getTotalElements());

        return GetTrashBinsResponseDto.builder()
                .trashBins(items)
                .page(PageResponseDto.toDto(trashBins))
                .build();
    }

    public UploadTrashBinImageResponseDto uploadTrashBinImage(MultipartFile image) throws IOException {
        log.debug("TrashBinService.uploadTrashBinImage started. originalFilename={}, size={}",
                image == null ? null : image.getOriginalFilename(), image == null ? null : image.getSize());

        if (image == null || image.isEmpty()) {
            throw invalidParameter("image is required");
        }

        // 1. 이미지 원본 파일명에서 확장자를 제거한 값을 위도로 파싱한다.
        Double latitude = parseLatitudeFromFilename(image.getOriginalFilename());
        log.debug("TrashBinService.uploadTrashBinImage parsed latitude from filename. latitude={}", latitude);

        // 2. 위도로 매핑할 휴지통 데이터를 먼저 확정한다. 같은 위도 데이터가 여러 건이면 잘못 매핑될 수 있어 실패시킨다.
        TrashBin trashBin = findTrashBinByLatitude(latitude);

        // 3. 매핑 대상이 확정된 뒤 기존 이미지 여부를 확인해 모호한 latitude의 잘못된 중복 응답을 막는다.
        UploadTrashBinImageResponseDto duplicateResponse = trashBinImageRepository.findFirstByTrashBin_Id(trashBin.getId())
                .map(UploadTrashBinImageResponseDto::alreadyExists)
                .orElse(null);
        if (duplicateResponse != null) {
            log.debug("TrashBinService.uploadTrashBinImage skipped. image already exists for latitude={}, trashBinId={}, imageId={}",
                    latitude, trashBin.getId(), duplicateResponse.imageId());
            return duplicateResponse;
        }

        // 4. 기존 S3 이미지 정책에 맞춰 WebP 압축 DTO를 생성한다.
        String storedFilename = imageUtil.toStoredFilename();
        S3ImageUploadRequestDto uploadRequestDto = imageCompressionUtil.compressImage(image, storedFilename);
        boolean uploadedToS3 = false;

        try {
            // 5. S3에 먼저 업로드한 뒤, 성공한 이미지 메타데이터만 DB에 저장한다.
            s3Util.uploadImage(uploadRequestDto);
            uploadedToS3 = true;

            String s3Key = s3Util.buildS3Key(storedFilename);
            String imageUrl = s3Util.toS3UrlByS3Key(s3Key);

            TrashBinImage trashBinImage = TrashBinImage.builder()
                    .originalName(uploadRequestDto.getOriginalFilename())
                    .storedName(uploadRequestDto.getStoredFilename())
                    .s3Key(s3Key)
                    .url(imageUrl)
                    .size(uploadRequestDto.getSize())
                    .contentType(uploadRequestDto.getS3ContentType())
                    .trashBin(trashBin)
                    .build();

            TrashBinImage savedImage = trashBinImageRepository.save(trashBinImage);

            // 6. 기존 목록 조회 응답과 호환되도록 대표 이미지 URL도 함께 갱신한다.
            trashBin.updatePhotoUrl(imageUrl);

            log.debug("TrashBinService.uploadTrashBinImage completed. trashBinId={}, imageId={}, s3Key={}",
                    trashBin.getId(), savedImage.getId(), savedImage.getS3Key());

            return UploadTrashBinImageResponseDto.uploaded(trashBin, savedImage);
        } catch (Exception e) {
            log.error("TrashBinService.uploadTrashBinImage failed. latitude={}, storedFilename={}",
                    latitude, storedFilename, e);
            if (uploadedToS3) {
                rollbackUploadedImage(storedFilename);
            }
            throw e;
        }
    }

    private TrashBin findTrashBinByLatitude(Double latitude) {
        List<TrashBin> trashBins = trashBinRepository.findAllByLatitude(latitude);
        if (trashBins.isEmpty()) {
            throw invalidParameter("No trash bin found by latitude. latitude=" + latitude);
        }
        if (trashBins.size() > 1) {
            throw conflict("Multiple trash bins found by latitude. Filename latitude must be unique. latitude=" + latitude);
        }

        return trashBins.getFirst();
    }

    private Double parseLatitudeFromFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw invalidParameter("image filename is required");
        }

        String filename = originalFilename.trim();
        int extensionIndex = filename.lastIndexOf('.');
        String latitudeText = extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;

        try {
            return Double.parseDouble(latitudeText);
        } catch (NumberFormatException e) {
            throw invalidParameter("image filename must be latitude value. originalFilename=" + originalFilename);
        }
    }

    private List<TrashBin> parseCsv(MultipartFile file) throws IOException {
        List<TrashBin> trashBins = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw invalidParameter("CSV header is empty");
            }

            List<String> headers = parseCsvLine(headerLine);
            log.debug("TrashBinService.parseCsv headers={}", headers);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                trashBins.add(TrashBin.builder()
                        .provinceName(getValue(headers, values, HEADER_PROVINCE_NAME))
                        .cityCountyName(getValue(headers, values, HEADER_CITY_COUNTY_NAME))
                        .address(getValue(headers, values, HEADER_ADDRESS))
                        .photoUrl(getValue(headers, values, HEADER_PHOTO_URL))
                        .locationDescription(getValue(headers, values, HEADER_LOCATION_DESCRIPTION))
                        .installationPoint(getValue(headers, values, HEADER_INSTALLATION_POINT))
                        .binType(getValue(headers, values, HEADER_BIN_TYPE))
                        .managementAgencyName(getValue(headers, values, HEADER_MANAGEMENT_AGENCY_NAME))
                        .managementAgencyPhoneNumber(getValue(headers, values, HEADER_MANAGEMENT_AGENCY_PHONE_NUMBER))
                        .latitude(parseDouble(getValue(headers, values, HEADER_LATITUDE)))
                        .longitude(parseDouble(getValue(headers, values, HEADER_LONGITUDE)))
                        .dataReferenceDate(parseLocalDate(getValue(headers, values, HEADER_DATA_REFERENCE_DATE)))
                        .existsYn(getValue(headers, values, HEADER_EXISTS_YN))
                        .build());
            }
        }

        return trashBins;
    }

    private void validateNoDuplicateCoordinates(List<TrashBin> parsedTrashBins) {
        Set<String> coordinatesInCurrentCsv = new HashSet<>();

        for (TrashBin trashBin : parsedTrashBins) {
            String coordinateKey = toCoordinateKey(trashBin);

            // 1. 좌표가 없는 행은 이미지 매핑 및 중복 검증 기준을 만들 수 없으므로 전체 업로드를 실패시킨다.
            if (coordinateKey == null) {
                log.debug("TrashBinService.validateNoDuplicateCoordinates failed. missing coordinate. address={}, latitude={}, longitude={}",
                        trashBin.getAddress(), trashBin.getLatitude(), trashBin.getLongitude());
                throw invalidParameter("Trash bin coordinate is required. address=" + trashBin.getAddress());
            }

            // 2. 같은 CSV 안에 중복 좌표가 있으면 전체 업로드를 실패시킨다.
            if (!coordinatesInCurrentCsv.add(coordinateKey)) {
                log.debug("TrashBinService.validateNoDuplicateCoordinates failed. duplicate in CSV. coordinateKey={}, address={}",
                        coordinateKey, trashBin.getAddress());
                throw invalidParameter("Duplicate trash bin coordinate in uploaded CSV. latitude="
                        + trashBin.getLatitude() + ", longitude=" + trashBin.getLongitude());
            }

            // 3. DB에 이미 같은 위도/경도 데이터가 있으면 기존 데이터를 유지하기 위해 전체 업로드를 실패시킨다.
            if (trashBinRepository.existsByLatitudeAndLongitude(trashBin.getLatitude(), trashBin.getLongitude())) {
                log.debug("TrashBinService.validateNoDuplicateCoordinates failed. existing DB row. coordinateKey={}, address={}",
                        coordinateKey, trashBin.getAddress());
                throw invalidParameter("Trash bin coordinate already exists. latitude="
                        + trashBin.getLatitude() + ", longitude=" + trashBin.getLongitude());
            }
        }
    }

    private String toCoordinateKey(TrashBin trashBin) {
        if (trashBin.getLatitude() == null || trashBin.getLongitude() == null) {
            return null;
        }
        return trashBin.getLatitude() + "|" + trashBin.getLongitude();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);

            if (currentChar == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                    continue;
                }
                inQuotes = !inQuotes;
                continue;
            }

            if (currentChar == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        values.add(current.toString().trim());
        return values;
    }

    private String getValue(List<String> headers, List<String> values, String headerName) {
        int index = headers.indexOf(headerName);
        if (index < 0 || index >= values.size()) {
            return null;
        }

        String value = values.get(index);
        return value == null || value.isBlank() ? null : value;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        // CSV 원본 중 일부 좌표가 "35.794500,"처럼 따옴표 안에 끝 쉼표를 포함한다.
        // 업로드 파일을 그대로 수용하기 위해 숫자 파싱 전 끝 쉼표만 정리한다.
        String normalizedValue = value.trim().replaceAll(",$", "");
        if (normalizedValue.isBlank()) {
            return null;
        }
        return Double.parseDouble(normalizedValue);
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private Sort resolveSort(String sort, String sortBy) {
        if (!"asc".equalsIgnoreCase(sort) && !"desc".equalsIgnoreCase(sort)) {
            throw invalidParameter("Unsupported trash bin sort direction: " + sort);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw invalidParameter("Unsupported trash bin sortBy: " + sortBy);
        }

        Sort.Direction direction = sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, sortBy);
    }

    private void rollbackUploadedImage(String storedFilename) {
        try {
            log.debug("TrashBinService.rollbackUploadedImage started. storedFilename={}", storedFilename);
            s3Util.deleteImage(storedFilename);
            log.debug("TrashBinService.rollbackUploadedImage completed. storedFilename={}", storedFilename);
        } catch (Exception rollbackException) {
            log.error("TrashBinService.rollbackUploadedImage failed. storedFilename={}", storedFilename, rollbackException);
        }
    }

    private BusinessException invalidParameter(String message) {
        return new BusinessException(ErrorCode.INVALID_PARAMETER, message);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }

}
