-- Welfare service code normalization
-- Execute once after backing up the database.

START TRANSACTION;

UPDATE tb_welfare_service
SET life_cycle_array = CASE
    WHEN life_cycle_array IN ('영유아') THEN '001'
    WHEN life_cycle_array IN ('아동') THEN '002'
    WHEN life_cycle_array IN ('청소년') THEN '003'
    WHEN life_cycle_array IN ('청년') THEN '004'
    WHEN life_cycle_array IN ('중장년') THEN '005'
    WHEN life_cycle_array IN ('노년') THEN '006'
    WHEN life_cycle_array IN ('임신·출산', '임신출산') THEN '007'
    ELSE life_cycle_array
END
WHERE life_cycle_array IS NOT NULL;

UPDATE tb_welfare_service
SET target_audience_array = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(target_audience_array, '다문화·탈북민', '010'),
                    '다문화탈북민', '010'),
                '다자녀', '020'),
            '보훈대상자', '030'),
        '장애인', '040'),
    '저소득', '050'),
 '한부모·조손', '060')
WHERE target_audience_array IS NOT NULL;

UPDATE tb_welfare_service
SET interest_theme_array = CASE
    WHEN interest_theme_array IN ('신체건강') THEN '010'
    WHEN interest_theme_array IN ('정신건강') THEN '020'
    WHEN interest_theme_array IN ('생활지원') THEN '030'
    WHEN interest_theme_array IN ('주거') THEN '040'
    WHEN interest_theme_array IN ('일자리') THEN '050'
    WHEN interest_theme_array IN ('문화·여가', '문화여가') THEN '060'
    WHEN interest_theme_array IN ('안전·위기', '안전위기') THEN '070'
    WHEN interest_theme_array IN ('임신·출산', '임신출산') THEN '080'
    WHEN interest_theme_array IN ('보육') THEN '090'
    WHEN interest_theme_array IN ('교육') THEN '100'
    WHEN interest_theme_array IN ('입양·위탁', '입양위탁') THEN '110'
    WHEN interest_theme_array IN ('보호·돌봄', '보호돌봄') THEN '120'
    WHEN interest_theme_array IN ('서민금융') THEN '130'
    WHEN interest_theme_array IN ('법률') THEN '140'
    ELSE interest_theme_array
END
WHERE interest_theme_array IS NOT NULL;

UPDATE tb_welfare_service
SET interest_theme_array = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        REPLACE(
                                            REPLACE(
                                                REPLACE(
                                                    REPLACE(interest_theme_array, '신체건강', '010'),
                                                '정신건강', '020'),
                                            '생활지원', '030'),
                                        '주거', '040'),
                                    '일자리', '050'),
                                '문화·여가', '060'),
                            '문화여가', '060'),
                        '안전·위기', '070'),
                    '안전위기', '070'),
                '임신·출산', '080'),
            '임신출산', '080'),
        '보육', '090'),
    '교육', '100'),
'입양·위탁', '110')
WHERE interest_theme_array IS NOT NULL;

UPDATE tb_welfare_service
SET interest_theme_array = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(
                                    REPLACE(
                                        REPLACE(
                                            REPLACE(
                                                REPLACE(
                                                    REPLACE(
                                                        REPLACE(
                                                            REPLACE(
                                                                REPLACE(interest_theme_array, '신체건강', '010'),
                                                            '정신건강', '020'),
                                                        '생활지원', '030'),
                                                    '주거', '040'),
                                                '일자리', '050'),
                                            '문화·여가', '060'),
                                        '문화여가', '060'),
                                    '안전·위기', '070'),
                                '안전위기', '070'),
                            '임신·출산', '080'),
                        '임신출산', '080'),
                    '보육', '090'),
                '교육', '100'),
            '입양·위탁', '110'),
        '입양위탁', '110'),
    '보호·돌봄', '120'),
'보호돌봄', '120')
WHERE interest_theme_array IS NOT NULL;

UPDATE tb_welfare_service
SET interest_theme_array = REPLACE(interest_theme_array, '서민금융', '130')
WHERE interest_theme_array LIKE '%서민금융%';

SELECT COUNT(*) AS remaining_energy_rows
FROM tb_welfare_service
WHERE is_active = 1
  AND interest_theme_array LIKE '%에너지%';

COMMIT;
