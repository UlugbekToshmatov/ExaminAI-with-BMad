package com.examinai.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class UserAccountCreateDto {

    @NotBlank(message = "Username is required")
    private String username;
    private String email;
    @NotNull(message = "Role is required")
    private Role role;
    @NotBlank(message = "Password is required")
    private String password;

    /** For {@link Role#INTERN}: at least one id required (validated in service). */
    private List<Long> stackIds = new ArrayList<>();

    public UserAccountCreateDto() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<Long> getStackIds() { return stackIds; }
    public void setStackIds(List<Long> stackIds) { this.stackIds = stackIds != null ? stackIds : new ArrayList<>(); }
}
