package com.motorcycle.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.entity.Announcement;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.service.AnnouncementService;
import com.motorcycle.repair.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired private UserService userService;
    @Autowired private AnnouncementService announcementService;
    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    private File uploadDir;

    @PostConstruct
    public void init() {
        uploadDir = new File(uploadPath).getAbsoluteFile();
        if (!uploadDir.exists()) uploadDir.mkdirs();
    }

    @GetMapping("/users/page")
    public Result<Page<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer role, @RequestParam(required = false) Integer status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (role != null) w.eq(User::getRole, role);
        if (status != null) w.eq(User::getStatus, status);
        w.orderByDesc(User::getCreateTime);
        return Result.success(userService.page(page, w));
    }

    @PutMapping("/users/status")
    public Result<Void> updateUserStatus(@RequestParam Long userId, @RequestParam Integer status) {
        User u = new User(); u.setId(userId); u.setStatus(status); userService.updateById(u);
        return Result.success();
    }

    @PutMapping("/users/role")
    public Result<Void> updateUserRole(@RequestParam Long userId, @RequestParam Integer role) {
        User u = new User(); u.setId(userId); u.setRole(role); userService.updateById(u);
        return Result.success();
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody User data) {
        User u = new User(); u.setId(id);
        if (data.getRealName() != null) u.setRealName(data.getRealName());
        if (data.getPhone() != null) u.setPhone(data.getPhone());
        if (data.getSkill() != null) u.setSkill(data.getSkill());
        if (data.getEmail() != null) u.setEmail(data.getEmail());
        if (data.getAvatar() != null) u.setAvatar(data.getAvatar());
        userService.updateById(u);
        return Result.success();
    }

    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件不能为空");
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File dest = new File(uploadDir, fileName);
        try {
            file.transferTo(dest.getAbsoluteFile());
            return Result.success("/uploads/" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/announcements/page")
    public Result<Page<Announcement>> getAnnouncementPage(
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(announcementService.getAnnouncementPage(pageNum, pageSize));
    }

    @PostMapping("/announcements")
    public Result<Void> addAnnouncement(@RequestBody Announcement a) { announcementService.addAnnouncement(a); return Result.success(); }

    @PutMapping("/announcements")
    public Result<Void> updateAnnouncement(@RequestBody Announcement a) { announcementService.updateAnnouncement(a); return Result.success(); }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) { announcementService.deleteAnnouncement(id); return Result.success(); }
}
