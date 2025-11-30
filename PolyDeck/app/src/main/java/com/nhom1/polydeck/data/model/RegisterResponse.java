package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    @SerializedName("_id")
    private String id;

    @SerializedName("ho_ten")
    private String hoTen;

    @SerializedName("email")
    private String email;

    @SerializedName("cap_do")
    private int capDo;

    @SerializedName("diem_tich_luy")
    private int diemTichLuy;

    @SerializedName("chuoi_ngay_hoc")
    private int chuoiNgayHoc;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCapDo() {
        return capDo;
    }

    public void setCapDo(int capDo) {
        this.capDo = capDo;
    }

    public int getDiemTichLuy() {
        return diemTichLuy;
    }

    public void setDiemTichLuy(int diemTichLuy) {
        this.diemTichLuy = diemTichLuy;
    }

    public int getChuoiNgayHoc() {
        return chuoiNgayHoc;
    }

    public void setChuoiNgayHoc(int chuoiNgayHoc) {
        this.chuoiNgayHoc = chuoiNgayHoc;
    }
}

