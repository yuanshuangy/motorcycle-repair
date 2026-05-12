package com.motorcycle.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("service_template")
public class ServiceTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceName;
    private BigDecimal price;
    private Integer duration;
    private String description;
    private String category;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
