package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class YeuCauHoTro {
    
    @SerializedName("_id")
    private String id;
    
    @SerializedName("ma_nguoi_dung")
    private String maNguoiDung;
    
    public String getMaNguoiDung() {
        return maNguoiDung;
    }
    
    public void setMaNguoiDung(String maNguoiDung) {
        this.maNguoiDung = maNguoiDung;
    }
    
    @SerializedName("noi_dung")
    private String noiDung;
    
    @SerializedName("ten_nguoi_gui")
    private String tenNguoiGui;
    
    @SerializedName("email_nguoi_gui")
    private String emailNguoiGui;
    
    @SerializedName("ngay_gui")
    private String ngayGui; // Changed to String to handle ISO date strings
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    
    public String getNoiDung() {
        return noiDung;
    }
    
    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }
    
    public String getTenNguoiGui() {
        return tenNguoiGui;
    }
    
    public void setTenNguoiGui(String tenNguoiGui) {
        this.tenNguoiGui = tenNguoiGui;
    }
    
    public String getEmailNguoiGui() {
        return emailNguoiGui;
    }
    
    public void setEmailNguoiGui(String emailNguoiGui) {
        this.emailNguoiGui = emailNguoiGui;
    }
    
    public String getNgayGui() {
        return ngayGui;
    }
    
    public void setNgayGui(String ngayGui) {
        this.ngayGui = ngayGui;
    }
    
    // Helper method to get ngay_gui as Date
    public Date getNgayGuiAsDate() {
        if (ngayGui == null || ngayGui.isEmpty()) return null;
        try {
            // Try ISO format first
            java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return isoFormat.parse(ngayGui);
        } catch (Exception e) {
            try {
                // Try simple date format
                java.text.SimpleDateFormat simpleFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                return simpleFormat.parse(ngayGui);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}























