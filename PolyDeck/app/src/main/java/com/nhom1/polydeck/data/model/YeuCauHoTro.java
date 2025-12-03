package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class YeuCauHoTro {
    
    @SerializedName("_id")
    private String id;
    
    @SerializedName("ma_nguoi_dung")
    private String maNguoiDung;
    
    @SerializedName("noi_dung")
    private String noiDung;
    
    @SerializedName("ten_nguoi_gui")
    private String tenNguoiGui;
    
    @SerializedName("email_nguoi_gui")
    private String emailNguoiGui;
    
    @SerializedName("ngay_gui")
    private Date ngayGui;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getMaNguoiDung() {
        return maNguoiDung;
    }
    
    public void setMaNguoiDung(String maNguoiDung) {
        this.maNguoiDung = maNguoiDung;
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
    
    public Date getNgayGui() {
        return ngayGui;
    }
    
    public void setNgayGui(Date ngayGui) {
        this.ngayGui = ngayGui;
    }
}





