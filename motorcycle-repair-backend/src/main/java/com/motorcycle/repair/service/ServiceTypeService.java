package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.ServiceType;
import com.motorcycle.repair.mapper.ServiceTypeMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServiceTypeService extends ServiceImpl<ServiceTypeMapper, ServiceType> {

    public Page<ServiceType> getServicePage(Integer pageNum, Integer pageSize, Long shopId) {
        Page<ServiceType> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceType> w = new LambdaQueryWrapper<>();
        if (shopId != null) w.eq(ServiceType::getShopId, shopId);
        w.eq(ServiceType::getStatus, 1); w.orderByDesc(ServiceType::getCreateTime);
        return this.page(page, w);
    }

    public List<ServiceType> getServicesByShop(Long shopId) {
        return this.list(new LambdaQueryWrapper<ServiceType>().eq(ServiceType::getShopId, shopId).eq(ServiceType::getStatus, 1));
    }

    public void addService(ServiceType s) {
        long dup = this.count(new LambdaQueryWrapper<ServiceType>()
                .eq(ServiceType::getShopId, s.getShopId())
                .eq(ServiceType::getServiceName, s.getServiceName())
                .eq(ServiceType::getStatus, 1));
        if (dup > 0) throw new RuntimeException("该店铺已存在同名服务「" + s.getServiceName() + "」");
        s.setStatus(1);
        this.save(s);
    }
    public void updateService(ServiceType s) { this.updateById(s); }
    public void deleteService(Long id) { this.removeById(id); }
}
