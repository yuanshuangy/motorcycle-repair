package com.motorcycle.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_vehicle")
public class UserVehicle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String plateNumber;
    private String brand;
    private String model;
    private String vinCode;
    private String color;
    private Integer purchaseYear;
    private Integer mileage;
    private Integer isDefault;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
