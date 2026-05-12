package com.motorcycle.repair.dto;
import lombok.Data;
@Data
public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private Integer role;
    private String roleName;
}
