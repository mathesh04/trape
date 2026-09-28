package com.trape.backend.auth.entity;

import com.trape.backend.common.entity.Auditable;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private String role = "CUSTOMER"; // CUSTOMER | ADMIN | SUPER_ADMIN

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected User() {
    }

    public User(String email, String phone, String passwordHash, String fullName, String role) {
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}
