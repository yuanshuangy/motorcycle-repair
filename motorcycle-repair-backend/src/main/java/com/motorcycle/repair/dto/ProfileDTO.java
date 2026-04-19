package com.motorcycle.repair.dto;
import lombok.Data;
@Data
public class ProfileDTO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String idCard;
    private String avatar;
    private Integer role;
    private String roleName;
    private Integer verified;
}
