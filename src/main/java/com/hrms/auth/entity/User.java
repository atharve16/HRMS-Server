package com.hrms.auth.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true)
    private String normalizedEmail;

    private AuthenticationIdentity authenticationIdentity;
    private UserRole role;
    private UserStatus status;

    @Indexed(unique = true, sparse = true)
    private String employeeId;

    @Indexed(unique = true, sparse = true)
    private Boolean initialAdminMarker;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant lastLoginAt;

    public static User initialAdmin(String normalizedEmail) {
        User user = new User();
        user.email = normalizedEmail;
        user.normalizedEmail = normalizedEmail;
        user.authenticationIdentity = AuthenticationIdentity.PASSWORDLESS_EMAIL_OTP;
        user.role = UserRole.ADMIN;
        user.status = UserStatus.ACTIVE;
        user.initialAdminMarker = true;
        return user;
    }

    public static User employee(String id, String normalizedEmail, String employeeId) {
        User user = new User();
        user.id = id;
        user.email = normalizedEmail;
        user.normalizedEmail = normalizedEmail;
        user.authenticationIdentity = AuthenticationIdentity.PASSWORDLESS_EMAIL_OTP;
        user.role = UserRole.EMPLOYEE;
        user.status = UserStatus.ACTIVE;
        user.employeeId = employeeId;
        return user;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public void setNormalizedEmail(String normalizedEmail) { this.normalizedEmail = normalizedEmail; }
    public AuthenticationIdentity getAuthenticationIdentity() { return authenticationIdentity; }
    public void setAuthenticationIdentity(AuthenticationIdentity authenticationIdentity) { this.authenticationIdentity = authenticationIdentity; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public Boolean getInitialAdminMarker() { return initialAdminMarker; }
    public void setInitialAdminMarker(Boolean initialAdminMarker) { this.initialAdminMarker = initialAdminMarker; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
