package com.project.ds_helper.domain.welfare.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_welfare_code")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareCodeEntity extends BaseTime {

    @Id
    @Column(name = "code_id", length = 50)
    private String codeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_group", nullable = false, length = 50)
    private WelfareCodeGroup codeGroup;

    @Column(name = "code_value", nullable = false, length = 10)
    private String codeValue;

    @Column(name = "code_name_ko", nullable = false, length = 100)
    private String codeNameKo;

    @Column(name = "normalized_name", length = 100)
    private String normalizedName;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}
