package com.project.ds_helper.domain.volunteer.admin.service;

import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.volunteer.admin.dto.request.AttendanceRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.CreateVolunteerEventRequest;
import com.project.ds_helper.domain.volunteer.admin.dto.request.RejectVolunteerApplicationRequest;
import com.project.ds_helper.domain.volunteer.application.entity.VolunteerApplication;
import com.project.ds_helper.domain.volunteer.application.repository.VolunteerApplicationRepository;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerApplicationStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerHistoryTargetType;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerMemberStatus;
import com.project.ds_helper.domain.volunteer.event.entity.VolunteerEvent;
import com.project.ds_helper.domain.volunteer.event.repository.VolunteerEventRepository;
import com.project.ds_helper.domain.volunteer.file.entity.VolunteerFile;
import com.project.ds_helper.domain.volunteer.file.repository.VolunteerFileRepository;
import com.project.ds_helper.domain.volunteer.history.service.VolunteerHistoryService;
import com.project.ds_helper.domain.volunteer.member.entity.VolunteerMember;
import com.project.ds_helper.domain.volunteer.member.repository.VolunteerMemberRepository;
import com.project.ds_helper.domain.volunteer.participation.entity.VolunteerParticipation;
import com.project.ds_helper.domain.volunteer.participation.repository.VolunteerParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VolunteerAdminService {
    private final VolunteerApplicationRepository applicationRepository;
    private final VolunteerMemberRepository memberRepository;
    private final VolunteerEventRepository eventRepository;
    private final VolunteerParticipationRepository participationRepository;
    private final VolunteerFileRepository fileRepository;
    private final VolunteerHistoryService historyService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<VolunteerApplication> applications() { return applicationRepository.findAll(); }

    @Transactional(readOnly = true)
    public VolunteerApplication applicationDetail(String applicationId) { return application(applicationId); }

    public VolunteerApplication approve(String adminId, String applicationId) {
        VolunteerApplication application = application(applicationId);
        if (memberRepository.findByUser_Id(application.getUser().getId()).isPresent()) throw new BusinessException(ErrorCode.CONFLICT, "이미 봉사단원입니다.");
        VolunteerApplicationStatus before = application.getStatus();
        application.approve(adminId, now());
        memberRepository.save(VolunteerMember.builder().user(application.getUser()).application(application).status(VolunteerMemberStatus.ACTIVE).joinedAt(now()).build());
        historyService.record(VolunteerHistoryTargetType.APPLICATION, applicationId, before.name(), application.getStatus().name(), adminId, null);
        return application;
    }

    public VolunteerApplication reject(String adminId, String applicationId, RejectVolunteerApplicationRequest request) {
        VolunteerApplication application = application(applicationId);
        VolunteerApplicationStatus before = application.getStatus();
        application.reject(adminId, request.rejectionReason(), request.adminMemo(), now());
        historyService.record(VolunteerHistoryTargetType.APPLICATION, applicationId, before.name(), application.getStatus().name(), adminId, request.rejectionReason());
        return application;
    }

    public VolunteerEvent createEvent(String adminId, CreateVolunteerEventRequest request) {
        validateEventRequest(request);
        VolunteerFile file = fileRepository.findById(request.imageFileId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (file.isPrivateFile()) throw new BusinessException(ErrorCode.CONFLICT, "공개 일정에는 공개 이미지만 사용할 수 있습니다.");
        return eventRepository.save(VolunteerEvent.builder().title(request.title()).type(request.type()).imageFile(file).startAt(request.startAt()).endAt(request.endAt()).recruitmentDeadlineAt(request.recruitmentDeadlineAt()).location(request.location()).capacity(request.capacity()).description(request.description()).supplies(request.supplies()).precautions(request.precautions()).status(request.status()).visibility(request.visibility()).createdBy(adminId).updatedBy(adminId).build());
    }

    @Transactional(readOnly = true)
    public List<VolunteerEvent> events() { return eventRepository.findAll(); }

    @Transactional(readOnly = true)
    public VolunteerEvent eventDetail(String eventId) { return event(eventId); }

    public VolunteerEvent updateEvent(String adminId, String eventId, CreateVolunteerEventRequest request) {
        validateEventRequest(request);
        VolunteerEvent event = event(eventId);
        long currentCount = participationRepository.countByEvent_IdAndStatus(eventId, com.project.ds_helper.domain.volunteer.common.enums.VolunteerParticipationStatus.APPLIED);
        if (request.capacity() < currentCount) throw new BusinessException(ErrorCode.CONFLICT, "현재 참여 인원보다 정원을 줄일 수 없습니다.");
        VolunteerFile file = fileRepository.findById(request.imageFileId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        event.update(request.title(), request.type(), file, request.startAt(), request.endAt(), request.recruitmentDeadlineAt(), request.location(), request.capacity(), request.description(), request.supplies(), request.precautions(), request.visibility(), adminId);
        return event;
    }

    public VolunteerEvent openEvent(String adminId, String eventId) { VolunteerEvent event = event(eventId); VolunteerEventStatus before = event.getStatus(); event.open(); historyService.record(VolunteerHistoryTargetType.EVENT, eventId, before.name(), event.getStatus().name(), adminId, null); return event; }
    public VolunteerEvent closeEvent(String adminId, String eventId) { VolunteerEvent event = event(eventId); VolunteerEventStatus before = event.getStatus(); event.closeManually(); historyService.record(VolunteerHistoryTargetType.EVENT, eventId, before.name(), event.getStatus().name(), adminId, null); return event; }
    public VolunteerEvent cancelEvent(String adminId, String eventId, String reason) { VolunteerEvent event = event(eventId); VolunteerEventStatus before = event.getStatus(); event.cancel(reason); historyService.record(VolunteerHistoryTargetType.EVENT, eventId, before.name(), event.getStatus().name(), adminId, reason); return event; }

    @Transactional(readOnly = true)
    public List<VolunteerParticipation> participations(String eventId) { event(eventId); return participationRepository.findByEvent_Id(eventId); }

    @Transactional(readOnly = true)
    public List<VolunteerMember> members() { return memberRepository.findAll(); }

    @Transactional(readOnly = true)
    public VolunteerMember memberDetail(String memberId) { return member(memberId); }

    public VolunteerMember suspendMember(String adminId, String memberId, String reason) { VolunteerMember member = member(memberId); VolunteerMemberStatus before = member.getStatus(); member.suspend(now()); historyService.record(VolunteerHistoryTargetType.MEMBER, memberId, before.name(), member.getStatus().name(), adminId, reason); return member; }
    public VolunteerMember activateMember(String adminId, String memberId, String reason) { VolunteerMember member = member(memberId); VolunteerMemberStatus before = member.getStatus(); member.activate(); historyService.record(VolunteerHistoryTargetType.MEMBER, memberId, before.name(), member.getStatus().name(), adminId, reason); return member; }
    public VolunteerMember withdrawMember(String adminId, String memberId, String reason) { VolunteerMember member = member(memberId); VolunteerMemberStatus before = member.getStatus(); member.withdraw(now()); historyService.record(VolunteerHistoryTargetType.MEMBER, memberId, before.name(), member.getStatus().name(), adminId, reason); return member; }

    public void attend(String adminId, String eventId, AttendanceRequest request) {
        VolunteerEvent event = eventRepository.findById(eventId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (event.getEndAt().isAfter(now())) throw new BusinessException(ErrorCode.CONFLICT, "종료 후에만 출석 처리할 수 있습니다.");
        if (request.attendedParticipationIds().stream().anyMatch(request.absentParticipationIds()::contains)) throw new BusinessException(ErrorCode.INVALID_PARAMETER, "출석과 불참 대상이 중복됩니다.");
        request.attendedParticipationIds().forEach(id -> { VolunteerParticipation p = participation(id, eventId); p.attend(adminId, now()); historyService.record(VolunteerHistoryTargetType.PARTICIPATION, id, "APPLIED", "ATTENDED", adminId, null); });
        request.absentParticipationIds().forEach(id -> { VolunteerParticipation p = participation(id, eventId); p.markAbsent(adminId, now()); historyService.record(VolunteerHistoryTargetType.PARTICIPATION, id, "APPLIED", "ABSENT", adminId, null); });
    }

    private VolunteerApplication application(String id) { return applicationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private VolunteerEvent event(String id) { return eventRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private VolunteerMember member(String id) { return memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private VolunteerParticipation participation(String id, String eventId) { VolunteerParticipation p = participationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); if (!p.getEvent().getId().equals(eventId)) throw new BusinessException(ErrorCode.INVALID_PARAMETER); return p; }
    private void validateEventRequest(CreateVolunteerEventRequest r) { if (!r.endAt().isAfter(r.startAt()) || r.recruitmentDeadlineAt().isAfter(r.startAt())) throw new BusinessException(ErrorCode.INVALID_PARAMETER, "일정 시간을 확인해 주세요."); if (r.status() == VolunteerEventStatus.OPEN && !r.startAt().isAfter(now())) throw new BusinessException(ErrorCode.CONFLICT, "시작 전 일정만 모집할 수 있습니다."); }
    private Instant now() { return clock.instant(); }
}
