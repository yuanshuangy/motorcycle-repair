package com.motorcycle.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.Employee;
import com.motorcycle.repair.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    @Autowired private EmployeeService employeeService;

    @GetMapping("/page")
    public Result<Page<Employee>> getEmployeePage(
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long shopId) {
        return Result.success(employeeService.getEmployeePage(pageNum, pageSize, shopId));
    }

    @GetMapping("/shop/{shopId}")
    public Result<List<Employee>> getEmployeesByShop(@PathVariable Long shopId) {
        return Result.success(employeeService.getEmployeesByShop(shopId));
    }

    @PostMapping
    public Result<Void> addEmployee(@RequestBody Employee employee) { employeeService.addEmployee(employee); return Result.success(); }

    @PostMapping("/with-account")
    public Result<Map<String, Object>> addEmployeeWithAccount(@RequestBody Map<String, Object> body) {
        try { return Result.success(employeeService.addEmployeeWithAccount(body)); }
        catch (Exception e) { return Result.error(400, e.getMessage()); }
    }

    @PutMapping
    public Result<Void> updateEmployee(@RequestBody Employee employee) { employeeService.updateEmployee(employee); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result<Void> deleteEmployee(@PathVariable Long id) { employeeService.deleteEmployee(id); return Result.success(); }
}
