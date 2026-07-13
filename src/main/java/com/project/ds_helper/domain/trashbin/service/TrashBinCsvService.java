package com.project.ds_helper.domain.trashbin.service;

import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.repository.TrashBinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
public class TrashBinCsvService {

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

    public List<TrashBin> parseCsv(MultipartFile file) throws IOException {
        List<TrashBin> trashBins = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw invalidParameter("CSV header is empty");
            }

            List<String> headers = parseCsvLine(headerLine);
            log.debug("TrashBinCsvService.parseCsv headers={}", headers);

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

    public void validateNoDuplicateCoordinates(List<TrashBin> parsedTrashBins) {
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

    private BusinessException invalidParameter(String message) {
        return new BusinessException(ErrorCode.INVALID_PARAMETER, message);
    }
}
