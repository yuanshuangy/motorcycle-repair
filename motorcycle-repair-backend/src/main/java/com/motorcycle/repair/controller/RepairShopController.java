package com.motorcycle.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.*;
import com.motorcycle.repair.mapper.AppointmentMapper;
import com.motorcycle.repair.mapper.ReviewMapper;
import com.motorcycle.repair.service.RepairShopService;
import com.motorcycle.repair.service.ShopTechnicianService;
import com.motorcycle.repair.service.TechRestScheduleService;
import com.motorcycle.repair.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shops")
public class RepairShopController {
    @Autowired private RepairShopService repairShopService;
    @Autowired private ShopTechnicianService shopTechnicianService;
    @Autowired private UserService userService;
    @Autowired private AppointmentMapper appointmentMapper;
    @Autowired private ReviewMapper reviewMapper;
    @Autowired private TechRestScheduleService techRestScheduleService;

    @GetMapping("/page")
    public Result<Page<RepairShop>> getShopPage(
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String shopName, @RequestParam(required = false) Integer auditStatus) {
        return Result.success(repairShopService.getShopPage(pageNum, pageSize, shopName, auditStatus));
    }

    @GetMapping("/list")
    public Result<List<RepairShop>> getAllShops() { return Result.success(repairShopService.getAllShops()); }

    @GetMapping("/approved")
    public Result<List<RepairShop>> getApprovedShops() { return Result.success(repairShopService.getApprovedShops()); }

    @GetMapping("/{id}")
    public Result<RepairShop> getShopById(@PathVariable Long id) {
        RepairShop s = repairShopService.getById(id);
        return s != null ? Result.success(s) : Result.error("维修店不存在");
    }

    @GetMapping("/user/{userId}")
    public Result<RepairShop> getShopByUserId(@PathVariable Long userId) {
        RepairShop s = repairShopService.getShopByUserId(userId);
        return s != null ? Result.success(s) : Result.error("未关联维修店");
    }

    @PostMapping
    public Result<Void> addShop(@RequestBody RepairShop shop) { repairShopService.addShop(shop); return Result.success(); }

    @PutMapping
    public Result<Void> updateShop(@RequestBody RepairShop shop) { repairShopService.updateShop(shop); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result<Void> deleteShop(@PathVariable Long id) { repairShopService.deleteShop(id); return Result.success(); }

    @PutMapping("/audit")
    public Result<Void> auditShop(@RequestParam Long id, @RequestParam Integer auditStatus, @RequestParam(required = false) String auditRemark) {
        repairShopService.auditShop(id, auditStatus, auditRemark); return Result.success();
    }

    @GetMapping("/{shopId}/technicians")
    public Result<List<User>> getShopTechnicians(@PathVariable Long shopId) {
        return Result.success(shopTechnicianService.getTechniciansByShop(shopId));
    }

    @PostMapping("/{shopId}/technicians")
    public Result<Void> bindTechnician(@PathVariable Long shopId, @RequestParam Long userId) {
        shopTechnicianService.bindTechnician(shopId, userId); return Result.success();
    }

    @DeleteMapping("/{shopId}/technicians")
    public Result<Void> unbindTechnician(@PathVariable Long shopId, @RequestParam Long userId) {
        shopTechnicianService.unbindTechnician(shopId, userId); return Result.success();
    }

    @PutMapping("/{shopId}/technicians/rest")
    public Result<Void> toggleTechRest(@PathVariable Long shopId, @RequestParam Long userId, @RequestParam Integer restStatus) {
        shopTechnicianService.toggleRestStatus(shopId, userId, restStatus); return Result.success();
    }

    @GetMapping("/technician/{userId}")
    public Result<List<ShopTechnician>> getTechnicianShops(@PathVariable Long userId) {
        return Result.success(shopTechnicianService.getShopsByTechnician(userId));
    }

    @GetMapping("/technicians/available")
    public Result<List<User>> getAvailableTechnicians() {
        return Result.success(userService.list(new LambdaQueryWrapper<User>().eq(User::getRole, 4).eq(User::getStatus, 1)));
    }

    @GetMapping("/{shopId}/technician-stats")
    public Result<List<Map<String, Object>>> getTechnicianStats(@PathVariable Long shopId) {
        List<User> techs = shopTechnicianService.getTechniciansByShop(shopId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User tech : techs) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("id", tech.getId());
            stat.put("realName", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            stat.put("phone", tech.getPhone());
            stat.put("avatar", tech.getAvatar());
            stat.put("skill", tech.getSkill() != null ? tech.getSkill() : "");
            ShopTechnician link = shopTechnicianService.getOne(new LambdaQueryWrapper<ShopTechnician>()
                    .eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getUserId, tech.getId()));
            stat.put("restStatus", link != null && link.getRestStatus() != null ? link.getRestStatus() : 0);
            stat.put("completedCount", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getEmployeeId, tech.getId()).eq(Appointment::getStatus, 3)));
            stat.put("processingCount", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getEmployeeId, tech.getId()).eq(Appointment::getStatus, 2)));
            stat.put("pendingCount", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getEmployeeId, tech.getId()).in(Appointment::getStatus, 0, 1)));
            stat.put("totalCount", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getEmployeeId, tech.getId())));
            long goodReviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getTechnicianId, tech.getId()).ge(Review::getRating, 4));
            long totalReviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getTechnicianId, tech.getId()));
            stat.put("goodReviewCount", goodReviewCount);
            stat.put("totalReviewCount", totalReviewCount);
            stat.put("avgRating", totalReviewCount > 0 ? String.format("%.1f", reviewMapper.selectList(new LambdaQueryWrapper<Review>().eq(Review::getTechnicianId, tech.getId())).stream().mapToInt(Review::getRating).average().orElse(0.0)) : "暂无");
            result.add(stat);
        }
        return Result.success(result);
    }

    @GetMapping("/{shopId}/rest-schedule")
    public Result<List<TechRestSchedule>> getRestSchedule(@PathVariable Long shopId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate nextWeekEnd = weekStart.plusDays(13);
        return Result.success(techRestScheduleService.getSchedule(shopId, today, nextWeekEnd));
    }

    @PostMapping("/{shopId}/rest-schedule/auto")
    public Result<List<TechRestSchedule>> autoRestSchedule(@PathVariable Long shopId) {
        return Result.success(techRestScheduleService.autoSchedule(shopId));
    }

    @DeleteMapping("/{shopId}/rest-schedule")
    public Result<Void> clearRestSchedule(@PathVariable Long shopId) {
        techRestScheduleService.clearScheduleAndResetStatus(shopId);
        return Result.success();
    }

    @GetMapping("/{shopId}/today-rest")
    public Result<List<Long>> getTodayRestTechIds(@PathVariable Long shopId) {
        LocalDate today = LocalDate.now();
        List<TechRestSchedule> schedules = techRestScheduleService.getSchedule(shopId, today, today);
        List<Long> restIds = new ArrayList<>();
        for (TechRestSchedule s : schedules) restIds.add(s.getUserId());
        return Result.success(restIds);
    }

    @GetMapping("/{shopId}/tomorrow-rest")
    public Result<List<Map<String, Object>>> getTomorrowRestTechs(@PathVariable Long shopId) {
        return Result.success(techRestScheduleService.getTomorrowRestTechs(shopId));
    }
}
