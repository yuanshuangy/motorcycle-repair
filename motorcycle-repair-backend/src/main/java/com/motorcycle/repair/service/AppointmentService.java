package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.dto.AppointmentDTO;
import com.motorcycle.repair.entity.*;
import com.motorcycle.repair.mapper.AppointmentMapper;
import com.motorcycle.repair.websocket.ShopWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class AppointmentService extends ServiceImpl<AppointmentMapper, Appointment> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AppointmentService.class);
    @Autowired private UserService userService;
    @Autowired private RepairShopService repairShopService;
    @Autowired private ServiceTypeService serviceTypeService;
    @Autowired private ShopTechnicianService shopTechnicianService;
    @Autowired private ReviewService reviewService;
    @Autowired private ShopWebSocketHandler shopWebSocketHandler;

    private final ConcurrentHashMap<String, ReentrantLock> bookingLocks = new ConcurrentHashMap<>();

    private ReentrantLock getBookingLock(Long shopId, Long employeeId, String dateStr) {
        String key = "booking:" + shopId + ":" + employeeId + ":" + dateStr;
        return bookingLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

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
        d.setWaitAutoAssign(a.getWaitAutoAssign());
        d.setEstimatedWaitMinutes(a.getEstimatedWaitMinutes());
        d.setWaitDiscountRate(a.getWaitDiscountRate());
        d.setCancelReason(a.getCancelReason());
        d.setEstimatedCompleteTime(a.getEstimatedCompleteTime() != null ? a.getEstimatedCompleteTime().format(FMT) : null);
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

    public void createAppointment(Appointment a, Long currentUserId) {
        String dateStr = a.getAppointmentTime() != null
            ? a.getAppointmentTime().toLocalDate().toString() : "unknown";
        Long lockEmployeeId = a.getEmployeeId() != null ? a.getEmployeeId() : 0L;
        ReentrantLock lock = getBookingLock(a.getShopId(), lockEmployeeId, dateStr);
        boolean acquired = false;
        try {
            acquired = lock.tryLock();
            if (!acquired) {
                throw new RuntimeException("该时段正在被其他人预约，请稍后再试");
            }
            _createAppointment(a);
        } finally {
            if (acquired) lock.unlock();
            bookingLocks.remove("booking:" + a.getShopId() + ":" + lockEmployeeId + ":" + dateStr);
        }
    }

    private void _createAppointment(Appointment a) {
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
                if (a.getWaitAutoAssign() != null && a.getWaitAutoAssign() == 1) {
                    a.setEmployeeId(null);
                } else {
                    throw new RuntimeException("该技师在预约时间段已有其他预约，请选择其他时间或技师，或选择\"完单后自动预约\"");
                }
            }
        }
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

                boolean autoConfirm = shop.getAutoConfirm() != null && shop.getAutoConfirm() == 1;

                if (a.getEmployeeId() == null) {
                    User bestTech = findBestRepairTech(techs, serviceName, desc, hasTow, a.getShopId(), a.getAppointmentTime(), a.getServiceId());
                    if (bestTech != null) {
                        a.setEmployeeId(bestTech.getId());
                    } else {
                        int estWait = calculateEstimatedWaitMinutes(shop.getId(), a.getServiceId());
                        a.setEstimatedWaitMinutes(estWait);
                        a.setWaitAutoAssign(1);
                    }
                }

                if (a.getEmployeeId() == null && a.getWaitAutoAssign() != null && a.getWaitAutoAssign() == 1) {
                    int estWait = calculateEstimatedWaitMinutes(shop.getId(), a.getServiceId());
                    a.setEstimatedWaitMinutes(estWait);
                }

                if (autoConfirm && a.getEmployeeId() != null) {
                    a.setStatus(1);
                }
                if (a.getEmployeeId() != null) {
                    calculateEstimatedCompleteTime(a);
                }
            }
        }
        this.save(a);
        if (a.getShopId() != null) {
            try {
                String svcName = "未知服务";
                if (a.getServiceId() != null) {
                    ServiceType sv = serviceTypeService.getById(a.getServiceId());
                    if (sv != null) svcName = sv.getServiceName();
                }
                String userName = "用户";
                if (a.getUserId() != null) {
                    User u = userService.getById(a.getUserId());
                    if (u != null) userName = u.getRealName() != null ? u.getRealName() : u.getUsername();
                }
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "NEW_APPOINTMENT");
                notification.put("orderNo", a.getOrderNo());
                notification.put("userName", userName);
                notification.put("serviceName", svcName);
                notification.put("appointmentTime", a.getAppointmentTime() != null ? a.getAppointmentTime().format(FMT) : "");
                if (a.getEmployeeId() != null) {
                    notification.put("message", "您有新的预约订单，请及时处理");
                } else {
                    notification.put("message", "您有新的预约订单（待分配技师），请及时处理");
                }
                shopWebSocketHandler.pushToShop(a.getShopId(), notification);
            } catch (Exception ignored) {}
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
        if (status == 1 || status == 2) {
            calculateEstimatedCompleteTime(a);
        }
        this.updateById(a);
    }

    public void assignEmployee(Long id, Long employeeId) {
        Appointment a = this.getById(id);
        if (a != null) {
            if (a.getAppointmentTime() != null && checkTimeConflict(a.getShopId(), employeeId, a.getAppointmentTime(), a.getServiceId())) {
                throw new RuntimeException("该技师在预约时间段已有其他预约，请选择其他技师");
            }
            a.setEmployeeId(employeeId); a.setStatus(1);
            calculateEstimatedCompleteTime(a);
            this.updateById(a);
        }
    }

    public void cancelAppointment(Long id, String cancelReason) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("预约不存在");
        if (a.getStatus() != 0 && a.getStatus() != 1) {
            throw new RuntimeException("只有待确认或已确认的订单才能取消，当前状态：" + getStatusName(a.getStatus()));
        }
        a.setStatus(4);
        if (cancelReason != null && !cancelReason.trim().isEmpty()) {
            a.setCancelReason(cancelReason.trim());
            String existingRemark = a.getRemark() != null ? a.getRemark().trim() : "";
            StringBuilder newRemark = new StringBuilder(existingRemark);
            if (newRemark.length() > 0) newRemark.append(" | ");
            newRemark.append("取消原因:").append(cancelReason.trim());
            a.setRemark(newRemark.toString());
        }
        this.updateById(a);
    }
    public void completeAppointment(Long id) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("预约不存在");
        if (a.getStatus() != 2) throw new RuntimeException("只有维修中的预约才能完成");
        Long freedEmployeeId = a.getEmployeeId();
        Long shopId = a.getShopId();
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
        log.info("完单处理: appointmentId={}, freedEmployeeId={}, shopId={}", id, freedEmployeeId, shopId);
        try {
            autoAssignPendingOrders(shopId, freedEmployeeId);
        } catch (Exception e) {
            log.error("自动派单异常: shopId={}, freedEmployeeId={}, error={}", shopId, freedEmployeeId, e.getMessage(), e);
        }
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

    public void confirmPrice(Long id, Double finalPrice, String priceRemark) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("订单不存在");
        if (a.getStatus() != 2) throw new RuntimeException("只有维修中的订单才能确认价格");
        if (finalPrice == null || finalPrice < 0) throw new RuntimeException("价格不能为负数");
        String existingRemark = a.getRemark() != null ? a.getRemark().trim() : "";
        StringBuilder newRemark = new StringBuilder(existingRemark);
        if (priceRemark != null && !priceRemark.trim().isEmpty()) {
            if (newRemark.length() > 0) newRemark.append(" | ");
            newRemark.append("价格备注:").append(priceRemark.trim());
        }
        a.setTotalAmount(finalPrice);
        a.setRemark(newRemark.toString());
        this.updateById(a);
    }

    public List<Map<String, Object>> getTransferTechs(Long id) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("订单不存在");
        Long shopId = a.getShopId();
        List<User> techs = shopTechnicianService.getActiveTechniciansByShop(shopId);
        LocalDateTime aptTime = a.getAppointmentTime();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User tech : techs) {
            if (tech.getId().equals(a.getEmployeeId())) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("id", tech.getId());
            item.put("name", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            item.put("skill", tech.getSkill() != null ? tech.getSkill() : "");
            long currentOrders = this.count(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getEmployeeId, tech.getId())
                    .in(Appointment::getStatus, 0, 1, 2));
            item.put("currentOrders", currentOrders);
            boolean hasConflict = false;
            if (aptTime != null) {
                hasConflict = checkTimeConflict(shopId, tech.getId(), aptTime, a.getServiceId());
            }
            item.put("hasConflict", hasConflict);
            result.add(item);
        }
        return result;
    }

    public void transferOrder(Long id, Long newTechId, String transferReason) {
        Appointment a = this.getById(id);
        if (a == null) throw new RuntimeException("订单不存在");
        if (a.getStatus() < 1 || a.getStatus() > 2) throw new RuntimeException("只有待处理或维修中的订单才能转单");
        if (newTechId != null && newTechId.equals(a.getEmployeeId())) throw new RuntimeException("不能转单给自己");
        User newTech = userService.getById(newTechId);
        if (newTech == null) throw new RuntimeException("目标技师不存在");
        if (a.getAppointmentTime() != null && checkTimeConflict(a.getShopId(), newTechId, a.getAppointmentTime(), a.getServiceId())) {
            throw new RuntimeException("目标技师在预约时间段已有其他预约，无法转单");
        }
        a.setEmployeeId(newTechId);
        calculateEstimatedCompleteTime(a);
        String existingRemark = a.getRemark() != null ? a.getRemark().trim() : "";
        StringBuilder newRemark = new StringBuilder(existingRemark);
        if (transferReason != null && !transferReason.trim().isEmpty()) {
            if (newRemark.length() > 0) newRemark.append(" | ");
            newRemark.append("转单原因:").append(transferReason.trim());
        }
        a.setRemark(newRemark.toString());
        this.updateById(a);
    }

    private int calculateEstimatedWaitMinutes(Long shopId, Long serviceId) {
        int avgDuration = 60;
        if (serviceId != null) {
            ServiceType sv = serviceTypeService.getById(serviceId);
            if (sv != null && sv.getDuration() != null) avgDuration = sv.getDuration();
        }
        long inProgressCount = this.count(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, shopId)
                .eq(Appointment::getEmployeeId, null)
                .in(Appointment::getStatus, 0, 1));
        long techCount = shopTechnicianService.getActiveTechniciansByShop(shopId)
                .stream().filter(t -> {
                    if (t.getSkill() == null) return true;
                    String[] skills = t.getSkill().split("[,，、]");
                    return skills.length != 1 || !skills[0].trim().equals("司机");
                }).count();
        if (techCount <= 0) techCount = 1;
        long busyTechs = this.list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, shopId)
                .isNotNull(Appointment::getEmployeeId)
                .in(Appointment::getStatus, 0, 1, 2)
                .select(Appointment::getEmployeeId)
                .isNotNull(Appointment::getEmployeeId))
                .stream().map(Appointment::getEmployeeId).distinct().count();
        long freeTechs = Math.max(0, techCount - busyTechs);
        int queuePosition = (int) inProgressCount;
        if (freeTechs > 0) {
            return Math.max(5, (queuePosition / (int) freeTechs) * avgDuration);
        }
        Appointment earliestBusy = this.getOne(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, shopId)
                .isNotNull(Appointment::getEmployeeId)
                .in(Appointment::getStatus, 1, 2)
                .isNotNull(Appointment::getEstimatedCompleteTime)
                .orderByAsc(Appointment::getEstimatedCompleteTime)
                .last("LIMIT 1"));
        if (earliestBusy != null && earliestBusy.getEstimatedCompleteTime() != null) {
            long waitMin = java.time.Duration.between(LocalDateTime.now(), earliestBusy.getEstimatedCompleteTime()).toMinutes();
            return (int) Math.max(5, waitMin + queuePosition * avgDuration);
        }
        earliestBusy = this.getOne(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, shopId)
                .isNotNull(Appointment::getEmployeeId)
                .eq(Appointment::getStatus, 2)
                .isNotNull(Appointment::getAppointmentTime)
                .orderByAsc(Appointment::getAppointmentTime)
                .last("LIMIT 1"));
        if (earliestBusy != null && earliestBusy.getAppointmentTime() != null) {
            int earliestDuration = 60;
            if (earliestBusy.getServiceId() != null) {
                ServiceType sv = serviceTypeService.getById(earliestBusy.getServiceId());
                if (sv != null && sv.getDuration() != null) earliestDuration = sv.getDuration();
            }
            LocalDateTime estimatedEnd = earliestBusy.getAppointmentTime().plusMinutes(earliestDuration);
            long waitMin = java.time.Duration.between(LocalDateTime.now(), estimatedEnd).toMinutes();
            return (int) Math.max(5, waitMin + queuePosition * avgDuration);
        }
        return (queuePosition + 1) * avgDuration;
    }

    private void autoAssignPendingOrders(Long shopId, Long freedEmployeeId) {
        if (shopId == null) return;
        RepairShop shop = repairShopService.getById(shopId);
        log.info("自动派单开始: shopId={}, freedEmployeeId={}", shopId, freedEmployeeId);
        List<Appointment> pendingOrders = this.list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, shopId)
                .isNull(Appointment::getEmployeeId)
                .in(Appointment::getStatus, 0, 1)
                .orderByAsc(Appointment::getCreateTime));
        log.info("待分配订单数: {}, 列表: {}", pendingOrders.size(), pendingOrders.stream().map(a -> a.getId() + ":" + a.getOrderNo()).toList());
        if (pendingOrders.isEmpty()) return;
        List<User> techs = shopTechnicianService.getActiveTechniciansByShop(shopId);
        log.info("店铺技师数: {}, 技师IDs: {}", techs.size(), techs.stream().map(t -> t.getId() + ":" + (t.getRealName() != null ? t.getRealName() : t.getUsername())).toList());
        if (freedEmployeeId != null) {
            User freedTech = userService.getById(freedEmployeeId);
            boolean isShopTech = techs.stream().anyMatch(t -> t.getId().equals(freedEmployeeId));
            log.info("完单技师: id={}, name={}, 是否店铺技师: {}", freedEmployeeId, freedTech != null ? (freedTech.getRealName() != null ? freedTech.getRealName() : freedTech.getUsername()) : "null", isShopTech);
            if (freedTech != null && isShopTech) {
                Appointment earliest = pendingOrders.get(0);
                earliest.setEmployeeId(freedEmployeeId);
                if (earliest.getStatus() == 0 && shop != null && shop.getAutoConfirm() != null && shop.getAutoConfirm() == 1) {
                    earliest.setStatus(1);
                }
                calculateEstimatedCompleteTime(earliest);
                applyWaitDiscount(earliest);
                this.updateById(earliest);
                log.info("自动派单成功: orderId={}, employeeId={}", earliest.getId(), freedEmployeeId);
                try {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "AUTO_ASSIGNED");
                    notification.put("orderNo", earliest.getOrderNo());
                    notification.put("employeeName", freedTech.getRealName() != null ? freedTech.getRealName() : freedTech.getUsername());
                    notification.put("message", "待分配订单已自动派单给" + (freedTech.getRealName() != null ? freedTech.getRealName() : freedTech.getUsername()));
                    shopWebSocketHandler.pushToShop(shopId, notification);
                } catch (Exception ignored) {}
                return;
            }
        }
        for (Appointment pending : pendingOrders) {
            String serviceName = null;
            if (pending.getServiceId() != null) {
                ServiceType sv = serviceTypeService.getById(pending.getServiceId());
                if (sv != null) serviceName = sv.getServiceName();
            }
            User bestTech = findBestRepairTech(techs, serviceName, pending.getProblemDescription(),
                    pending.getTowService() != null && pending.getTowService() == 1,
                    shopId, pending.getAppointmentTime(), pending.getServiceId());
            if (bestTech != null) {
                pending.setEmployeeId(bestTech.getId());
                if (pending.getStatus() == 0) {
                    if (shop != null && shop.getAutoConfirm() != null && shop.getAutoConfirm() == 1) {
                        pending.setStatus(1);
                    }
                }
                calculateEstimatedCompleteTime(pending);
                applyWaitDiscount(pending);
                this.updateById(pending);
                log.info("自动派单成功(findBestRepairTech): orderId={}, employeeId={}", pending.getId(), bestTech.getId());
                try {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "AUTO_ASSIGNED");
                    notification.put("orderNo", pending.getOrderNo());
                    notification.put("employeeName", bestTech.getRealName() != null ? bestTech.getRealName() : bestTech.getUsername());
                    notification.put("message", "待分配订单已自动派单给" + (bestTech.getRealName() != null ? bestTech.getRealName() : bestTech.getUsername()));
                    shopWebSocketHandler.pushToShop(shopId, notification);
                } catch (Exception ignored) {}
                break;
            }
        }
    }

    private void applyWaitDiscount(Appointment a) {
        if (a.getEstimatedWaitMinutes() == null || a.getEstimatedWaitMinutes() <= 0) return;
        if (a.getCreateTime() == null) return;
        long actualWaitMin = java.time.Duration.between(a.getCreateTime(), LocalDateTime.now()).toMinutes();
        if (actualWaitMin <= a.getEstimatedWaitMinutes()) return;
        long overMin = actualWaitMin - a.getEstimatedWaitMinutes();
        double discountRate = 1.0;
        if (overMin <= 15) {
            discountRate = 0.95;
        } else if (overMin <= 30) {
            discountRate = 0.90;
        } else if (overMin <= 60) {
            discountRate = 0.85;
        } else {
            discountRate = 0.80;
        }
        a.setWaitDiscountRate(discountRate);
        if (a.getTotalAmount() != null && a.getTotalAmount() > 0) {
            double discounted = Math.round(a.getTotalAmount() * discountRate * 100.0) / 100.0;
            String existingRemark = a.getRemark() != null ? a.getRemark().trim() : "";
            StringBuilder newRemark = new StringBuilder(existingRemark);
            if (newRemark.length() > 0) newRemark.append(" | ");
            newRemark.append("等待超时").append(overMin).append("分钟，享").append(Math.round((1 - discountRate) * 100)).append("折优惠，原价¥").append(a.getTotalAmount()).append("，优惠后¥").append(discounted);
            a.setRemark(newRemark.toString());
            a.setTotalAmount(discounted);
        }
    }

    private void calculateEstimatedCompleteTime(Appointment a) {
        if (a.getEmployeeId() == null) return;
        int duration = 60;
        if (a.getServiceId() != null) {
            ServiceType sv = serviceTypeService.getById(a.getServiceId());
            if (sv != null && sv.getDuration() != null) duration = sv.getDuration();
        }
        LocalDateTime baseTime = LocalDateTime.now();
        List<Appointment> techOrders = this.list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getEmployeeId, a.getEmployeeId())
                .in(Appointment::getStatus, 1, 2)
                .ne(Appointment::getId, a.getId())
                .isNotNull(Appointment::getAppointmentTime)
                .orderByAsc(Appointment::getAppointmentTime));
        if (!techOrders.isEmpty()) {
            Appointment lastOrder = techOrders.get(techOrders.size() - 1);
            int lastDuration = 60;
            if (lastOrder.getServiceId() != null) {
                ServiceType sv = serviceTypeService.getById(lastOrder.getServiceId());
                if (sv != null && sv.getDuration() != null) lastDuration = sv.getDuration();
            }
            LocalDateTime lastEnd = lastOrder.getEstimatedCompleteTime() != null
                    ? lastOrder.getEstimatedCompleteTime()
                    : lastOrder.getAppointmentTime().plusMinutes(lastDuration);
            if (lastEnd.isAfter(baseTime)) {
                baseTime = lastEnd;
            }
        }
        a.setEstimatedCompleteTime(baseTime.plusMinutes(duration));
    }

    public List<Map<String, Object>> getTechBusyInfo(Long shopId) {
        List<User> techs = shopTechnicianService.getActiveTechniciansByShop(shopId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User tech : techs) {
            boolean isDedicatedDriver = false;
            if (tech.getSkill() != null) {
                String[] skills = tech.getSkill().split("[,，、]");
                if (skills.length == 1 && skills[0].trim().equals("司机")) isDedicatedDriver = true;
            }
            if (isDedicatedDriver) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id", tech.getId());
            item.put("name", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            item.put("avatar", tech.getAvatar());
            item.put("skill", tech.getSkill() != null ? tech.getSkill() : "");

            List<Appointment> busyOrders = this.list(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getEmployeeId, tech.getId())
                    .in(Appointment::getStatus, 1, 2)
                    .orderByAsc(Appointment::getAppointmentTime));

            item.put("busyOrderCount", busyOrders.size());
            item.put("isBusy", !busyOrders.isEmpty());

            List<Map<String, Object>> orderInfos = new ArrayList<>();
            LocalDateTime earliestCompleteTime = null;
            for (Appointment order : busyOrders) {
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("id", order.getId());
                orderInfo.put("orderNo", order.getOrderNo());
                orderInfo.put("status", order.getStatus());
                String svcName = "未知服务";
                int svcDuration = 60;
                if (order.getServiceId() != null) {
                    ServiceType sv = serviceTypeService.getById(order.getServiceId());
                    if (sv != null) {
                        svcName = sv.getServiceName();
                        if (sv.getDuration() != null) svcDuration = sv.getDuration();
                    }
                }
                orderInfo.put("serviceName", svcName);
                orderInfo.put("serviceDuration", svcDuration);
                orderInfo.put("appointmentTime", order.getAppointmentTime() != null ? order.getAppointmentTime().format(FMT) : null);

                LocalDateTime estComplete = order.getEstimatedCompleteTime();
                if (estComplete == null && order.getAppointmentTime() != null) {
                    estComplete = order.getAppointmentTime().plusMinutes(svcDuration);
                }
                orderInfo.put("estimatedCompleteTime", estComplete != null ? estComplete.format(FMT) : null);

                if (estComplete != null) {
                    long remainingMin = java.time.Duration.between(LocalDateTime.now(), estComplete).toMinutes();
                    orderInfo.put("remainingMinutes", Math.max(0, remainingMin));
                    if (earliestCompleteTime == null || estComplete.isBefore(earliestCompleteTime)) {
                        earliestCompleteTime = estComplete;
                    }
                } else {
                    orderInfo.put("remainingMinutes", null);
                }

                orderInfos.add(orderInfo);
            }
            item.put("orders", orderInfos);
            if (earliestCompleteTime != null) {
                long minToFree = java.time.Duration.between(LocalDateTime.now(), earliestCompleteTime).toMinutes();
                item.put("earliestFreeMinutes", Math.max(0, minToFree));
                item.put("earliestFreeTime", earliestCompleteTime.format(FMT));
            } else {
                item.put("earliestFreeMinutes", null);
                item.put("earliestFreeTime", null);
            }
            result.add(item);
        }
        result.sort((a, b) -> {
            boolean aBusy = (boolean) a.get("isBusy");
            boolean bBusy = (boolean) b.get("isBusy");
            if (aBusy != bBusy) return aBusy ? 1 : -1;
            Long aMin = (Long) a.get("earliestFreeMinutes");
            Long bMin = (Long) b.get("earliestFreeMinutes");
            if (aMin == null && bMin == null) return 0;
            if (aMin == null) return 1;
            if (bMin == null) return -1;
            return Long.compare(aMin, bMin);
        });
        return result;
    }
}
