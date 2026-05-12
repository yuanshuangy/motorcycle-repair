package com.motorcycle.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLogRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String operation;
    private Long operatorId;
    private String operatorName;
    private String operatorIp;
    private String method;
    private String args;
    private String result;
    private Long durationMs;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
