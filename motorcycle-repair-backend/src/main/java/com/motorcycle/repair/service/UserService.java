package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.dto.*;
import com.motorcycle.repair.entity.DictData;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.mapper.UserMapper;
import com.motorcycle.repair.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{6,20}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^.{2,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public LoginResponse login(LoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword()))
            throw new RuntimeException("用户名或密码错误");
        if (user.getStatus() != 1) throw new RuntimeException("账号已被禁用");
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginResponse resp = new LoginResponse();
        resp.setToken(token); resp.setId(user.getId()); resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName()); resp.setRole(user.getRole());
        resp.setAvatar(user.getAvatar()); resp.setRoleName(getRoleName(user.getRole()));
        return resp;
    }

    public List<String> validateRegister(RegisterDTO dto) {
        List<String> errors = new ArrayList<>();
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            errors.add("用户名不能为空");
        } else if (!USERNAME_PATTERN.matcher(dto.getUsername()).matches()) {
            errors.add("用户名需4-20位，只能包含字母、数字和下划线");
        } else if (this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) > 0) {
            errors.add("用户名已存在");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            errors.add("密码不能为空");
        } else if (!PASSWORD_PATTERN.matcher(dto.getPassword()).matches()) {
            errors.add("密码需6-20位");
        }
        if (dto.getRealName() == null || dto.getRealName().trim().isEmpty()) {
            errors.add("姓名不能为空");
        } else if (!NAME_PATTERN.matcher(dto.getRealName()).matches()) {
            errors.add("姓名需2-20个字符");
        }
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            errors.add("手机号不能为空");
        } else if (!PHONE_PATTERN.matcher(dto.getPhone()).matches()) {
            errors.add("手机号格式不正确");
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
                errors.add("邮箱格式不正确");
            }
        }
        return errors;
    }

    public void register(RegisterDTO dto) {
        List<String> errors = validateRegister(dto);
        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join("；", errors));
        }
        User u = new User();
        u.setUsername(dto.getUsername()); u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setRealName(dto.getRealName()); u.setPhone(dto.getPhone()); u.setEmail(dto.getEmail());
        u.setRole(3); u.setStatus(1); u.setVerified(0);
        this.save(u);
    }

    public boolean existsByUsername(String username) {
        return this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    public String getNextTechUsername() {
        for (int i = 9; i <= 999; i++) {
            String username = "zhang" + String.format("%03d", i);
            if (!existsByUsername(username)) return username;
        }
        return "zhang999";
    }

    public ProfileDTO getProfile(Long userId) {
        User u = this.getById(userId);
        if (u == null) return null;
        ProfileDTO p = new ProfileDTO();
        p.setId(u.getId()); p.setUsername(u.getUsername()); p.setRealName(u.getRealName());
        p.setPhone(u.getPhone()); p.setEmail(u.getEmail()); p.setIdCard(u.getIdCard());
        p.setAvatar(u.getAvatar()); p.setRole(u.getRole()); p.setRoleName(getRoleName(u.getRole()));
        p.setVerified(u.getVerified());
        return p;
    }

    public void updateProfile(User user) { this.updateById(user); }

    public void updateAvatar(Long userId, String avatar) {
        User u = new User(); u.setId(userId); u.setAvatar(avatar); this.updateById(u);
    }

    public void updatePassword(Long userId, String oldPwd, String newPwd) {
        User u = this.getById(userId);
        if (u == null) throw new RuntimeException("用户不存在");
        if (!passwordEncoder.matches(oldPwd, u.getPassword())) throw new RuntimeException("原密码错误");
        u.setPassword(passwordEncoder.encode(newPwd));
        this.updateById(u);
    }

    public void verifyUser(Long userId, String idCard) {
        User u = this.getById(userId);
        if (u != null) { u.setIdCard(idCard); u.setVerified(1); this.updateById(u); }
    }

    @Autowired
    private DictDataService dictDataService;

    public String getRoleName(Integer role) {
        if (role == null) return "未知";
        List<DictData> list = dictDataService.getByType("user_role");
        for (DictData d : list) { if (d.getDictValue().equals(String.valueOf(role))) return d.getDictLabel(); }
        return "未知";
    }
}
