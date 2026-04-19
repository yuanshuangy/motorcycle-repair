package com.motorcycle.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("dict_type")
public class DictType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictType;
    private String dictName;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createTime;
}
