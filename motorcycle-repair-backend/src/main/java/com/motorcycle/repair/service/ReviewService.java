package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.dto.ReviewDTO;
import com.motorcycle.repair.entity.RepairShop;
import com.motorcycle.repair.entity.Review;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService extends ServiceImpl<ReviewMapper, Review> {
    @Autowired private RepairShopService repairShopService;
    @Autowired private UserService userService;
    @Lazy @Autowired private AppointmentService appointmentService;
    @Autowired private MessageService messageService;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Page<ReviewDTO> getReviewPage(Integer pageNum, Integer pageSize, Long shopId, Long userId) {
        Page<Review> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<>();
        if (shopId != null) w.eq(Review::getShopId, shopId);
        if (userId != null) w.eq(Review::getUserId, userId);
        w.orderByDesc(Review::getCreateTime);
        Page<Review> result = this.page(page, w);
        Page<ReviewDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    private ReviewDTO toDTO(Review r) {
        ReviewDTO d = new ReviewDTO();
        d.setId(r.getId()); d.setAppointmentId(r.getAppointmentId());
        d.setUserId(r.getUserId()); d.setShopId(r.getShopId());
        d.setTechnicianId(r.getTechnicianId());
        d.setRating(r.getRating()); d.setContent(r.getContent()); d.setImages(r.getImages());
        d.setCreateTime(r.getCreateTime() != null ? r.getCreateTime().format(FMT) : null);
        try {
            User u = userService.getById(r.getUserId());
            if (u != null) { d.setUserName(u.getRealName()); d.setUserAvatar(u.getAvatar()); }
            RepairShop s = repairShopService.getById(r.getShopId());
            if (s != null) d.setShopName(s.getShopName());
            if (r.getTechnicianId() != null) {
                User tech = userService.getById(r.getTechnicianId());
                if (tech != null) d.setTechnicianName(tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            }
        } catch (Exception ignored) {}
        return d;
    }

    @Transactional
    public void addReview(Review review) {
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }
        if (review.getAppointmentId() != null) {
            long dupCount = this.count(new LambdaQueryWrapper<Review>()
                    .eq(Review::getAppointmentId, review.getAppointmentId())
                    .eq(Review::getUserId, review.getUserId()));
            if (dupCount > 0) throw new RuntimeException("您已评价过该预约");
        }
        if (review.getShopId() == null && review.getAppointmentId() != null) {
            com.motorcycle.repair.entity.Appointment apt = appointmentService.getById(review.getAppointmentId());
            if (apt != null) {
                review.setShopId(apt.getShopId());
                if (review.getTechnicianId() == null && apt.getEmployeeId() != null) {
                    review.setTechnicianId(apt.getEmployeeId());
                }
            }
        }
        this.save(review);
        if (review.getTechnicianId() != null && review.getRating() != null && review.getRating() >= 4) {
            try {
                User tech = userService.getById(review.getTechnicianId());
                User user = userService.getById(review.getUserId());
                String techName = tech != null ? (tech.getRealName() != null ? tech.getRealName() : tech.getUsername()) : "技师";
                String userName = user != null ? (user.getRealName() != null ? user.getRealName() : user.getUsername()) : "客户";
                String title = "👍 收到客户好评！";
                String content = String.format("客户【%s】对您的服务给出了%d星好评！继续保持优质服务，赢得更多客户信赖！", userName, review.getRating());
                if (review.getContent() != null && !review.getContent().isEmpty()) {
                    content += "\n客户评价：「" + review.getContent() + "」";
                }
                messageService.sendMessage(review.getTechnicianId(), review.getUserId(), 3, title, content, 4);
            } catch (Exception ignored) {}
        }
        if (review.getShopId() != null) {
            List<Review> reviews = this.list(new LambdaQueryWrapper<Review>().eq(Review::getShopId, review.getShopId()));
            if (!reviews.isEmpty()) {
                double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                repairShopService.updateRating(review.getShopId(), avg);
            }
        }
    }
}
