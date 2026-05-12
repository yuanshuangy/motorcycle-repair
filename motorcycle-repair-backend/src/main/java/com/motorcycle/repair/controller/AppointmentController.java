package com.motorcycle.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.motorcycle.repair.annotation.AuditLog;
import com.motorcycle.repair.dto.AppointmentDTO;
import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.Appointment;
import com.motorcycle.repair.entity.RepairRecord;
import com.motorcycle.repair.filter.JwtAuthenticationFilter;
import com.motorcycle.repair.service.AppointmentService;
import com.motorcycle.repair.service.RepairRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AppointmentController.class);
    @Autowired private AppointmentService appointmentService;
    @Autowired private RepairRecordService repairRecordService;

    @GetMapping("/page")
    public Result<Page<AppointmentDTO>> getAppointmentPage(
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long shopId, @RequestParam(required = false) Integer status,
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal) {
        return Result.success(appointmentService.getAppointmentPage(pageNum, pageSize, principal.getUserId(), principal.getRole(), shopId, status));
    }

    @GetMapping("/{id}")
    public Result<AppointmentDTO> getById(@PathVariable Long id) { return Result.success(appointmentService.getByIdDTO(id)); }

    @PostMapping
    @AuditLog(operation = "创建预约", module = "预约管理")
    public Result<Void> createAppointment(@RequestBody Appointment appointment,
                                          @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal) {
        try {
            appointmentService.createAppointment(appointment, principal != null ? principal.getUserId() : null);
            return Result.success();
        } catch (Exception e) {
            log.error("创建预约失败: {}", e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage() : "预约失败，请稍后重试";
            return Result.error(400, msg);
        }
    }

    @PutMapping("/status/{id}")
    @AuditLog(operation = "更新订单状态", module = "预约管理")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        appointmentService.updateStatus(id, status); return Result.success();
    }

    @PutMapping("/assign/{id}")
    @AuditLog(operation = "派单分配技师", module = "预约管理")
    public Result<Void> assignEmployee(@PathVariable Long id, @RequestParam Long employeeId) {
        try { appointmentService.assignEmployee(id, employeeId); return Result.success(); }
        catch (Exception e) { return Result.error(400, e.getMessage()); }
    }

    @PutMapping("/cancel/{id}")
    @AuditLog(operation = "取消预约", module = "预约管理")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestParam(required = false) String cancelReason) {
        appointmentService.cancelAppointment(id, cancelReason); return Result.success();
    }

    @PutMapping("/complete/{id}")
    @AuditLog(operation = "完成维修", module = "预约管理")
    public Result<Void> complete(@PathVariable Long id) { appointmentService.completeAppointment(id); return Result.success(); }

    @PutMapping("/noshow/{id}")
    @AuditLog(operation = "标记爽约", module = "预约管理")
    public Result<Void> noshow(@PathVariable Long id) { appointmentService.noshowAppointment(id); return Result.success(); }

    @PutMapping("/pickup/{id}")
    public Result<Void> confirmPickup(@PathVariable Long id) { appointmentService.confirmPickup(id); return Result.success(); }

    @PutMapping("/pickup-failed/{id}")
    public Result<Void> pickupFailed(@PathVariable Long id) { appointmentService.pickupFailed(id); return Result.success(); }

    @PutMapping("/vehicle-arrived/{id}")
    public Result<Void> vehicleArrived(@PathVariable Long id) { appointmentService.vehicleArrived(id); return Result.success(); }

    @PutMapping("/pay/{id}")
    @AuditLog(operation = "支付订单", module = "预约管理")
    public Result<Void> pay(@PathVariable Long id, @RequestParam String payMethod) {
        appointmentService.payAppointment(id, payMethod); return Result.success();
    }

    @GetMapping("/recommend-tech")
    public Result<List<Map<String, Object>>> recommendTech(
            @RequestParam Long shopId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime appointmentTime) {
        return Result.success(appointmentService.recommendTechnicians(shopId, serviceId, appointmentTime));
    }

    @GetMapping("/tech-busy-info")
    public Result<List<Map<String, Object>>> getTechBusyInfo(@RequestParam Long shopId) {
        return Result.success(appointmentService.getTechBusyInfo(shopId));
    }

    @GetMapping("/check-conflict")
    public Result<Map<String, Object>> checkConflict(
            @RequestParam Long shopId,
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime appointmentTime,
            @RequestParam(required = false) Long serviceId) {
        boolean conflict = appointmentService.checkTimeConflict(shopId, employeeId, appointmentTime, serviceId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("hasConflict", conflict);
        result.put("message", conflict ? "该技师在预约时间段已有其他预约" : "该时间段可用");
        return Result.success(result);
    }

    @GetMapping("/record/{appointmentId}")
    public Result<RepairRecord> getRecord(@PathVariable Long appointmentId) {
        return Result.success(repairRecordService.getByAppointmentId(appointmentId));
    }

    @PostMapping("/record")
    public Result<Void> addRecord(@RequestBody RepairRecord record) {
        repairRecordService.addRecord(record); return Result.success();
    }

    @PutMapping("/record")
    public Result<Void> updateRecord(@RequestBody RepairRecord record) {
        repairRecordService.updateRecord(record); return Result.success();
    }

    @PutMapping("/confirm-price/{id}")
    @AuditLog(operation = "确认维修价格", module = "预约管理")
    public Result<Void> confirmPrice(@PathVariable Long id, @RequestParam Double finalPrice, @RequestParam(required = false) String priceRemark) {
        appointmentService.confirmPrice(id, finalPrice, priceRemark); return Result.success();
    }

    @GetMapping("/transfer-techs/{id}")
    public Result<List<Map<String, Object>>> getTransferTechs(@PathVariable Long id) {
        return Result.success(appointmentService.getTransferTechs(id));
    }

    @PutMapping("/transfer/{id}")
    @AuditLog(operation = "转单", module = "预约管理")
    public Result<Void> transferOrder(@PathVariable Long id, @RequestParam Long newTechId, @RequestParam(required = false) String transferReason) {
        try { appointmentService.transferOrder(id, newTechId, transferReason); return Result.success(); }
        catch (Exception e) { return Result.error(400, e.getMessage()); }
    }
}
