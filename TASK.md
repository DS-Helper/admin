# Volunteer Domain Rebuild

| Item | Status | Verification |
|---|---|---|
| Remove legacy volunteer schedule domain | Completed | Legacy `tb_volunteer_schedule` references removed from source |
| Disable Admin volunteer schema migrations | Completed | No Admin Volunteer Flyway migration remains |
| Map V6 shared schema entities | Completed | V6 Entity, Enum, UTC converter, and Repository mappings added |
| Add state history and outbox services | Completed | Shared V6 history/outbox mapping and services added |
| Implement admin APIs | Completed | 공개 일정 이미지 업로드 API 포함 |
| Set JPA schema validation | Completed | `ddl-auto=validate` |
| Full build and related tests | In progress | `compileJava` 성공, 승인 상태 전이 단위 테스트 추가; 전체 테스트 대기 |
| Upload public event images | Completed | JPG/JPEG/PNG 20MB 검증, WebP 변환, 공개 VolunteerFile 저장, S3 롤백 테스트 통과 |
