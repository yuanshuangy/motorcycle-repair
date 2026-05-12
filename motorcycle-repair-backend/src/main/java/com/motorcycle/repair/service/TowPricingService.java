package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.TowPricing;
import com.motorcycle.repair.mapper.TowPricingMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TowPricingService extends ServiceImpl<TowPricingMapper, TowPricing> {

    public List<TowPricing> getActivePricings() {
        return this.list(new LambdaQueryWrapper<TowPricing>()
                .eq(TowPricing::getStatus, 1)
                .orderByAsc(TowPricing::getSortOrder));
    }

    public BigDecimal calculateTowFee(double distance) {
        List<TowPricing> pricings = getActivePricings();
        for (TowPricing p : pricings) {
            if (distance > p.getMinDistance() && distance <= p.getMaxDistance()) {
                return p.getPrice();
            }
        }
        if (!pricings.isEmpty()) return pricings.get(pricings.size() - 1).getPrice();
        return BigDecimal.ZERO;
    }
}
