package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.ShopTechnician;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.mapper.ShopTechnicianMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShopTechnicianService extends ServiceImpl<ShopTechnicianMapper, ShopTechnician> {
    @Autowired private UserService userService;

    public List<User> getTechniciansByShop(Long shopId) {
        List<ShopTechnician> links = this.list(new LambdaQueryWrapper<ShopTechnician>()
                .eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getStatus, 1));
        List<User> techs = new ArrayList<>();
        for (ShopTechnician link : links) {
            User u = userService.getById(link.getUserId());
            if (u != null && u.getRole() == 4) {
                techs.add(u);
            }
        }
        return techs;
    }

    public List<ShopTechnician> getShopsByTechnician(Long userId) {
        return this.list(new LambdaQueryWrapper<ShopTechnician>()
                .eq(ShopTechnician::getUserId, userId).eq(ShopTechnician::getStatus, 1));
    }

    public void bindTechnician(Long shopId, Long userId) {
        ShopTechnician existing = this.getOne(new LambdaQueryWrapper<ShopTechnician>()
                .eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getUserId, userId));
        if (existing != null) {
            existing.setStatus(1);
            this.updateById(existing);
        } else {
            ShopTechnician st = new ShopTechnician();
            st.setShopId(shopId);
            st.setUserId(userId);
            st.setStatus(1);
            st.setRestStatus(0);
            this.save(st);
        }
    }

    public void unbindTechnician(Long shopId, Long userId) {
        ShopTechnician existing = this.getOne(new LambdaQueryWrapper<ShopTechnician>()
                .eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getUserId, userId));
        if (existing != null) {
            existing.setStatus(0);
            this.updateById(existing);
        }
    }

    public void toggleRestStatus(Long shopId, Long userId, Integer restStatus) {
        ShopTechnician existing = this.getOne(new LambdaQueryWrapper<ShopTechnician>()
                .eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getUserId, userId));
        if (existing != null) {
            existing.setRestStatus(restStatus);
            this.updateById(existing);
        }
    }

    public List<User> getActiveTechniciansByShop(Long shopId) {
        List<ShopTechnician> links = this.list(new LambdaQueryWrapper<ShopTechnician>()
                .eq(ShopTechnician::getShopId, shopId)
                .eq(ShopTechnician::getStatus, 1)
                .eq(ShopTechnician::getRestStatus, 0));
        List<User> techs = new ArrayList<>();
        for (ShopTechnician link : links) {
            User u = userService.getById(link.getUserId());
            if (u != null && u.getRole() == 4) {
                techs.add(u);
            }
        }
        return techs;
    }
}
