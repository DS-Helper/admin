# 신고 및 검수 관리 시스템 설계서 (Report Management System)

본 문서는 기존 도메인 엔티티(`Board`, `Comment` 등)를 수정하지 않고, 별도의 `Report` 엔티티를 통해 소통방 관리 요구사항을 충족하기 위한 기술 설계서입니다.

## 1. 설계 원칙
- **비침투적 설계 (Non-intrusive Design)**: 기존 도메인 엔티티의 코드를 수정하거나 필드를 추가하지 않습니다.
- **관심사 분리 (Separation of Concerns)**: 비즈니스 로직(게시글 작성 등)과 운영 관리 로직(검수, 신고, 제재)을 분리합니다.
- **확장성**: 게시물, 댓글 외에 향후 추가될 수 있는 다른 도메인(예: 챌린지, 후기 등)에도 동일한 신고 시스템을 적용할 수 있도록 유연하게 설계합니다.

## 2. 데이터 모델 설계

### 2.1 Report 엔티티 (신고 및 검수 기록)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `id` | String (UUID) | 식별자 |
| `targetType` | Enum | 대상 유형 (`BOARD`, `COMMENT`) |
| `targetId` | String | 대상 엔티티의 식별자 ID |
| `inspectionStatus` | Enum | 검수 상태 (`PENDING`, `APPROVED`, `HIDDEN`) |
| `reportCount` | int | 누적 신고 횟수 |
| `adminMemo` | String (TEXT) | 관리자 전용 메모 |
| `reason` | String | 최근 신고 또는 제재 사유 |
| `sanctionType` | Enum | 제재 유형 (`WARNING`, `RESTRICTION`, `BAN`, `NONE`) |
| `targetUserId` | String | 대상 게시물/댓글의 작성자 ID (제재 관리용) |

### 2.2 Enum 정의
- **InspectionStatus**: `PENDING`(검토필요), `APPROVED`(검수완료), `HIDDEN`(숨김)
- **TargetType**: `BOARD`(게시물), `COMMENT`(댓글)
- **SanctionType**: `NONE`(없음), `WARNING`(경고), `RESTRICTION`(작성제한), `BAN`(계정제재)

## 3. 주요 동작 메커니즘

### 3.1 신규 콘텐츠 등록 (게시물/댓글)
1. 사용자가 게시물 또는 댓글을 등록합니다.
2. **이벤트 리스너(EntityListener)** 또는 **Service 계층**에서 `Report` 엔티티를 생성합니다.
   - `inspectionStatus`를 `PENDING`으로 설정하여 관리자 목록에 '검토필요'로 노출되게 합니다.

### 3.2 관리자 리스트 조회
1. `Board` 또는 `Comment` 테이블을 주 테이블로 하여 `Report` 테이블과 **Outer Join**을 수행합니다.
2. `Report` 정보가 없는 경우 기본값을 노출하거나, 생성 시점에 항상 `Report`를 생성하도록 보장합니다.
3. 검색 및 필터링(상태별, 신고수별)은 `Report` 테이블의 컬럼을 기준으로 수행합니다.

### 3.3 상태 변경 및 제재 액션
1. **검수 완료**: `Report`의 `inspectionStatus`를 `APPROVED`로 변경합니다.
2. **숨김 처리**: 
   - `Report`의 `inspectionStatus`를 `HIDDEN`으로 변경합니다.
   - 기존 엔티티(`Board` 또는 `Comment`)의 `is_deleted` 필드를 `true`로 업데이트하여 사용자 화면에서 제거합니다.
3. **복구 처리**:
   - `Report`의 `inspectionStatus`를 `APPROVED`로 변경합니다.
   - 기존 엔티티의 `is_deleted` 필드를 `false`로 업데이트합니다.

## 4. 기대 효과
- **데이터 무결성**: 기존 서비스의 DB 스키마 변경 없이 기능을 추가하므로 리스크가 매우 낮습니다.
- **성능**: 운영 관리 데이터가 별도 분리되어 있어, 일반 사용자용 조회 쿼리에 영향을 주지 않습니다.
- **유지보수**: 관리자 기능의 요구사항이 변경되어도 `Report` 엔티티만 수정하면 되므로 유지보수가 용이합니다.
