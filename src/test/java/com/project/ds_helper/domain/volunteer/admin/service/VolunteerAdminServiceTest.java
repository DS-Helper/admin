package com.project.ds_helper.domain.volunteer.admin.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.volunteer.application.entity.VolunteerApplication;
import com.project.ds_helper.domain.volunteer.application.repository.VolunteerApplicationRepository;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerApplicationStatus;
import com.project.ds_helper.domain.volunteer.event.repository.VolunteerEventRepository;
import com.project.ds_helper.domain.volunteer.file.repository.VolunteerFileRepository;
import com.project.ds_helper.domain.volunteer.history.service.VolunteerHistoryService;
import com.project.ds_helper.domain.volunteer.member.entity.VolunteerMember;
import com.project.ds_helper.domain.volunteer.member.repository.VolunteerMemberRepository;
import com.project.ds_helper.domain.volunteer.participation.repository.VolunteerParticipationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerAdminServiceTest {
    @Mock private VolunteerApplicationRepository applicationRepository;
    @Mock private VolunteerMemberRepository memberRepository;
    @Mock private VolunteerEventRepository eventRepository;
    @Mock private VolunteerParticipationRepository participationRepository;
    @Mock private VolunteerFileRepository fileRepository;
    @Mock private VolunteerHistoryService historyService;
    @Mock private Clock clock;
    @InjectMocks private VolunteerAdminService service;

    @Test
    void approve_createsActiveMemberAndChangesApplicationStatus() {
        User user = User.builder().id("user-1").build();
        VolunteerApplication application = VolunteerApplication.builder().id("application-1").user(user).status(VolunteerApplicationStatus.PENDING).build();
        Instant now = Instant.parse("2026-07-12T00:00:00Z");
        when(applicationRepository.findById("application-1")).thenReturn(Optional.of(application));
        when(memberRepository.findByUser_Id("user-1")).thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(now);

        service.approve("admin-1", "application-1");

        ArgumentCaptor<VolunteerMember> memberCaptor = ArgumentCaptor.forClass(VolunteerMember.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(application.getStatus()).isEqualTo(VolunteerApplicationStatus.APPROVED);
        assertThat(memberCaptor.getValue().getUser()).isSameAs(user);
        assertThat(memberCaptor.getValue().getJoinedAt()).isEqualTo(now);
        verify(historyService).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("application-1"), org.mockito.ArgumentMatchers.eq("PENDING"), org.mockito.ArgumentMatchers.eq("APPROVED"), org.mockito.ArgumentMatchers.eq("admin-1"), org.mockito.ArgumentMatchers.isNull());
    }
}
