package com.motorcycle.repair.controller;

import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.DictData;
import com.motorcycle.repair.entity.TowPricing;
import com.motorcycle.repair.service.AmapDistanceService;
import com.motorcycle.repair.service.DictDataService;
import com.motorcycle.repair.service.SystemConfigService;
import com.motorcycle.repair.service.TowPricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    @Autowired
    private AmapDistanceService amapDistanceService;

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

    @GetMapping("/calculate-distance")
    public Result<Map<String, Object>> calculateDistance(
            @RequestParam String originAddress, @RequestParam String destAddress) {
        Map<String, Object> result = new HashMap<>();
        try {
            double distance = amapDistanceService.calculateDistance(originAddress, destAddress);
            result.put("distance", distance);
            if (distance > 0) {
                result.put("towFee", towPricingService.calculateTowFee(distance));
                result.put("source", "amap");
            } else {
                result.put("towFee", 0);
                result.put("source", "manual");
                result.put("message", "无法自动计算距离，请手动输入");
            }
        } catch (Exception e) {
            result.put("distance", -1);
            result.put("towFee", 0);
            result.put("source", "manual");
            result.put("message", "距离计算服务异常，请手动输入");
        }
        return Result.success(result);
    }

    @GetMapping("/calculate-distance-by-coords")
    public Result<Map<String, Object>> calculateDistanceByCoords(
            @RequestParam double originLng, @RequestParam double originLat,
            @RequestParam String destAddress,
            @RequestParam(defaultValue = "wgs84") String coordType) {
        Map<String, Object> result = new HashMap<>();
        try {
            double distance = amapDistanceService.calculateDistanceByCoords(originLng, originLat, destAddress, coordType);
            result.put("distance", distance);
            if (distance > 0) {
                result.put("towFee", towPricingService.calculateTowFee(distance));
                result.put("source", "amap");
            } else {
                result.put("towFee", 0);
                result.put("source", "manual");
                result.put("message", "无法自动计算距离");
            }
            String address = amapDistanceService.reverseGeocode(originLng, originLat, coordType);
            result.put("originAddress", address);
        } catch (Exception e) {
            result.put("distance", -1);
            result.put("towFee", 0);
            result.put("source", "manual");
            result.put("message", "距离计算服务异常");
        }
        return Result.success(result);
    }

    @GetMapping("/reverse-geocode")
    public Result<String> reverseGeocode(@RequestParam double lng, @RequestParam double lat,
                                          @RequestParam(defaultValue = "wgs84") String coordType) {
        return Result.success(amapDistanceService.reverseGeocode(lng, lat, coordType));
    }

    @GetMapping("/verify-amap-key")
    public Result<String> verifyAmapKey() {
        return Result.success(amapDistanceService.verifyApiKey());
    }
}
