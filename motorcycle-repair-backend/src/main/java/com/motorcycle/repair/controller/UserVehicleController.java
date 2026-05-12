package com.motorcycle.repair.controller;

import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.UserVehicle;
import com.motorcycle.repair.service.UserVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class UserVehicleController {

    @Autowired
    private UserVehicleService userVehicleService;

    @GetMapping("/user/{userId}")
    public Result<List<UserVehicle>> getByUser(@PathVariable Long userId) {
        return Result.success(userVehicleService.getByUserId(userId));
    }

    @PostMapping
    public Result<Void> add(@RequestBody UserVehicle vehicle) {
        userVehicleService.save(vehicle);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody UserVehicle vehicle) {
        userVehicleService.updateById(vehicle);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userVehicleService.removeById(id);
        return Result.success();
    }

    @PutMapping("/default/{userId}/{vehicleId}")
    public Result<Void> setDefault(@PathVariable Long userId, @PathVariable Long vehicleId) {
        userVehicleService.setDefault(userId, vehicleId);
        return Result.success();
    }
}
