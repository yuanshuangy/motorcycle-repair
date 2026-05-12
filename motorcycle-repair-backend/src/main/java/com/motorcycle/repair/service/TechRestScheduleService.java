package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.ShopTechnician;
import com.motorcycle.repair.entity.TechRestSchedule;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.mapper.TechRestScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class TechRestScheduleService extends ServiceImpl<TechRestScheduleMapper, TechRestSchedule> {

    @Autowired private ShopTechnicianService shopTechnicianService;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    public List<TechRestSchedule> getSchedule(Long shopId, LocalDate startDate, LocalDate endDate) {
        return this.list(new LambdaQueryWrapper<TechRestSchedule>()
                .eq(TechRestSchedule::getShopId, shopId)
                .ge(TechRestSchedule::getRestDate, startDate)
                .le(TechRestSchedule::getRestDate, endDate)
                .eq(TechRestSchedule::getStatus, 1));
    }

    public List<TechRestSchedule> autoSchedule(Long shopId) {
        List<User> techs = shopTechnicianService.getTechniciansByShop(shopId);
        if (techs.isEmpty()) return Collections.emptyList();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextWeekEnd = weekStart.plusDays(13);

        this.remove(new LambdaQueryWrapper<TechRestSchedule>()
                .eq(TechRestSchedule::getShopId, shopId)
                .ge(TechRestSchedule::getRestDate, weekStart)
                .le(TechRestSchedule::getRestDate, nextWeekEnd));

        Map<String, Set<Long>> skillToTechs = buildSkillMap(techs);

        List<Long> techIds = new ArrayList<>();
        for (User tech : techs) techIds.add(tech.getId());

        List<LocalDate> week1Dates = new ArrayList<>();
        List<LocalDate> week2Dates = new ArrayList<>();
        for (LocalDate d = weekStart; d.isBefore(weekStart.plusDays(7)); d = d.plusDays(1)) {
            if (!d.isBefore(today)) week1Dates.add(d);
        }
        for (LocalDate d = weekStart.plusDays(7); !d.isAfter(nextWeekEnd); d = d.plusDays(1)) {
            week2Dates.add(d);
        }

        Map<Long, Integer> week1RestCount = new HashMap<>();
        Map<Long, Integer> week2RestCount = new HashMap<>();
        Map<Long, Set<LocalDate>> techRestDays = new HashMap<>();
        for (Long id : techIds) {
            week1RestCount.put(id, 0);
            week2RestCount.put(id, 0);
            techRestDays.put(id, new LinkedHashSet<>());
        }

        Map<LocalDate, Set<Long>> dayRestTechs = new LinkedHashMap<>();
        for (LocalDate d : week1Dates) dayRestTechs.put(d, new HashSet<>());
        for (LocalDate d : week2Dates) dayRestTechs.put(d, new HashSet<>());

        boolean week1Ok = scheduleWeek(techIds, week1Dates, week1RestCount, techRestDays, dayRestTechs, skillToTechs, 2);
        boolean week2Ok = scheduleWeek(techIds, week2Dates, week2RestCount, techRestDays, dayRestTechs, skillToTechs, 2);

        if (!week1Ok || !week2Ok) {
            int added = addTechniciansIfNeeded(shopId, techs, techIds, skillToTechs,
                    week1RestCount, week2RestCount, techRestDays, dayRestTechs,
                    week1Dates, week2Dates);
            if (added > 0) {
                this.remove(new LambdaQueryWrapper<TechRestSchedule>()
                        .eq(TechRestSchedule::getShopId, shopId)
                        .ge(TechRestSchedule::getRestDate, weekStart)
                        .le(TechRestSchedule::getRestDate, nextWeekEnd));
                dayRestTechs.clear();
                for (LocalDate d : week1Dates) dayRestTechs.put(d, new HashSet<>());
                for (LocalDate d : week2Dates) dayRestTechs.put(d, new HashSet<>());
                for (Long id : techIds) {
                    week1RestCount.put(id, 0);
                    week2RestCount.put(id, 0);
                    techRestDays.get(id).clear();
                }
                scheduleWeek(techIds, week1Dates, week1RestCount, techRestDays, dayRestTechs, skillToTechs, 2);
                scheduleWeek(techIds, week2Dates, week2RestCount, techRestDays, dayRestTechs, skillToTechs, 2);
            }
        }

        for (Map.Entry<LocalDate, Set<Long>> entry : dayRestTechs.entrySet()) {
            for (Long techId : entry.getValue()) {
                TechRestSchedule schedule = new TechRestSchedule();
                schedule.setShopId(shopId);
                schedule.setUserId(techId);
                schedule.setRestDate(entry.getKey());
                schedule.setStatus(1);
                this.save(schedule);
            }
        }

        syncRestStatus(shopId, today);

        return this.list(new LambdaQueryWrapper<TechRestSchedule>()
                .eq(TechRestSchedule::getShopId, shopId)
                .ge(TechRestSchedule::getRestDate, today)
                .le(TechRestSchedule::getRestDate, nextWeekEnd)
                .eq(TechRestSchedule::getStatus, 1));
    }

    private Map<String, Set<Long>> buildSkillMap(List<User> techs) {
        Map<String, Set<Long>> skillToTechs = new HashMap<>();
        for (User tech : techs) {
            String skill = tech.getSkill() != null ? tech.getSkill() : "";
            for (String s : skill.split("[,，、]")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty() && !trimmed.equals("司机")) {
                    skillToTechs.computeIfAbsent(trimmed, k -> new HashSet<>()).add(tech.getId());
                }
            }
        }
        return skillToTechs;
    }

    private boolean scheduleWeek(List<Long> techIds, List<LocalDate> dates,
                                  Map<Long, Integer> weekRestCount,
                                  Map<Long, Set<LocalDate>> techRestDays,
                                  Map<LocalDate, Set<Long>> dayRestTechs,
                                  Map<String, Set<Long>> skillToTechs,
                                  int maxRestPerWeek) {
        int totalTechs = techIds.size();
        boolean allScheduled = true;
        int totalRestNeeded = totalTechs * maxRestPerWeek;
        int totalRestSlots = dates.size();
        int baseRestPerDay = totalRestSlots > 0 ? totalRestNeeded / totalRestSlots : 0;
        int extraDays = totalRestSlots > 0 ? totalRestNeeded % totalRestSlots : 0;

        for (int di = 0; di < dates.size(); di++) {
            LocalDate date = dates.get(di);
            Set<Long> resting = dayRestTechs.get(date);
            int targetRest = baseRestPerDay + (di < extraDays ? 1 : 0);
            targetRest = Math.min(targetRest, totalTechs - 2);
            if (targetRest < 0) targetRest = 0;

            List<Long> candidates = new ArrayList<>(techIds);
            candidates.sort((a, b) -> {
                int ra = weekRestCount.getOrDefault(a, 0);
                int rb = weekRestCount.getOrDefault(b, 0);
                if (ra != rb) return ra - rb;
                return a.compareTo(b);
            });

            int restedThisDay = 0;
            for (Long techId : candidates) {
                if (restedThisDay >= targetRest) break;
                if (weekRestCount.getOrDefault(techId, 0) >= maxRestPerWeek) continue;
                if (resting.contains(techId)) continue;

                Set<LocalDate> myRest = techRestDays.get(techId);
                if (myRest.contains(date.minusDays(1)) || myRest.contains(date.plusDays(1))) continue;

                Set<Long> wouldBeActive = new HashSet<>(techIds);
                wouldBeActive.removeAll(resting);
                wouldBeActive.remove(techId);

                boolean skillOk = true;
                for (Map.Entry<String, Set<Long>> entry : skillToTechs.entrySet()) {
                    Set<Long> canDo = new HashSet<>(entry.getValue());
                    canDo.retainAll(wouldBeActive);
                    int minRequired = canDo.size() >= 3 ? 2 : 1;
                    if (canDo.size() < minRequired) { skillOk = false; break; }
                }
                if (!skillOk) { allScheduled = false; continue; }

                long activeCount = totalTechs - resting.size() - 1;
                if (activeCount < 2) { allScheduled = false; continue; }

                resting.add(techId);
                weekRestCount.put(techId, weekRestCount.get(techId) + 1);
                myRest.add(date);
                restedThisDay++;
            }
        }
        return allScheduled;
    }

    private int addTechniciansIfNeeded(Long shopId, List<User> techs, List<Long> techIds,
                                        Map<String, Set<Long>> skillToTechs,
                                        Map<Long, Integer> week1RestCount,
                                        Map<Long, Integer> week2RestCount,
                                        Map<Long, Set<LocalDate>> techRestDays,
                                        Map<LocalDate, Set<Long>> dayRestTechs,
                                        List<LocalDate> week1Dates,
                                        List<LocalDate> week2Dates) {
        if (techIds.size() >= 8) return 0;
        int needed = 0;
        for (Map.Entry<String, Set<Long>> entry : skillToTechs.entrySet()) {
            if (entry.getValue().size() < 3) {
                needed = Math.max(needed, 3 - entry.getValue().size());
            }
        }
        if (techIds.size() + needed < 4) {
            needed = Math.max(needed, 4 - techIds.size());
        }
        needed = Math.min(needed, 3);
        needed = Math.min(needed, 8 - techIds.size());
        if (needed <= 0) return 0;

        String encoded = passwordEncoder.encode("admin123");
        int added = 0;
        for (int i = 0; i < needed; i++) {
            int nextNum = getNextTechNumber();
            String username = "zhang" + String.format("%03d", nextNum);
            User existing = userService.findByUsernameIncludeDeleted(username);
            if (existing != null) {
                if (existing.getDeleted() != null && existing.getDeleted() == 1) {
                    userService.getBaseMapper().recoverDeletedUser(
                            username, encoded,
                            "新技师" + nextNum,
                            "1380000" + String.format("%05d", nextNum + 1000),
                            username + "@moto.com", 4, 1, 1,
                            "司机,常规保养,轮胎更换,刹车检修");
                    existing = userService.findByUsernameIncludeDeleted(username);
                } else {
                    continue;
                }
            } else {
                existing = new User();
                existing.setUsername(username);
                existing.setPassword(encoded);
                existing.setRealName("新技师" + nextNum);
                existing.setPhone("1380000" + String.format("%05d", nextNum + 1000));
                existing.setEmail(username + "@moto.com");
                existing.setRole(4);
                existing.setStatus(1);
                existing.setVerified(1);
                existing.setSkill("司机,常规保养,轮胎更换,刹车检修");
                userService.save(existing);
            }

            shopTechnicianService.bindTechnician(shopId, existing.getId());

            techIds.add(existing.getId());
            techs.add(existing);
            week1RestCount.put(existing.getId(), 0);
            week2RestCount.put(existing.getId(), 0);
            techRestDays.put(existing.getId(), new LinkedHashSet<>());
            for (String s : existing.getSkill().split("[,，、]")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty() && !trimmed.equals("司机")) {
                    skillToTechs.computeIfAbsent(trimmed, k -> new HashSet<>()).add(existing.getId());
                }
            }
            added++;
        }
        return added;
    }

    private int getNextTechNumber() {
        int maxNum = 8;
        List<User> allTechs = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 4).likeRight(User::getUsername, "zhang"));
        for (User u : allTechs) {
            String name = u.getUsername();
            if (name.startsWith("zhang")) {
                try {
                    int num = Integer.parseInt(name.substring(5));
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        for (int i = 9; i <= maxNum + 1; i++) {
            String username = "zhang" + String.format("%03d", i);
            User existing = userService.findByUsernameIncludeDeleted(username);
            if (existing == null) return i;
        }
        return maxNum + 1;
    }

    public void clearScheduleAndResetStatus(Long shopId) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeekEnd = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(13);
        this.remove(new LambdaQueryWrapper<TechRestSchedule>()
                .eq(TechRestSchedule::getShopId, shopId)
                .ge(TechRestSchedule::getRestDate, today)
                .le(TechRestSchedule::getRestDate, nextWeekEnd));
        List<ShopTechnician> links = shopTechnicianService.list(
                new LambdaQueryWrapper<ShopTechnician>().eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getStatus, 1));
        for (ShopTechnician link : links) {
            if (link.getRestStatus() != null && link.getRestStatus() == 1) {
                link.setRestStatus(0);
                shopTechnicianService.updateById(link);
            }
        }
    }

    public void syncRestStatus(Long shopId, LocalDate date) {
        List<ShopTechnician> links = shopTechnicianService.list(
                new LambdaQueryWrapper<ShopTechnician>().eq(ShopTechnician::getShopId, shopId).eq(ShopTechnician::getStatus, 1));
        List<TechRestSchedule> todaySchedule = this.list(new LambdaQueryWrapper<TechRestSchedule>()
                .eq(TechRestSchedule::getShopId, shopId).eq(TechRestSchedule::getRestDate, date).eq(TechRestSchedule::getStatus, 1));
        Set<Long> restingIds = new HashSet<>();
        for (TechRestSchedule s : todaySchedule) restingIds.add(s.getUserId());
        for (ShopTechnician link : links) {
            int shouldRest = restingIds.contains(link.getUserId()) ? 1 : 0;
            if (link.getRestStatus() == null || link.getRestStatus() != shouldRest) {
                link.setRestStatus(shouldRest);
                shopTechnicianService.updateById(link);
            }
        }
    }

    public boolean isTechResting(Long shopId, Long userId, LocalDate date) {
        return this.count(new LambdaQueryWrapper<TechRestSchedule>()
                .eq(TechRestSchedule::getShopId, shopId)
                .eq(TechRestSchedule::getUserId, userId)
                .eq(TechRestSchedule::getRestDate, date)
                .eq(TechRestSchedule::getStatus, 1)) > 0;
    }

    public List<Map<String, Object>> getTomorrowRestTechs(Long shopId) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<TechRestSchedule> schedules = this.list(new LambdaQueryWrapper<TechRestSchedule>()
                .eq(TechRestSchedule::getShopId, shopId)
                .eq(TechRestSchedule::getRestDate, tomorrow)
                .eq(TechRestSchedule::getStatus, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TechRestSchedule s : schedules) {
            User tech = userService.getById(s.getUserId());
            if (tech != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", tech.getId());
                m.put("name", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
                m.put("skill", tech.getSkill());
                result.add(m);
            }
        }
        return result;
    }
}
