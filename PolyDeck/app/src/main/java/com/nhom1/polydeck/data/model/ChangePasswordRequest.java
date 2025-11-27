package com.nhom1.polydeck.data.model;

public class ChangePasswordRequest {
    public String email;
    public String old_password;
    public String new_password;

    public ChangePasswordRequest(String email, String oldPassword, String newPassword) {
        this.email = email;
        this.old_password = oldPassword;
        this.new_password = newPassword;
    }
}

