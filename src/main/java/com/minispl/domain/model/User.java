package com.minispl.domain.model;

import com.minispl.domain.enums.UserRole;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String fullName;
    private UserRole role;
    private String email;
    private LocalDateTime createdAt;

    public User() {}

    public User(int id, String fullName, UserRole role, String email, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.createdAt = createdAt;
    }

    public User(String fullName, UserRole role, String email) {
        this.fullName = fullName;
        this.role = role;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
