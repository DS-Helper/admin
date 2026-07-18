package com.project.ds_helper.domain.volunteer.admin.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.domain.volunteer.admin.dto.request.AttendanceRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.CreateVolunteerEventRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.RejectVolunteerApplicationRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.ChangeVolunteerStatusRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.VolunteerApplicationSearchRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.VolunteerEventSearchRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.VolunteerMemberSearchRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.response.AdminPageResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.AdminVolunteerApplicationResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.AdminVolunteerEventResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.AdminVolunteerMemberResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.AdminVolunteerMemberDetailResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.AdminVolunteerParticipationResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.AdminVolunteerEventParticipationsResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.VolunteerApplicationPhotoResponse;
import com.project.ds_helper.domain.volunteer.admin.dto.response.VolunteerAttendanceResponse;
import com.project.ds_helper.domain.volunteer.admin.service.VolunteerAdminService;
import com.project.ds_helper.domain.volunteer.file.dto.response.VolunteerEventImageUploadResponse;
import com.project.ds_helper.domain.volunteer.file.service.VolunteerEventImageService;
import com.project.ds_helper.domain.post.util.S3Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/volunteer")
@Tag(name = "봉사단 관리자", description = "V6 공유 봉사 스키마 기반 관리자 API")
public class VolunteerAdminController {
    private final VolunteerAdminService service;
    private final VolunteerEventImageService eventImageService;
    private final S3Util s3Util;

    @Operation(summary = "봉사단 가입 신청 목록 조회")
    @GetMapping("/applications")
    public ResponseEntity<ResponseVo<?>> applications(@ModelAttribute VolunteerApplicationSearchRequest request) { return ok(AdminPageResponse.from(service.applications(request), AdminVolunteerApplicationResponse::from)); }

    @Operation(summary = "봉사단 가입 신청 상세 조회")
    @GetMapping("/applications/{id}")
    public ResponseEntity<ResponseVo<?>> application(@PathVariable String id) { return ok(AdminVolunteerApplicationResponse.from(service.applicationDetail(id))); }

    @Operation(summary = "봉사단 가입 신청 승인")
    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<ResponseVo<?>> approve(Authentication authentication, @PathVariable String id) { return ok(AdminVolunteerApplicationResponse.from(service.approve(adminId(authentication), id))); }

    @Operation(summary = "봉사단 가입 신청 반려")
    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<ResponseVo<?>> reject(Authentication authentication, @PathVariable String id, @Valid @RequestBody RejectVolunteerApplicationRequest request) { return ok(AdminVolunteerApplicationResponse.from(service.reject(adminId(authentication), id, request))); }

    @Operation(summary = "가입 신청 사진 식별자 조회", description = "S3 key는 반환하지 않습니다.")
    @GetMapping("/applications/{id}/photo")
    public ResponseEntity<ResponseVo<?>> photo(@PathVariable String id) { var file = service.applicationDetail(id).getPhotoFile(); if (!file.isPrivateFile() || !file.isActive()) throw new com.project.ds_helper.common.exception.BusinessException(com.project.ds_helper.common.enums.ErrorCode.ACCESS_DENIED); return ok(new VolunteerApplicationPhotoResponse(file.getId(), s3Util.toS3UrlByS3Key(file.getS3Key()), service.currentTime().plus(s3Util.presignedUrlLifetime()))); }

    @Operation(summary = "봉사단원 목록 조회")
    @GetMapping("/members")
    public ResponseEntity<ResponseVo<?>> members(@ModelAttribute VolunteerMemberSearchRequest request) { return ok(AdminPageResponse.from(service.members(request), service::memberResponse)); }

    @Operation(summary = "봉사단원 상세 조회")
    @GetMapping("/members/{id}")
    public ResponseEntity<ResponseVo<?>> member(@PathVariable String id) { return ok(service.memberDetailResponse(id)); }

    @PostMapping("/members/{id}/suspend")
    public ResponseEntity<ResponseVo<?>> suspend(Authentication a, @PathVariable String id, @RequestBody(required = false) ChangeVolunteerStatusRequest r) { service.suspendMember(adminId(a), id, r == null ? null : r.reason()); return ok(service.memberDetailResponse(id)); }
    @PostMapping("/members/{id}/activate")
    public ResponseEntity<ResponseVo<?>> activate(Authentication a, @PathVariable String id, @RequestBody(required = false) ChangeVolunteerStatusRequest r) { service.activateMember(adminId(a), id, r == null ? null : r.reason()); return ok(service.memberDetailResponse(id)); }
    @PostMapping("/members/{id}/withdraw")
    public ResponseEntity<ResponseVo<?>> withdraw(Authentication a, @PathVariable String id, @RequestBody(required = false) ChangeVolunteerStatusRequest r) { service.withdrawMember(adminId(a), id, r == null ? null : r.reason()); return ok(service.memberDetailResponse(id)); }

    @Operation(summary = "봉사 일정 등록", description = "이미지 파일 메타데이터 ID를 사용해 V6 VolunteerEvent를 생성합니다.")
    @PostMapping("/events")
    public ResponseEntity<ResponseVo<?>> createEvent(Authentication authentication, @Valid @RequestBody CreateVolunteerEventRequest request) { return ok(eventResponse(service.createEvent(adminId(authentication), request))); }

    @Operation(summary = "봉사 일정 공개 이미지 업로드", description = "JPG/JPEG/PNG 파일을 최대 20MB까지 받아 WebP로 변환한 뒤 공개 VolunteerFile로 저장합니다.")
    @PostMapping(value = "/event-images", consumes = "multipart/form-data")
    public ResponseEntity<ResponseVo<VolunteerEventImageUploadResponse>> uploadEventImage(
            Authentication authentication,
            @RequestPart("image") MultipartFile image
    ) throws java.io.IOException {
        VolunteerEventImageUploadResponse response = eventImageService.upload(adminId(authentication), image);
        return ResponseEntity.ok(new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), response));
    }

    @Operation(summary = "봉사 일정 목록 조회")
    @GetMapping("/events")
    public ResponseEntity<ResponseVo<?>> events(@ModelAttribute VolunteerEventSearchRequest request) { return ok(AdminPageResponse.from(service.events(request), this::eventResponse)); }

    @GetMapping("/events/{id}")
    public ResponseEntity<ResponseVo<?>> event(@PathVariable String id) { return ok(eventResponse(service.eventDetail(id))); }
    @PatchMapping("/events/{id}")
    public ResponseEntity<ResponseVo<?>> updateEvent(Authentication a, @PathVariable String id, @Valid @RequestBody CreateVolunteerEventRequest r) { return ok(eventResponse(service.updateEvent(adminId(a), id, r))); }
    @PostMapping("/events/{id}/open")
    public ResponseEntity<ResponseVo<?>> openEvent(Authentication a, @PathVariable String id) { return ok(eventResponse(service.openEvent(adminId(a), id))); }
    @PostMapping("/events/{id}/close")
    public ResponseEntity<ResponseVo<?>> closeEvent(Authentication a, @PathVariable String id) { return ok(eventResponse(service.closeEvent(adminId(a), id))); }
    @PostMapping("/events/{id}/cancel")
    public ResponseEntity<ResponseVo<?>> cancelEvent(Authentication a, @PathVariable String id, @RequestBody(required = false) ChangeVolunteerStatusRequest r) { return ok(eventResponse(service.cancelEvent(adminId(a), id, r == null ? null : r.reason()))); }

    @Operation(summary = "일정 참여자 목록 조회")
    @GetMapping("/events/{id}/participations")
    public ResponseEntity<ResponseVo<?>> participations(@PathVariable String id) { var event = service.eventDetail(id); return ok(new AdminVolunteerEventParticipationsResponse(eventResponse(event), service.participations(id).stream().filter(p -> p.getStatus() != com.project.ds_helper.domain.volunteer.common.enums.VolunteerParticipationStatus.CANCELED).map(AdminVolunteerParticipationResponse::from).toList())); }

    @Operation(summary = "봉사 출석 일괄 처리")
    @PostMapping("/events/{id}/attendance")
    public ResponseEntity<ResponseVo<VolunteerAttendanceResponse>> attend(Authentication authentication, @PathVariable String id, @Valid @RequestBody AttendanceRequest request) { service.attend(adminId(authentication), id, request); return ResponseEntity.ok(new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), new VolunteerAttendanceResponse(id, request.attendedParticipationIds().size(), request.absentParticipationIds().size(), service.currentTime()))); }

    private String adminId(Authentication authentication) { return String.valueOf(authentication.getPrincipal()); }
    private ResponseEntity<ResponseVo<?>> ok(Object data) { return ResponseEntity.ok(new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), data)); }
    private AdminVolunteerEventResponse eventResponse(com.project.ds_helper.domain.volunteer.event.entity.VolunteerEvent event) { return AdminVolunteerEventResponse.from(event, s3Util.toS3UrlByS3Key(event.getImageFile().getS3Key()), service.participantCount(event.getId())); }
}
