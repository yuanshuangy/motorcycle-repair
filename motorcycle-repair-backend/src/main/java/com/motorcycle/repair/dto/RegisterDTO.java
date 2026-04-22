package com.motorcycle.repair.dto;
import lombok.Data;
@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer role;
    private String skill;
}
