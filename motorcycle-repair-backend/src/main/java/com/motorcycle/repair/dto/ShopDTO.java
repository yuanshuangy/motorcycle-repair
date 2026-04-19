package com.motorcycle.repair.dto;
import lombok.Data;
@Data
public class ShopDTO {
    private Long id;
    private Long userId;
    private String shopName;
    private String address;
    private String phone;
    private String description;
    private String businessHours;
    private String coverImage;
    private Double rating;
    private Integer status;
    private Integer auditStatus;
    private String auditRemark;
    private Integer serviceCount;
    private Integer employeeCount;
}
