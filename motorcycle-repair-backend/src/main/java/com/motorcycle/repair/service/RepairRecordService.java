package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.RepairRecord;
import com.motorcycle.repair.mapper.RepairRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class RepairRecordService extends ServiceImpl<RepairRecordMapper, RepairRecord> {

    public RepairRecord getByAppointmentId(Long appointmentId) {
        return this.getOne(new LambdaQueryWrapper<RepairRecord>().eq(RepairRecord::getAppointmentId, appointmentId));
    }

    public void addRecord(RepairRecord r) { this.save(r); }
    public void updateRecord(RepairRecord r) { this.updateById(r); }
}
