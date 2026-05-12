package com.motorcycle.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repair_shop")
public class RepairShop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String shopName;
    private String address;
    private String phone;
    private String description;
    private String businessHours;
    private String license;
    private String coverImage;
    private Double rating;
    private Integer status;
    private Integer auditStatus;
    private String auditRemark;
    private Integer autoAssign;
    private Integer autoAcceptTech;
    private Integer autoConfirm;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
