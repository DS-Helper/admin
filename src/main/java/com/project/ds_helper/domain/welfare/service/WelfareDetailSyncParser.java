package com.project.ds_helper.domain.welfare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.time.LocalDateTime;

class WelfareDetailSyncParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    DetailSyncPayload buildDetailSyncPayload(JsonNode root, LocalDateTime syncStartedAt) {
        return DetailSyncPayload.success(
                findText(root, "sprtTrgtCn", "tgtrDtlCn", "targetDetailContent"),
                findText(root, "alwServCn", "benefitContent"),
                findText(root, "slctCritCn", "selectionCriteriaContent"),
                findText(root, "aplyMtdCn", "applmetList", "applicationMethodList"),
                findHomepageUrl(root),
                findRawJson(root, "inqplCtadrList"),
                findRawJson(root, "inqplHmpgReldList"),
                findRawJson(root, "baslawList"),
                findRawJson(root, "basfrmList"),
                syncStartedAt
        );
    }

    JsonNode readJsonOrXmlTree(String body) throws IOException {
        String trimmedBody = body.trim();
        if (trimmedBody.startsWith("<")) {
            return xmlMapper.readTree(trimmedBody);
        }
        return objectMapper.readTree(trimmedBody);
    }

    /**
     * 공공데이터 응답 wrapper가 바뀌어도 필요한 필드를 찾을 수 있도록 JSON 트리를 재귀 탐색한다.
     */
    String findText(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                if (value.isValueNode()) {
                    return value.asText();
                }
                String nestedValue = findText(value, fieldNames);
                if (nestedValue != null) {
                    return nestedValue;
                }
            }
        }

        if (node.isObject()) {
            for (JsonNode child : node) {
                String found = findText(child, fieldNames);
                if (found != null) {
                    return found;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                String found = findText(child, fieldNames);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * 홈페이지 URL은 문의처 전화번호와 같은 필드명(wlfareInfoReldCn)을 공유할 수 있다.
     * 따라서 전역 재귀 탐색이 아니라 홈페이지 전용 노드 안에서만 보조 URL을 찾는다.
     */
    String findHomepageUrl(JsonNode root) {
        String directHomepageUrl = findText(root, "hmpgUrl", "homepageUrl");
        if (directHomepageUrl != null) {
            return directHomepageUrl;
        }

        JsonNode homepageNode = findNode(root, "inqplHmpgReldList");
        return findText(homepageNode, "wlfareInfoReldCn");
    }

    JsonNode findNode(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode value = node.get(fieldName);
        if (value != null && !value.isNull()) {
            return value;
        }

        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findNode(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    String findRawJson(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode value = node.get(fieldName);
        if (value != null && !value.isNull()) {
            return value.toString();
        }

        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                String found = findRawJson(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}
