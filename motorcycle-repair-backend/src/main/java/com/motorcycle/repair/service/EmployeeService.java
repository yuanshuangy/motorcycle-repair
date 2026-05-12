package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.Employee;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService extends ServiceImpl<EmployeeMapper, Employee> {
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    public Page<Employee> getEmployeePage(Integer pageNum, Integer pageSize, Long shopId) {
        Page<Employee> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Employee> w = new LambdaQueryWrapper<>();
        if (shopId != null) w.eq(Employee::getShopId, shopId);
        w.eq(Employee::getStatus, 1); w.orderByDesc(Employee::getCreateTime);
        return this.page(page, w);
    }

    public List<Employee> getEmployeesByShop(Long shopId) {
        return this.list(new LambdaQueryWrapper<Employee>().eq(Employee::getShopId, shopId).eq(Employee::getStatus, 1));
    }

    public void addEmployee(Employee e) { e.setStatus(1); this.save(e); }
    public void updateEmployee(Employee e) { this.updateById(e); }
    public void deleteEmployee(Long id) { this.removeById(id); }

    public Map<String, Object> addEmployeeWithAccount(Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String employeeName = (String) body.get("employeeName");
        String phone = body.get("phone") != null ? (String) body.get("phone") : "";
        String position = body.get("position") != null ? (String) body.get("position") : "";
        String skill = body.get("skill") != null ? (String) body.get("skill") : "";
        Long shopId = body.get("shopId") != null ? Long.valueOf(body.get("shopId").toString()) : null;

        if (username == null || username.isEmpty()) throw new RuntimeException("请填写账号");
        if (password == null || password.isEmpty()) throw new RuntimeException("请填写密码");
        if (employeeName == null || employeeName.isEmpty()) throw new RuntimeException("请填写员工姓名");

        User existing = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) throw new RuntimeException("账号已存在");

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(employeeName);
        user.setPhone(phone);
        user.setRole(4);
        user.setStatus(1);
        user.setVerified(0);
        userService.save(user);

        Employee emp = new Employee();
        emp.setShopId(shopId);
        emp.setUserId(user.getId());
        emp.setEmployeeName(employeeName);
        emp.setPhone(phone);
        emp.setPosition(position);
        emp.setSkill(skill);
        emp.setStatus(1);
        this.save(emp);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("employeeId", emp.getId());
        result.put("username", username);
        return result;
    }
}
