package com.motorcycle.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.motorcycle.repair.entity.AuditLogRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogRecord> {
}
