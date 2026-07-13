# Volunteer Domain Rebuild

| Item | Status | Verification |
|---|---|---|
| Remove legacy volunteer schedule domain | Completed | Legacy `tb_volunteer_schedule` references removed from source |
| Disable Admin volunteer schema migrations | Completed | No Admin Volunteer Flyway migration remains |
| Map V6 shared schema entities | Completed | V6 Entity, Enum, UTC converter, and Repository mappings added |
| Add state history and outbox services | Completed | Shared V6 history/outbox mapping and services added |
| Implement admin APIs | In progress | 모든 명세 경로 추가 완료, 신청 사진·일정 이미지는 presigned URL 반환 |
| Set JPA schema validation | Completed | `ddl-auto=validate` |
| Full build and related tests | In progress | `compileJava` 성공, 승인 상태 전이 단위 테스트 추가; 전체 테스트 대기 |
