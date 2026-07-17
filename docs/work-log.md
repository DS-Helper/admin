# 작업 내역 관리 문서

## 2026-07-06

| 작업 | 상태 | 범위 | 검증 |
|---|---|---|---|
| `TrashBinService` SRP 분리 | 완료 | CSV 파싱/중복 검증과 이미지 업로드 책임을 별도 서비스로 분리 | `./gradlew.bat test` 통과 |
| `BoardService` SRP 탐색 | 진행 중 | 조회/작성/수정/이미지/권한 책임이 한 클래스에 집중된 상태 확인 | 분리 설계 검토 중 |
| `BoardService` 조회 분리 시도 | 진행 중 | 조회 책임을 `BoardQueryService`로 이동 시도, 기존 테스트와 충돌 확인 | `BoardServiceTest` 다수 실패 |
| `UserService` SRP 분리 | 진행 중 | 프로필 조회/수정 책임을 `UserProfileService`로 분리 | `./gradlew.bat test` 중 `BoardServiceTest` 실패 |
| `NotificationService` SRP 탐색 | 진행 중 | 생성/조회/읽음 처리/카운트가 단일 서비스에 집중 | 분리 후보 확인 |
| `NotificationService` SRP 분리 | 진행 중 | 조회/카운트와 읽음 변경을 분리 | 빌드 검증 예정 |
| `NotificationService` facade 복구 | 진행 중 | 기존 `NotificationService` 진입점 유지 위해 위임 메서드 복구 | `BoardServiceTest`, `UserServiceTest` 기존 실패 지속 |
| `PostService` SRP 탐색 | 진행 중 | 조회/작성/수정/삭제/이미지 처리 책임 집중 | 분리 후보 확인 |
| `CommentService` SRP 탐색 | 진행 중 | 생성/수정/삭제/조회와 알림 오케스트레이션 포함 | 분리 후보 확인 |
| `ReplyService` SRP 분리 | 진행 중 | 조회/생성과 문의 상태 변경을 분리 | 빌드 검증 예정 |
| `ReplyService` facade 복구 | 진행 중 | 기존 `ReplyService` 진입점 유지 | `BoardServiceTest`, `NotificationServiceTest`, `UserServiceTest` 실패 지속 |
| `CommentService` SRP 탐색 | 진행 중 | 생성/수정/삭제/조회 + 알림 오케스트레이션 확인 | 분리 가능성 검토 중 |
| `CommentService` 알림 위임 확인 | 완료 | 댓글 생성 후 알림은 `NotificationFacade`로 위임 | 기존 구조 유지 확인 |
| `UserService` 테스트 계약 복구 | 진행 중 | `UserProfileService` 위임 구조에 맞춰 테스트 더블 보강 | 빌드 검증 예정 |
| `ReplyService` 테스트 계약 복구 | 진행 중 | `ReplyQueryService`/`ReplyCommandService` 위임 구조에 맞춰 테스트 더블 보강 필요 | `./gradlew.bat test` 실패 확인 |
| `NotificationService` facade 폴백 복구 | 진행 중 | 조회/카운트는 기존 로직 폴백, 읽음 변경은 추후 정리 | 재검증 예정 |
| `NotificationService`/`UserService` 테스트 정리 | 진행 중 | facade 구조 변경에 따른 Mockito 스텁 조정 필요 | `./gradlew.bat test` 실패 지속 |
| `NotificationService` 읽음 처리 폴백 복구 | 진행 중 | `markAsRead`, `markAllAsRead` 기존 로직 복구 | 재검증 예정 |
| `현재 빌드 재검증` | 진행 중 | `BoardServiceTest` 중심 실패 지속 확인 | 전체 테스트 실패 상태 |
| `UserServiceTest`/`NotificationControllerTest` 정리 | 진행 중 | 새 facade 분리 구조에 맞춰 mock 대상 조정 | 재검증 예정 |
| `ReplyServiceTest`/`UserServiceTest` 불필요 스텁 제거 | 진행 중 | facade 위임 구조에 맞지 않는 Mockito 스텁 정리 | 재검증 예정 |
| `UserServiceTest` 직접 로직 복구 | 진행 중 | `UserService` 직접 로직 기준으로 테스트 정리 | 재검증 예정 |
| `UserServiceTest` 불필요 mock 제거 | 진행 중 | 삭제된 `UserProfileService` mock 정리 | 재검증 예정 |
| `UserServiceTest` 불필요 스텁 제거 추가 | 진행 중 | 유지 시나리오에서 호출되지 않는 `isManagedS3Url` 제거 | 재검증 예정 |
| `BoardService` 조회 로직 복구 | 진행 중 | `BoardQueryService` 분리로 깨진 목록/상세/내글 조회를 직접 로직으로 복귀 | `./gradlew.bat test` 재검증 예정 |
| `BoardService` 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `UserService` 프로필 책임 분리 | 진행 중 | 마이페이지 조회/수정 로직을 `UserProfileService`로 이동 | 테스트 재검증 예정 |
| `UserService`/`UserProfileService` 테스트 분리 | 완료 | 프로필 테스트를 별도 테스트 클래스로 이동 | 관련 테스트 통과 |
| `InquiryService` 책임 분리 | 진행 중 | 문의 조회를 `InquiryQueryService`, 생성/업로드를 `InquiryCommandService`로 분리 | 테스트 재검증 예정 |
| `InquiryServiceTest` 위임 계약 전환 | 진행 중 | 기존 직접 로직 테스트를 새 query/command 구조에 맞게 조정 | 재검증 예정 |
| `InquiryControllerTest` 위임 계약 전환 | 진행 중 | 컨트롤러가 `InquiryQueryService`/`InquiryCommandService`를 직접 주입하도록 조정 | 재검증 예정 |
| `InquiryService`/컨트롤러 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `WelfareService` 책임 분리 | 진행 중 | 추천/조회를 `WelfareCommandService`/`WelfareQueryService`로 분리 | 테스트 재검증 예정 |
| `WelfareService`/컨트롤러 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `VolunteerAdminService` 책임 분리 | 진행 중 | 일정/신청/참여/통계를 전용 서비스로 분리 | 테스트 재검증 예정 |
| `VolunteerAdminService` 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `ReplyService` 책임 정리 | 진행 중 | 답변 조회/생성을 query/command로 단순 위임 | 테스트 재검증 예정 |
| `ReplyService` 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `ReservationService` 조회 책임 분리 | 진행 중 | 기예약 슬롯 조회를 `ReservationQueryService`로 이동 | 테스트 재검증 예정 |
| `ReservationService` 테스트 계약 전환 | 진행 중 | 예약 조회 테스트를 query 서비스 기준으로 재정리 | 재검증 예정 |
| `ReservationControllerTest` 위임 계약 전환 | 진행 중 | 컨트롤러 mock 대상을 `ReservationQueryService`로 변경 | 재검증 예정 |
| `ReservationService` 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `JaCoCo 커버리지 점검` | 진행 중 | 전체 커버리지 57% 확인, 목표 90% 대비 부족 | 추가 테스트/정리 필요 |
| `CommentService` SRP 분리 후보 확인 | 진행 중 | 조회/작성/수정/삭제가 단일 서비스에 집중 | 다음 분리 대상 |
| `CommentService` 책임 분리 | 진행 중 | 조회를 `CommentQueryService`, 생성/수정/삭제를 `CommentCommandService`로 분리 | 테스트 재검증 예정 |
| `CommentService` 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `VolunteerService` 테스트 추가 | 진행 중 | 봉사 일정/신청/참여/통계 핵심 경로 테스트 추가 | 재검증 예정 |
| `VolunteerService` 전체 테스트 재검증 | 완료 | `./gradlew.bat test` 성공 | 현재 회귀 없음 |
| `UserService` 분기 테스트 보강 | 진행 중 | 로그인/가입/탈퇴/토큰 발급 분기 테스트 추가 | 전체 재검증 예정 |
| `TrashBinCsvService`/`TrashBinImageService` 테스트 추가 | 진행 중 | trashbin helper 서비스 직접 테스트 추가 | 전체 재검증 예정 |
| `UserService` 분기 테스트 보강 | 완료 | `./gradlew.bat test --tests com.project.ds_helper.domain.user.service.UserServiceTest` 성공 | 전체 재검증 예정 |
| `TrashBinCsvService`/`TrashBinImageService` 테스트 추가 | 완료 | `./gradlew.bat test --tests com.project.ds_helper.domain.trashbin.service.TrashBinCsvServiceTest --tests com.project.ds_helper.domain.trashbin.service.TrashBinImageServiceTest` 성공 | 전체 재검증 예정 |
| `common.s3`/`common.web`/`common.security` 테스트 추가 | 완료 | 설정/보안/CORS 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.swagger`/`common.websocket`/`domain.user.webClient` 테스트 추가 | 완료 | Swagger/WebSocket/OAuth WebClient 설정 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.websocket`/`common.security` 추가 테스트 보강 | 완료 | WebSocketConfig/SecurityConfig 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.exception`/`common.redis` 테스트 추가 | 완료 | GlobalExceptionHandler/RedisConfig/RedisKey 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.security` 추가 테스트 보강 | 완료 | SecurityConfig 체인 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `reservation.dto.request`/`inquiry.dto.request` 테스트 추가 | 완료 | 요청 DTO 엔티티 변환 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `reservation.dto.request` 추가 보강 | 완료 | update/delete 요청 DTO 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `welfare.util`/`welfare.config` 테스트 추가 | 완료 | 복지 유틸/설정 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.security` 보안 응답 테스트 보강 | 완료 | SecurityResponseWriter 추가 분기 테스트 및 재검증 성공 | 전체 재검증 예정 |
| `reply`/`volunteer` 엔티티 및 DTO 테스트 추가 | 완료 | 답변/봉사 엔티티 기본 분기 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `volunteer.enums` 테스트 추가 | 완료 | 봉사 상태 enum 분기 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `volunteer.entity` 식별자 생성 테스트 추가 | 완료 | 봉사 엔티티 prePersist 식별자 생성 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.exception` 분기 테스트 보강 | 완료 | GlobalExceptionHandler의 추가 예외 분기 테스트 보강 및 재검증 성공 | 전체 재검증 예정 |
| `common.security` 설정 테스트 보강 | 완료 | SecurityConfig CORS/필터체인 테스트 보강 및 재검증 성공 | 전체 재검증 예정 |
| `common.util` 추가 테스트 | 완료 | FileUtil/CookieUtil 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.util` 추가 테스트 보강 | 완료 | ObjectMapperUtil/PasswordUtil/UserUtil 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.util` 추가 테스트 보강 2 | 완료 | PostUtil/GmailUtil/DiscordWebhookUtil 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.filter` 테스트 추가 | 완료 | CustomLoginFilter/UrlFilter 분기 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.filter` 테스트 추가 보강 | 완료 | JwtFilter/CustomLogoutFilter 분기 테스트 추가 및 재검증 성공 | 전체 재검증 예정 |
| `common.filter` 로그아웃/토큰 예외 테스트 보강 | 완료 | CustomLogoutFilter 실패/성공 분기와 JwtFilter 예외 분기 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `UserService` 토큰 책임 분리 | 완료 | 토큰 발급/로그인 체크를 `UserAuthTokenService`로 이동 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `volunteer.service` 테스트 보강 | 완료 | 하위 admin 서비스별 핵심 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `volunteer.service` 테스트 보강 2 | 완료 | 커서 DTO/신청 DTO 타입 정합성 보정 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `board.service` 조회 책임 테스트 보강 | 완료 | `BoardQueryService` 핵심 조회 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `UserService` 기관 가입 분기 테스트 보강 | 완료 | 인증서 없는 기관 가입 경로 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `common.security` 위임 테스트 보강 | 완료 | `CustomAuthenticationEntryPoint`/`CustomAccessDeniedHandler` 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `volunteer.service` 통계 테스트 보강 | 완료 | `VolunteerStatisticsAdminService` 요약/통계 경로 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `common.exception` 분기 테스트 보강 2 | 완료 | `BadRequestException`/본문 파싱 실패 경로 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `volunteer.service` 상태변경/조회 테스트 보강 | 완료 | 신청/참여 상태변경 및 참여 목록 경로 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `UserAuthTokenService` 분기 테스트 보강 | 완료 | 로그인 체크 실패 분기 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `UserProfileService` 분기 테스트 보강 | 완료 | 이메일 중복/비관리 이미지 삭제 분기 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `domain.user.service` OAuth 분기 테스트 보강 | 완료 | Google/Kakao refresh token 재발급 및 누락 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `reservation.service` 책임/기본 분기 테스트 보강 | 완료 | 서비스 델리게이션과 날짜 기본값 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `reservation.service` 생성/취소 분기 테스트 보강 | 완료 | 개인/기관 예약 생성·취소 핵심 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `reservation.service` 생성 실패 분기 테스트 보강 | 완료 | 개인/기관 예약 생성 검증 실패 경로 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `reservation.service` 수정 분기 테스트 보강 | 완료 | 개인/기관 예약 수정 성공 경로 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `reservation.service` 조회 분기 테스트 보강 | 완료 | 예약 슬롯 조회 기본값/다중 구간 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `reservation.service` 상태 조회 분기 테스트 보강 | 완료 | 개인/기관 예약 상태별 전체/필터 조회 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `welfare.service` 위임/조회 테스트 추가 | 완료 | `WelfareService`/`WelfareQueryService` 핵심 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `welfare.service` 코드/추천 테스트 추가 | 완료 | `WelfareCodeService`/`WelfareCommandService` 핵심 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `trashbin.service` 추가 분기 테스트 보강 | 진행중 | 쓰레기통 이미지/CSV 서비스 경계 분기 보강 | 재검증 예정 |
| `trashbin.service` 이미지 실패 분기 테스트 보강 | 완료 | 이미지 파일명/빈 입력 예외 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `notification.service` 명령/디스패치 테스트 추가 | 완료 | NotificationCommandService/LoggingPushDispatchService 핵심 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `common.util` JwtUtil 테스트 보강 | 완료 | JWT 헤더 파싱 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 재검증 예정 |
| `welfare.service` 위임/조회 테스트 추가 | 진행중 | WelfareService/WelfareQueryService 핵심 분기 테스트 추가 | 재검증 예정 |
| `board.service` 조회/이미지 테스트 보강 | 진행중 | BoardQueryService/BoardImageService 분기 테스트 추가 후 `./gradlew.bat test` 성공 | 전체 커버리지 재점검 필요 |
| `board.service` 조회 분기 추가 보강 | 진행중 | BoardQueryService 인증/정렬/미존재 예외 분기 추가 | 재검증 예정 |
| `board.service` 조회 분기 재보강 | 진행중 | BoardQueryService 인증/anonymous/미존재/정렬 분기 재검증 후 `./gradlew.bat test` 성공 | 전체 커버리지 재점검 필요 |
| `welfare.service` 코드 서비스 테스트 보강 | 진행중 | WelfareCodeService 공개 분기 테스트 추가 | 재검증 예정 |
| `welfare.service` 패키지 커버리지 재검증 | 완료 | 전체 `./gradlew.bat test` 후 `com.project.ds_helper.domain.welfare.service` 99% 확인 | 남은 저커버리지 분기 추가 보강 가능 |
| `welfare.service` 잔여 분기 보강 | 진행중 | WelfareCommandService/WelfareQueryService null·blank 분기 추가 | 재검증 예정 |
| `welfare.service` 잔여 분기 재검증 | 완료 | `./gradlew.bat test` 후 `com.project.ds_helper.domain.welfare.service` 99% 확인 | 90% 기준 충족 |
| `reservation.service` 상태 조회/시간 검증 테스트 보강 | 진행중 | 개인/기관 목록 상태 분기와 시간 범위 예외 추가 | 재검증 예정 |
| `reservation.service` 상태 조회 테스트 재검증 | 완료 | `ReservationMutationServiceTest` 통과 후 상태 조회 분기 확인 | 전체 패키지 재점검 필요 |
| `reservation.service` 생성/수정 거절 분기 추가 | 진행중 | 개인/기관 요일·당일·본인 검증 실패 분기 추가 | 재검증 예정 |
| `reservation.service` 단건/전체 조회 테스트 보강 | 진행중 | 개인/기관 단건 및 전체 조회 분기 추가 | 재검증 예정 |
| `reservation.service` 패키지 커버리지 재검증 | 완료 | 전체 `./gradlew.bat test` 후 `com.project.ds_helper.domain.reservation.service` 90% 확인 | 기준 충족 |
| `reservation.service` 잔여 예외 분기 추가 | 진행중 | 기관 예약 당일/요일/시간/단건 예외 분기 추가 | 재검증 예정 |
| `reservation.service` 패키지 최종 재검증 | 완료 | 전체 `./gradlew.bat test` 후 `com.project.ds_helper.domain.reservation.service` 94% 확인 | 기준 충족 |
| `trashbin.service` 분기 테스트 보강 | 완료 | `TrashBinServiceTest`/`TrashBinImageServiceTest`/`TrashBinCsvServiceTest`에 private 분기 및 롤백 분기 추가 | `./gradlew.bat test` 통과 |
| `trashbin.service` 패키지 재검증 | 완료 | `com.project.ds_helper.domain.trashbin.service` 총 97% 확인 | 기준 충족 |
| `notification.service` 커서/푸시 테스트 보강 | 완료 | `NotificationQueryServiceTest` 추가, `NotificationFacadeTest` 분기 보강 | `./gradlew.bat test` 통과 |
| `notification.service` 패키지 재검증 | 완료 | `com.project.ds_helper.domain.notification.service` 93% 확인 | 기준 충족 |
| `admin.service` 예약/문의 테스트 보강 | 완료 | `AdminReservationServiceTest`/`AdminInquiryServiceTest`에 조회 분기 추가 | `./gradlew.bat test` 통과 |
| `admin.service` 패키지 재검증 | 완료 | `com.project.ds_helper.domain.admin.service` 98% 확인 | 기준 충족 |
| `admin.service` 기관 예약 상태변경 테스트 보강 | 완료 | `AdminOrganizationReservationServiceTest`에 취소/미존재 분기 추가 후 `./gradlew.bat test` 성공 | `com.project.ds_helper.domain.admin.service` 100% 확인 |
| `board.service` 패키지 재검증 | 완료 | `BoardServiceTest`/`BoardQueryServiceTest`/`BoardImageServiceTest`/`BoardLikeServiceTest`/`BoardScrapServiceTest` 전체 실행 후 `com.project.ds_helper.domain.board.service` 99% 확인 | 기준 충족 |
| `volunteer.service` 패키지 재검증 | 완료 | `VolunteerAdminServiceTest`/`VolunteerApplicationAdminServiceTest`/`VolunteerParticipationAdminServiceTest`/`VolunteerScheduleAdminServiceTest`/`VolunteerStatisticsAdminServiceTest` 전체 실행 후 `com.project.ds_helper.domain.volunteer.service` 98% 확인 | 기준 충족 |
| `volunteer.admin` 관리자 API 재구성 | 완료 | 관리자 컨트롤러를 `/api/v1/admin/volunteer` 기준으로 재정리하고 일정/신청/참여/통계/회원 관리 응답·테스트를 추가 | `./gradlew.bat test` 성공 |
| `reservation.service` 생성 테스트 회귀 수정 | 완료 | 예약 생성 테스트의 날짜를 미래 일요일로 정정해 당일 예약 예외를 제거 | `./gradlew.bat test` 성공 |
| `volunteer.admin` 문서 상태 계약 전환 | 진행중 | `volunteer_plan.md` 기준으로 관리자 경로 권한과 일정·신청·참여 상태값을 전환하고 구형 테스트를 폐기 | 전체 관리자 API·엔티티 재구성 및 테스트 재작성 필요 |
| `volunteer.entity` BE 공통 엔티티·마이그레이션 정렬 | 진행중 | 원본 BE의 `VolunteerNotification` 및 V3 봉사 테이블 마이그레이션을 추가하고 Admin 전용 `VolunteerMember`를 V4로 분리 | Enum·관리자 서비스 계약 정렬 및 테스트 필요 |
| `volunteer.entity` BE 공통 모델 호환 정렬 | 완료 | 공통 Entity 5종과 상태 Enum을 원본 BE와 동일하게 맞추고 V3를 이식, Admin 전용 봉사단원은 V4로 분리 | Gradle 데몬 종료 대기으로 컴파일 결과 재확인 필요 |
| `volunteer.admin` V6 공유 스키마 전환 | 진행중 | V6 Entity/Repository, 관리자 신청·단원·일정·출석 API를 추가하고 legacy schedule 참조 제거 | `compileJava` 성공, API 테스트·Outbox 관리자 이벤트 확장 필요 |
| `s3.util` Presigned URL 전환 | 진행중 | S3 GET URL 생성을 presigned URL로 전환하고 Board 이미지 보존 비교를 S3 key 기준으로 변경 | 소스/테스트 컴파일 재확인 대기 |
| `volunteer.admin` API 계약 정합화 | 완료 | record DTO, UUID `id`, 검색·필터·페이지 응답, private 사진 presigned URL, 출석 결과 DTO·업무 오류 코드 추가 | `compileJava`, volunteer admin/file 테스트 통과 |
