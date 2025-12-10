package com.nhom1.polydeck.data.model;
import com.google.gson.annotations.SerializedName;

public class AdminStats {
    // Support both camelCase and snake_case for backward compatibility
    @SerializedName(value = "tong_nguoi_dung", alternate = {"tongNguoiDung"})
    private int tongNguoiDung;

    @SerializedName(value = "tong_bo_tu", alternate = {"tongBoTu"})
    private int tongBoTu;

    @SerializedName(value = "nguoi_hoat_dong", alternate = {"nguoiHoatDong"})
    private int nguoiHoatDong;

    @SerializedName(value = "tong_tu_vung", alternate = {"tongTuVung"})
    private int tongTuVung;

    // FIX: Added back the missing fields and methods
    // Support both camelCase and snake_case for backward compatibility
    @SerializedName(value = "ty_le_nguoi_dung", alternate = {"tyLeNguoiDung"})
    private String tyLeNguoiDung;

    @SerializedName(value = "ty_le_bo_tu", alternate = {"tyLeBoTu"})
    private String tyLeBoTu;

    @SerializedName(value = "ty_le_hoat_dong", alternate = {"tyLeHoatDong"})
    private String tyLeHoatDong;
    
    @SerializedName(value = "ty_le_tu_vung", alternate = {"tyLeTuVung"})
    private String tyLeTuVung;

    // Getters and Setters
    public int getTongNguoiDung() { return tongNguoiDung; }
    public void setTongNguoiDung(int tongNguoiDung) { this.tongNguoiDung = tongNguoiDung; }

    public int getTongBoTu() { return tongBoTu; }
    public void setTongBoTu(int tongBoTu) { this.tongBoTu = tongBoTu; }

    public int getNguoiHoatDong() { return nguoiHoatDong; }
    public void setNguoiHoatDong(int nguoiHoatDong) { this.nguoiHoatDong = nguoiHoatDong; }

    public int getTongTuVung() { return tongTuVung; }
    public void setTongTuVung(int tongTuVung) { this.tongTuVung = tongTuVung; }

    public String getTyLeNguoiDung() { return tyLeNguoiDung; }
    public void setTyLeNguoiDung(String tyLeNguoiDung) { this.tyLeNguoiDung = tyLeNguoiDung; }

    public String getTyLeBoTu() { return tyLeBoTu; }
    public void setTyLeBoTu(String tyLeBoTu) { this.tyLeBoTu = tyLeBoTu; }

    public String getTyLeHoatDong() { return tyLeHoatDong; }
    public void setTyLeHoatDong(String tyLeHoatDong) { this.tyLeHoatDong = tyLeHoatDong; }

    public String getTyLeTuVung() { return tyLeTuVung; }
    public void setTyLeTuVung(String tyLeTuVung) { this.tyLeTuVung = tyLeTuVung; }
}