package com.motorcycle.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("tow_pricing")
public class TowPricing {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Double minDistance;
    private Double maxDistance;
    private BigDecimal price;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createTime;
}
