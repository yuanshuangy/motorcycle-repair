package com.motorcycle.repair.controller;

import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.DictData;
import com.motorcycle.repair.entity.TowPricing;
import com.motorcycle.repair.service.DictDataService;
import com.motorcycle.repair.service.SystemConfigService;
import com.motorcycle.repair.service.TowPricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/config")
public class ConfigController {

    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private DictDataService dictDataService;
    @Autowired
    private TowPricingService towPricingService;

    @GetMapping("/system")
    public Result<Map<String, String>> getSystemConfigs() {
        return Result.success(systemConfigService.getAllConfigs());
    }

    @GetMapping("/dict/{dictType}")
    public Result<List<DictData>> getDictData(@PathVariable String dictType) {
        return Result.success(dictDataService.getByType(dictType));
    }

    @GetMapping("/tow-pricing")
    public Result<List<TowPricing>> getTowPricing() {
        return Result.success(towPricingService.getActivePricings());
    }

    @GetMapping("/tow-fee")
    public Result<java.math.BigDecimal> calculateTowFee(@RequestParam double distance) {
        return Result.success(towPricingService.calculateTowFee(distance));
    }
}
