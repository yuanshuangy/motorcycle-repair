package com.motorcycle.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public Result<Void> createAppointment(@RequestBody Appointment appointment) {
        try { appointmentService.createAppointment(appointment); return Result.success(); }
        catch (Exception e) { return Result.error(400, e.getMessage()); }
    }

    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        appointmentService.updateStatus(id, status); return Result.success();
    }

    @PutMapping("/assign/{id}")
    public Result<Void> assignEmployee(@PathVariable Long id, @RequestParam Long employeeId) {
        try { appointmentService.assignEmployee(id, employeeId); return Result.success(); }
        catch (Exception e) { return Result.error(400, e.getMessage()); }
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) { appointmentService.cancelAppointment(id); return Result.success(); }

    @PutMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id) { appointmentService.completeAppointment(id); return Result.success(); }

    @PutMapping("/noshow/{id}")
    public Result<Void> noshow(@PathVariable Long id) { appointmentService.noshowAppointment(id); return Result.success(); }

    @PutMapping("/pickup/{id}")
    public Result<Void> confirmPickup(@PathVariable Long id) { appointmentService.confirmPickup(id); return Result.success(); }

    @PutMapping("/pickup-failed/{id}")
    public Result<Void> pickupFailed(@PathVariable Long id) { appointmentService.pickupFailed(id); return Result.success(); }

    @PutMapping("/vehicle-arrived/{id}")
    public Result<Void> vehicleArrived(@PathVariable Long id) { appointmentService.vehicleArrived(id); return Result.success(); }

    @PutMapping("/pay/{id}")
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
}
