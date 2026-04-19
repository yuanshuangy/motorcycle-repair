package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.motorcycle.repair.dto.StatisticsDTO;
import com.motorcycle.repair.entity.*;
import com.motorcycle.repair.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    @Autowired private UserMapper userMapper;
    @Autowired private RepairShopMapper repairShopMapper;
    @Autowired private ServiceTypeMapper serviceTypeMapper;
    @Autowired private AppointmentMapper appointmentMapper;
    @Autowired private MessageMapper messageMapper;
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private ShopTechnicianMapper shopTechnicianMapper;
    @Autowired private RepairRecordMapper repairRecordMapper;

    public StatisticsDTO getAdminStatistics(String rankPeriod) {
        StatisticsDTO dto = new StatisticsDTO();
        dto.setUserCount((long) userMapper.selectCount(null));
        dto.setShopCount((long) repairShopMapper.selectCount(null));
        dto.setServiceCount((long) serviceTypeMapper.selectCount(null));
        dto.setAppointmentCount((long) appointmentMapper.selectCount(null));
        dto.setTodayAppointmentCount(countToday(null));
        dto.setPendingAppointmentCount(countByStatus(null, 0));
        dto.setProcessingAppointmentCount(countByStatus(null, 2));
        dto.setCompletedAppointmentCount(countByStatus(null, 3));
        dto.setCancelledAppointmentCount(countByStatus(null, 4));
        RepairShop mainShop = getMainShop();
        Long mainShopId = mainShop != null ? mainShop.getId() : null;
        dto.setTodayRevenue(sumRevenue(mainShopId, LocalDate.now(), LocalDate.now()));
        dto.setYesterdayRevenue(sumRevenue(mainShopId, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)));
        dto.setWeekRevenue(sumRevenue(mainShopId, getWeekStart(), LocalDate.now()));
        dto.setMonthRevenue(sumRevenue(mainShopId, LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        dto.setQuarterRevenue(sumRevenue(mainShopId, getQuarterStart(), LocalDate.now()));
        dto.setYearRevenue(sumRevenue(mainShopId, LocalDate.now().withDayOfYear(1), LocalDate.now()));
        dto.setTodayProfit(sumProfit(mainShopId, LocalDate.now(), LocalDate.now()));
        dto.setYesterdayProfit(sumProfit(mainShopId, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)));
        dto.setWeekProfit(sumProfit(mainShopId, getWeekStart(), LocalDate.now()));
        dto.setMonthProfit(sumProfit(mainShopId, LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        dto.setQuarterProfit(sumProfit(mainShopId, getQuarterStart(), LocalDate.now()));
        dto.setYearProfit(sumProfit(mainShopId, LocalDate.now().withDayOfYear(1), LocalDate.now()));
        dto.setRevenueRanking(getTechOrderRankingByPeriod(mainShopId, 10, rankPeriod));
        dto.setYesterdayRanking(getTechYesterdayOrderRanking(mainShopId, 1));
        return dto;
    }

    private RepairShop getMainShop() {
        List<RepairShop> shops = repairShopMapper.selectList(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getAuditStatus, 1).eq(RepairShop::getStatus, 1));
        return shops.isEmpty() ? null : shops.get(0);
    }

    public StatisticsDTO getShopStatistics(Long userId, String rankPeriod) {
        StatisticsDTO dto = new StatisticsDTO();
        RepairShop shop = repairShopMapper.selectOne(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getUserId, userId));
        if (shop != null) {
            Long shopId = shop.getId();
            dto.setAppointmentCount(countByShop(shopId, null));
            dto.setTodayAppointmentCount(countToday(shopId));
            dto.setPendingAppointmentCount(countByStatus(shopId, 0));
            dto.setProcessingAppointmentCount(countByStatus(shopId, 2));
            dto.setCompletedAppointmentCount(countByStatus(shopId, 3));
            dto.setCancelledAppointmentCount(countByStatus(shopId, 4));
            dto.setServiceCount((long) serviceTypeMapper.selectCount(new LambdaQueryWrapper<ServiceType>().eq(ServiceType::getShopId, shopId)));
            dto.setEmployeeCount((long) employeeMapper.selectCount(new LambdaQueryWrapper<Employee>().eq(Employee::getShopId, shopId).eq(Employee::getStatus, 1)));
            dto.setTechnicianCount((long) shopTechnicianMapper.selectCount(new LambdaQueryWrapper<ShopTechnician>().eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getStatus, 1)));
            dto.setTodayRevenue(sumRevenue(shopId, LocalDate.now(), LocalDate.now()));
            dto.setYesterdayRevenue(sumRevenue(shopId, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)));
            dto.setWeekRevenue(sumRevenue(shopId, getWeekStart(), LocalDate.now()));
            dto.setMonthRevenue(sumRevenue(shopId, LocalDate.now().withDayOfMonth(1), LocalDate.now()));
            dto.setQuarterRevenue(sumRevenue(shopId, getQuarterStart(), LocalDate.now()));
            dto.setYearRevenue(sumRevenue(shopId, LocalDate.now().withDayOfYear(1), LocalDate.now()));
            dto.setTodayProfit(sumProfit(shopId, LocalDate.now(), LocalDate.now()));
            dto.setYesterdayProfit(sumProfit(shopId, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)));
            dto.setWeekProfit(sumProfit(shopId, getWeekStart(), LocalDate.now()));
            dto.setMonthProfit(sumProfit(shopId, LocalDate.now().withDayOfMonth(1), LocalDate.now()));
            dto.setQuarterProfit(sumProfit(shopId, getQuarterStart(), LocalDate.now()));
            dto.setYearProfit(sumProfit(shopId, LocalDate.now().withDayOfYear(1), LocalDate.now()));
            dto.setRevenueRanking(getTechOrderRankingByPeriod(shopId, 10, rankPeriod));
            dto.setYesterdayRanking(getTechYesterdayOrderRanking(shopId, 1));
            dto.setMyRankInShop(getShopRevenueRank(shopId));
        }
        return dto;
    }

    public StatisticsDTO getUserStatistics(Long userId) {
        StatisticsDTO dto = new StatisticsDTO();
        dto.setMyAppointmentCount(countByUser(userId, null));
        dto.setMyPendingCount(countByUserAndStatus(userId, new Integer[]{0, 1, 2}));
        dto.setMyCompletedCount(countByUserAndStatus(userId, new Integer[]{3}));
        dto.setUnreadMessageCount((long) messageMapper.selectCount(
            new LambdaQueryWrapper<Message>().eq(Message::getUserId, userId).eq(Message::getIsRead, 0)));
        return dto;
    }

    public StatisticsDTO getTechStatistics(Long userId, String rankPeriod) {
        StatisticsDTO dto = new StatisticsDTO();
        dto.setMyAppointmentCount(countByEmployee(userId, null));
        dto.setTodayAppointmentCount(countTodayByEmployee(userId));
        dto.setMyPendingCount(countByEmployeeAndStatus(userId, new Integer[]{0, 1}));
        dto.setMyCompletedCount(countByEmployeeAndStatus(userId, new Integer[]{3}));
        dto.setProcessingAppointmentCount(countByEmployeeAndStatus(userId, new Integer[]{2}));
        dto.setUnreadMessageCount((long) messageMapper.selectCount(
            new LambdaQueryWrapper<Message>().eq(Message::getUserId, userId).eq(Message::getIsRead, 0)));
        dto.setMyOrderRank(getTechOrderRank(userId));
        dto.setOrderRanking(getTechOrderRankingByPeriod(null, 10, rankPeriod));
        dto.setYesterdayRanking(getTechYesterdayOrderRanking(null, 1));
        return dto;
    }



    private LocalDate getWeekStart() {
        LocalDate d = LocalDate.now();
        return d.minusDays(d.getDayOfWeek().getValue() - 1);
    }

    private LocalDate getQuarterStart() {
        LocalDate d = LocalDate.now();
        int quarter = (d.getMonthValue() - 1) / 3;
        return d.withMonth(quarter * 3 + 1).withDayOfMonth(1);
    }

    private Double sumRevenue(Long shopId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        if (shopId != null) w.eq(Appointment::getShopId, shopId);
        w.eq(Appointment::getStatus, 3);
        w.ge(Appointment::getCreateTime, start.atStartOfDay());
        w.le(Appointment::getCreateTime, end.atTime(LocalTime.MAX));
        List<Appointment> list = appointmentMapper.selectList(w);
        return list.stream().mapToDouble(a -> a.getTotalAmount() != null ? a.getTotalAmount() : 0).sum();
    }

    private Double sumProfit(Long shopId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        if (shopId != null) w.eq(Appointment::getShopId, shopId);
        w.eq(Appointment::getStatus, 3);
        w.ge(Appointment::getCreateTime, start.atStartOfDay());
        w.le(Appointment::getCreateTime, end.atTime(LocalTime.MAX));
        List<Appointment> appointments = appointmentMapper.selectList(w);
        double revenue = appointments.stream().mapToDouble(a -> a.getTotalAmount() != null ? a.getTotalAmount() : 0).sum();
        double cost = 0;
        for (Appointment a : appointments) {
            LambdaQueryWrapper<RepairRecord> rw = new LambdaQueryWrapper<>();
            rw.eq(RepairRecord::getAppointmentId, a.getId());
            RepairRecord r = repairRecordMapper.selectOne(rw);
            if (r != null) cost += (r.getPartsCost() != null ? r.getPartsCost() : 0) + (r.getLaborCost() != null ? r.getLaborCost() : 0);
        }
        return revenue - cost;
    }

    private List<Map<String, Object>> getShopRevenueRanking(Long excludeShopId, int limit) {
        List<RepairShop> shops = repairShopMapper.selectList(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getStatus, 1).eq(RepairShop::getAuditStatus, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RepairShop shop : shops) {
            if (excludeShopId != null && shop.getId().equals(excludeShopId)) continue;
            Double rev = sumRevenue(shop.getId(), LocalDate.now().withDayOfMonth(1), LocalDate.now());
            Map<String, Object> m = new HashMap<>();
            m.put("id", shop.getId());
            m.put("name", shop.getShopName());
            m.put("value", rev);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private Long getShopRevenueRank(Long shopId) {
        List<RepairShop> shops = repairShopMapper.selectList(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getStatus, 1).eq(RepairShop::getAuditStatus, 1));
        List<Map<String, Object>> all = new ArrayList<>();
        for (RepairShop shop : shops) {
            Double rev = sumRevenue(shop.getId(), LocalDate.now().withDayOfMonth(1), LocalDate.now());
            Map<String, Object> m = new HashMap<>();
            m.put("id", shop.getId());
            m.put("value", rev);
            all.add(m);
        }
        all.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).get("id").equals(shopId)) return (long) (i + 1);
        }
        return (long) all.size();
    }

    private List<Map<String, Object>> getShopOrderRanking(Long excludeShopId, int limit) {
        List<RepairShop> shops = repairShopMapper.selectList(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getStatus, 1).eq(RepairShop::getAuditStatus, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RepairShop shop : shops) {
            if (excludeShopId != null && shop.getId().equals(excludeShopId)) continue;
            long cnt = countByShop(shop.getId(), 3);
            Map<String, Object> m = new HashMap<>();
            m.put("id", shop.getId());
            m.put("name", shop.getShopName());
            m.put("value", (double) cnt);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getTechOrderRanking(Long shopId, int limit) {
        List<User> techs = new ArrayList<>();
        if (shopId != null) {
            List<ShopTechnician> links = shopTechnicianMapper.selectList(new LambdaQueryWrapper<ShopTechnician>().eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getStatus, 1));
            for (ShopTechnician link : links) {
                User u = userMapper.selectById(link.getUserId());
                if (u != null && u.getRole() == 4) techs.add(u);
            }
        } else {
            techs = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, 4).eq(User::getStatus, 1));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (User tech : techs) {
            long cnt = countByEmployee(tech.getId(), 3);
            Map<String, Object> m = new HashMap<>();
            m.put("id", tech.getId());
            m.put("name", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            m.put("avatar", tech.getAvatar());
            m.put("value", (double) cnt);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getEmployeeOrderRanking(Long shopId, int limit) {
        LambdaQueryWrapper<Employee> ew = new LambdaQueryWrapper<Employee>().eq(Employee::getStatus, 1);
        if (shopId != null) ew.eq(Employee::getShopId, shopId);
        List<Employee> emps = employeeMapper.selectList(ew);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : emps) {
            long cnt = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getEmployeeId, emp.getId()).eq(Appointment::getStatus, 3));
            Map<String, Object> m = new HashMap<>();
            m.put("id", emp.getId());
            m.put("name", emp.getEmployeeName());
            m.put("value", (double) cnt);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getTechGlobalOrderRanking(int limit) {
        return getTechOrderRanking(null, limit);
    }

    private List<Map<String, Object>> getTechOrderRankingByPeriod(Long shopId, int limit, String rankPeriod) {
        List<User> techs = new ArrayList<>();
        if (shopId != null) {
            List<ShopTechnician> links = shopTechnicianMapper.selectList(new LambdaQueryWrapper<ShopTechnician>().eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getStatus, 1));
            for (ShopTechnician link : links) {
                User u = userMapper.selectById(link.getUserId());
                if (u != null && u.getRole() == 4) techs.add(u);
            }
        } else {
            techs = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, 4).eq(User::getStatus, 1));
        }
        LocalDate start = null, end = LocalDate.now();
        if ("today".equals(rankPeriod)) { start = LocalDate.now(); }
        else if ("week".equals(rankPeriod)) { start = getWeekStart(); }
        else if ("month".equals(rankPeriod)) { start = LocalDate.now().withDayOfMonth(1); }
        List<Map<String, Object>> result = new ArrayList<>();
        for (User tech : techs) {
            LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
            w.eq(Appointment::getEmployeeId, tech.getId());
            w.eq(Appointment::getStatus, 3);
            if (start != null) w.ge(Appointment::getCreateTime, start.atStartOfDay());
            w.le(Appointment::getCreateTime, end.atTime(LocalTime.MAX));
            long periodCount = appointmentMapper.selectCount(w);
            long totalCount = countByEmployee(tech.getId(), 3);
            Map<String, Object> m = new HashMap<>();
            m.put("id", tech.getId());
            m.put("name", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            m.put("avatar", tech.getAvatar());
            m.put("value", (double) periodCount);
            m.put("totalCount", totalCount);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getTechYesterdayOrderRanking(Long shopId, int limit) {
        List<User> techs = new ArrayList<>();
        if (shopId != null) {
            List<ShopTechnician> links = shopTechnicianMapper.selectList(new LambdaQueryWrapper<ShopTechnician>().eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getStatus, 1));
            for (ShopTechnician link : links) {
                User u = userMapper.selectById(link.getUserId());
                if (u != null && u.getRole() == 4) techs.add(u);
            }
        } else {
            techs = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, 4).eq(User::getStatus, 1));
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User tech : techs) {
            LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
            w.eq(Appointment::getEmployeeId, tech.getId());
            w.eq(Appointment::getStatus, 3);
            w.ge(Appointment::getCreateTime, yesterday.atStartOfDay());
            w.le(Appointment::getCreateTime, yesterday.atTime(LocalTime.MAX));
            long cnt = appointmentMapper.selectCount(w);
            if (cnt > 0) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", tech.getId());
                m.put("name", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
                m.put("value", (double) cnt);
                result.add(m);
            }
        }
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private Long getTechOrderRank(Long userId) {
        List<User> techs = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, 4).eq(User::getStatus, 1));
        List<Map<String, Object>> all = new ArrayList<>();
        for (User tech : techs) {
            long cnt = countByEmployee(tech.getId(), 3);
            Map<String, Object> m = new HashMap<>();
            m.put("id", tech.getId());
            m.put("value", (double) cnt);
            all.add(m);
        }
        all.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).get("id").equals(userId)) return (long) (i + 1);
        }
        return (long) all.size();
    }

    private Long getEmployeeOrderRank(Long empId) {
        List<Employee> emps = employeeMapper.selectList(new LambdaQueryWrapper<Employee>().eq(Employee::getStatus, 1));
        List<Map<String, Object>> all = new ArrayList<>();
        for (Employee emp : emps) {
            long cnt = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getEmployeeId, emp.getId()).eq(Appointment::getStatus, 3));
            Map<String, Object> m = new HashMap<>();
            m.put("id", emp.getId());
            m.put("value", (double) cnt);
            all.add(m);
        }
        all.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).get("id").equals(empId)) return (long) (i + 1);
        }
        return (long) all.size();
    }

    private List<Map<String, Object>> getEmployeeGlobalOrderRanking(int limit) {
        List<Employee> emps = employeeMapper.selectList(new LambdaQueryWrapper<Employee>().eq(Employee::getStatus, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : emps) {
            long cnt = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getEmployeeId, emp.getId()).eq(Appointment::getStatus, 3));
            Map<String, Object> m = new HashMap<>();
            m.put("id", emp.getId());
            m.put("name", emp.getEmployeeName());
            m.put("value", (double) cnt);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private long countToday(Long shopId) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        if (shopId != null) w.eq(Appointment::getShopId, shopId);
        w.ge(Appointment::getAppointmentTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
        w.le(Appointment::getAppointmentTime, LocalDateTime.of(LocalDate.now(), LocalTime.MAX));
        return appointmentMapper.selectCount(w);
    }

    private long countByStatus(Long shopId, Integer status) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        if (shopId != null) w.eq(Appointment::getShopId, shopId);
        if (status != null) w.eq(Appointment::getStatus, status);
        return appointmentMapper.selectCount(w);
    }

    private long countByShop(Long shopId, Integer status) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        w.eq(Appointment::getShopId, shopId);
        if (status != null) w.eq(Appointment::getStatus, status);
        return appointmentMapper.selectCount(w);
    }

    private long countByUser(Long userId, Integer status) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        w.eq(Appointment::getUserId, userId);
        if (status != null) w.eq(Appointment::getStatus, status);
        return appointmentMapper.selectCount(w);
    }

    private long countByUserAndStatus(Long userId, Integer[] statuses) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        w.eq(Appointment::getUserId, userId);
        w.in(Appointment::getStatus, statuses);
        return appointmentMapper.selectCount(w);
    }

    private long countByEmployee(Long employeeId, Integer status) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        w.eq(Appointment::getEmployeeId, employeeId);
        if (status != null) w.eq(Appointment::getStatus, status);
        return appointmentMapper.selectCount(w);
    }

    private long countTodayByEmployee(Long employeeId) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        w.eq(Appointment::getEmployeeId, employeeId);
        w.ge(Appointment::getCreateTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
        w.le(Appointment::getCreateTime, LocalDateTime.of(LocalDate.now(), LocalTime.MAX));
        return appointmentMapper.selectCount(w);
    }

    private long countByEmployeeAndStatus(Long employeeId, Integer[] statuses) {
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        w.eq(Appointment::getEmployeeId, employeeId);
        w.in(Appointment::getStatus, statuses);
        return appointmentMapper.selectCount(w);
    }
}
