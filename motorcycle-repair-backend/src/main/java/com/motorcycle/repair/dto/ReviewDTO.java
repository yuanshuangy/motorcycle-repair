package com.motorcycle.repair.dto;
import lombok.Data;
@Data
public class ReviewDTO {
    private Long id;
    private Long appointmentId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long shopId;
    private String shopName;
    private Long technicianId;
    private String technicianName;
    private Integer rating;
    private String content;
    private String images;
    private String createTime;
}
