package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.dto.AppointmentDTO;
import com.motorcycle.repair.entity.*;
import com.motorcycle.repair.mapper.AppointmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService extends ServiceImpl<AppointmentMapper, Appointment> {
    @Autowired private UserService userService;
    @Autowired private RepairShopService repairShopService;
    @Autowired private ServiceTypeService serviceTypeService;
    @Autowired private ShopTechnicianService shopTechnicianService;
    @Autowired private ReviewService reviewService;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Page<AppointmentDTO> getAppointmentPage(Integer pageNum, Integer pageSize, Long userId, Integer role, Long shopId, Integer status) {
        Page<Appointment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Appointment> w = new LambdaQueryWrapper<>();
        if (role == 3) w.eq(Appointment::getUserId, userId);
        else if (role == 2 && shopId != null) w.eq(Appointment::getShopId, shopId);
        else if (role == 4) {
            w.and(wrapper -> wrapper.eq(Appointment::getEmployeeId, userId).or().eq(Appointment::getDriverId, userId));
        }
        if (status != null) w.eq(Appointment::getStatus, status);
        w.orderByDesc(Appointment::getCreateTime);
        Page<Appointment> result = this.page(page, w);
        Page<AppointmentDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    public AppointmentDTO toDTO(Appointment a) {
        AppointmentDTO d = new AppointmentDTO();
        d.setId(a.getId()); d.setOrderNo(a.getOrderNo()); d.setUserId(a.getUserId());
        d.setShopId(a.getShopId()); d.setServiceId(a.getServiceId()); d.setEmployeeId(a.getEmployeeId());
        d.setDriverId(a.getDriverId());
        d.setMotorcycleBrand(a.getMotorcycleBrand()); d.setMotorcycleModel(a.getMotorcycleModel());
        d.setProblemDescription(a.getProblemDescription()); d.setStatus(a.getStatus());
        d.setStatusName(getStatusName(a.getStatus())); d.setRemark(a.getRemark());
        d.setTotalAmount(a.getTotalAmount());
        d.setPayStatus(a.getPayStatus());
        d.setPayMethod(a.getPayMethod());
        d.setAppointmentTime(a.getAppointmentTime() != null ? a.getAppointmentTime().format(FMT) : null);
        d.setCreateTime(a.getCreateTime() != null ? a.getCreateTime().format(FMT) : null);
        d.setCompleteTime(a.getCompleteTime() != null ? a.getCompleteTime().format(FMT) : null);
        d.setUpdateTime(a.getUpdateTime() != null ? a.getUpdateTime().format(FMT) : null);
        d.setOvertimeMinutes(a.getOvertimeMinutes());
        try {
            d.setHasReview(reviewService.count(new LambdaQueryWrapper<Review>().eq(Review::getAppointmentId, a.getId())) > 0);
        } catch (Exception e) { d.setHasReview(false); }
        d.setTowService(a.getTowService());
        d.setTowAddress(a.getTowAddress());
        d.setTowDistance(a.getTowDistance());
        d.setTowFee(a.getTowFee());
        d.setPickupStatus(a.getPickupStatus());
        d.setPickupStatusName(getPickupStatusName(a.getPickupStatus()));
        try {
            User u = userService.getById(a.getUserId());
            if (u != null) {
                d.setUserName(u.getRealName() != null ? u.getRealName() : u.getUsername());
                d.setUserPhone(u.getPhone());
                d.setUserEmail(u.getEmail());
                d.setUserAvatar(u.getAvatar());
            }
            RepairShop s = repairShopService.getById(a.getShopId());
            if (s != null) d.setShopName(s.getShopName());
            ServiceType sv = serviceTypeService.getById(a.getServiceId());
            if (sv != null) { d.setServiceName(sv.getServiceName()); d.setServicePrice(sv.getPrice()); d.setServiceDuration(sv.getDuration()); }
            if (a.getEmployeeId() != null) {
                User tech = userService.getById(a.getEmployeeId());
                if (tech != null) {
                    d.setEmployeeName(tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
                    d.setEmployeePhone(tech.getPhone());
                    d.setEmployeeAvatar(tech.getAvatar());
                    d.setEmployeeSkill(tech.getSkill());
                    ShopTechnician st = shopTechnicianService.getOne(new LambdaQueryWrapper<ShopTechnician>()
                            .eq(ShopTechnician::getShopId, a.getShopId()).eq(ShopTechnician::getUserId, tech.getId()));
                    d.setEmployeeRole(st != null && st.getPosition() != null ? st.getPosition() : "技师");
                }
            }
            if (a.getDriverId() != null) {
                User driver = userService.getById(a.getDriverId());
                if (driver != null) {
                    d.setDriverName(driver.getRealName() != null ? driver.getRealName() : driver.getUsername());
                    d.setDriverPhone(driver.getPhone());
                    d.setDriverAvatar(driver.getAvatar());
                }
            }
        } catch (Exception ignored) {}
        return d;
    }

    @Autowired
    private DictDataService dictDataService;

    private String getStatusName(Integer s) {
        if (s == null) return "未知";
        List<DictData> list = dictDataService.getByType("appointment_status");
        for (DictData d : list) { if (d.getDictValue().equals(String.valueOf(s))) return d.getDictLabel(); }
        return "未知";
    }

    private String getPickupStatusName(Integer s) {
        if (s == null) return "无需接车";
        List<DictData> list = dictDataService.getByType("pickup_status");
        for (DictData d : list) { if (d.getDictValue().equals(String.valueOf(s))) return d.getDictLabel(); }
        return "未知";
    }

    public void confirmPickup(Long id) {
        Appointment a = this.getById(id);
        if (a != null && a.getTowService() != null && a.getTowService() == 1) {
            a.setPickupStatus(1);
            this.updateById(a);
        }
    }

    public void pickupFailed(Long id) {
        Appointment a = this.getById(id);
        if (a != null && a.getTowService() != null && a.getTowService() == 1) {
            a.setPickupStatus(3);
            this.updateById(a);
        }
    }

    public void vehicleArrived(Long id) {
        Appointment a = this.getById(id);
        if (a != null && a.getTowService() != null && a.getTowService() == 1) {
            a.setPickupStatus(2);
            this.updateById(a);
        }
    }

    public AppointmentDTO getByIdDTO(Long id) { Appointment a = this.getById(id); return a != null ? toDTO(a) : null; }

    public String validateBusinessHours(Long shopId, LocalDateTime appointmentTime) {
        RepairShop shop = repairShopService.getById(shopId);
        if (shop == null || shop.getBusinessHours() == null || shop.getBusinessHours().trim().isEmpty()) return null;
        String bh = shop.getBusinessHours().trim();
        try {
            String[] parts = bh.split("-");
            if (parts.length == 2) {
                String openStr = parts[0].trim();
                String closeStr = parts[1].trim();
                LocalTime openTime = parseTime(openStr);
                LocalTime closeTime = parseTime(closeStr);
                if (openTime != null && closeTime != null) {
                    LocalTime aptTime = appointmentTime.toLocalTime();
                    if (aptTime.isBefore(openTime) || aptTime.isAfter(closeTime) || aptTime.equals(closeTime)) {
                        return "预约时间不在店铺营业时间(" + bh + ")内";
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private LocalTime parseTime(String timeStr) {
        try {
            if (timeStr.contains(":")) {
                String[] p = timeStr.split(":");
                return LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            }
            int hour = Integer.parseInt(timeStr);
            return LocalTime.of(hour, 0);
        } catch (Exception e) { return null; }
    }

    private LocalTime parseBusinessCloseTime(String businessHours) {
        if (businessHours == null) return null;
        String[] parts = businessHours.split("[-~到至]");
        if (parts.length >= 2) {
            return parseTime(parts[parts.length - 1].trim());
        }
        return null;
    }

    public boolean checkTimeConflict(Long shopId, Long employeeId, LocalDateTime appointmentTime, Long serviceId) {
        if (employeeId == null || appointmentTime == null) return false;
        Integer duration = 60;
        if (serviceId != null) {
            ServiceType sv = serviceTypeService.getById(serviceId);
            if (sv != null && sv.getDuration() != null) duration = sv.getDuration();
        }
        LocalDateTime endTime = appointmentTime.plusMinutes(duration);
        List<Appointment> existing = this.list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getEmployeeId, employeeId)
                .in(Appointment::getStatus, 0, 1, 2));
        for (Appointment apt : existing) {
            if (apt.getAppointmentTime() == null) continue;
            int aptDuration = 60;
            if (apt.getServiceId() != null) {
                ServiceType aptService = serviceTypeService.getById(apt.getServiceId());
                if (aptService != null && aptService.getDuration() != null) aptDuration = aptService.getDuration();
            }
            LocalDateTime aptEnd = apt.getAppointmentTime().plusMinutes(aptDuration);
            if (appointmentTime.isBefore(aptEnd) && endTime.isAfter(apt.getAppointmentTime())) {
                return true;
            }
        }
        return false;
    }

    public List<Map<String, Object>> recommendTechnicians(Long shopId, Long serviceId, LocalDateTime appointmentTime) {
        List<User> techs = shopTechnicianService.getActiveTechniciansByShop(shopId);
        String serviceName = null;
        if (serviceId != null) {
            ServiceType sv = serviceTypeService.getById(serviceId);
            if (sv != null) serviceName = sv.getServiceName();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (User tech : techs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tech.getId());
            item.put("name", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            item.put("avatar", tech.getAvatar());
            item.put("phone", tech.getPhone());
            item.put("skill", tech.getSkill() != null ? tech.getSkill() : "");
            long currentOrders = this.count(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getEmployeeId, tech.getId())
                    .in(Appointment::getStatus, 0, 1, 2));
            item.put("currentOrders", currentOrders);
            boolean hasConflict = false;
            if (appointmentTime != null) {
                hasConflict = checkTimeConflict(shopId, tech.getId(), appointmentTime, serviceId);
            }
            item.put("hasConflict", hasConflict);
            boolean skillMatch = false;
            if (serviceName != null && tech.getSkill() != null) {
                String[] skills = tech.getSkill().split("[,，、]");
                for (String sk : skills) {
                    if (serviceName.contains(sk.trim()) || sk.trim().contains(serviceName)) {
                        skillMatch = true;
                        break;
                    }
                }
            }
            item.put("skillMatch", skillMatch);
            result.add(item);
        }
        result.sort((a, b) -> {
            boolean aConflict = (boolean) a.get("hasConflict");
            boolean bConflict = (boolean) b.get("hasConflict");
            if (aConflict != bConflict) return aConflict ? 1 : -1;
            boolean aSkill = (boolean) a.get("skillMatch");
            boolean bSkill = (boolean) b.get("skillMatch");
            if (aSkill != bSkill) return aSkill ? -1 : 1;
            return Long.compare((long) a.get("currentOrders"), (long) b.get("currentOrders"));
        });
        return result;
    }

    public void createAppointment(Appointment a) {
        a.setOrderNo("MT" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000))); a.setStatus(0);
        if (a.getServiceId() != null) {
            ServiceType sv = serviceTypeService.getById(a.getServiceId());
            if (sv != null) a.setTotalAmount(sv.getPrice());
        }
        if (a.getTowService() != null && a.getTowService() == 1 && a.getTowFee() != null) {
            double base = a.getTotalAmount() != null ? a.getTotalAmount() : 0;
            a.setTotalAmount(base + a.getTowFee());
        }
        if (a.getAppointmentTime() != null && a.getShopId() != null) {
            String bhError = validateBusinessHours(a.getShopId(), a.getAppointmentTime());
            if (bhError != null) throw new RuntimeException(bhError);
        }
        if (a.getEmployeeId() != null && a.getAppointmentTime() != null) {
            if (checkTimeConflict(a.getShopId(), a.getEmployeeId(), a.getAppointmentTime(), a.getServiceId())) {
                throw new RuntimeException("该技师在预约时间段已有其他预约，请选择其他时间或技师");
            }
        }
        this.save(a);
        if (a.getShopId() != null) {
            RepairShop shop = repairShopService.getById(a.getShopId());
            if (shop != null) {
                List<User> techs = shopTechnicianService.getActiveTechniciansByShop(shop.getId());
                String serviceName = null;
                if (a.getServiceId() != null) {
                    ServiceType sv = serviceTypeService.getById(a.getServiceId());
                    if (sv != null) serviceName = sv.getServiceName();
                }
                String desc = a.getProblemDescription();
                boolean hasTow = a.getTowService() != null && a.getTowService() == 1;

                if (hasTow) {
                    User bestDriver = findBestDriver(techs);
                    if (bestDriver != null) {
                        a.setDriverId(bestDriver.getId());
                    }
                }

                boolean autoAssign = shop.getAutoAssign() != null && shop.getAutoAssign() == 1;
                boolean autoConfirm = shop.getAutoConfirm() != null && shop.getAutoConfirm() == 1;

                if (a.getEmployeeId() == null && autoAssign) {
                    User bestTech = findBestRepairTech(techs, serviceName, desc, hasTow, a.getShopId(), a.getAppointmentTime(), a.getServiceId());
                    if (bestTech != null) {
                        a.setEmployeeId(bestTech.getId());
                    }
                }

                if (autoConfirm) {
                    a.setStatus(1);
                }

                this.updateById(a);
            }
        }
    }

    private User findBestDriver(List<User> techs) {
        User bestDriver = null;
        long minOrders = Long.MAX_VALUE;
        for (User tech : techs) {
            if (tech.getSkill() == null) continue;
            String[] skills = tech.getSkill().split("[,，、]");
            boolean isDriver = false;
            for (String sk : skills) {
                if (sk.trim().equals("司机")) { isDriver = true; break; }
            }
            if (!isDriver) continue;
            long currentOrders = this.count(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getDriverId, tech.getId())
                    .in(Appointment::getStatus, 0, 1, 2));
            if (currentOrders < minOrders) {
                minOrders = currentOrders;
                bestDriver = tech;
            }
        }
        return bestDriver;
    }

    private User findBestRepairTech(List<User> techs, String serviceName, String desc, boolean hasTow, Long shopId, LocalDateTime appointmentTime, Long serviceId) {
        User bestTech = null;
        long minOrders = Long.MAX_VALUE;
        int bestPriority = -1;
        for (User tech : techs) {
            boolean isDedicatedDriver = false;
            if (tech.getSkill() != null) {
                String[] skills = tech.getSkill().split("[,，、]");
                if (skills.length == 1 && skills[0].trim().equals("司机")) {
                    isDedicatedDriver = true;
                }
            }
            if (isDedicatedDriver) continue;

            long currentOrders = this.count(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getEmployeeId, tech.getId())
                    .in(Appointment::getStatus, 0, 1, 2));
            boolean hasConflict = false;
            if (appointmentTime != null) {
                hasConflict = checkTimeConflict(shopId, tech.getId(), appointmentTime, serviceId);
            }
            if (hasConflict) continue;
            int priority = 0;
            if (tech.getSkill() != null) {
                String[] skills = tech.getSkill().split("[,，、]");
                for (String sk : skills) {
                    String s = sk.trim();
                    if (!s.isEmpty() && !s.equals("司机")) {
                        if ((serviceName != null && (serviceName.contains(s) || s.contains(serviceName))) ||
                            (desc != null && !desc.isEmpty() && desc.contains(s))) {
                            if (priority < 2) priority = 2;
                        }
                    }
                }
            }
            if (hasTow && priority < 1) priority = 1;
            if (bestTech == null) { bestTech = tech; minOrders = currentOrders; bestPriority = priority; continue; }
            if (priority > bestPriority) { bestTech = tech; minOrders = currentOrders; bestPriority = priority; continue; }
            if (priority == bestPriority && currentOrders < minOrders) { bestTech = tech; minOrders = currentOrders; }
        }
        if (bestTech == null && !techs.isEmpty()) {
            for (User tech : techs) {
                boolean isDedicatedDriver = false;
                if (tech.getSkill() != null) {
                    String[] skills = tech.getSkill().split("[,，、]");
                    if (skills.length == 1 && skills[0].trim().equals("司机")) isDedicatedDriver = true;
                }
                if (isDedicatedDriver) continue;
                boolean hasConflict = false;
                if (appointmentTime != null) hasConflict = checkTimeConflict(shopId, tech.getId(), appointmentTime, serviceId);
                if (!hasConflict) { bestTech = tech; break; }
            }
        }
        return bestTech;
    }

    public void updateStatus(Long id, Integer status) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("预约不存在");
        Integer current = a.getStatus();
        boolean valid = switch (status) {
            case 0 -> false;
            case 1 -> current == 0;
            case 2 -> current == 1;
            case 3 -> current == 2;
            case 4 -> current == 0 || current == 1;
            case 5 -> current == 0 || current == 1;
            default -> false;
        };
        if (!valid) throw new RuntimeException("不允许从" + getStatusName(current) + "变更为" + getStatusName(status));
        a.setStatus(status);
        this.updateById(a);
    }

    public void assignEmployee(Long id, Long employeeId) {
        Appointment a = this.getById(id);
        if (a != null) {
            if (a.getAppointmentTime() != null && checkTimeConflict(a.getShopId(), employeeId, a.getAppointmentTime(), a.getServiceId())) {
                throw new RuntimeException("该技师在预约时间段已有其他预约，请选择其他技师");
            }
            a.setEmployeeId(employeeId); a.setStatus(1); this.updateById(a);
        }
    }

    public void cancelAppointment(Long id) { updateStatus(id, 4); }
    public void completeAppointment(Long id) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("预约不存在");
        if (a.getStatus() != 2) throw new RuntimeException("只有维修中的预约才能完成");
        a.setStatus(3);
        a.setCompleteTime(LocalDateTime.now());
        if (a.getShopId() != null) {
            RepairShop shop = repairShopService.getById(a.getShopId());
            if (shop != null && shop.getBusinessHours() != null) {
                LocalTime closeTime = parseBusinessCloseTime(shop.getBusinessHours());
                if (closeTime != null && a.getCompleteTime().toLocalTime().isAfter(closeTime)) {
                    long extraMin = java.time.Duration.between(closeTime, a.getCompleteTime().toLocalTime()).toMinutes();
                    a.setOvertimeMinutes((int) extraMin);
                }
            }
        }
        this.updateById(a);
    }

    public void noshowAppointment(Long id) {
        Appointment a = this.getById(id);
        if (a != null && (a.getStatus() == 0 || a.getStatus() == 1)) {
            a.setStatus(5);
            this.updateById(a);
        }
    }

    public void payAppointment(Long id, String payMethod) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("预约不存在");
        if (a.getStatus() != 3) throw new RuntimeException("只有已完成的预约才能支付");
        if (a.getPayStatus() != null && a.getPayStatus() == 1) throw new RuntimeException("该预约已支付");
        if (payMethod == null || payMethod.trim().isEmpty()) throw new RuntimeException("请选择支付方式");
        a.setPayStatus(1);
        a.setPayMethod(payMethod);
        a.setPayTime(java.time.LocalDateTime.now());
        this.updateById(a);
    }
}
