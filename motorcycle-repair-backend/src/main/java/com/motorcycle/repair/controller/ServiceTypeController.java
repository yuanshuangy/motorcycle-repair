package com.motorcycle.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.ServiceTemplate;
import com.motorcycle.repair.entity.ServiceType;
import com.motorcycle.repair.service.ServiceTemplateService;
import com.motorcycle.repair.service.ServiceTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceTypeController {
    @Autowired private ServiceTypeService serviceTypeService;
    @Autowired private ServiceTemplateService serviceTemplateService;

    @GetMapping("/page")
    public Result<Page<ServiceType>> getServicePage(
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long shopId) {
        return Result.success(serviceTypeService.getServicePage(pageNum, pageSize, shopId));
    }

    @GetMapping("/shop/{shopId}")
    public Result<List<ServiceType>> getServicesByShop(@PathVariable Long shopId) {
        return Result.success(serviceTypeService.getServicesByShop(shopId));
    }

    @GetMapping("/{id}")
    public Result<ServiceType> getServiceById(@PathVariable Long id) {
        ServiceType s = serviceTypeService.getById(id);
        return s != null ? Result.success(s) : Result.error("服务不存在");
    }

    @PostMapping
    public Result<Void> addService(@RequestBody ServiceType service) { serviceTypeService.addService(service); return Result.success(); }

    @PutMapping
    public Result<Void> updateService(@RequestBody ServiceType service) { serviceTypeService.updateService(service); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result<Void> deleteService(@PathVariable Long id) { serviceTypeService.deleteService(id); return Result.success(); }

    @GetMapping("/templates")
    public Result<List<ServiceTemplate>> getTemplates() { return Result.success(serviceTemplateService.getAllTemplates()); }

    @PostMapping("/initFromTemplate")
    public Result<Void> initFromTemplate(@RequestParam Long shopId) { serviceTemplateService.initShopServices(shopId); return Result.success(); }
}
