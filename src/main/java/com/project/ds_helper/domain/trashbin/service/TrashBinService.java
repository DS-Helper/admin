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
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImagesResponseDto;
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
import java.util.List;
import java.util.HashSet;
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

    private final TrashBinRepository trashBinRepository;
    private final TrashBinImageRepository trashBinImageRepository;
    private final ImageUtil imageUtil;
    private final ImageCompressionUtil imageCompressionUtil;
    private final S3Util s3Util;
    private final TrashBinCsvService trashBinCsvService;
    private final TrashBinImageService trashBinImageService;

    public UploadTrashBinsResponseDto uploadTrashBins(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw invalidParameter("CSV file is required");
        }

        log.debug("TrashBinService.uploadTrashBins started. originalFilename={}, size={}",
                file.getOriginalFilename(), file.getSize());

        List<TrashBin> trashBins = trashBinCsvService != null ? trashBinCsvService.parseCsv(file) : parseCsv(file);
        if (trashBinCsvService != null) {
            trashBinCsvService.validateNoDuplicateCoordinates(trashBins);
        } else {
            validateNoDuplicateCoordinates(trashBins);
        }
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
        if (trashBinImageService != null) {
            return trashBinImageService.uploadTrashBinImage(image);
        }

        if (image == null || image.isEmpty()) {
            throw invalidParameter("image is required");
        }

        Double latitude = parseLatitudeFromFilename(image.getOriginalFilename());
        TrashBin trashBin = findTrashBinByLatitude(latitude);

        UploadTrashBinImageResponseDto duplicateResponse = trashBinImageRepository.findFirstByTrashBin_Id(trashBin.getId())
                .map(UploadTrashBinImageResponseDto::alreadyExists)
                .orElse(null);
        if (duplicateResponse != null) {
            return duplicateResponse;
        }

        String storedFilename = imageUtil.toStoredFilename();
        S3ImageUploadRequestDto uploadRequestDto = imageCompressionUtil.compressImage(image, storedFilename);
        boolean uploadedToS3 = false;

        try {
            s3Util.uploadImage(uploadRequestDto);
            uploadedToS3 = true;

            String s3Key = s3Util.buildS3Key(storedFilename);
            String imageUrl = s3Util.toS3UrlByS3Key(s3Key);
            TrashBinImage savedImage = trashBinImageRepository.save(TrashBinImage.builder()
                    .originalName(uploadRequestDto.getOriginalFilename())
                    .storedName(uploadRequestDto.getStoredFilename())
                    .s3Key(s3Key)
                    .url(imageUrl)
                    .size(uploadRequestDto.getSize())
                    .contentType(uploadRequestDto.getS3ContentType())
                    .trashBin(trashBin)
                    .build());

            trashBin.updatePhotoUrl(imageUrl);
            return UploadTrashBinImageResponseDto.uploaded(trashBin, savedImage);
        } catch (Exception e) {
            if (uploadedToS3) {
                rollbackUploadedImage(storedFilename);
            }
            throw e;
        }
    }

    public UploadTrashBinImagesResponseDto uploadTrashBinImages(List<MultipartFile> images) throws IOException {
        if (trashBinImageService != null) {
            return trashBinImageService.uploadTrashBinImages(images);
        }

        if (images == null || images.isEmpty()) {
            throw invalidParameter("images are required");
        }

        List<UploadTrashBinImageResponseDto> uploadedImages = new ArrayList<>(images.size());
        try {
            for (MultipartFile image : images) {
                uploadedImages.add(uploadTrashBinImage(image));
            }
            return UploadTrashBinImagesResponseDto.from(uploadedImages);
        } catch (Exception e) {
            rollbackUploadedImages(uploadedImages);
            throw e;
        }
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

    private BusinessException invalidParameter(String message) {
        return new BusinessException(ErrorCode.INVALID_PARAMETER, message);
    }

    private TrashBin findTrashBinByLatitude(Double latitude) {
        List<TrashBin> trashBins = trashBinRepository.findAllByLatitude(latitude);
        if (trashBins.isEmpty()) {
            throw invalidParameter("No trash bin found by latitude. latitude=" + latitude);
        }
        if (trashBins.size() > 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "Multiple trash bins found by latitude. Filename latitude must be unique. latitude=" + latitude);
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

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw invalidParameter("CSV header is empty");
            }

            List<String> headers = parseCsvLine(headerLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                trashBins.add(TrashBin.builder()
                        .provinceName(getValue(headers, values, "시도명"))
                        .cityCountyName(getValue(headers, values, "도시구군명"))
                        .address(getValue(headers, values, "설치주소"))
                        .photoUrl(getValue(headers, values, "사진"))
                        .locationDescription(getValue(headers, values, "위치 설명"))
                        .installationPoint(getValue(headers, values, "설치지점"))
                        .binType(getValue(headers, values, "쓰레기통 종류"))
                        .managementAgencyName(getValue(headers, values, "관리기관명"))
                        .managementAgencyPhoneNumber(getValue(headers, values, "관리기관전화번호"))
                        .latitude(parseDouble(getValue(headers, values, "위도")))
                        .longitude(parseDouble(getValue(headers, values, "경도")))
                        .dataReferenceDate(parseLocalDate(getValue(headers, values, "데이터기준일자")))
                        .existsYn(getValue(headers, values, "존재여부"))
                        .build());
            }
        }

        return trashBins;
    }

    private void validateNoDuplicateCoordinates(List<TrashBin> parsedTrashBins) {
        Set<String> coordinatesInCurrentCsv = new HashSet<>();
        for (TrashBin trashBin : parsedTrashBins) {
            String coordinateKey = toCoordinateKey(trashBin);
            if (coordinateKey == null) {
                throw invalidParameter("Trash bin coordinate is required. address=" + trashBin.getAddress());
            }
            if (!coordinatesInCurrentCsv.add(coordinateKey)) {
                throw invalidParameter("Duplicate trash bin coordinate in uploaded CSV. latitude=" + trashBin.getLatitude() + ", longitude=" + trashBin.getLongitude());
            }
            if (trashBinRepository.existsByLatitudeAndLongitude(trashBin.getLatitude(), trashBin.getLongitude())) {
                throw invalidParameter("Trash bin coordinate already exists. latitude=" + trashBin.getLatitude() + ", longitude=" + trashBin.getLongitude());
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

    private void rollbackUploadedImage(String storedFilename) {
        try {
            s3Util.deleteImage(storedFilename);
        } catch (Exception ignored) {
            log.error("TrashBinService.rollbackUploadedImage failed. storedFilename={}", storedFilename, ignored);
        }
    }

    private void rollbackUploadedImages(List<UploadTrashBinImageResponseDto> uploadedImages) {
        for (UploadTrashBinImageResponseDto uploadedImage : uploadedImages) {
            if (!uploadedImage.alreadyExists()) {
                rollbackUploadedImage(uploadedImage.storedName());
            }
        }
    }
}
