# DSHelper 봉사단 기능

# Next.js · Spring Boot 개발 업무 분리 가이드

## 1. 전체 아키텍처

```text
사용자 브라우저
    ↓
Next.js
- 화면 렌더링
- 사용자 입력
- 버튼 및 폼 상태
- API 요청
- 서버 응답 표시
    ↓ HTTPS
Spring Boot
- 인증 및 권한 확인
- 업무 규칙 판단
- 상태 변경
- 동시성 제어
- DB 저장
- 파일 저장
- 자동 처리
- 알림 생성
    ↓
MySQL / 파일 저장소 / 알림 시스템
```

## 1-1. 핵심 책임 원칙

### Next.js가 담당하는 것

```text
사용자에게 무엇을 보여줄 것인가
사용자가 어떤 값을 입력했는가
서버의 처리 결과를 어떻게 표현할 것인가
```

### Spring Boot가 담당하는 것

```text
사용자가 해당 작업을 수행할 수 있는가
현재 상태에서 상태 변경이 가능한가
정원이 남아 있는가
시간이 겹치는가
취소 가능 시간이 지났는가
DB에 어떤 상태로 저장할 것인가
```

### 가장 중요한 원칙

Next.js는 업무 규칙을 최종 판단하지 않는다.

예를 들어 Next.js에서 다음 검사를 할 수는 있다.

```text
현재 시간이 봉사 시작 2시간 전을 지났으므로
취소 버튼을 비활성화
```

하지만 사용자가 API를 직접 호출할 수 있으므로 Spring Boot에서도 반드시 다시 검사해야 한다.

```text
Next.js 검사
→ 사용자 편의를 위한 사전 검사

Spring Boot 검사
→ 실제 처리를 결정하는 최종 검사
```

---

# 2. 기술 구성

## 2-1. Next.js

```text
Next.js 16.2
App Router
TypeScript
React
React Hook Form
Zod
TanStack Query 또는 SWR
Tailwind CSS
Playwright
Vitest
Mock Service Worker
```

App Router에서는 페이지와 레이아웃이 기본적으로 Server Component이며, 클릭 이벤트, 폼 상태, 브라우저 API처럼 상호작용이 필요한 부분만 Client Component로 구성한다.

## 2-2. Spring Boot

```text
Java 21
Spring Boot 4.1.x
Spring Security
Spring Data JPA
Hibernate
MySQL 8
Flyway
Bean Validation
JUnit 5
Testcontainers
```

## 2-3. 배포 구조

```text
프론트엔드
https://dshelper.kr

백엔드
https://server.dshelper.kr
```

Next.js와 Spring Boot가 다른 서브도메인을 사용하므로 다음 설정이 필요하다.

### Next.js

```text
NEXT_PUBLIC_API_BASE_URL=https://server.dshelper.kr
```

### Spring Boot

```text
허용 Origin
https://dshelper.kr

Credentials 허용 여부
인증 구조에 따라 true 설정
```

---

# 3. 전체 책임 분리표

| 업무          | Next.js  | Spring Boot    |
| ----------- | -------- | -------------- |
| 봉사단 소개 표시   | 담당       | 소개 데이터 제공      |
| 로그인 여부 표시   | 담당       | 인증 상태 검증       |
| 가입 신청 폼     | 담당       | 신청 데이터 검증·저장   |
| 본인 사진 선택    | 담당       | 파일 검증·비공개 저장   |
| 가입 상태 표시    | 담당       | 실제 가입 상태 반환    |
| 가입 승인·반려    | 관리자 화면   | 상태 전이·DB 처리    |
| 일정 목록 표시    | 담당       | 일정 조회·필터·권한 검사 |
| 참여자 이름 마스킹  | 응답 표시    | 마스킹된 데이터 생성    |
| 참여 신청 버튼    | 담당       | 참여 가능 여부 최종 판단 |
| 시간 중복 검사    | 안내용 가능   | 최종 검사          |
| 정원 검사       | 응답 표시    | 락을 사용한 최종 검사   |
| 참여 취소 버튼    | 담당       | 2시간 제한 최종 검사   |
| 일정 자동 완료    | 해당 없음    | 스케줄러 처리        |
| 출석·불참 화면    | 담당       | 상태 변경·기록 저장    |
| 누적 참여 횟수 표시 | 담당       | 통계 계산          |
| 총 참여 시간 표시  | 담당       | 통계 계산          |
| 알림 목록 표시    | 담당       | 알림 생성·저장·발송    |
| 개인정보 접근 통제  | 민감정보 미노출 | 권한 검사·접근 차단    |
| 관리자 메모      | 입력·표시    | 관리자만 조회·저장     |
| 감사 로그       | 해당 없음    | 처리자·변경 이력 저장   |

---

# 4. 공통 API 통신 원칙

## 4-1. API 응답 형식

성공 응답 형식을 통일한다.

```json
{
  "data": {
    "id": 1
  }
}
```

목록 응답:

```json
{
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

오류 응답:

```json
{
  "code": "VOLUNTEER_EVENT_CAPACITY_FULL",
  "message": "봉사 모집 인원이 마감되었습니다.",
  "fieldErrors": []
}
```

필드 오류:

```json
{
  "code": "INVALID_REQUEST",
  "message": "입력값을 확인해 주세요.",
  "fieldErrors": [
    {
      "field": "motivation",
      "message": "지원동기를 20자 이상 입력해 주세요."
    }
  ]
}
```

---

## 4-2. Next.js가 오류 코드를 사용하는 방법

Next.js는 서버 메시지만 그대로 출력하지 않고 오류 코드에 따라 화면 동작을 결정한다.

```typescript
switch (error.code) {
  case 'VOLUNTEER_EVENT_CAPACITY_FULL':
    // 참여 버튼 비활성화
    // 일정 데이터 다시 조회
    break

  case 'VOLUNTEER_TIME_CONFLICT':
    // 겹치는 일정 안내
    break

  case 'VOLUNTEER_CANCEL_DEADLINE_EXPIRED':
    // 취소 버튼 비활성화
    // 사용자에게 관리자 문의 안내
    break
}
```

---

## 4-3. 기능 가능 여부 응답

Next.js에서 업무 규칙을 중복 구현하지 않도록 Spring Boot가 기능 가능 여부를 반환한다.

```json
{
  "id": 10,
  "title": "송해공원 플로깅 봉사",
  "status": "OPEN",
  "participantCount": 5,
  "capacity": 8,
  "myParticipationStatus": null,
  "capabilities": {
    "canApply": true,
    "canCancel": false,
    "canViewParticipants": true
  }
}
```

### 장점

```text
Spring Boot
→ 실제 규칙에 따라 canApply 결정

Next.js
→ canApply 값으로 버튼 표시
```

사용자가 활동정지되거나 일정이 마감되어도 프론트엔드에 복잡한 업무 규칙을 중복 작성할 필요가 없다.

---

# 5. Next.js 전체 업무

# 5-1. Next.js 역할

Next.js는 다음 항목을 담당한다.

1. 봉사단 사용자 화면
2. 봉사단 관리자 화면
3. 폼 입력 및 클라이언트 검증
4. API 호출
5. 로딩·오류·빈 화면 처리
6. 사용자 권한에 따른 메뉴 표시
7. 이미지 선택 및 미리보기
8. 확인 모달과 안내 문구
9. 서버 상태 변경 후 화면 갱신
10. 접근성과 반응형 UI

Next.js에는 다음 로직을 두지 않는다.

1. 모집 인원 최종 판단
2. 가입 승인 가능 여부 최종 판단
3. 출석 처리 가능 여부 최종 판단
4. 시간 중복 최종 판단
5. 봉사 참여 기록 계산
6. 사용자 권한 최종 판단
7. 일정 자동 완료
8. DB 상태 변경

---

# 5-2. Next.js 권장 디렉터리

```text
src
├── app
│   ├── (public)
│   │   └── volunteer
│   │       ├── page.tsx
│   │       └── loading.tsx
│   │
│   ├── (user)
│   │   └── volunteer
│   │       ├── application
│   │       │   ├── page.tsx
│   │       │   └── edit
│   │       │       └── page.tsx
│   │       ├── application-status
│   │       │   └── page.tsx
│   │       ├── events
│   │       │   ├── page.tsx
│   │       │   └── [eventId]
│   │       │       └── page.tsx
│   │       └── my
│   │           └── page.tsx
│   │
│   ├── admin
│   │   └── volunteer
│   │       ├── applications
│   │       │   ├── page.tsx
│   │       │   └── [applicationId]
│   │       │       └── page.tsx
│   │       ├── members
│   │       │   ├── page.tsx
│   │       │   └── [memberId]
│   │       │       └── page.tsx
│   │       └── events
│   │           ├── page.tsx
│   │           ├── new
│   │           │   └── page.tsx
│   │           └── [eventId]
│   │               ├── page.tsx
│   │               ├── edit
│   │               │   └── page.tsx
│   │               └── attendance
│   │                   └── page.tsx
│   │
│   ├── layout.tsx
│   ├── error.tsx
│   └── not-found.tsx
│
├── features
│   └── volunteer
│       ├── api
│       ├── components
│       ├── hooks
│       ├── schemas
│       ├── types
│       └── utils
│
├── components
│   ├── common
│   ├── form
│   └── modal
│
└── lib
    ├── api
    ├── auth
    ├── date
    └── errors
```

---

# 5-3. Next.js 페이지 목록

## 사용자 화면

| 경로                              | 화면       |
| ------------------------------- | -------- |
| `/volunteer`                    | 봉사단 소개   |
| `/volunteer/application`        | 가입 신청    |
| `/volunteer/application-status` | 가입 상태    |
| `/volunteer/application/edit`   | 가입 신청 수정 |
| `/volunteer/events`             | 봉사 일정 목록 |
| `/volunteer/events/[eventId]`   | 일정 상세    |
| `/volunteer/my`                 | 내 봉사활동   |

## 관리자 화면

| 경로                                        | 화면       |
| ----------------------------------------- | -------- |
| `/admin/volunteer/applications`           | 가입 신청 목록 |
| `/admin/volunteer/applications/[id]`      | 가입 신청 상세 |
| `/admin/volunteer/members`                | 봉사단원 목록  |
| `/admin/volunteer/members/[id]`           | 봉사단원 상세  |
| `/admin/volunteer/events`                 | 일정 관리    |
| `/admin/volunteer/events/new`             | 일정 등록    |
| `/admin/volunteer/events/[id]`            | 일정 상세    |
| `/admin/volunteer/events/[id]/edit`       | 일정 수정    |
| `/admin/volunteer/events/[id]/attendance` | 출석 관리    |

---

# 5-4. Server Component와 Client Component 분리

## Server Component 권장 대상

```text
봉사단 소개 페이지
봉사 일정 최초 목록
봉사 일정 상세 최초 데이터
내 봉사활동 최초 데이터
관리자 신청 목록 최초 데이터
관리자 일정 목록 최초 데이터
```

## Client Component 필요 대상

```text
가입 신청 폼
이미지 업로드
일정 필터
참여하기 버튼
취소하기 버튼
확인 모달
출석 체크
관리자 메모 입력
페이지 내 검색
토스트 알림
```

Next.js의 페이지와 레이아웃은 기본적으로 Server Component이며, 상태·이벤트·브라우저 API가 필요한 부분에만 `"use client"`를 적용한다.

예시:

```tsx
// Server Component
export default async function VolunteerEventPage() {
  const events = await getVolunteerEvents()

  return <VolunteerEventList initialEvents={events} />
}
```

```tsx
'use client'

export function VolunteerApplyButton({
  eventId,
  canApply,
}: {
  eventId: number
  canApply: boolean
}) {
  // 클릭 이벤트와 요청 상태 처리
}
```

---

# 5-5. Next.js API 계층

## API 함수

```text
features/volunteer/api
├── volunteer-application-api.ts
├── volunteer-event-api.ts
├── volunteer-participation-api.ts
├── volunteer-member-api.ts
└── volunteer-admin-api.ts
```

예시:

```typescript
export async function applyVolunteerEvent(
  eventId: number,
): Promise<VolunteerParticipationResponse> {
  return apiClient.post(
    `/api/v1/volunteer-events/${eventId}/participations`,
  )
}
```

컴포넌트에서 직접 `fetch`를 반복 작성하지 않는다.

잘못된 구조:

```tsx
function Button() {
  fetch('https://server.dshelper.kr/...')
}
```

권장 구조:

```text
컴포넌트
→ Hook
→ API 함수
→ 공통 API Client
```

---

# 5-6. 데이터 조회 및 캐시

## 자주 변경되지 않는 데이터

```text
봉사단 소개
봉사 유형
지역 목록
```

필요한 경우 짧은 캐시를 사용할 수 있다.

## 실시간 정합성이 중요한 데이터

```text
현재 참여 인원
내 참여 상태
모집 상태
가입 신청 상태
출석 상태
```

이 데이터는 오래 캐시하면 안 된다.

참여 신청 또는 취소 성공 후 다음 데이터를 다시 조회한다.

```text
일정 상세
일정 목록
내 참여 예정 목록
내 봉사 요약
```

Next.js Server Component는 서버에서 API 데이터를 조회할 수 있고, Client Component에서는 TanStack Query나 SWR 같은 클라이언트 데이터 라이브러리를 사용할 수 있다.

---

# 5-7. 가입 신청 폼

## Next.js 할 일

1. 이름 입력
2. 연락처 입력
3. 생년월일 입력
4. 성별 선택
5. 거주 동네 입력
6. 희망 봉사활동 복수 선택
7. 지원동기 입력
8. 사진 선택
9. 사진 미리보기
10. 파일 크기 안내
11. 필수값 검사
12. 중복 제출 차단
13. 신청 완료 화면 이동

## 클라이언트 검증

```typescript
const volunteerApplicationSchema = z.object({
  name: z.string().trim().min(1).max(50),
  phone: z.string().trim().min(10).max(20),
  birthDate: z.string().min(1),
  gender: z.string().min(1),
  neighborhood: z.string().trim().min(1).max(100),
  preferredActivities: z.array(z.string()).min(1),
  motivation: z.string().trim().min(20).max(2000),
})
```

## 파일 사전 검사

```text
파일 존재 여부
확장자
브라우저 제공 MIME 타입
20MB 이하
이미지 미리보기 가능 여부
```

다만 실제 파일 유효성은 Spring Boot가 다시 검사한다.

## 전송 방식

```http
Content-Type: multipart/form-data
```

```text
application
→ JSON Blob

photo
→ File
```

---

# 5-8. 가입 상태 화면

## Next.js 할 일

서버가 반환한 상태에 따라 UI를 구분한다.

### PENDING

```text
승인 대기
수정하기 버튼
신청 취소 버튼
```

### APPROVED

```text
가입 승인
봉사 일정 보러가기 버튼
```

### REJECTED

```text
가입 반려
공개 반려 사유
재신청 버튼
```

### CANCELED

```text
신청 취소
다시 신청하기 버튼
```

버튼 노출은 서버가 반환한 `capabilities`를 기준으로 한다.

```json
{
  "status": "PENDING",
  "capabilities": {
    "canEdit": true,
    "canCancel": true,
    "canReapply": false
  }
}
```

---

# 5-9. 봉사 일정 목록

## Next.js 할 일

1. 상태별 필터
2. 일정 카드 표시
3. 이미지 표시
4. 날짜와 시간 표시
5. 모집 인원 표시
6. 현재 인원 표시
7. 모집 상태 배지
8. 내 참여 상태 표시
9. 참여하기 버튼
10. 취소하기 버튼
11. 참여자 팝업
12. 페이지네이션 또는 무한 스크롤
13. 빈 목록 화면
14. 로딩 스켈레톤

## 필터

```text
전체
모집중
모집마감
완료
```

URL 쿼리 파라미터로 상태를 유지한다.

```text
/volunteer/events?status=OPEN&page=0
```

새로고침하거나 링크를 공유해도 같은 필터가 유지된다.

---

# 5-10. 봉사 참여 신청

## Next.js 처리 흐름

```text
참여하기 클릭
→ 확인 모달
→ 버튼 로딩 처리
→ Spring Boot API 호출
→ 성공 토스트
→ 일정 데이터 재조회
```

확인 문구:

```text
송해공원 플로깅 봉사에 참여하시겠습니까?

일시: 2026년 7월 18일 10:00~12:00
장소: 송해공원 일대
```

## 오류 처리

### 정원 마감

```text
모집 인원이 마감되었습니다.
일정 정보를 새로고침합니다.
```

### 시간 중복

```text
같은 시간대에 신청한 봉사 일정이 있습니다.
```

### 활동정지

```text
현재 봉사 일정에 참여할 수 없는 상태입니다.
관리자에게 문의해 주세요.
```

### 중복 신청

```text
이미 신청한 봉사 일정입니다.
```

---

# 5-11. 봉사 참여 취소

## Next.js 처리 흐름

```text
취소하기 클릭
→ 취소 확인 모달
→ Spring Boot API 호출
→ 성공 시 화면 갱신
```

취소 확인 문구:

```text
봉사 참여를 취소하시겠습니까?

봉사 시작 2시간 전까지만 직접 취소할 수 있습니다.
```

2시간이 지나면 서버가 반환한 `canCancel=false`를 기준으로 버튼을 비활성화한다.

```text
취소 가능 시간이 지났습니다.
관리자에게 문의해 주세요.
```

브라우저 시간만으로 버튼 상태를 결정하지 않는다.

---

# 5-12. 내 봉사활동

## 탭

```text
참여 예정
참여 완료
```

## 참여 예정

```text
APPLIED 상태
봉사 시작 전
```

표시 항목:

```text
이미지
제목
일시
장소
취소 가능 여부
```

## 참여 완료

```text
ATTENDED 상태
```

표시 항목:

```text
이미지
제목
일시
장소
인정 참여 시간
```

## 요약 카드

```text
누적 참여 횟수
총 참여 시간
```

Next.js는 서버에서 계산한 값을 표시한다.

프론트엔드가 일정 배열을 받아 직접 누적 통계를 계산하지 않는다.

---

# 5-13. 관리자 가입 신청 화면

## 목록 화면

Next.js 할 일:

1. 이름 검색
2. 연락처 검색
3. 상태 필터
4. 신청일 필터
5. 페이지네이션
6. 상세 이동
7. 승인·반려 가능 여부 표시

## 상세 화면

표시 항목:

```text
이름
연락처
생년월일
성별
사는 동네
희망 활동
지원동기
본인 사진
신청 상태
신청일
처리일
공개 반려 사유
관리자 메모
```

## 승인

```text
승인 버튼 클릭
→ 확인 모달
→ 승인 API
→ 완료 메시지
→ 목록 또는 상세 정보 갱신
```

## 반려

반려 모달 입력값:

```text
사용자 공개용 반려 사유
관리자 내부 메모
```

두 값을 명확히 분리해서 표시한다.

---

# 5-14. 관리자 봉사단원 화면

## 목록

```text
이름
성별
연락처
가입일
활동 상태
누적 참여 횟수
상세보기
```

## 상세

```text
기본 정보
가입 신청 정보
참여 예정 일정
출석 내역
불참 내역
취소 내역
누적 통계
관리자 메모
상태 변경 이력
```

## 상태 변경

```text
활동중
활동정지
탈퇴
```

활동정지 또는 탈퇴 시 확인 모달에 영향도를 표시한다.

```text
이 회원은 참여 예정 봉사 2건이 있습니다.
활동정지 후 예정 봉사 처리 여부를 확인해 주세요.
```

실제 상태 변경 가능 여부는 Spring Boot가 판단한다.

---

# 5-15. 관리자 일정 등록 및 수정

## 일정 입력 폼

```text
봉사 이미지
봉사 제목
봉사 유형
시작 일시
종료 일시
모집 마감 일시
장소
모집 인원
활동 내용
준비물
유의사항
상태
공개 여부
```

## Next.js 사전 검증

```text
종료 시간 > 시작 시간
모집 마감 시간 <= 시작 시간
모집 인원 >= 1
필수 항목 입력
이미지 선택
```

Spring Boot가 동일 검증을 다시 수행한다.

## 일정 수정 주의 화면

시간이나 장소를 변경하는 경우:

```text
참여자에게 일정 변경 알림이 발송됩니다.
```

현재 참여 인원보다 모집 인원을 작게 입력한 경우 프론트에서 경고하되, Spring Boot가 최종 차단한다.

---

# 5-16. 관리자 출석 화면

## Next.js 할 일

1. 참여자 목록 표시
2. 전체 선택
3. 출석 선택
4. 불참 선택
5. 미처리 상태 표시
6. 일괄 저장
7. 변경 확인 모달
8. 저장 결과 표시

권장 UI:

| 이름  | 연락처           | 신청 상태 | 출석 결과   |
| --- | ------------- | ----- | ------- |
| 홍길동 | 010-****-1234 | 참여 신청 | 출석 / 불참 |

일괄 요청:

```json
{
  "attendedParticipationIds": [1, 2, 3],
  "absentParticipationIds": [4, 5]
}
```

화면에서 참석자 이름만 보내지 않는다.

서버가 발급한 participation ID를 사용한다.

---

# 5-17. Next.js 로딩·오류·빈 화면

## loading.tsx

다음 페이지에 로딩 화면을 제공한다.

```text
봉사 일정 목록
일정 상세
내 봉사활동
관리자 신청 목록
관리자 봉사단원 목록
관리자 일정 목록
```

Next.js의 `loading.tsx`는 해당 라우트 세그먼트에 즉시 로딩 UI를 제공할 수 있다.

## error.tsx

```text
데이터를 불러오지 못했습니다.
다시 시도해 주세요.
```

## 빈 화면

```text
등록된 봉사 일정이 없습니다.
참여 예정인 봉사가 없습니다.
아직 완료한 봉사가 없습니다.
승인 대기 신청자가 없습니다.
```

---

# 5-18. Next.js 보안

Next.js에서 다음 정보를 클라이언트 코드에 포함하지 않는다.

```text
스토리지 비밀키
관리자 내부 API 키
DB 접속 정보
JWT 서명키
원본 사진 저장 경로
```

`NEXT_PUBLIC_` 환경변수에는 브라우저에 공개돼도 되는 값만 넣는다.

```text
NEXT_PUBLIC_API_BASE_URL
```

관리자 메뉴를 숨기는 것만으로 보안을 구현하지 않는다.

```text
Next.js 관리자 메뉴 숨김
→ 사용자 편의

Spring Security 관리자 권한 검사
→ 실제 보안
```

---

# 5-19. Next.js 테스트

## 단위 테스트

```text
상태별 버튼 노출
오류 코드별 메시지
이름 마스킹 표시
날짜 표시
취소 마감 표시
Zod 폼 검증
```

## 컴포넌트 테스트

```text
가입 신청 폼 입력
사진 미리보기
일정 필터
참여 확인 모달
취소 확인 모달
출석 체크
```

## E2E 테스트

```text
로그인 → 가입 신청
가입 승인 후 일정 조회
일정 참여 신청
일정 참여 취소
관리자 일정 등록
관리자 출석 처리
사용자 완료 기록 확인
```

## Mock Service Worker

Spring Boot가 완성되지 않은 상태에서도 API 응답을 모킹하여 화면을 먼저 개발할 수 있다.

---

# 6. Spring Boot 전체 업무

# 6-1. Spring Boot 역할

Spring Boot는 다음 항목을 담당한다.

1. 인증 및 권한 검사
2. 가입 신청 데이터 저장
3. 가입 승인·반려
4. 봉사단원 생성 및 상태 관리
5. 일정 등록·수정·취소
6. 참여 신청·취소
7. 정원 검사
8. 동시성 제어
9. 시간 중복 검사
10. 출석·불참 처리
11. 누적 실적 계산
12. 파일 검증·저장
13. 일정 자동 완료
14. 모집 자동 마감
15. 알림 이벤트 생성
16. 관리자 처리 이력 저장
17. 개인정보 접근 제한

---

# 6-2. Spring Boot 패키지 구조

```text
server.dshelper.volunteer
├── application
│   ├── controller
│   ├── service
│   ├── domain
│   ├── repository
│   └── dto
│
├── member
│   ├── controller
│   ├── service
│   ├── domain
│   ├── repository
│   └── dto
│
├── event
│   ├── controller
│   ├── service
│   ├── domain
│   ├── repository
│   └── dto
│
├── participation
│   ├── controller
│   ├── service
│   ├── domain
│   ├── repository
│   └── dto
│
├── attendance
├── file
├── notification
├── scheduler
├── history
└── common
    ├── exception
    ├── security
    ├── validation
    └── time
```

---

# 6-3. 핵심 엔티티

## VolunteerApplication

```text
봉사단 가입 신청서
```

주요 필드:

```text
id
userId
name
phone
birthDate
gender
neighborhood
preferredActivities
motivation
photoFileId
status
rejectionReason
adminMemo
reviewedBy
reviewedAt
createdAt
updatedAt
version
```

## VolunteerMember

```text
승인된 봉사단원
```

주요 필드:

```text
id
userId
applicationId
status
joinedAt
suspendedAt
withdrawnAt
createdAt
updatedAt
version
```

## VolunteerEvent

```text
봉사 일정
```

주요 필드:

```text
id
title
type
imageFileId
startAt
endAt
recruitmentDeadlineAt
location
capacity
description
supplies
precautions
status
closeReason
visibility
cancelReason
createdBy
updatedBy
createdAt
updatedAt
version
```

## VolunteerParticipation

```text
봉사 일정 참여 신청
```

주요 필드:

```text
id
eventId
volunteerMemberId
status
appliedAt
canceledAt
cancelReason
attendanceCheckedAt
attendanceCheckedBy
createdAt
updatedAt
version
```

---

# 6-4. 상태 Enum

## 가입 신청

```java
public enum VolunteerApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELED
}
```

## 봉사단원

```java
public enum VolunteerMemberStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN
}
```

## 일정

```java
public enum VolunteerEventStatus {
    DRAFT,
    OPEN,
    CLOSED,
    COMPLETED,
    CANCELED
}
```

## 공개 여부

```java
public enum VolunteerEventVisibility {
    PUBLIC,
    PRIVATE
}
```

## 마감 사유

```java
public enum VolunteerEventCloseReason {
    CAPACITY,
    MANUAL,
    DEADLINE
}
```

## 참여

```java
public enum VolunteerParticipationStatus {
    APPLIED,
    CANCELED,
    ATTENDED,
    ABSENT
}
```

---

# 6-5. Spring Boot 상태 전이

## 가입 신청

```text
PENDING → APPROVED
PENDING → REJECTED
PENDING → CANCELED
```

## 봉사단원

```text
ACTIVE → SUSPENDED
ACTIVE → WITHDRAWN
SUSPENDED → ACTIVE
SUSPENDED → WITHDRAWN
```

## 일정

```text
DRAFT → OPEN
OPEN → CLOSED
OPEN → COMPLETED
OPEN → CANCELED
CLOSED → OPEN
CLOSED → COMPLETED
CLOSED → CANCELED
```

`CLOSED → OPEN`은 다음 경우만 허용한다.

```text
정원 마감으로 CLOSED
+
취소자가 발생
+
모집 마감 전
+
봉사 시작 전
```

## 참여

```text
APPLIED → CANCELED
APPLIED → ATTENDED
APPLIED → ABSENT
CANCELED → APPLIED
```

엔티티 전체 setter를 열지 않고 의미 있는 메서드를 제공한다.

```java
application.approve(adminId, now);
application.reject(adminId, reason, memo, now);

event.open(now);
event.closeByCapacity(now);
event.closeManually(adminId, now);
event.cancel(reason, adminId, now);

participation.cancelByUser(now);
participation.cancelByAdmin(reason, adminId, now);
participation.attend(adminId, now);
participation.markAbsent(adminId, now);
```

---

# 6-6. Spring Boot 가입 신청 구현

## 신청 생성

처리 순서:

```text
1. 로그인 사용자 확인
2. 기존 활동중 봉사단원 확인
3. PENDING 신청 존재 확인
4. 요청값 검증
5. 사진 검증
6. 사진 임시 저장
7. 신청서 저장
8. 알림 Outbox 저장
9. 트랜잭션 완료
```

검사 항목:

```text
활동중 봉사단원인지
승인 대기 신청이 이미 있는지
필수값이 입력되었는지
사진이 유효한지
```

## 신청 수정

```text
본인의 신청서인지
PENDING 상태인지
허용된 항목인지
```

## 신청 취소

```text
본인의 신청서인지
PENDING 상태인지
```

데이터를 삭제하지 않고 상태를 `CANCELED`로 변경한다.

---

# 6-7. Spring Boot 가입 승인

처리 순서:

```text
1. 관리자 권한 확인
2. 신청서 비관적 락 또는 낙관적 락 조회
3. PENDING 상태 확인
4. 기존 VolunteerMember 존재 여부 확인
5. 신청 상태 APPROVED
6. VolunteerMember 생성
7. 상태 변경 이력 저장
8. 승인 알림 Outbox 저장
```

`volunteer_member.user_id`에는 고유 제약조건을 적용한다.

```sql
UNIQUE (user_id)
```

관리자가 승인 버튼을 두 번 눌러도 봉사단원이 두 개 생성되면 안 된다.

---

# 6-8. Spring Boot 일정 등록

검증 항목:

```text
제목 필수
봉사 이미지 필수
종료 시간 > 시작 시간
모집 마감 시간 <= 시작 시간
모집 인원 >= 1
시작 전 일정만 OPEN 등록 가능
공개 상태와 일정 상태 조합 확인
```

시간은 서버 내부와 DB에서 `Instant` 또는 명확한 UTC 기준으로 관리한다.

화면 입력의 `Asia/Seoul` 시간을 서버에서 UTC로 변환한다.

---

# 6-9. Spring Boot 일정 수정

수정 규칙:

1. 완료된 일정은 일반 수정 불가
2. 취소된 일정은 수정 제한
3. 현재 참여 인원보다 모집 인원을 줄일 수 없음
4. 출석 처리 후 시간 변경 불가
5. 시간이나 장소 변경 시 참여자 알림 생성
6. 변경 전후 값을 감사 로그에 저장

현재 참여자가 8명인데 모집 인원을 5명으로 변경하려는 경우:

```text
VOLUNTEER_CAPACITY_BELOW_CURRENT_PARTICIPANTS
```

---

# 6-10. Spring Boot 참여 신청

## 최종 검사 항목

1. 로그인 사용자
2. `ACTIVE` 봉사단원
3. 공개 일정
4. `OPEN` 상태
5. 모집 마감 전
6. 봉사 시작 전
7. 동일 일정 중복 신청 없음
8. 시간 중복 없음
9. 정원 여유
10. 일정 자체가 취소되지 않음

## 처리 순서

```text
1. 봉사단원 행 락
2. 일정 행 락
3. 봉사단원 상태 확인
4. 일정 상태 확인
5. 시간 중복 검사
6. 현재 참여 인원 검사
7. 기존 참여 이력 확인
8. 신규 생성 또는 재신청 처리
9. 정원 도달 시 일정 마감
10. 알림 Outbox 저장
```

---

# 6-11. 동시 신청 처리

## 문제

정원 8명, 현재 7명일 때 두 명이 동시에 신청할 수 있다.

```text
A가 7명 조회
B가 7명 조회
A 저장
B 저장
결과 9명
```

## Spring Boot 해결 방식

### 일정 비관적 락

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    select e
    from VolunteerEvent e
    where e.id = :eventId
""")
Optional<VolunteerEvent> findByIdForUpdate(Long eventId);
```

Spring Data JPA는 repository 쿼리 메서드에 `@Lock`을 지정할 수 있다.

### 봉사단원 비관적 락

같은 사용자가 겹치는 두 일정에 동시에 신청하는 것을 방지한다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    select m
    from VolunteerMember m
    where m.id = :memberId
""")
Optional<VolunteerMember> findByIdForUpdate(Long memberId);
```

### 락 순서

```text
항상 봉사단원 락
→ 일정 락
```

모든 코드에서 락 순서를 통일한다.

### DB 고유 제약조건

```sql
UNIQUE (event_id, volunteer_member_id)
```

동일 사용자의 동일 일정 중복 신청을 DB에서도 차단한다.

---

# 6-12. 시간 중복 검사

SQL 기준:

```sql
SELECT EXISTS (
    SELECT 1
    FROM volunteer_participation p
    JOIN volunteer_event e
      ON e.id = p.event_id
    WHERE p.volunteer_member_id = :memberId
      AND p.status = 'APPLIED'
      AND e.start_at < :newEndAt
      AND e.end_at > :newStartAt
);
```

다음은 중복으로 보지 않는다.

```text
기존 일정 10:00~12:00
신규 일정 12:00~14:00
```

다음은 중복이다.

```text
기존 일정 10:00~12:00
신규 일정 11:30~13:00
```

---

# 6-13. Spring Boot 참여 취소

## 사용자 취소

```text
현재 시간 <= 시작 시간 - 2시간
```

검사 항목:

```text
본인의 참여 정보인지
현재 APPLIED 상태인지
일정이 시작되지 않았는지
취소 가능 시간이 지나지 않았는지
```

처리:

```text
APPLIED → CANCELED
canceledAt 기록
상태 변경 이력 저장
알림 생성
```

## 관리자 취소

관리자는 제한 시간 이후에도 처리할 수 있다.

```text
처리 사유 필수
관리자 ID 기록
사용자 취소와 구분
```

## 모집 재개

다음 조건을 모두 충족하면 일정 상태를 다시 `OPEN`으로 변경한다.

```text
현재 CLOSED
closeReason = CAPACITY
현재 참여 인원 < capacity
모집 마감 전
봉사 시작 전
```

---

# 6-14. Spring Boot 출석 처리

관리자는 일정 종료 후 다음 상태로 처리한다.

```text
APPLIED → ATTENDED
APPLIED → ABSENT
```

검증 항목:

```text
관리자 권한
해당 일정의 참여자인지
현재 상태가 APPLIED인지
일정이 종료되었는지
중복 대상 ID가 없는지
출석·불참 목록이 겹치지 않는지
```

일괄 처리 시 부분 실패가 발생하지 않도록 하나의 트랜잭션에서 처리한다.

---

# 6-15. 참여 기록 및 통계

## 참여 횟수

```sql
COUNT(*)
WHERE status = 'ATTENDED'
```

## 총 참여 시간

기본 정책:

```text
ATTENDED 일정의
endAt - startAt 합계
```

취소와 불참은 통계에서 제외한다.

```text
CANCELED 제외
ABSENT 제외
```

일정 자체가 취소된 경우도 참여 실적으로 계산하지 않는다.

---

# 6-16. 자동 처리

## 모집 마감

```text
현재 시간 >= recruitmentDeadlineAt
AND status = OPEN
→ CLOSED
→ closeReason = DEADLINE
```

## 일정 완료

```text
현재 시간 >= endAt
AND status IN (OPEN, CLOSED)
→ COMPLETED
```

## 중요한 제한

```text
일정 종료
→ 일정 상태 자동 완료 가능

일정 종료
→ 참여자 자동 출석 금지
```

출석은 관리자가 확인해야 한다.

## 스케줄러

```java
@Scheduled(cron = "0 * * * * *")
public void closeExpiredRecruitments() {
    volunteerEventBatchService.closeExpiredRecruitments();
}

@Scheduled(cron = "20 * * * * *")
public void completeFinishedEvents() {
    volunteerEventBatchService.completeFinishedEvents();
}
```

스케줄러는 여러 번 실행돼도 결과가 중복되지 않도록 조건부 UPDATE로 구현한다.

---

# 6-17. 사진 파일 처리

## Spring Boot 검증

1. 20MB 이하
2. 확장자 검사
3. MIME 타입 검사
4. 파일 시그니처 검사
5. 이미지 디코딩 검사
6. 해상도 제한
7. 메타데이터 제거
8. 안전한 파일명 생성
9. 원본 경로 비공개
10. 관리자 또는 본인만 조회

단순히 다음 값만 신뢰하지 않는다.

```java
file.getOriginalFilename()
file.getContentType()
```

## 저장 구조

```text
MySQL
→ 파일 메타데이터

Private Object Storage
→ 실제 파일
```

## 관리자 사진 조회

```http
GET /api/v1/admin/volunteer-applications/{id}/photo
```

검사:

```text
로그인
→ 봉사 관리자 권한
→ 신청서 존재
→ 파일 존재
→ 파일 응답
```

사용자 간 사진 조회 API는 제공하지 않는다.

---

# 6-18. 개인정보 처리

## 일반 사용자 참여자 목록 응답

```json
{
  "participants": [
    {
      "maskedName": "홍길*"
    }
  ]
}
```

원본 이름, 연락처, 사용자 ID를 반환하지 않는다.

## 관리자 응답

관리자 권한을 확인한 후 필요한 개인정보를 반환한다.

## 관리자 메모

```text
관리자 응답에만 포함
사용자 DTO에 필드 자체를 넣지 않음
```

단순히 프론트엔드에서 필드를 숨기는 방식으로 처리하지 않는다.

---

# 6-19. Spring Security

## 요청 권한

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        HttpMethod.GET,
        "/api/v1/volunteer/introduction"
    ).permitAll()

    .requestMatchers(
        "/api/v1/admin/volunteer/**"
    ).hasRole("VOLUNTEER_ADMIN")

    .requestMatchers(
        "/api/v1/volunteer/**",
        "/api/v1/volunteer-events/**"
    ).authenticated()

    .anyRequest().authenticated()
);
```

Spring Security는 `authorizeHttpRequests`를 통해 HTTP 요청 권한을 설정하며, 메서드 단위 권한은 `@EnableMethodSecurity`, `@PreAuthorize`로 적용할 수 있다.

## 서비스 메서드 권한

```java
@PreAuthorize("hasRole('VOLUNTEER_ADMIN')")
@Transactional
public void approveApplication(Long applicationId) {
}
```

## 활동중 봉사단원 검사

봉사단원 상태는 JWT 역할만으로 판단하지 않고 DB에서 확인한다.

```text
JWT 발급 후 사용자가 활동정지될 수 있기 때문
```

---

# 6-20. 알림 처리

## 알림 종류

```text
가입 신청 완료
가입 승인
가입 반려
참여 신청 완료
참여 취소 완료
새 일정 등록
일정 변경
일정 취소
봉사 사전 알림
출석 처리 완료
```

## Outbox 방식

```text
업무 데이터 저장
+
알림 Outbox 저장
→ 같은 DB 트랜잭션 완료
→ 별도 발송 처리
```

알림 발송 실패로 가입 승인이나 참여 신청이 롤백되면 안 된다.

## Outbox 필드

```text
id
eventKey
eventType
receiverId
aggregateId
payload
status
retryCount
nextRetryAt
createdAt
sentAt
```

`eventKey`는 중복 발송을 막기 위해 고유하게 만든다.

```text
VOLUNTEER_APPROVED:{applicationId}:{userId}
```

---

# 6-21. 관리자 감사 로그

다음 작업을 기록한다.

```text
가입 승인
가입 반려
활동정지
활동 재개
탈퇴 처리
일정 등록
일정 수정
수동 모집마감
일정 취소
출석 처리
불참 처리
관리자 강제 취소
```

로그 필드:

```text
targetType
targetId
action
previousValue
nextValue
changedBy
changeReason
createdAt
```

---

# 6-22. Spring Boot API 목록

## 사용자 API

```http
GET    /api/v1/volunteer/introduction
POST   /api/v1/volunteer-applications
GET    /api/v1/volunteer-applications/me/latest
PATCH  /api/v1/volunteer-applications/{id}
DELETE /api/v1/volunteer-applications/{id}

GET    /api/v1/volunteer-events
GET    /api/v1/volunteer-events/{eventId}
GET    /api/v1/volunteer-events/{eventId}/participants

POST   /api/v1/volunteer-events/{eventId}/participations
DELETE /api/v1/volunteer-events/{eventId}/participations/me

GET    /api/v1/volunteer-members/me/upcoming-events
GET    /api/v1/volunteer-members/me/completed-events
GET    /api/v1/volunteer-members/me/summary
```

## 관리자 API

```http
GET   /api/v1/admin/volunteer/applications
GET   /api/v1/admin/volunteer/applications/{id}
POST  /api/v1/admin/volunteer/applications/{id}/approve
POST  /api/v1/admin/volunteer/applications/{id}/reject
GET   /api/v1/admin/volunteer/applications/{id}/photo

GET   /api/v1/admin/volunteer/members
GET   /api/v1/admin/volunteer/members/{id}
POST  /api/v1/admin/volunteer/members/{id}/suspend
POST  /api/v1/admin/volunteer/members/{id}/activate
POST  /api/v1/admin/volunteer/members/{id}/withdraw

POST  /api/v1/admin/volunteer/events
GET   /api/v1/admin/volunteer/events
GET   /api/v1/admin/volunteer/events/{id}
PATCH /api/v1/admin/volunteer/events/{id}
POST  /api/v1/admin/volunteer/events/{id}/open
POST  /api/v1/admin/volunteer/events/{id}/close
POST  /api/v1/admin/volunteer/events/{id}/cancel

GET   /api/v1/admin/volunteer/events/{id}/participations
POST  /api/v1/admin/volunteer/events/{id}/attendance
```

---

# 7. 기능별 상세 업무 분리

## 7-1. 봉사단 소개

### Next.js

```text
소개 화면 구현
활동 분야 표시
활동 지역 표시
가입 조건 표시
가입하기 버튼
로그인 여부에 따른 이동
가입 상태별 버튼 문구
```

### Spring Boot

```text
가입 상태 조회 API
로그인 사용자 상태 반환
가입 가능 여부 계산
```

---

## 7-2. 가입 신청

### Next.js

```text
폼 UI
클라이언트 검증
사진 선택
사진 미리보기
FormData 전송
전송 중 버튼 차단
성공·실패 메시지
```

### Spring Boot

```text
로그인 검증
중복 신청 검사
요청 검증
사진 실제 검증
신청서 저장
알림 생성
```

---

## 7-3. 가입 승인·반려

### Next.js

```text
관리자 상세 화면
승인 확인 모달
반려 사유 폼
처리 결과 갱신
```

### Spring Boot

```text
관리자 권한
PENDING 상태 확인
중복 처리 차단
승인 시 봉사단원 생성
반려 사유 저장
상태 이력 저장
알림 생성
```

---

## 7-4. 봉사 일정 목록

### Next.js

```text
카드 UI
필터
페이지네이션
참여 상태 버튼
참여자 팝업
로딩·빈 화면
```

### Spring Boot

```text
접근 권한
필터 쿼리
현재 참여 인원
내 참여 상태
capabilities 계산
참여자 이름 마스킹
```

---

## 7-5. 참여 신청

### Next.js

```text
신청 확인 모달
API 호출
로딩 상태
오류 메시지
데이터 재조회
```

### Spring Boot

```text
봉사단원 상태 검사
일정 락
봉사단원 락
정원 검사
시간 중복 검사
중복 신청 검사
참여 저장
자동 마감
알림 생성
```

---

## 7-6. 참여 취소

### Next.js

```text
취소 확인 모달
canCancel에 따른 버튼
취소 결과 갱신
```

### Spring Boot

```text
본인 참여 확인
APPLIED 상태 확인
2시간 제한 확인
CANCELED 전환
정원 마감 재개 검사
이력·알림 저장
```

---

## 7-7. 내 봉사활동

### Next.js

```text
참여 예정 탭
참여 완료 탭
누적 통계 카드
일정 상세 이동
```

### Spring Boot

```text
APPLIED 예정 일정 조회
ATTENDED 완료 일정 조회
출석 횟수 계산
참여 시간 계산
```

---

## 7-8. 관리자 일정 관리

### Next.js

```text
일정 폼
이미지 선택
상태 변경 버튼
수정 확인 모달
취소 사유 입력
```

### Spring Boot

```text
관리자 권한
입력 검증
일정 저장
현재 인원보다 정원 축소 차단
변경 알림
취소 처리
감사 로그
```

---

## 7-9. 출석 관리

### Next.js

```text
참여자 목록
출석·불참 선택
일괄 저장
미처리 표시
```

### Spring Boot

```text
일정 종료 확인
참여 상태 확인
ATTENDED·ABSENT 전환
처리 관리자 기록
통계 반영
알림 생성
```

---

# 8. 프론트엔드와 백엔드 공유 타입

백엔드 Enum과 프론트엔드 타입의 이름을 일치시킨다.

```typescript
export type VolunteerApplicationStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELED'

export type VolunteerMemberStatus =
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'WITHDRAWN'

export type VolunteerEventStatus =
  | 'DRAFT'
  | 'OPEN'
  | 'CLOSED'
  | 'COMPLETED'
  | 'CANCELED'

export type VolunteerParticipationStatus =
  | 'APPLIED'
  | 'CANCELED'
  | 'ATTENDED'
  | 'ABSENT'
```

프론트엔드에 표시하는 한글 문구는 별도 매핑한다.

```typescript
export const EVENT_STATUS_LABEL = {
  DRAFT: '작성중',
  OPEN: '모집중',
  CLOSED: '모집마감',
  COMPLETED: '완료',
  CANCELED: '일정 취소',
} as const
```

API에서는 한글 상태값을 전달하지 않는다.

---

# 9. 주요 처리 흐름

## 9-1. 가입 신청

```text
Next.js
가입 폼 입력
→ FormData 전송

Spring Boot
로그인 확인
→ 중복 신청 검사
→ 사진 검사
→ 신청서 PENDING 저장
→ 알림 저장

Next.js
신청 완료 표시
→ 가입 상태 화면 이동
```

---

## 9-2. 가입 승인

```text
Next.js 관리자
승인 클릭

Spring Boot
관리자 권한 확인
→ 신청서 락
→ PENDING 확인
→ APPROVED 변경
→ VolunteerMember ACTIVE 생성
→ 알림 저장

Next.js
승인 완료 표시
```

---

## 9-3. 참여 신청

```text
Next.js
참여하기 클릭
→ 확인 모달
→ API 요청

Spring Boot
봉사단원 락
→ 일정 락
→ 활동 상태 확인
→ 시간 중복 확인
→ 정원 확인
→ APPLIED 저장
→ 정원 도달 시 CLOSED
→ 알림 저장

Next.js
일정 재조회
→ 버튼을 취소하기로 변경
```

---

## 9-4. 참여 취소

```text
Next.js
취소하기 클릭
→ API 요청

Spring Boot
참여 정보 확인
→ 시작 2시간 전인지 확인
→ CANCELED 처리
→ 모집 재개 가능 여부 확인
→ 알림 저장

Next.js
일정 재조회
→ 참여하기 버튼 표시
```

---

## 9-5. 출석 처리

```text
Next.js 관리자
출석·불참 선택
→ 일괄 저장

Spring Boot
일정 종료 확인
→ 참여 상태 검사
→ ATTENDED / ABSENT 처리
→ 이력 저장
→ 알림 저장

Next.js 사용자
내 봉사활동 조회
→ ATTENDED 일정 표시
```

---

# 10. 개발 순서

## 1단계: 공통 계약

### 함께 결정

```text
Enum
API 경로
요청 DTO
응답 DTO
오류 코드
날짜 형식
페이지네이션 형식
권한 이름
```

API 계약이 정해지기 전에 프론트와 백엔드가 각자 임의로 개발하지 않는다.

---

## 2단계: Spring Boot 핵심 도메인

```text
DB 테이블
Enum
엔티티
상태 전이
Repository
Flyway
예외 코드
```

---

## 3단계: Next.js 공통 UI

```text
페이지 라우팅
공통 API Client
폼 컴포넌트
모달
토스트
로딩
오류 화면
Mock API
```

---

## 4단계: 가입 신청

### Spring Boot

```text
가입 신청 API
사진 처리
관리자 승인·반려
```

### Next.js

```text
가입 신청 폼
가입 상태 화면
관리자 신청 관리
```

---

## 5단계: 봉사 일정

### Spring Boot

```text
일정 등록
일정 수정
목록·상세
취소
```

### Next.js

```text
사용자 일정 목록·상세
관리자 일정 등록·수정
```

---

## 6단계: 참여 신청

### Spring Boot

```text
동시성 락
정원
시간 중복
취소
```

### Next.js

```text
참여·취소 버튼
확인 모달
오류 처리
```

---

## 7단계: 출석과 통계

### Spring Boot

```text
출석·불참
통계 조회
```

### Next.js

```text
출석 관리 화면
내 봉사활동
```

---

## 8단계: 자동화와 알림

### Spring Boot

```text
자동 마감
자동 완료
Outbox
알림 재시도
```

### Next.js

```text
알림 표시
일정 변경 안내
```

---

# 11. 테스트 책임 분리

## Next.js 테스트 대상

```text
화면이 상태에 맞게 표시되는가
버튼이 capabilities에 맞게 표시되는가
폼 오류가 표시되는가
API 오류 메시지가 정확한가
사용자 흐름이 정상인가
```

## Spring Boot 테스트 대상

```text
상태 전이가 정확한가
권한이 정확한가
정원을 초과하지 않는가
시간 중복을 막는가
2시간 취소 제한이 정확한가
출석만 실적으로 계산하는가
개인정보 접근을 차단하는가
```

## 통합 테스트 대상

```text
Next.js 요청 DTO와 Spring Boot DTO가 일치하는가
Enum 값이 일치하는가
오류 코드가 일치하는가
인증 쿠키 또는 토큰이 정상 전달되는가
CORS 설정이 정상인가
파일 업로드가 정상인가
```

---

# 12. 절대 하지 말아야 할 구현

## Next.js에서 하면 안 되는 것

```text
현재 인원만 보고 최종 신청 가능 판단
브라우저 시간만 보고 최종 취소 판단
관리자 메뉴를 숨긴 것으로 권한 처리 완료
사용자 응답으로 관리자 메모까지 받은 후 화면에서만 숨김
프론트엔드에서 누적 참여 시간을 최종 계산
```

## Spring Boot에서 하면 안 되는 것

```text
일정 종료 후 전원 자동 출석
조회 후 락 없이 정원 신청 저장
사용자 사진을 공개 URL로 제공
승인된 신청서를 반려 상태로 되돌림
취소 데이터를 물리 삭제
클라이언트가 보낸 사용자 ID를 본인 ID로 신뢰
알림 실패로 가입 승인 전체 롤백
```

---

# 13. 완료 조건

## Next.js 완료 조건

* 모든 사용자·관리자 페이지가 구현되어 있다.
* 로딩·오류·빈 상태가 존재한다.
* 모든 폼에 사전 검증이 있다.
* 중복 클릭이 차단된다.
* 서버 오류 코드가 적절히 표시된다.
* 관리자 메모와 개인정보가 일반 화면에 포함되지 않는다.
* 모바일과 데스크톱에서 정상 동작한다.
* 핵심 사용자 흐름 E2E 테스트가 통과한다.

## Spring Boot 완료 조건

* 상태 전이가 서버에서 강제된다.
* 동일 일정 중복 신청이 DB에서 차단된다.
* 동시 신청에도 정원을 초과하지 않는다.
* 같은 시간대 일정 중복 참여가 차단된다.
* 시작 2시간 이내 사용자 취소가 차단된다.
* 관리자가 출석 처리한 건만 실적에 포함된다.
* 사진이 인증 없이 노출되지 않는다.
* 관리자 API에 권한 검사가 적용된다.
* 자동 마감과 자동 완료가 중복 실행에 안전하다.
* 알림 실패가 핵심 업무를 롤백하지 않는다.
* MySQL Testcontainers 동시성 테스트가 통과한다.

---

# 14. 최종 책임 요약

```text
Next.js
= 화면과 사용자 경험

Spring Boot
= 규칙과 데이터의 최종 책임
```

구체적으로 다음과 같이 구분한다.

```text
버튼을 보여주는 것
→ Next.js

버튼을 눌렀을 때 실제 처리를 허용할지 결정하는 것
→ Spring Boot
```

```text
현재 참여 인원을 표시하는 것
→ Next.js

참여 인원이 정원을 넘지 않게 보장하는 것
→ Spring Boot
```

```text
사진을 선택하고 미리보기 하는 것
→ Next.js

사진이 실제 이미지인지 검사하고 비공개 저장하는 것
→ Spring Boot
```

```text
출석 체크 화면을 제공하는 것
→ Next.js

출석 상태와 봉사 실적을 저장하는 것
→ Spring Boot
```

```text
취소 버튼을 비활성화하는 것
→ Next.js

취소 가능 시간이 지났는지 최종 판단하는 것
→ Spring Boot
```

이 책임 경계를 유지하면 화면과 서버에 업무 규칙이 중복되는 문제를 줄일 수 있고, 사용자가 직접 API를 호출하더라도 데이터와 권한을 안전하게 보호할 수 있다.
