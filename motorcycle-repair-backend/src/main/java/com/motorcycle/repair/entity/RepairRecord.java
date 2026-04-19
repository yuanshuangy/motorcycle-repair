package com.motorcycle.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repair_record")
public class RepairRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long appointmentId;
    private Long shopId;
    private Long employeeId;
    private String diagnosis;
    private String repairItems;
    private String partsUsed;
    private Double partsCost;
    private Double laborCost;
    private Double totalCost;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
