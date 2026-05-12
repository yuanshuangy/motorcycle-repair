package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.UserVehicle;
import com.motorcycle.repair.mapper.UserVehicleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserVehicleService extends ServiceImpl<UserVehicleMapper, UserVehicle> {

    public List<UserVehicle> getByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<UserVehicle>()
                .eq(UserVehicle::getUserId, userId)
                .orderByDesc(UserVehicle::getIsDefault)
                .orderByDesc(UserVehicle::getCreateTime));
    }

    public void setDefault(Long userId, Long vehicleId) {
        List<UserVehicle> all = this.list(new LambdaQueryWrapper<UserVehicle>()
                .eq(UserVehicle::getUserId, userId));
        for (UserVehicle v : all) {
            v.setIsDefault(v.getId().equals(vehicleId) ? 1 : 0);
            this.updateById(v);
        }
    }
}
