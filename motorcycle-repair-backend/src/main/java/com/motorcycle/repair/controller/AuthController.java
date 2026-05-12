package com.motorcycle.repair.controller;

import com.motorcycle.repair.dto.*;
import com.motorcycle.repair.entity.ChampionMessage;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.service.AppointmentService;
import com.motorcycle.repair.service.ChampionMessageService;
import com.motorcycle.repair.service.MessageService;
import com.motorcycle.repair.service.OssService;
import com.motorcycle.repair.service.StatisticsService;
import com.motorcycle.repair.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired private UserService userService;
    @Autowired private StatisticsService statisticsService;
    @Autowired private MessageService messageService;
    @Autowired private ChampionMessageService championMessageService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private OssService ossService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginDTO dto) {
        try { return Result.success(userService.login(dto)); }
        catch (Exception e) { return Result.error(401, e.getMessage()); }
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto) {
        try { userService.register(dto); return Result.success(); }
        catch (Exception e) { return Result.error(400, e.getMessage()); }
    }

    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.existsByUsername(username);
        return Result.success(exists);
    }

    @GetMapping("/next-tech-username")
    public Result<String> getNextTechUsername() {
        return Result.success(userService.getNextTechUsername());
    }

    @GetMapping("/profile")
    public Result<ProfileDTO> getProfile(@RequestParam Long userId) {
        ProfileDTO p = userService.getProfile(userId);
        return p != null ? Result.success(p) : Result.error("用户不存在");
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody User user) {
        userService.updateProfile(user); return Result.success();
    }

    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestParam Long userId, @RequestParam String avatar) {
        userService.updateAvatar(userId, avatar); return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordDTO dto) {
        try { userService.updatePassword(dto.getUserId(), dto.getOldPassword(), dto.getNewPassword()); return Result.success(); }
        catch (Exception e) { return Result.error(400, e.getMessage()); }
    }

    @PostMapping("/verify")
    public Result<Void> verifyUser(@RequestParam Long userId, @RequestParam String idCard) {
        userService.verifyUser(userId, idCard); return Result.success();
    }

    @GetMapping("/statistics")
    public Result<StatisticsDTO> getStatistics(@RequestParam Long userId, @RequestParam Integer role,
            @RequestParam(required = false) String rankPeriod) {
        StatisticsDTO s;
        if (role == 1) s = statisticsService.getAdminStatistics(rankPeriod);
        else if (role == 2) s = statisticsService.getShopStatistics(userId, rankPeriod);
        else if (role == 4) s = statisticsService.getTechStatistics(userId, rankPeriod);
        else s = statisticsService.getUserStatistics(userId);
        return Result.success(s);
    }

    @GetMapping("/messages")
    public Result<?> getMessages(@RequestParam Long userId, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(messageService.getMessagePage(userId, pageNum, pageSize));
    }

    @GetMapping("/messages/unread")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }

    @PutMapping("/messages/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id); return Result.success();
    }

    @PutMapping("/messages/readAll")
    public Result<Void> markAllAsRead(@RequestParam Long userId) {
        messageService.markAllAsRead(userId); return Result.success();
    }

    @PostMapping("/messages/send")
    public Result<Void> sendMessage(@RequestParam Long userId, @RequestParam Long senderId,
            @RequestParam Integer senderRole, @RequestParam String title, @RequestParam String content,
            @RequestParam(defaultValue = "0") Integer type) {
        messageService.sendMessage(userId, senderId, senderRole, title, content, type);
        return Result.success();
    }

    @PostMapping("/messages/sendToShopTechs")
    public Result<Void> sendToShopTechnicians(@RequestParam Long shopId, @RequestParam Long senderId,
            @RequestParam String title, @RequestParam String content,
            @RequestParam(defaultValue = "0") Integer type) {
        messageService.sendToShopTechnicians(shopId, senderId, title, content, type);
        return Result.success();
    }

    @GetMapping("/praise-template")
    public Result<String> generatePraiseTemplate(@RequestParam String type, @RequestParam String name, @RequestParam Integer rank, @RequestParam(defaultValue = "月度") String period) {
        String medal = rank == 1 ? "🥇冠军" : rank == 2 ? "🥈亚军" : rank == 3 ? "🥉季军" : "第" + rank + "名";
        String content;
        if ("revenue".equals(type)) {
            content = String.format("恭喜【%s】在%s维修店流水排行榜中荣获%s！您的优质服务和精湛技术赢得了客户的高度认可，为行业树立了标杆。感谢您的辛勤付出，期待您继续创造佳绩！", name, period, medal);
        } else {
            content = String.format("恭喜【%s】在%s接单排行榜中荣获%s！您的高效服务和专业精神深受客户信赖，是团队中的佼佼者。感谢您的努力付出，期待您再创辉煌！", name, period, medal);
        }
        return Result.success(content);
    }

    @PostMapping("/praise-send")
    public Result<Void> sendPraise(@RequestParam Long targetUserId, @RequestParam Long senderId,
            @RequestParam Integer senderRole,
            @RequestParam String type, @RequestParam String name, @RequestParam Integer rank,
            @RequestParam(defaultValue = "月度") String period,
            @RequestParam(required = false) String customContent) {
        String medal = rank == 1 ? "🥇冠军" : rank == 2 ? "🥈亚军" : rank == 3 ? "🥉季军" : "第" + rank + "名";
        String title = String.format("🏆%s排行榜表扬通知", period);
        String content;
        if (customContent != null && !customContent.trim().isEmpty()) {
            content = customContent;
        } else if ("revenue".equals(type)) {
            content = String.format("恭喜【%s】在%s维修店流水排行榜中荣获%s！您的优质服务和精湛技术赢得了客户的高度认可，为行业树立了标杆。感谢您的辛勤付出，期待您继续创造佳绩！", name, period, medal);
        } else {
            content = String.format("恭喜【%s】在%s接单排行榜中荣获%s！您的高效服务和专业精神深受客户信赖，是团队中的佼佼者。感谢您的努力付出，期待您再创辉煌！", name, period, medal);
        }
        messageService.sendMessage(targetUserId, senderId, senderRole, title, content, 4);
        return Result.success();
    }

    @GetMapping("/champion-message")
    public Result<ChampionMessage> getChampionMessage(@RequestParam Long userId, @RequestParam String championDate) {
        LocalDate date = LocalDate.parse(championDate);
        ChampionMessage cm = championMessageService.getByUserAndDate(userId, date);
        return cm != null ? Result.success(cm) : Result.success(null);
    }

    @PostMapping("/champion-message")
    public Result<ChampionMessage> saveChampionMessage(@RequestParam Long userId, @RequestParam String championDate, @RequestParam String message) {
        LocalDate date = LocalDate.parse(championDate);
        ChampionMessage cm = championMessageService.saveOrUpdateMessage(userId, date, message);
        return Result.success(cm);
    }

    @GetMapping("/overtime")
    public Result<String> getOvertime(@RequestParam Long userId) {
        java.time.YearMonth ym = java.time.YearMonth.now();
        java.time.LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        java.time.LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);
        java.util.List<com.motorcycle.repair.entity.Appointment> completed = appointmentService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.motorcycle.repair.entity.Appointment>()
                .eq(com.motorcycle.repair.entity.Appointment::getEmployeeId, userId)
                .eq(com.motorcycle.repair.entity.Appointment::getStatus, 3)
                .ge(com.motorcycle.repair.entity.Appointment::getCompleteTime, monthStart)
                .le(com.motorcycle.repair.entity.Appointment::getCompleteTime, monthEnd)
                .isNotNull(com.motorcycle.repair.entity.Appointment::getOvertimeMinutes)
                .gt(com.motorcycle.repair.entity.Appointment::getOvertimeMinutes, 0)
        );
        int totalMinutes = 0;
        for (com.motorcycle.repair.entity.Appointment a : completed) {
            if (a.getOvertimeMinutes() != null) totalMinutes += a.getOvertimeMinutes();
        }
        double hours = totalMinutes / 60.0;
        return Result.success(String.format("%.1f", hours));
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUserInfo(@PathVariable Long id, @RequestBody User data) {
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
        try {
            String url = ossService.uploadFile(file);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
