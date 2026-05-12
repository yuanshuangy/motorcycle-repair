package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.AuditLogRecord;
import com.motorcycle.repair.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService extends ServiceImpl<AuditLogMapper, AuditLogRecord> {
}
