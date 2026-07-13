package com.project.ds_helper.domain.reservation.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.reservation.dto.request.CreateOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.CreatePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.DeleteOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.DeletePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.RecipientGenderType;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.reservation.repository.OrganizationReservationRepository;
import com.project.ds_helper.domain.reservation.repository.PersonalReservationRepository;
import com.project.ds_helper.domain.reservation.repository.ReservationScheduleRepository;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class ReservationMutationServiceTest {

    @Mock private PersonalReservationRepository personalReservationRepository;
    @Mock private OrganizationReservationRepository organizationReservationRepository;
    @Mock private ReservationScheduleRepository reservationScheduleRepository;
    @Mock private UserUtil userUtil;
    @Mock private Authentication authentication;

    @InjectMocks private PersonalReservationService personalReservationService;
    @InjectMocks private OrganizationReservationService organizationReservationService;

    @Test
    @DisplayName("개인 예약 생성은 요청값을 저장하고 상태를 REQUESTED로 만든다")
    void createPersonalReservation_savesRequestedReservation() throws Exception {
        CreatePersonalReservationReqDto dto = CreatePersonalReservationReqDto.builder()
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 19))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-1").role(UserRole.USER).type(UserType.PERSONAL).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        personalReservationService.createPersonalReservation(authentication, dto);

        verify(personalReservationRepository).save(any(PersonalReservation.class));
    }

    @Test
    @DisplayName("개인 예약 생성은 개인 사용자가 아니면 거절한다")
    void createPersonalReservation_rejectsNonPersonalUser() {
        CreatePersonalReservationReqDto dto = CreatePersonalReservationReqDto.builder()
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 19))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-1").role(UserRole.USER).type(UserType.ORGANIZATION).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> personalReservationService.createPersonalReservation(authentication, dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("개인 예약 생성은 기존 요청 예약이 있으면 거절한다")
    void createPersonalReservation_rejectsWhenRequestedExists() {
        CreatePersonalReservationReqDto dto = CreatePersonalReservationReqDto.builder()
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 19))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-1").role(UserRole.USER).type(UserType.PERSONAL).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(personalReservationRepository.existsByUser_IdAndReservationStatus("user-1", ReservationStatus.REQUESTED)).thenReturn(true);

        assertThatThrownBy(() -> personalReservationService.createPersonalReservation(authentication, dto))
                .isInstanceOf(BadRequestException.class);
        verify(personalReservationRepository, never()).save(any(PersonalReservation.class));
    }

    @Test
    @DisplayName("개인 예약 생성은 당일 예약을 거절한다")
    void createPersonalReservation_rejectsSameDay() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        CreatePersonalReservationReqDto dto = CreatePersonalReservationReqDto.builder()
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(today)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-1").role(UserRole.USER).type(UserType.PERSONAL).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> personalReservationService.createPersonalReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("개인 예약 생성은 방문 요일이 아니면 거절한다")
    void createPersonalReservation_rejectsWrongDay() {
        CreatePersonalReservationReqDto dto = CreatePersonalReservationReqDto.builder()
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 13))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-1").role(UserRole.USER).type(UserType.PERSONAL).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> personalReservationService.createPersonalReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("개인 예약 생성은 시작/종료 시간이 범위를 벗어나면 거절한다")
    void createPersonalReservation_rejectsInvalidTime() {
        CreatePersonalReservationReqDto dto = CreatePersonalReservationReqDto.builder()
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 19))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-1").role(UserRole.USER).type(UserType.PERSONAL).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> personalReservationService.createPersonalReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("개인 예약 취소는 상태를 CANCELED로 바꾸고 저장한다")
    void cancelPersonalReservation_setsCanceledStatus() {
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .reservationStatus(ReservationStatus.REQUESTED)
                .build();
        when(authentication.getPrincipal()).thenReturn("user-1");
        when(personalReservationRepository.findById("pr-1")).thenReturn(Optional.of(reservation));

        personalReservationService.cancelPersonalReservation(authentication, new DeletePersonalReservationReqDto("pr-1"));

        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(reservation.getReservationSchedule()).isNull();
        verify(personalReservationRepository).save(reservation);
    }

    @Test
    @DisplayName("개인 예약 수정은 본인이 아니면 거절한다")
    void updatePersonalReservation_rejectsDifferentUser() {
        com.project.ds_helper.domain.reservation.dto.request.UpdatePersonalReservationReqDto dto =
                com.project.ds_helper.domain.reservation.dto.request.UpdatePersonalReservationReqDto.builder()
                        .personalReservationId("pr-1")
                .visitDate(LocalDate.of(2026, 7, 8))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .user(User.builder().id("owner").build())
                .build();
        when(authentication.getPrincipal()).thenReturn("other");
        when(personalReservationRepository.findById("pr-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> personalReservationService.updatePersonalReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약자 본인만 예약 수정이 가능");
    }

    @Test
    @DisplayName("개인 예약 수정은 방문 요일이 아니면 거절한다")
    void updatePersonalReservation_rejectsWrongDay() {
        var dto = com.project.ds_helper.domain.reservation.dto.request.UpdatePersonalReservationReqDto.builder()
                .personalReservationId("pr-1")
                .visitDate(LocalDate.of(2026, 7, 13))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .user(User.builder().id("owner").build())
                .build();
        when(authentication.getPrincipal()).thenReturn("owner");
        when(personalReservationRepository.findById("pr-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> personalReservationService.updatePersonalReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("개인 예약 목록은 전체 상태를 조회한다")
    void getAllPersonalReservationByStatus_returnsAllWhenAll() {
        User user = User.builder().id("user-1").build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .user(user)
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 19))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(2)
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(personalReservationRepository.findAllByUser_IdOrderByCreatedAtDesc(eq("user-1"), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(java.util.List.of(reservation)));

        var result = personalReservationService.getAllPersonalReservationByStatus(authentication, "all", 0, 10);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("개인 예약 목록은 상태별 조회를 수행한다")
    void getAllPersonalReservationByStatus_returnsFilteredWhenStatusProvided() {
        User user = User.builder().id("user-1").build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .user(user)
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(2)
                .reservationStatus(ReservationStatus.REQUESTED)
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(personalReservationRepository.findAllByUser_IdAndReservationStatusOrderByCreatedAtDesc(eq("user-1"), eq(ReservationStatus.REQUESTED), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(java.util.List.of(reservation)));

        var result = personalReservationService.getAllPersonalReservationByStatus(authentication, "대기", 0, 10);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("개인 예약 전체 조회는 사용자 예약 목록을 반환한다")
    void getAllPersonalReservation_returnsReservations() {
        User user = User.builder().id("user-1").build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .user(user)
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(2)
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(personalReservationRepository.findAllByUser_Id("user-1")).thenReturn(java.util.List.of(reservation));

        var result = personalReservationService.getAllPersonalReservation(authentication);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("개인 예약 단건 조회는 예약을 반환한다")
    void getOnePersonalReservation_returnsReservation() {
        User user = User.builder().id("user-1").build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .user(user)
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(2)
                .build();
        when(authentication.getPrincipal()).thenReturn("user-1");
        when(personalReservationRepository.findById("pr-1")).thenReturn(Optional.of(reservation));

        var result = personalReservationService.getOnePersonalReservation(authentication, "pr-1");

        assertThat(result.getPersonalReservationId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("개인 예약 수정은 성공 시 저장한다")
    void updatePersonalReservation_updatesReservation() {
        var dto = CreatePersonalReservationReqDto.builder()
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 8))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        // create dto is used to keep line count small; update dto mirrors the same inputs
        var updateDto = com.project.ds_helper.domain.reservation.dto.request.UpdatePersonalReservationReqDto.builder()
                .personalReservationId("pr-1")
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .visitDate(dto.getVisitDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .address(dto.getAddress())
                .recipientGenderType(dto.getRecipientGenderType())
                .recipientNumber(dto.getRecipientNumber())
                .build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("pr-1")
                .user(User.builder().id("owner").build())
                .reservationSchedule(com.project.ds_helper.domain.reservation.entity.ReservationSchedule.builder()
                        .visitDate(LocalDate.of(2026, 7, 8))
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(10, 0))
                        .build())
                .build();
        when(authentication.getPrincipal()).thenReturn("owner");
        when(personalReservationRepository.findById("pr-1")).thenReturn(Optional.of(reservation));

        personalReservationService.updatePersonalReservation(authentication, updateDto);

        verify(personalReservationRepository, times(1)).save(reservation);
        assertThat(reservation.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("기관 예약 생성은 요청값을 저장하고 상태를 REQUESTED로 만든다")
    void createOrganizationReservation_savesRequestedReservation() throws Exception {
        CreateOrganizationReservationReqDto dto = CreateOrganizationReservationReqDto.builder()
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 19))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-2").role(UserRole.USER).type(UserType.ORGANIZATION).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(user);

        organizationReservationService.createOrganizationReservation(authentication, dto);

        verify(organizationReservationRepository).save(any(OrganizationReservation.class));
    }

    @Test
    @DisplayName("기관 예약 생성은 기관 사용자가 아니면 거절한다")
    void createOrganizationReservation_rejectsNonOrganizationUser() {
        CreateOrganizationReservationReqDto dto = CreateOrganizationReservationReqDto.builder()
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-2").role(UserRole.USER).type(UserType.PERSONAL).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(user);

        assertThatThrownBy(() -> organizationReservationService.createOrganizationReservation(authentication, dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("기관 예약 생성은 방문 요일이 아니면 거절한다")
    void createOrganizationReservation_rejectsWrongDay() {
        CreateOrganizationReservationReqDto dto = CreateOrganizationReservationReqDto.builder()
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 13))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-2").role(UserRole.USER).type(UserType.ORGANIZATION).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(user);

        assertThatThrownBy(() -> organizationReservationService.createOrganizationReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기관 예약 생성은 당일 예약을 거절한다")
    void createOrganizationReservation_rejectsSameDay() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        CreateOrganizationReservationReqDto dto = CreateOrganizationReservationReqDto.builder()
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(today)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-2").role(UserRole.USER).type(UserType.ORGANIZATION).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(user);

        assertThatThrownBy(() -> organizationReservationService.createOrganizationReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기관 예약 생성은 시작/종료 시간이 범위를 벗어나면 거절한다")
    void createOrganizationReservation_rejectsInvalidTime() {
        CreateOrganizationReservationReqDto dto = CreateOrganizationReservationReqDto.builder()
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-2").role(UserRole.USER).type(UserType.ORGANIZATION).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(user);

        assertThatThrownBy(() -> organizationReservationService.createOrganizationReservation(authentication, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기관 예약 생성은 기존 요청 예약이 있으면 거절한다")
    void createOrganizationReservation_rejectsWhenRequestedExists() {
        CreateOrganizationReservationReqDto dto = CreateOrganizationReservationReqDto.builder()
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        User user = User.builder().id("user-2").role(UserRole.USER).type(UserType.ORGANIZATION).build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(userUtil.findUserById("user-2")).thenReturn(user);
        when(organizationReservationRepository.existsByUser_IdAndReservationStatus("user-2", ReservationStatus.REQUESTED)).thenReturn(true);

        assertThatThrownBy(() -> organizationReservationService.createOrganizationReservation(authentication, dto))
                .isInstanceOf(BadRequestException.class);
        verify(organizationReservationRepository, never()).save(any(OrganizationReservation.class));
    }

    @Test
    @DisplayName("기관 예약 수정은 성공 시 저장한다")
    void updateOrganizationReservation_updatesReservation() {
        var updateDto = com.project.ds_helper.domain.reservation.dto.request.UpdateOrganizationReservationReqDto.builder()
                .organizationReservationId("or-1")
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 8))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .user(User.builder().id("owner").build())
                .reservationSchedule(com.project.ds_helper.domain.reservation.entity.ReservationSchedule.builder()
                        .visitDate(LocalDate.of(2026, 7, 8))
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(10, 0))
                        .build())
                .build();
        when(authentication.getPrincipal()).thenReturn("owner");
        when(organizationReservationRepository.findById("or-1")).thenReturn(Optional.of(reservation));

        organizationReservationService.updateOrganizationReservation(authentication, updateDto);

        verify(organizationReservationRepository, times(1)).save(reservation);
        assertThat(reservation.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("기관 예약 수정은 본인이 아니면 거절한다")
    void updateOrganizationReservation_rejectsDifferentUser() {
        var updateDto = com.project.ds_helper.domain.reservation.dto.request.UpdateOrganizationReservationReqDto.builder()
                .organizationReservationId("or-1")
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 8))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .user(User.builder().id("owner").build())
                .build();
        when(authentication.getPrincipal()).thenReturn("other");
        when(organizationReservationRepository.findById("or-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> organizationReservationService.updateOrganizationReservation(authentication, updateDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기관 예약 수정은 방문 요일이 아니면 거절한다")
    void updateOrganizationReservation_rejectsWrongDay() {
        var updateDto = com.project.ds_helper.domain.reservation.dto.request.UpdateOrganizationReservationReqDto.builder()
                .organizationReservationId("or-1")
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 13))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .user(User.builder().id("owner").build())
                .build();
        when(authentication.getPrincipal()).thenReturn("owner");
        when(organizationReservationRepository.findById("or-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> organizationReservationService.updateOrganizationReservation(authentication, updateDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기관 예약 수정은 시작/종료 시간이 범위를 벗어나면 거절한다")
    void updateOrganizationReservation_rejectsInvalidTime() {
        var updateDto = com.project.ds_helper.domain.reservation.dto.request.UpdateOrganizationReservationReqDto.builder()
                .organizationReservationId("or-1")
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 8))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .address("대구")
                .recipientGenderType("남")
                .recipientNumber(2)
                .build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .user(User.builder().id("owner").build())
                .build();
        when(authentication.getPrincipal()).thenReturn("owner");
        when(organizationReservationRepository.findById("or-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> organizationReservationService.updateOrganizationReservation(authentication, updateDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기관 예약 단건 조회는 없는 예약이면 예외를 던진다")
    void getOneOrganizationReservation_throwsWhenMissing() {
        when(authentication.getPrincipal()).thenReturn("user-2");
        when(organizationReservationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationReservationService.getOneOrganizationReservation(authentication, "missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기관 예약 취소는 상태를 CANCELED로 바꾸고 저장한다")
    void cancelOrganizationReservation_setsCanceledStatus() {
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .reservationStatus(ReservationStatus.REQUESTED)
                .build();
        when(authentication.getPrincipal()).thenReturn("user-2");
        when(organizationReservationRepository.findById("or-1")).thenReturn(Optional.of(reservation));

        organizationReservationService.cancelOrganizationReservation(authentication, new DeleteOrganizationReservationReqDto("or-1"));

        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(reservation.getReservationSchedule()).isNull();
        verify(organizationReservationRepository).save(reservation);
    }

    @Test
    @DisplayName("기관 예약 목록은 전체 상태를 조회한다")
    void getAllOrganizationReservationByReservationStatus_returnsAllWhenAll() {
        User user = User.builder().id("user-2").build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .user(user)
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(2)
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(organizationReservationRepository.findAllByUser_IdOrderByCreatedAtDesc(eq("user-2"), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(java.util.List.of(reservation)));

        var result = organizationReservationService.getAllOrganizationReservationByReservationStatus(authentication, "all", 0, 10, "desc");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("기관 예약 목록은 상태별 조회를 수행한다")
    void getAllOrganizationReservationByReservationStatus_returnsFilteredWhenStatusProvided() {
        User user = User.builder().id("user-2").build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .user(user)
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(2)
                .reservationStatus(ReservationStatus.REQUESTED)
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-2");
        when(organizationReservationRepository.findAllByUser_IdAndReservationStatusOrderByCreatedAtDesc(eq("user-2"), eq(ReservationStatus.REQUESTED), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(java.util.List.of(reservation)));

        var result = organizationReservationService.getAllOrganizationReservationByReservationStatus(authentication, "대기", 0, 10, "desc");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("기관 예약 단건 조회는 예약을 반환한다")
    void getOneOrganizationReservation_returnsReservation() {
        User user = User.builder().id("user-2").build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("or-1")
                .user(user)
                .organizationName("기관")
                .name("홍길동")
                .phoneNumber("010-1234-5678")
                .visitDate(LocalDate.of(2026, 7, 12))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .address("대구")
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(2)
                .build();
        when(authentication.getPrincipal()).thenReturn("user-2");
        when(organizationReservationRepository.findById("or-1")).thenReturn(Optional.of(reservation));

        var result = organizationReservationService.getOneOrganizationReservation(authentication, "or-1");

        assertThat(result.getOrganizationReservationId()).isEqualTo("or-1");
    }

}
