package com.project.ds_helper.domain.volunteer.admin.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.domain.volunteer.admin.dto.request.AttendanceRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.CreateVolunteerEventRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.RejectVolunteerApplicationRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.ChangeVolunteerStatusRequest;
import com.project.ds_helper.domain.volunteer.admin.service.VolunteerAdminService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/volunteer")
@Tag(name = "봉사단 관리자", description = "V6 공유 봉사 스키마 기반 관리자 API")
public class VolunteerAdminController {
    private final VolunteerAdminService service;
    private final S3Util s3Util;

    @Operation(summary = "봉사단 가입 신청 목록 조회")
    @GetMapping("/applications")
    public ResponseEntity<ResponseVo<?>> applications() { return ok(service.applications().stream().map(this::applicationResponse).toList()); }

    @Operation(summary = "봉사단 가입 신청 상세 조회")
    @GetMapping("/applications/{id}")
    public ResponseEntity<ResponseVo<?>> application(@PathVariable String id) { return ok(applicationResponse(service.applicationDetail(id))); }

    @Operation(summary = "봉사단 가입 신청 승인")
    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<ResponseVo<?>> approve(Authentication authentication, @PathVariable String id) { return ok(applicationResponse(service.approve(adminId(authentication), id))); }

    @Operation(summary = "봉사단 가입 신청 반려")
    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<ResponseVo<?>> reject(Authentication authentication, @PathVariable String id, @Valid @RequestBody RejectVolunteerApplicationRequest request) { return ok(applicationResponse(service.reject(adminId(authentication), id, request))); }

    @Operation(summary = "가입 신청 사진 식별자 조회", description = "S3 key는 반환하지 않습니다.")
    @GetMapping("/applications/{id}/photo")
    public ResponseEntity<ResponseVo<?>> photo(@PathVariable String id) { var file = service.applicationDetail(id).getPhotoFile(); return ok(java.util.Map.of("volunteerFileId", file.getId(), "url", s3Util.toS3UrlByS3Key(file.getS3Key()))); }

    @Operation(summary = "봉사단원 목록 조회")
    @GetMapping("/members")
    public ResponseEntity<ResponseVo<?>> members() { return ok(service.members().stream().map(this::memberResponse).toList()); }

    @Operation(summary = "봉사단원 상세 조회")
    @GetMapping("/members/{id}")
    public ResponseEntity<ResponseVo<?>> member(@PathVariable String id) { return ok(memberResponse(service.memberDetail(id))); }

    @PostMapping("/members/{id}/suspend")
    public ResponseEntity<ResponseVo<?>> suspend(Authentication a, @PathVariable String id, @RequestBody(required = false) ChangeVolunteerStatusRequest r) { return ok(memberResponse(service.suspendMember(adminId(a), id, r == null ? null : r.reason()))); }
    @PostMapping("/members/{id}/activate")
    public ResponseEntity<ResponseVo<?>> activate(Authentication a, @PathVariable String id, @RequestBody(required = false) ChangeVolunteerStatusRequest r) { return ok(memberResponse(service.activateMember(adminId(a), id, r == null ? null : r.reason()))); }
    @PostMapping("/members/{id}/withdraw")
    public ResponseEntity<ResponseVo<?>> withdraw(Authentication a, @PathVariable String id, @RequestBody(required = false) ChangeVolunteerStatusRequest r) { return ok(memberResponse(service.withdrawMember(adminId(a), id, r == null ? null : r.reason()))); }

    @Operation(summary = "봉사 일정 등록", description = "이미지 파일 메타데이터 ID를 사용해 V6 VolunteerEvent를 생성합니다.")
    @PostMapping("/events")
    public ResponseEntity<ResponseVo<?>> createEvent(Authentication authentication, @Valid @RequestBody CreateVolunteerEventRequest request) { return ok(eventResponse(service.createEvent(adminId(authentication), request))); }

    @Operation(summary = "봉사 일정 목록 조회")
    @GetMapping("/events")
    public ResponseEntity<ResponseVo<?>> events() { return ok(service.events().stream().map(this::eventResponse).toList()); }

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
    public ResponseEntity<ResponseVo<?>> participations(@PathVariable String id) { return ok(service.participations(id).stream().map(p -> java.util.Map.of("volunteerParticipationId", p.getId(), "volunteerMemberId", p.getMember().getId(), "name", p.getMember().getUser().getName(), "status", p.getStatus(), "appliedAt", p.getAppliedAt())).toList()); }

    @Operation(summary = "봉사 출석 일괄 처리")
    @PostMapping("/events/{id}/attendance")
    public ResponseEntity<ResponseVo<Void>> attend(Authentication authentication, @PathVariable String id, @Valid @RequestBody AttendanceRequest request) { service.attend(adminId(authentication), id, request); return ResponseEntity.ok(new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), null)); }

    private String adminId(Authentication authentication) { return String.valueOf(authentication.getPrincipal()); }
    private ResponseEntity<ResponseVo<?>> ok(Object data) { return ResponseEntity.ok(new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), data)); }
    private java.util.Map<String, Object> applicationResponse(com.project.ds_helper.domain.volunteer.application.entity.VolunteerApplication a) { java.util.Map<String, Object> r = new java.util.LinkedHashMap<>(); r.put("volunteerApplicationId", a.getId()); r.put("userId", a.getUser().getId()); r.put("name", a.getName()); r.put("phone", a.getPhone()); r.put("birthDate", a.getBirthDate()); r.put("gender", a.getGender()); r.put("neighborhood", a.getNeighborhood()); r.put("preferredActivities", a.getPreferredActivities()); r.put("motivation", a.getMotivation()); r.put("status", a.getStatus()); r.put("rejectionReason", a.getRejectionReason()); r.put("adminMemo", a.getAdminMemo()); r.put("reviewedAt", a.getReviewedAt()); return r; }
    private java.util.Map<String, Object> memberResponse(com.project.ds_helper.domain.volunteer.member.entity.VolunteerMember m) { return java.util.Map.of("volunteerMemberId", m.getId(), "userId", m.getUser().getId(), "name", m.getUser().getName(), "status", m.getStatus(), "joinedAt", m.getJoinedAt()); }
    private java.util.Map<String, Object> eventResponse(com.project.ds_helper.domain.volunteer.event.entity.VolunteerEvent e) { java.util.Map<String, Object> r = new java.util.LinkedHashMap<>(); r.put("volunteerEventId", e.getId()); r.put("title", e.getTitle()); r.put("type", e.getType()); r.put("imageFileId", e.getImageFile().getId()); r.put("imageUrl", s3Util.toS3UrlByS3Key(e.getImageFile().getS3Key())); r.put("startAt", e.getStartAt()); r.put("endAt", e.getEndAt()); r.put("recruitmentDeadlineAt", e.getRecruitmentDeadlineAt()); r.put("location", e.getLocation()); r.put("capacity", e.getCapacity()); r.put("description", e.getDescription()); r.put("supplies", e.getSupplies()); r.put("precautions", e.getPrecautions()); r.put("status", e.getStatus()); r.put("visibility", e.getVisibility()); return r; }
}
