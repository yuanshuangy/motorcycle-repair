package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.RepairShop;
import com.motorcycle.repair.mapper.RepairShopMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RepairShopService extends ServiceImpl<RepairShopMapper, RepairShop> {
    @Autowired private ServiceTemplateService serviceTemplateService;

    public Page<RepairShop> getShopPage(Integer pageNum, Integer pageSize, String shopName, Integer auditStatus) {
        Page<RepairShop> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairShop> w = new LambdaQueryWrapper<>();
        if (shopName != null && !shopName.isEmpty()) w.like(RepairShop::getShopName, shopName);
        if (auditStatus != null) w.eq(RepairShop::getAuditStatus, auditStatus);
        w.eq(RepairShop::getStatus, 1); w.orderByDesc(RepairShop::getRating);
        return this.page(page, w);
    }

    public List<RepairShop> getAllShops() {
        return this.list(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getStatus, 1).eq(RepairShop::getAuditStatus, 1));
    }

    public List<RepairShop> getApprovedShops() {
        return this.list(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getStatus, 1).eq(RepairShop::getAuditStatus, 1));
    }

    public RepairShop getShopByUserId(Long userId) {
        return this.getOne(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getUserId, userId));
    }

    public void addShop(RepairShop shop) { shop.setStatus(1); shop.setAuditStatus(0); this.save(shop); }
    public void updateShop(RepairShop shop) { this.updateById(shop); }
    public void deleteShop(Long id) { this.removeById(id); }

    public void auditShop(Long id, Integer auditStatus, String auditRemark) {
        RepairShop shop = this.getById(id);
        if (shop != null) {
            shop.setAuditStatus(auditStatus);
            shop.setAuditRemark(auditRemark);
            this.updateById(shop);
            if (auditStatus == 1) {
                serviceTemplateService.initShopServices(id);
            }
        }
    }

    public void updateRating(Long shopId, Double rating) {
        RepairShop shop = this.getById(shopId);
        if (shop != null) { shop.setRating(rating); this.updateById(shop); }
    }
}
